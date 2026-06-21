<template>
  <div v-loading="loading">
    <h2>物料成本趋势</h2>
    <el-table :data="trendData" stripe>
      <el-table-column prop="period" label="期间" />
      <el-table-column prop="materialCode" label="料号" />
      <el-table-column prop="price" label="单价" />
      <el-table-column prop="totalCost" label="总成本" />
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useDashboardStore } from '@/stores/dashboard'
import { useDashboardList } from '@/composables/useDashboardData'

const dashboardStore = useDashboardStore()
const { items: trendData, loading, load } = useDashboardList(() => dashboardStore.loadMaterialCostTrend({}))

onMounted(load)
</script>
