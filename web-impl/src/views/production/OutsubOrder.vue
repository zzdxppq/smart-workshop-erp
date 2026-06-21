<template>
  <ErpPageShell title="委外转单" description="采购视图：待委外工序直接下单，或经询比价定标后一键转单；禁止修改工序归属。">
    <el-alert type="warning" :closable="false" title="V1.3.7 红线：仅选厂商下单，不可修改生管工序归属决策" />

    <el-table
      v-loading="loading"
      :data="pending"
      stripe
      border
      row-key="id"
      class="outsub-table"
      style="margin-top: 16px"
      @expand-change="onExpandChange"
    >
      <el-table-column type="expand" width="48">
        <template #default="{ row }">
          <div class="expand-panel">
            <el-descriptions :column="3" border size="small" style="margin-bottom: 12px">
              <el-descriptions-item label="工单号">{{ row.workorderNo ?? '—' }}</el-descriptions-item>
              <el-descriptions-item label="产品编码">{{ row.productCode ?? '—' }}</el-descriptions-item>
              <el-descriptions-item label="工序">{{ row.processName ?? '—' }} (#{{ row.processSeq }})</el-descriptions-item>
            </el-descriptions>
            <h4 class="expand-title">加工图纸（Epic 3 · 下单前确认）</h4>
            <DrawingPicker
              v-model="row._drawingNo"
              :allow-upload="false"
              style="max-width: 480px"
              @select="(d) => onDrawingSelect(row as PendingRow, d)"
            />
            <p v-if="row._materialHint" class="material-hint">
              关联物料：{{ row._materialHint }}
              <DrawingNoCell
                v-if="row._drawingNo"
                :drawing-no="row._drawingNo"
                :material-code="row.productCode"
                style="margin-left: 8px"
              />
            </p>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="workorderNo" label="工单号" width="140" />
      <el-table-column label="图号/产品" min-width="180">
        <template #default="{ row }">
          <DrawingNoCell
            v-if="row._drawingNo || row.productCode"
            :drawing-no="row._drawingNo"
            :material-code="row.productCode"
          />
          <span v-else class="muted">展开确认图纸</span>
        </template>
      </el-table-column>
      <el-table-column prop="processName" label="工序" min-width="120" />
      <el-table-column prop="processSeq" label="序号" width="70" />
      <el-table-column label="归属" width="90">
        <template #default>
          <el-tag type="warning">委外</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="厂商" min-width="200">
        <template #default="{ row }">
          <VendorSelect
            v-model="row._vendorId"
            placeholder="请选择厂商"
            @change="(v) => onVendorChange(row as PendingRow, v)"
          />
        </template>
      </el-table-column>
      <el-table-column label="历史价" width="100">
        <template #default="{ row }">
          <span v-if="row._historyPrice != null" class="history-price">¥{{ row._historyPrice }}</span>
          <span v-else class="muted">—</span>
        </template>
      </el-table-column>
      <el-table-column label="单价" width="120">
        <template #default="{ row }">
          <el-input-number v-model="row._unitPrice" :min="0" :precision="2" size="small" controls-position="right" />
        </template>
      </el-table-column>
      <el-table-column label="交期" width="160">
        <template #default="{ row }">
          <el-date-picker v-model="row._deliveryDate" type="date" value-format="YYYY-MM-DD" size="small" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" :loading="row._submitting" @click="createOrder(row as PendingRow)">
            下单
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !pending.length" description="暂无待委外工序" />
  </ErpPageShell>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import DrawingPicker from '@/components/erp/DrawingPicker.vue'
import VendorSelect from '@/components/form/VendorSelect.vue'
import type { VendorOption } from '@/composables/useMasterData'
import type { Drawing } from '@/api/generated/models/Drawing'
import { fetchDrawingById, resolveDrawingId } from '@/composables/useDrawingLookup'
import { E5AllocationService } from '@/api/generated/services/E5AllocationService'
import { E6OutsubService } from '@/api/generated/services/E6OutsubService'
import { parsePageItems, unwrapResult } from '@/utils/apiPage'
import { useOutsourceStore } from '@/stores/outsource'
import type { ProcessAllocation } from '@/api/generated/models/ProcessAllocation'

type PendingRow = ProcessAllocation & {
  workorderNo?: string
  productCode?: string
  processName?: string
  _drawingNo?: string
  _drawingId?: number
  _materialHint?: string
  _vendorId?: number
  _historyPrice?: number
  _unitPrice?: number
  _deliveryDate?: string
  _submitting?: boolean
}

const loading = ref(false)
const pending = ref<PendingRow[]>([])
const outsourceStore = useOutsourceStore()

function onDrawingSelect(row: PendingRow, d: Drawing) {
  row._drawingNo = d.drawingNo ?? ''
  row._drawingId = d.id
  row._materialHint = d.materialCode ?? d.title ?? ''
  if (d.materialCode && !row.productCode) {
    row.productCode = d.materialCode
  }
}

async function onVendorChange(row: PendingRow, vendor?: VendorOption) {
  row._historyPrice = undefined
  if (!vendor?.id || !row.processName) return
  try {
    const data = unwrapResult<{ medianPrice?: number; price?: number }>(
      await outsourceStore.getHistoryPrice(vendor.id, row.processName),
    )
    const price = data.medianPrice ?? data.price
    if (price != null) {
      row._historyPrice = price
      if (!row._unitPrice || row._unitPrice <= 0) {
        row._unitPrice = price
      }
    }
  } catch {
    // 无历史价时静默
  }
}

function onExpandChange(row: PendingRow, expandedRows: PendingRow[] | boolean) {
  const expanded = Array.isArray(expandedRows)
    ? expandedRows.some((r) => r.id === row.id)
    : expandedRows
  if (!expanded) return
  if (row._drawingNo || !row.productCode) return
  void prefillDrawing(row)
}

async function prefillDrawing(row: PendingRow) {
  const id = await resolveDrawingId({ materialCode: row.productCode })
  if (!id) return
  const d = await fetchDrawingById(id)
  if (d?.drawingNo) {
    row._drawingNo = d.drawingNo
    row._drawingId = d.id
    row._materialHint = d.materialCode ?? d.title ?? ''
  }
}

async function reload() {
  loading.value = true
  try {
    pending.value = parsePageItems(await E5AllocationService.pendingAllocations()).items.map((item) => ({
      ...(item as PendingRow),
      _drawingNo: (item as PendingRow).productCode ? '' : '',
      _vendorId: undefined,
      _historyPrice: undefined,
      _unitPrice: 0,
      _deliveryDate: '',
      _submitting: false,
    }))
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function createOrder(row: PendingRow) {
  if (row.id == null) {
    ElMessage.error('分配记录无效')
    return
  }
  if (!row._drawingId) {
    if (row._drawingNo || row.productCode) {
      const id = await resolveDrawingId({ drawingNo: row._drawingNo, materialCode: row.productCode })
      if (id) row._drawingId = id
    }
  }
  if (!row._drawingId) {
    ElMessage.warning('请先展开行并选择/确认加工图纸')
    return
  }
  if (!row._vendorId) {
    ElMessage.warning('请选择厂商')
    return
  }
  if (!row._unitPrice || row._unitPrice <= 0) {
    ElMessage.warning('请填写单价')
    return
  }
  if (!row._deliveryDate) {
    ElMessage.warning('请选择交期')
    return
  }

  row._submitting = true
  try {
    const created = unwrapResult<{ outsourceNo?: string }>(await E6OutsubService.createOutsubOrder({
      allocationId: row.id,
      vendorId: row._vendorId,
      unitPrice: row._unitPrice,
      deliveryDate: row._deliveryDate,
      drawingId: row._drawingId,
    }))
    ElMessage.success(`委外单已创建：${created.outsourceNo ?? ''}`)
    await reload()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '下单失败')
  } finally {
    row._submitting = false
  }
}

onMounted(reload)
</script>

<style scoped>
.outsub-table :deep(.el-table__expanded-cell) {
  background: var(--erp-bg-muted, #f6f8fa) !important;
  padding: 0 !important;
}
.expand-panel {
  padding: 12px 24px 16px;
  background: var(--erp-bg-card);
}
.expand-title {
  margin: 0 0 10px;
  font-size: 14px;
  font-weight: 600;
}
.material-hint {
  margin: 10px 0 0;
  font-size: 13px;
  color: var(--erp-text-secondary);
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}
.history-price {
  color: var(--erp-color-primary);
  font-weight: 600;
}
.muted {
  color: var(--erp-text-muted);
  font-size: 12px;
}
</style>
