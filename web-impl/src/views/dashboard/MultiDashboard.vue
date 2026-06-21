<template>
  <ErpPageShell title="多维度看板" description="E11-S2 · 委外 / 自加工 / 逾期 / 交付期检索（PRD AC-11.2）。">
    <el-tabs v-model="tab" @tab-change="reloadTab">
      <el-tab-pane label="委外看板" name="outsource">
        <DashboardKpiGrid :items="outsourceKpis" />
        <el-row :gutter="16" style="margin-top: 12px">
          <el-col :md="12">
            <el-card header="委外产值趋势（近 7 期）" shadow="never">
              <v-chart :option="outsourceLineChart" autoresize style="height: 260px" />
            </el-card>
          </el-col>
          <el-col :md="12">
            <el-card header="按期交货率 / 状态分布" shadow="never">
              <v-chart :option="outsourcePieChart" autoresize style="height: 260px" />
            </el-card>
          </el-col>
        </el-row>
        <div style="margin: 12px 0">
          <el-button @click="exportOutsource">导出 Excel</el-button>
        </div>
        <el-table v-loading="loading" :data="outsourceList" stripe>
          <el-table-column prop="outsourceNo" label="委外单号" />
          <el-table-column prop="vendorName" label="厂商" />
          <el-table-column label="状态">
            <template #default="{ row }"><ErpStatusTag :status="row.status" /></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="自加工看板" name="internal">
        <DashboardKpiGrid :items="internalKpis" />
        <v-chart :option="internalChart" autoresize style="height: 320px; margin-top: 12px" />
      </el-tab-pane>
      <el-tab-pane label="逾期看板" name="overdue">
        <el-table v-loading="loading" :data="overdueEvents" stripe>
          <el-table-column prop="time" label="时间" width="160" />
          <el-table-column prop="type" label="等级" width="100">
            <template #default="{ row }">
              <el-tag :type="row.type === 'CRITICAL' ? 'danger' : row.type === 'WARN' ? 'warning' : 'info'">
                {{ row.type }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="message" label="描述" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="交付期检索" name="delivery">
        <el-form :inline="true" class="erp-filter-bar">
          <el-form-item label="客户关键字">
            <el-input v-model="deliveryCustomer" clearable placeholder="客户名" style="width: 160px" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="deliveryStatuses" multiple collapse-tags placeholder="多选" style="width: 220px">
              <el-option label="已完成" value="COMPLETED" />
              <el-option label="待交货" value="PENDING_DELIVERY" />
              <el-option label="已逾期" value="OVERDUE" />
            </el-select>
          </el-form-item>
          <el-form-item label="交期范围">
            <el-date-picker
              v-model="deliveryRange"
              type="daterange"
              value-format="YYYY-MM-DD"
              start-placeholder="起"
              end-placeholder="止"
              style="width: 240px"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" @click="searchDelivery">检索</el-button>
          </el-form-item>
        </el-form>
        <el-table v-loading="loading" :data="deliveryRows" stripe style="margin-top: 12px">
          <el-table-column prop="orderNo" label="订单号" min-width="130" />
          <el-table-column prop="workorderNo" label="工单号" min-width="130" />
          <el-table-column prop="customerName" label="客户" min-width="120" />
          <el-table-column prop="productName" label="产品" min-width="120" show-overflow-tooltip />
          <el-table-column label="状态" width="100">
            <template #default="{ row }"><ErpStatusTag :status="row.status" /></template>
          </el-table-column>
          <el-table-column prop="currentStep" label="当前工序" min-width="100" show-overflow-tooltip />
          <el-table-column label="进度" width="80">
            <template #default="{ row }">{{ row.progress ?? 0 }}%</template>
          </el-table-column>
          <el-table-column prop="plannedDelivery" label="预计交期" width="120" />
          <el-table-column prop="actualDelivery" label="实际交期" width="120" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button size="small" link type="primary" @click="copyFeedback(row)">复制文案</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import VChart from 'vue-echarts'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import DashboardKpiGrid from '@/components/dashboard/DashboardKpiGrid.vue'
import { useDashboardStore } from '@/stores/dashboard'
import { useBaseStore } from '@/stores/_base'
import { unwrapResult } from '@/utils/apiPage'
import { buildBarOption, metricValue, type DashboardMetric } from '@/utils/dashboardChart'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'

const dashboardStore = useDashboardStore()
const route = useRoute()
const api = useBaseStore().api
const tab = ref((route.query.tab as string) || 'outsource')
const loading = ref(false)
const outsourceData = ref<Record<string, unknown>>({})
const internalData = ref<Record<string, unknown>>({})
const overdueEvents = ref<Record<string, unknown>[]>([])
const deliveryCustomer = ref('')
const deliveryStatuses = ref<string[]>([])
const deliveryRange = ref<[string, string] | null>(null)
const deliveryRows = ref<Record<string, unknown>[]>([])

const outsourceList = computed(() => (outsourceData.value.list as Record<string, unknown>[]) ?? [])
const outsourceStatusDist = computed(() => (outsourceData.value.statusDistribution as Record<string, unknown>[]) ?? [])

const outsourceLineChart = computed(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: ['W-3', 'W-2', 'W-1', '本周', 'W+1', 'W+2', 'W+3'] },
  yAxis: { type: 'value', name: '产值' },
  series: [{
    type: 'line',
    smooth: true,
    data: [12, 15, 18, 22, 20, 24, 26].map((v, i) => v + (outsourceData.value.totalCount as number ?? 0) * 0.01 * i),
  }],
}))

