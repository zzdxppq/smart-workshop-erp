<template>
  <div class="barcode-print">
    <h2>条码打印</h2>
    <el-alert type="info" :closable="false" title="支持 ZPL 直连 / A4 PDF 浏览器打印 · 最多一次 30 条" style="margin-bottom: 12px" />

    <el-row :gutter="12" align="middle" style="margin-bottom: 12px">
      <el-col :span="6">
        <el-select v-model="printMode" placeholder="打印模式">
          <el-option label="A4 PDF 浏览器" value="PDF_BROWSER" />
          <el-option label="ZPL 标签机" value="ZPL_DIRECT" />
        </el-select>
      </el-col>
      <el-col :span="6" v-if="printMode === 'ZPL_DIRECT'">
        <el-select v-model="printerId" placeholder="选择标签打印机" filterable style="width: 100%">
          <el-option
            v-for="p in printers"
            :key="p.id"
            :label="`${p.name} (${p.ip}:${p.port})`"
            :value="p.id!"
          />
        </el-select>
      </el-col>
      <el-col :span="12" style="text-align: right">
        <el-button type="primary" :loading="printing" :disabled="!selectedRows.length" @click="printBarcodes">
          打印选中 ({{ selectedRows.length }})
        </el-button>
        <el-button @click="reload">刷新列表</el-button>
      </el-col>
    </el-row>

    <el-card>
      <el-table
        v-loading="loading"
        :data="barcodes"
        stripe
        border
        @selection-change="onSelectionChange"
      >
        <el-table-column type="selection" width="48" />
        <el-table-column prop="barcodeNo" label="条码号（完整复合码）" min-width="220" />
        <el-table-column prop="materialCode" label="料号" min-width="160" />
        <el-table-column prop="spec" label="规格" min-width="120" />
        <el-table-column prop="qty" label="数量" width="80" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <ErpStatusTag :status="row.status" :label="barcodeStatusLabel(row.status)" />
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        style="margin-top: 12px"
        @current-change="reload"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useMaterialStore } from '@/stores/material'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'
import { bizStatusLabel, BARCODE_STATUS } from '@/utils/statusLabels'
import { parsePageItems, unwrapResult } from '@/utils/apiPage'

function barcodeStatusLabel(s?: string) {
  return bizStatusLabel(s, BARCODE_STATUS)
}
import { E12PrintService } from '@/api/generated/services/E12PrintService'
import { E12PrinterService } from '@/api/generated/services/E12PrinterService'
import type { SysPrinter } from '@/api/generated/models/SysPrinter'
import type { PrinterAvailableResponse } from '@/api/generated/models/PrinterAvailableResponse'
import type { ZplPrintRequest } from '@/api/generated/models/ZplPrintRequest'

interface BarcodeRow {
  barcodeNo: string
  materialCode?: string
  spec?: string
  qty?: number
  status?: string
}

const route = useRoute()
const materialStore = useMaterialStore()
const barcodes = ref<BarcodeRow[]>([])
const selectedRows = ref<BarcodeRow[]>([])
const loading = ref(false)
const printing = ref(false)
const page = ref(1)
const pageSize = 20
const total = ref(0)
const printMode = ref<'PDF_BROWSER' | 'ZPL_DIRECT'>('PDF_BROWSER')
const printerId = ref<number | undefined>(undefined)
const printers = ref<SysPrinter[]>([])

async function loadPrinters() {
  try {
    const data = unwrapResult<PrinterAvailableResponse>(
      await E12PrinterService.getAvailablePrinters('LABEL', 1),
    )
    printers.value = (data.printers ?? []) as SysPrinter[]
    if (printers.value.length && printerId.value == null) {
      printerId.value = printers.value[0].id
    }
  } catch {
    printers.value = []
  }
}

function inferTemplateCode(barcodeNo: string): ZplPrintRequest['templateCode'] {
  if (barcodeNo.startsWith('GD-')) return 'GD'
  if (barcodeNo.startsWith('LZ-')) return 'LZ'
  if (barcodeNo.startsWith('SB-')) return 'SB'
  if (barcodeNo.startsWith('WW-')) return 'WW'
  if (barcodeNo.startsWith('WL-')) return 'WL'
  return 'GD'
}

function onSelectionChange(rows: BarcodeRow[]) {
  selectedRows.value = rows
}

async function reload() {
  loading.value = true
  try {
    const pageData = parsePageItems(await materialStore.listBarcodes({ page: page.value - 1, size: pageSize }))
    barcodes.value = pageData.items as BarcodeRow[]
    total.value = pageData.total
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载条码失败')
    barcodes.value = []
  } finally {
    loading.value = false
    await applyPresetSelection()
  }
}

/** 从 ?codes=WL-A001-BATCH-...,WL-A002-BATCH-... 预选本次入库生成的条码 */
async function applyPresetSelection() {
  const raw = route.query.codes
  if (!raw || typeof raw !== 'string') return
  const codes = raw.split(',').map(s => s.trim()).filter(Boolean)
  if (!codes.length) return
  const set = new Set(codes)
  // 当前页内能命中的勾上；剩余未命中的追加为合成行（避免再翻页查找）
  selectedRows.value = barcodes.value.filter(b => set.has(b.barcodeNo))
  const missing = codes.filter(c => !barcodes.value.some(b => b.barcodeNo === c))
  if (missing.length) {
    const synthetic: BarcodeRow[] = missing.map(c => ({ barcodeNo: c, status: 'NEW' }))
    barcodes.value = [...synthetic, ...barcodes.value]
    selectedRows.value = [...selectedRows.value, ...synthetic]
    ElMessage.success(`已预选 ${codes.length} 张本次入库条码`)
  }
  await nextTick()
}

async function printBarcodes() {
  if (!selectedRows.value.length) return
  if (selectedRows.value.length > 30) {
    ElMessage.error('单次最多打印 30 条')
    return
  }

  printing.value = true
  try {
    if (printMode.value === 'ZPL_DIRECT') {
      if (!printerId.value) {
        ElMessage.warning('请先选择标签打印机')
        return
      }
      for (const row of selectedRows.value) {
        await E12PrintService.printLabelsZpl({
          templateCode: inferTemplateCode(row.barcodeNo),
          qrContent: row.barcodeNo,
          lines: [row.materialCode || '', row.spec || '', String(row.qty ?? '')].filter(Boolean),
          printerId: printerId.value,
          count: 1,
        })
      }
      ElMessage.success(`已发送 ${selectedRows.value.length} 条 ZPL 打印任务`)
    } else {
      const pdf = unwrapResult<{ pdfBase64?: string }>(await E12PrintService.printLabelsPdfA4({
        items: selectedRows.value.map(row => ({
          templateCode: inferTemplateCode(row.barcodeNo),
          qrContent: row.barcodeNo,
          lines: [row.materialCode || '', row.spec || '', String(row.qty ?? '')].filter(Boolean),
        })),
        remark: 'barcode-print batch',
      }))
      const base64 = pdf.pdfBase64
      if (base64) {
        const bin = atob(base64)
        const bytes = new Uint8Array(bin.length)
        for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i)
        const blob = new Blob([bytes], { type: 'application/pdf' })
        const url = URL.createObjectURL(blob)
        const w = window.open(url)
        w?.print()
      }
      ElMessage.success(`已生成 PDF · ${selectedRows.value.length} 标签`)
    }
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '打印失败')
  } finally {
    printing.value = false
  }
}

onMounted(() => {
  void loadPrinters()
  reload()
})
</script>

<style scoped>
.barcode-print {
  padding: 16px;
}
</style>
