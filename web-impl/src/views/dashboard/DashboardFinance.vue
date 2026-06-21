<template>
  <ErpPageShell title="财务驾驶舱" description="E11 · 应收应付、利润与毛利率（FR-9-4）。">
    <el-form :inline="true" class="erp-filter-bar">
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="load">刷新</el-button>
      </el-form-item>
    </el-form>

    <DashboardKpiGrid :items="kpis" />

    <el-row :gutter="16">
      <el-col :md="14">
        <el-card header="财务指标" shadow="never">
          <v-chart :option="metricChart" autoresize style="height: 320px" />
        </el-card>
      </el-col>
      <el-col :md="10">
        <el-card header="趋势" shadow="never">
          <v-chart :option="trendChart" autoresize style="height: 320px" />
        </el-card>
      </el-col>
    </el-row>

    <el-alert
      v-if="Number(stats?.grossMargin ?? 0) < 10"
      type="warning"
      :closable="false"
      title="毛利率预警"
      description="当前毛利率低于 10%，请关注订单成本与委外费用（PRD FR-9-4-2）。"
      style="margin-top: 16px"
    />
  </ErpPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import VChart from 'vue-echarts'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import DashboardKpiGrid from '@/components/dashboard/DashboardKpiGrid.vue'
import { useDashboardStore } from '@/stores/dashboard'
import { useDashboardStat } from '@/composables/useDashboardData'
import { buildBarOption, buildTrendOption, type DashboardMetric } from '@/utils/dashboardChart'

const dashboardStore = useDashboardStore()
const { data: stats, loading, load } = useDashboardStat<Record<string, unknown>>(
  () => dashboardStore.loadFinanceStats(),
)

const kpis = computed(() => [
  { key: 'revenue', label: '利润', prefix: '¥', value: stats.value?.profit ?? stats.value?.revenue ?? 0 },
  { key: 'cost', label: '成本', prefix: '¥', value: stats.value?.cost ?? 0 },
  { key: 'recv', label: '应收账款', prefix: '¥', value: stats.value?.receivables ?? 0 },
  { key: 'margin', label: '毛利率', value: stats.value?.grossMargin ?? 0, suffix: '%', color: '#bf8700' },
])

const metricChart = computed(() =>
  buildBarOption((stats.value?.metrics as DashboardMetric[]) ?? [], '财务域指标'),
)
const trendChart = computed(() =>
  buildTrendOption((stats.value?.trend as Record<string, unknown>[]) ?? [], '月度趋势'),
)

onMounted(load)
</script>
