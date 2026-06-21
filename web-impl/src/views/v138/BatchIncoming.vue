<template>
  <ErpPageShell
    title="分批到货"
    description="按采购订单登记物料批次到货，系统自动更新 PO 状态并触发来料检验。"
  >
    <el-form :model="form" label-width="100px" label-position="right">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="采购单">
            <PoSelect v-model="form.poId" placeholder="请选择采购单" @change="onPoChange" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="到货时间">
            <el-date-picker
              v-model="form.arrivedAt"
              type="datetime"
              placeholder="选择到货时间"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-divider content-position="left">到货明细</el-divider>

      <div class="toolbar">
        <el-button type="primary" plain @click="addItem">添加物料</el-button>
        <el-button v-if="form.poId" :loading="loadingPo" @click="loadFromPo">从采购单加载行项</el-button>
      </div>

      <el-table :data="form.items" border stripe style="margin-top: 12px">
        <el-table-column label="料号" min-width="260">
          <template #default="{ row }">
            <MaterialSelect v-model="row.materialId" @change="(m) => onMaterialPick(row as BatchItem, m)" />
          </template>
        </el-table-column>
        <el-table-column label="本批数量" width="140">
          <template #default="{ row }">
            <el-input-number v-model="row.quantity" :min="1" size="small" controls-position="right" />
          </template>
        </el-table-column>
        <el-table-column label="PO 订购/已到货" width="140">
          <template #default="{ row }">
            <span v-if="row.ordered != null">{{ row.arrived ?? 0 }} / {{ row.ordered }}</span>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ $index }">
            <el-button link type="danger" @click="form.items.splice($index, 1)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="form-actions">
        <el-button type="primary" :loading="submitting" @click="submit">提交批次</el-button>
      </div>
    </el-form>

    <el-dialog v-model="resultDialog.visible" title="批次创建结果" width="640px">
      <div v-if="resultDialog.data">
        <p>PO 状态：<el-tag type="warning">{{ resultDialog.data.poStatusAfter }}</el-tag></p>
        <p class="result-label">生成批次</p>
        <el-table :data="resultDialog.data.batches" border size="small">
          <el-table-column prop="batchNo" label="批次号" />
          <el-table-column prop="materialId" label="物料 ID" />
          <el-table-column prop="quantity" label="数量" />
        </el-table>
        <p v-if="resultDialog.data.qualityOrders?.length" class="result-hint">
          已触发来料检：{{ resultDialog.data.qualityOrders.join('、') }}
        </p>
      </div>
    </el-dialog>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import PoSelect from '@/components/form/PoSelect.vue'
import MaterialSelect from '@/components/form/MaterialSelect.vue'
import { useBaseStore } from '@/stores/_base'
import { useSourcingStore } from '@/stores/sourcing'
import { useMasterData, type MaterialOption } from '@/composables/useMasterData'
import { unwrapResult } from '@/utils/apiPage'
import type { BatchCreateResponse } from '@/api/generated/models/BatchCreateResponse'

interface BatchItem {
  materialId?: number
  quantity: number
  poItemId?: number
  ordered?: number
  arrived?: number
}

const submitting = ref(false)
const loadingPo = ref(false)
const { loadMaterials, resolveMaterialId } = useMasterData()
const form = ref({
  poId: undefined as number | undefined,
  arrivedAt: new Date(),
  items: [] as BatchItem[],
})

function addItem() {
  form.value.items.push({ quantity: 1 })
}

function onMaterialPick(row: BatchItem, material?: MaterialOption) {
  if (material) row.materialId = material.id
}

function onPoChange() {
  form.value.items = []
}

async function loadFromPo() {
  if (!form.value.poId) return
  loadingPo.value = true
  try {
    const materials = await loadMaterials()
    const detail = unwrapResult<{ items?: Array<Record<string, unknown>> }>(
      await useSourcingStore().getPo(form.value.poId),
    )
    const status = unwrapResult<{ items?: Array<{ materialId?: number; ordered?: number; arrived?: number }> }>(
      await useBaseStore().api.get(`/incoming/po-status/${form.value.poId}`),
    )
    const progressMap = new Map<number, { ordered?: number; arrived?: number }>()
    for (const s of status.items ?? []) {
      if (s.materialId != null) progressMap.set(s.materialId, s)
    }
    form.value.items = (detail.items ?? []).map((line) => {
      const materialId = (line.materialId as number | undefined)
        ?? resolveMaterialId(line.materialCode as string, materials)
      const prog = materialId != null ? progressMap.get(materialId) : undefined
      const ordered = (line.qty as number | undefined) ?? prog?.ordered
      const arrived = prog?.arrived ?? 0
      const remain = ordered != null ? Math.max(ordered - arrived, 1) : 1
      return {
        materialId,
        poItemId: line.poItemId as number | undefined,
        quantity: remain,
        ordered,
        arrived,
      }
    })
    if (!form.value.items.length) {
      ElMessage.warning('该采购单暂无行项')
    }
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载采购单失败')
  } finally {
    loadingPo.value = false
  }
}

const resultDialog = ref({
  visible: false,
  data: null as BatchCreateResponse | null,
})

async function submit() {
  if (!form.value.poId) {
    ElMessage.warning('请选择采购单')
    return
  }
  if (!form.value.items.length || form.value.items.some((it) => !it.materialId)) {
    ElMessage.warning('请完善到货明细并选择物料')
    return
  }
  submitting.value = true
  try {
    resultDialog.value.data = unwrapResult<BatchCreateResponse>(
      await useBaseStore().api.post('/incoming/batch-create', {
        poId: form.value.poId,
        arrivedAt: form.value.arrivedAt instanceof Date
          ? form.value.arrivedAt.toISOString()
          : form.value.arrivedAt,
        items: form.value.items.map(({ materialId, quantity, poItemId }) => ({
          materialId,
          quantity,
          poItemId,
        })),
      }),
    )
    resultDialog.value.visible = true
    ElMessage.success('批次创建成功')
  } catch (e: unknown) {
    ElMessage.error((e as Error).message || '提交失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 4px;
}

.form-actions {
  margin-top: 20px;
}

.result-label {
  margin: 12px 0 8px;
  font-size: 13px;
  color: var(--erp-text-secondary);
}

.result-hint {
  margin-top: 12px;
  font-size: 13px;
  color: var(--erp-text-secondary);
}
</style>
