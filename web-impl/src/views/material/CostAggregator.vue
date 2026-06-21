<template>
  <div class="cost-aggregator-page">
    <h2>料号成本 / 价格面板</h2>
    <p class="subtitle">PRD FR-11-S5 · 5 Tab：价格 / 材料 / 工时 / 外协 / 总成本</p>

    <el-form :inline="true" style="margin-bottom: 12px">
      <el-form-item label="料号">
        <el-input v-model="materialCode" placeholder="如 MAT-001" clearable @keyup.enter="fetchAll" />
      </el-form-item>
      <el-form-item label="期间">
        <el-input v-model="period" placeholder="2026-06" clearable style="width: 120px" />
      </el-form-item>
      <el-form-item label="客户 ID">
        <el-input-number v-model="customerId" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="fetchAll">查询</el-button>
        <el-button :disabled="!materialCode" @click="exportXlsx">导出 Excel</el-button>
        <el-button :disabled="!materialCode" @click="exportPdf">导出 PDF</el-button>
      </el-form-item>
    </el-form>

    <el-tabs v-model="tab" v-loading="loading">
      <el-tab-pane label="价格" name="price">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-statistic title="最近报价" :value="priceData.latestPrice ?? 0" :precision="2" prefix="¥" />
          </el-col>
          <el-col :span="8">
            <el-statistic title="采购限价" :value="priceData.priceLimit ?? 0" :precision="2" prefix="¥" />
          </el-col>
          <el-col :span="8">
            <el-statistic title="标准售价" :value="priceData.standardPrice ?? 0" :precision="2" prefix="¥" />
          </el-col>
        </el-row>
        <el-table v-if="vendorRows.length" :data="vendorRows" stripe border size="small" style="margin-top: 16px">
          <el-table-column prop="vendorName" label="厂商" />
          <el-table-column prop="unitPrice" label="单价" width="100" />
          <el-table-column prop="leadDays" label="交期(天)" width="100" />
          <el-table-column prop="qualityScore" label="质量分" width="90" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="材料成本" name="material">
        <el-statistic title="材料成本" :value="cost.materialCost ?? 0" :precision="2" prefix="¥" />
        <div ref="materialChartRef" class="chart" />
      </el-tab-pane>

      <el-tab-pane label="工时成本" name="labor">
        <el-statistic title="工时成本" :value="cost.laborCost ?? 0" :precision="2" prefix="¥" />
        <el-row :gutter="16" style="margin-top: 12px">
          <el-col :span="12">
            <el-statistic title="表处成本" :value="cost.surfaceCost ?? 0" :precision="2" prefix="¥" />
          </el-col>
          <el-col :span="12">
            <el-statistic title="管理费分摊" :value="cost.mfgCost ?? 0" :precision="2" prefix="¥" />
          </el-col>
        </el-row>
        <div ref="laborChartRef" class="chart" />
      </el-tab-pane>

      <el-tab-pane label="外协成本" name="outsource">
        <el-statistic title="外协成本" :value="cost.outsourceCost ?? 0" :precision="2" prefix="¥" />
      </el-tab-pane>

      <el-tab-pane label="总成本" name="total">
        <el-statistic title="总成本" :value="cost.totalCost ?? 0" :precision="2" prefix="¥" />
        <p v-if="cost.computedAt" class="meta">计算时间：{{ cost.computedAt }} · 版本 {{ cost.version ?? '—' }}</p>
        <el-descriptions v-if="cost.totalCost" :column="2" border style="margin-top: 16px">
          <el-descriptions-item label="材料">¥{{ (cost.materialCost ?? 0).toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="工时">¥{{ (cost.laborCost ?? 0).toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="表处">¥{{ (cost.surfaceCost ?? 0).toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="外协">¥{{ (cost.outsourceCost ?? 0).toFixed(2) }}</el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { E9CostAggService } from '@/api/generated/services/E9CostAggService'
import type { CostAggregate } from '@/api/generated/models/CostAggregate'
import { useDashboardStore } from '@/stores/dashboard'
import { unwrapResult } from '@/utils/apiPage'

const route = useRoute()
const dashboardStore = useDashboardStore()
const tab = ref('price')
const materialCode = ref((route.query.code as string) || '')
const period = ref('')
const customerId = ref<number | undefined>()
const loading = ref(false)
const cost = ref<CostAggregate>({})
const priceData = ref<Record<string, number>>({})
const vendorRows = ref<Record<string, unknown>[]>([])
const trendMonths = ref<string[]>([])
const trendMaterial = ref<number[]>([])
const trendLabor = ref<number[]>([])

const materialChartRef = ref<HTMLDivElement>()
const laborChartRef = ref<HTMLDivElement>()
let materialChart: echarts.ECharts | null = null
let laborChart: echarts.ECharts | null = null

function renderCharts() {
  if (materialChartRef.value) {
    materialChart ??= echarts.init(materialChartRef.value)
    materialChart.setOption({
      title: { text: '材料成本 12 月趋势', textStyle: { fontSize: 13 } },
      xAxis: { type: 'category', data: trendMonths.value },
      yAxis: { type: 'value' },
      series: [{ type: 'line', data: trendMaterial.value, smooth: true }],
      grid: { left: 48, right: 16, top: 40, bottom: 32 },
    })
  }
  if (laborChartRef.value) {
    laborChart ??= echarts.init(laborChartRef.value)
    laborChart.setOption({
      title: { text: '工时成本 12 月趋势', textStyle: { fontSize: 13 } },
      xAxis: { type: 'category', data: trendMonths.value },
      yAxis: { type: 'value' },
      series: [{ type: 'line', data: trendLabor.value, smooth: true, itemStyle: { color: '#6366f1' } }],
      grid: { left: 48, right: 16, top: 40, bottom: 32 },
    })
  }
}

async function fetchAll() {
  if (!materialCode.value.trim()) {
    ElMessage.warning('请输入物料编码')
    return
  }
  loading.value = true
  const code = materialCode.value.trim()
  const params = { materialCode: code, period: period.value || undefined, customerId: customerId.value }
  try {
    const [costRes, priceRes, trendRes, vendorRes] = await Promise.all([
      E9CostAggService.getCostAggregate(code, period.value || undefined, customerId.value),
      dashboardStore.loadMaterialPrice(params).catch(() => null),
      dashboardStore.loadMaterialCostTrend(params).catch(() => null),
      dashboardStore.loadMaterialVendorCompare(params).catch(() => null),
    ])

    cost.value = unwrapResult<CostAggregate>(costRes)
    const price = unwrapResult<Record<string, unknown>>(priceRes ?? { code: 0, data: {} })
    priceData.value = {
      latestPrice: Number(price.latestPrice ?? price.lastPrice ?? 0),
      priceLimit: Number(price.priceLimit ?? price.limitPrice ?? 0),
      standardPrice: Number(price.standardPrice ?? 0),
    }

    const trend = unwrapResult<{ months?: string[]; material?: number[]; labor?: number[] }>(trendRes ?? { code: 0, data: {} })
    trendMonths.value = trend.months ?? []
    trendMaterial.value = trend.material ?? []
    trendLabor.value = trend.labor ?? []

    const vendors = unwrapResult<{ items?: Record<string, unknown>[] } | Record<string, unknown>[]>(
      vendorRes ?? { code: 0, data: [] },
    )
    vendorRows.value = Array.isArray(vendors) ? vendors : (vendors.items ?? [])

    await nextTick()
    renderCharts()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '查询失败')
    cost.value = {}
  } finally {
    loading.value = false
  }
}

async function exportXlsx() {
  await exportFile('xlsx')
}

async function exportPdf() {
  await exportFile('pdf')
}

async function exportFile(format: 'xlsx' | 'pdf') {
  if (!materialCode.value.trim()) return
  try {
    const blob = await E9CostAggService.exportCostAggregate(materialCode.value.trim(), format)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${materialCode.value}-cost.${format === 'pdf' ? 'pdf' : 'xlsx'}`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '导出失败')
  }
}

watch(tab, async () => {
  await nextTick()
  renderCharts()
})

onMounted(() => {
  if (materialCode.value) fetchAll()
  window.addEventListener('resize', renderCharts)
})

onUnmounted(() => {
  materialChart?.dispose()
  laborChart?.dispose()
  window.removeEventListener('resize', renderCharts)
})
</script>

<style scoped>
.cost-aggregator-page {
  padding: 16px;
}
.subtitle {
  margin: -8px 0 12px;
  font-size: 13px;
  color: var(--erp-text-secondary);
}
.meta {
  margin-top: 12px;
  color: #666;
  font-size: 13px;
}
.chart {
  margin-top: 16px;
  height: 260px;
}
</style>