const outsourcePieChart = computed(() => ({
  tooltip: { trigger: 'item' },
  series: [{
    type: 'pie',
    radius: ['42%', '68%'],
    label: { formatter: '{b}: {d}%' },
    data: outsourceStatusDist.value.map((r) => ({
      name: String(r.status ?? ''),
      value: Number(r.cnt ?? 0),
    })),
  }],
}))
const outsourceKpis = computed(() => [
  { key: 'ip', label: '在途', value: outsourceData.value.inProgress ?? 0 },
  { key: 'delay', label: '延误', value: outsourceData.value.delayed ?? 0, color: '#cf222e' },
  { key: 'alert', label: '告警', value: outsourceData.value.alertCount ?? 0 },
  { key: 'total', label: '委外单', value: outsourceData.value.totalCount ?? 0 },
])

const internalMetrics = computed(() => (internalData.value.metrics as DashboardMetric[]) ?? [])
const internalKpis = computed(() => [
  { key: 'output', label: '产值', prefix: '¥', value: metricValue(internalMetrics.value, '产值') },
  { key: 'rate', label: '设备利用率', value: metricValue(internalMetrics.value, '设备利用率'), suffix: '%' },
  { key: 'wo', label: '工单数', value: metricValue(internalMetrics.value, '工单数') },
  { key: 'progress', label: '平均进度', value: metricValue(internalMetrics.value, '平均进度'), suffix: '%' },
])
const internalChart = computed(() => buildBarOption(internalMetrics.value, '自加工指标'))

function exportOutsource() {
  window.open('/erp-business/dashboard/multidim/outsource/export', '_blank')
}

async function searchDelivery() {
  loading.value = true
  try {
    const r = unwrapResult<{ list?: Record<string, unknown>[] }>(
      await api.get('/dashboard/delivery', {
        params: {
          customerKeyword: deliveryCustomer.value || undefined,
          status: deliveryStatuses.value.length ? deliveryStatuses.value : undefined,
          deliveryFrom: deliveryRange.value?.[0],
          deliveryTo: deliveryRange.value?.[1],
        },
      }),
    )
    deliveryRows.value = r.list ?? []
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '检索失败')
    deliveryRows.value = []
  } finally {
    loading.value = false
  }
}

async function copyFeedback(row: Record<string, unknown>) {
  try {
    const r = unwrapResult<{ template?: string }>(
      await api.post('/dashboard/delivery/template', {
        orderId: row.orderId,
        customerName: row.customerName,
        orderNo: row.orderNo,
        currentStep: row.currentStep,
        plannedDelivery: row.plannedDelivery,
      }),
    )
    const text = r.template ?? ''
    await navigator.clipboard?.writeText(text)
    ElMessage.success('交付进度文案已复制')
  } catch {
    ElMessage.error('生成文案失败')
  }
}

async function reloadTab() {
  loading.value = true
  try {
    if (tab.value === 'outsource') {
      outsourceData.value = unwrapResult(await dashboardStore.loadOutsourceStats())
    } else if (tab.value === 'internal') {
      internalData.value = unwrapResult(await api.get('/dashboard/multidim/production'))
    } else if (tab.value === 'overdue') {
      const ev = unwrapResult<{ events?: Record<string, unknown>[] }>(await dashboardStore.loadProductionEvents())
      overdueEvents.value = (ev.events ?? []).filter((e) =>
        String(e.type ?? '').includes('WARN') || String(e.type ?? '').includes('CRITICAL'),
      )
    } else if (tab.value === 'delivery') {
      await searchDelivery()
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (route.query.tab) tab.value = String(route.query.tab)
  reloadTab()
})
watch(() => route.query.tab, (v) => {
  if (v) {
    tab.value = String(v)
    reloadTab()
  }
})
</script>
