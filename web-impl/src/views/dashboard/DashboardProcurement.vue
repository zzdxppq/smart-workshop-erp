<template>
  <ErpPageShell title="采购驾驶舱" description="E11 · 询价、PO 执行与待委外工序概览。">
    <RoleWorkflowPanel />
    <el-button type="primary" :loading="loading" style="margin-bottom: 12px" @click="load">刷新</el-button>
    <DashboardKpiGrid :items="kpis" />
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :md="14">
        <el-card header="采购指标" shadow="never">
          <v-chart :option="metricChart" autoresize style="height: 320px" />
        </el-card>
      </el-col>
      <el-col :md="10">
        <el-card header="快捷入口" shadow="never">
          <el-button type="primary" @click="$router.push('/sourcing/rfq')">询价比价 RFQ</el-button>
          <el-button style="margin-left: 8px" @click="$router.push('/sourcing/po')">采购订单 PO</el-button>
          <el-button style="margin-left: 8px" @click="$router.push('/sourcing/outsub-order')">委外下单</el-button>
          <el-button style="margin-left: 8px" @click="$router.push('/production/outsub-panel')">委外面板</el-button>
        </el-card>
      </el-col>
    </el-row>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import VChart from 'vue-echarts'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import DashboardKpiGrid from '@/components/dashboard/DashboardKpiGrid.vue'
import RoleWorkflowPanel from '@/components/dashboard/RoleWorkflowPanel.vue'
import { useBaseStore } from '@/stores/_base'
import { unwrapResult } from '@/utils/apiPage'
import { buildBarOption, type DashboardMetric } from '@/utils/dashboardChart'

const loading = ref(false)
const stats = ref<Record<string, unknown>>({})

const kpis = computed(() => [
  { key: 'rfq', label: '开放询价', value: stats.value.openRfq ?? 0 },
  { key: 'po', label: '待执行 PO', value: stats.value.pendingPo ?? 0, color: '#0969da' },
  { key: 'arr', label: '在途到货', value: stats.value.arriving ?? 0, color: '#bf8700' },
  { key: 'out', label: '待委外工序', value: stats.value.pendingOutsource ?? 0, color: '#cf222e' },
])

const metricChart = computed(() =>
  buildBarOption((stats.value.metrics as DashboardMetric[]) ?? [], '采购域指标'),
)

async function load() {
  loading.value = true
  try {
    stats.value = unwrapResult<Record<string, unknown>>(await useBaseStore().api.get('/dashboard/procurement'))
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
