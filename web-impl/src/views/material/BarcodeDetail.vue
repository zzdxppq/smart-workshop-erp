<template>
  <div v-loading="loading" class="barcode-detail">
    <h2>条码详情</h2>
    <el-card v-if="barcode">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="条码号">{{ barcode.barcodeNo }}</el-descriptions-item>
        <el-descriptions-item label="料号">{{ barcode.materialCode }}</el-descriptions-item>
        <el-descriptions-item label="物料名称">{{ barcode.materialName }}</el-descriptions-item>
        <el-descriptions-item label="规格">{{ barcode.spec }}</el-descriptions-item>
        <el-descriptions-item label="单位">{{ barcode.unit }}</el-descriptions-item>
        <el-descriptions-item label="数量">{{ barcode.qty }}</el-descriptions-item>
        <el-descriptions-item label="批次号">{{ barcode.batchNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <ErpStatusTag :status="barcode.status" :label="barcodeStatusLabel(barcode.status)" />
        </el-descriptions-item>
        <el-descriptions-item label="5 段成本">
          <div v-if="barcode.costBreakdown">
            物料：{{ barcode.costBreakdown.material }}<br />
            人工：{{ barcode.costBreakdown.labor }}<br />
            机器：{{ barcode.costBreakdown.machine }}<br />
            制费：{{ barcode.costBreakdown.overhead }}<br />
            委外：{{ barcode.costBreakdown.outsource }}<br />
            合计：{{ barcode.costBreakdown.total }}
          </div>
        </el-descriptions-item>
      </el-descriptions>
      <div style="margin-top: 16px">
        <h4>扫码历史</h4>
        <el-table :data="barcode.history || []" size="small">
          <el-table-column label="类型" width="90">
            <template #default="{ row }">{{ scanTypeLabel(String(row.scanType ?? '')) }}</template>
          </el-table-column>
          <el-table-column prop="scanAt" label="时间" />
          <el-table-column prop="scanLocation" label="地点" />
        </el-table>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useMaterialStore } from '@/stores/material'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'
import { bizStatusLabel, BARCODE_STATUS, SCAN_TYPE_LABELS, commonStatusLabel } from '@/utils/statusLabels'
import { unwrapResult } from '@/utils/apiPage'

function barcodeStatusLabel(s?: string) {
  return bizStatusLabel(s, BARCODE_STATUS)
}

function scanTypeLabel(s?: string) {
  return commonStatusLabel(s, SCAN_TYPE_LABELS)
}

const route = useRoute()
const materialStore = useMaterialStore()
const barcode = ref<{
  barcodeNo?: string
  materialCode?: string
  materialName?: string
  spec?: string
  unit?: string
  qty?: number
  batchNo?: string
  status?: string
  costBreakdown?: { material?: number; labor?: number; machine?: number; overhead?: number; outsource?: number; total?: number }
  history?: Record<string, unknown>[]
} | null>(null)
const loading = ref(false)

onMounted(async () => {
  const barcodeNo = route.params.barcodeNo as string
  if (!barcodeNo) {
    ElMessage.error('无效的条码号')
    return
  }
  loading.value = true
  try {
    barcode.value = unwrapResult(await materialStore.parseBarcode(barcodeNo))
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载失败')
    barcode.value = null
  } finally {
    loading.value = false
  }
})
</script>
