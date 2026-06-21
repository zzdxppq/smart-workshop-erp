<template>
  <ErpPageShell title="品质驾驶舱" description="E11 · 合格率、不良率与 CMM 超差（PRD AC-11.2）。" v-loading="loading">
    <RoleWorkflowPanel />
    <DashboardKpiGrid :items="kpis" />
    <el-row :gutter="16" style="margin-top: 8px">
      <el-col :md="14">
        <el-card header="品质指标" shadow="never">
          <v-chart :option="metricChart" autoresize style="height: 320px" />
        </el-card>
      </el-col>
      <el-col :md="10">
        <el-card header="指标明细" shadow="never">
          <el-table :data="metrics" size="small" stripe>
            <el-table-column prop="name" label="指标" />
            <el-table-column label="数值" width="120">
              <template #default="{ row }">{{ row.value }}{{ row.unit || '' }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import VChart from 'vue-echarts'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import DashboardKpiGrid from '@/components/dashboard/DashboardKpiGrid.vue'
import RoleWorkflowPanel from '@/components/dashboard/RoleWorkflowPanel.vue'
import { useDashboardStore } from '@/stores/dashboard'
import { useDashboardStat } from '@/composables/useDashboardData'
import { buildBarOption, type DashboardMetric } from '@/utils/dashboardChart'

const dashboardStore = useDashboardStore()
const { data: stats, loading, load } = useDashboardStat<Record<string, unknown>>(
  () => dashboardStore.loadQualityStats(),
)

const metrics = computed(() => (stats.value?.metrics as DashboardMetric[]) ?? [])
const kpis = computed(() => [
  { key: 'pass', label: '一次合格率', value: stats.value?.passRate ?? 0, suffix: '%', color: '#1a7f37' },
  { key: 'defect', label: '不良率', value: stats.value?.defectRate ?? 0, suffix: '%', color: '#cf222e' },
  { key: 'rework', label: '返工率', value: stats.value?.reworkRate ?? 0, suffix: '%' },
  { key: 'cmm', label: 'CMM 超差', value: stats.value?.cmmOver ?? stats.value?.openDefects ?? 0, suffix: ' 次' },
])
const metricChart = computed(() => buildBarOption(metrics.value, '品质域指标'))

onMounted(load)
</script>
