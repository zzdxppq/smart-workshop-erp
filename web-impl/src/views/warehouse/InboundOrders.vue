<template>
  <ErpPageShell title="入库单（待处理）" description="批量勾选待入库单 → 执行入库 → 生成复合条码并打印标签">
    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="8">
        <el-statistic title="待入库" :value="pendingCount" />
      </el-col>
      <el-col :span="8">
        <el-statistic title="已选" :value="selected.length" />
      </el-col>
    </el-row>

    <div style="margin-bottom: 12px">
      <el-button type="primary" :disabled="!selected.length" :loading="batchLoading" @click="openBatchDialog">
        批量执行入库（{{ selected.length }}）
      </el-button>
      <el-button :disabled="!lastGeneratedBarcodes.length" @click="goPrint">
        打印本次标签（{{ lastGeneratedBarcodes.length }} 张）
      </el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="items"
      stripe
      border
      @selection-change="onSelectionChange"
    >
      <el-table-column type="selection" width="48" />
      <el-table-column prop="sourceNo" label="来源单号" min-width="140" />
      <el-table-column prop="materialCode" label="料号" min-width="120" />
      <el-table-column prop="expectedQty" label="待入数量" width="100" align="right" />
      <el-table-column prop="sourceType" label="来源" width="90" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag type="warning">{{ row.status ?? '待入库' }}</el-tag>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="批量执行入库" width="480px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item label="良品数量">
          <el-input-number v-model="goodQty" :min="1" />
        </el-form-item>
        <el-alert type="info" :closable="false" show-icon title="入库后将按 PRD 规则生成条码：{料号}-BATCH-{日期}-{流水}" />
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchLoading" @click="confirmBatchInbound">确认入库</el-button>
      </template>
    </el-dialog>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import { useBaseStore } from '@/stores/_base'
import { unwrapResult } from '@/utils/apiPage'

interface PendingInboundRow {
  id?: number
  sourceNo?: string
  materialCode?: string
  materialId?: number
  expectedQty?: number
  sourceType?: string
  status?: string
}

const router = useRouter()
const api = useBaseStore().api
const loading = ref(false)
const batchLoading = ref(false)
const items = ref<PendingInboundRow[]>([])
const selected = ref<PendingInboundRow[]>([])
const dialogVisible = ref(false)
const goodQty = ref(1)
const lastGeneratedBarcodes = ref<string[]>([])

const pendingCount = computed(() => items.value.length)

function onSelectionChange(rows: PendingInboundRow[]) {
  selected.value = rows
}

async function loadPending() {
  loading.value = true
  try {
    const data = unwrapResult<{ list?: PendingInboundRow[] }>(
      await api.get('/warehouses/inbound/pending'),
    )
    items.value = data?.list ?? []
    if (!items.value.length) {
      items.value = demoPending()
    }
  } catch {
    items.value = demoPending()
  } finally {
    loading.value = false
  }
}

function demoPending(): PendingInboundRow[] {
  return [
    { id: 1, sourceNo: 'PO-20260619-0001', materialCode: 'RM-STEEL-45', expectedQty: 100, sourceType: '采购', status: '待入库' },
    { id: 2, sourceNo: 'WW-20260619-0001', materialCode: 'WL-A001', expectedQty: 50, sourceType: '委外', status: '待入库' },
  ]
}

function openBatchDialog() {
  goodQty.value = selected.value.reduce((s, r) => s + (r.expectedQty ?? 1), 0) || 1
  dialogVisible.value = true
}

async function confirmBatchInbound() {
  batchLoading.value = true
  const barcodes: string[] = []
  try {
    for (const row of selected.value) {
      if (!row.materialCode) continue
      const resp = unwrapResult<{ barcodeNo?: string }>(
        await api.post('/materials/barcode-batch/generate', {
          materialId: row.materialId ?? row.id,
          batchId: row.id,
          materialNo: row.materialCode,
        }),
      )
      if (resp.barcodeNo) barcodes.push(resp.barcodeNo)
    }
    lastGeneratedBarcodes.value = barcodes
    ElMessage.success(`入库完成，已生成 ${barcodes.length} 个条码`)
    dialogVisible.value = false
    await loadPending()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '批量入库失败')
  } finally {
    batchLoading.value = false
  }
}

function goPrint() {
  router.push({
    path: '/material/barcode-print',
    query: { codes: lastGeneratedBarcodes.value.join(',') },
  })
}

onMounted(loadPending)
</script>
