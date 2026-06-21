<template>
  <div v-loading="loading">
    <h2>委外成本</h2>
    <el-card v-if="cost">
      <h3>委外成本分析</h3>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="本月委外金额">{{ cost.monthlyAmount }}</el-descriptions-item>
        <el-descriptions-item label="占比">{{ cost.percentage }}%</el-descriptions-item>
        <el-descriptions-item label="TOP 供应商 1">{{ cost.topVendor1 }}</el-descriptions-item>
        <el-descriptions-item label="TOP 供应商 2">{{ cost.topVendor2 }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useDashboardStore } from '@/stores/dashboard'
import { useDashboardStat } from '@/composables/useDashboardData'

const dashboardStore = useDashboardStore()
const { data: cost, loading, load } = useDashboardStat(() => dashboardStore.loadOutsourceCost())

onMounted(load)
</script>
