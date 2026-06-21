<template>
  <ErpPageShell title="总经理驾驶舱" description="E11-S3 · 产值/交付/利润汇总与利润率预警（PRD AC-9.4 / AC-11.3）。">
    <el-button type="primary" :loading="loading" style="margin-bottom: 12px" @click="load">刷新</el-button>

    <DashboardKpiGrid v-if="summary" :items="summaryKpis" />

    <el-row :gutter="16" style="margin-top: 8px">
      <el-col :md="12">
        <el-card header="销售指标" shadow="never">
          <v-chart :option="salesChart" autoresize style="height: 260px" />
        </el-card>
      </el-col>
      <el-col :md="12">
        <el-card header="财务指标" shadow="never">
          <v-chart :option="financeChart" autoresize style="height: 260px" />
        </el-card>
      </el-col>
    </el-row>

    <el-card header="利润率预警（跌破阈值标黄/标红）" shadow="never" style="margin-top: 16px">
      <el-table :data="profitAlerts" stripe>
        <el-table-column prop="orderNo" label="订单号" min-width="140" />
        <el-table-column prop="customerName" label="客户" />
        <el-table-column prop="profitRate" label="利润率" width="100">
          <template #default="{ row }">
            <el-tag :type="row.alertLevel === 'CRITICAL' ? 'danger' : 'warning'">
              {{ row.profitRate }}%
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="revenue" label="收入" width="100" />
        <el-table-column prop="totalCost" label="成本" width="100" />
      </el-table>
    </el-card>

    <el-card header="销售龙虎榜 Top 5" shadow="never" style="margin-top: 16px">
      <el-table :data="ranking" stripe size="small">
        <el-table-column prop="rank" label="#" width="50" />
        <el-table-column prop="salesman" label="业务员" />
        <el-table-column prop="customerName" label="客户" />
        <el-table-column prop="amount" label="金额" />
      </el-table>
    </el-card>

    <el-card header="生产告警" shadow="never" style="margin-top: 16px">
      <el-table :data="productionAlerts" stripe size="small">
        <el-table-column prop="workorderNo" label="工单" />
        <el-table-column prop="alertMessage" label="描述" />
        <el-table-column prop="alertType" label="等级" width="90" />
      </el-table>
    </el-card>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import VChart from 'vue-echarts'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import DashboardKpiGrid from '@/components/dashboard/DashboardKpiGrid.vue'
import { useBaseStore } from '@/stores/_base'
import { unwrapResult } from '@/utils/apiPage'
import { buildBarOption, type DashboardMetric } from '@/utils/dashboardChart'

const api = useBaseStore().api
const loading = ref(false)
const cockpit = ref<Record<string, unknown>>({})

const summary = computed(() => cockpit.value.sales as Record<string, unknown> | undefined)
const summaryKpis = computed(() => {
  const sales = cockpit.value.sales as Record<string, unknown> | undefined
  const finance = cockpit.value.finance as Record<string, unknown> | undefined
  const production = cockpit.value.production as Record<string, unknown> | undefined
  return [
    { key: 'orders', label: '订单数', value: sales?.monthOrders ?? 0 },
    { key: 'amount', label: '订单额', prefix: '¥', value: sales?.monthAmount ?? 0 },
    { key: 'profit', label: '利润', prefix: '¥', value: finance?.profit ?? 0, color: '#1a7f37' },
    { key: 'wo', label: '在制工单', value: production?.workorderCount ?? 0 },
  ]
})

const profitAlerts = computed(() => (cockpit.value.profitAlerts as Record<string, unknown>[]) ?? [])
const ranking = computed(() => {
  const r = cockpit.value.salesRanking as { list?: Record<string, unknown>[] } | undefined
  return (r?.list ?? []).slice(0, 5)
})
const productionAlerts = computed(() =>
  (cockpit.value.productionAlerts as Record<string, unknown>[]) ?? [],
)

const salesChart = computed(() =>
  buildBarOption((summary.value?.metrics as DashboardMetric[]) ?? [], '销售'),
)
const financeChart = computed(() =>
  buildBarOption(((cockpit.value.finance as Record<string, unknown>)?.metrics as DashboardMetric[]) ?? [], '财务'),
)

async function load() {
  loading.value = true
  try {
    cockpit.value = unwrapResult<Record<string, unknown>>(await api.get('/dashboard/gm'))
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
