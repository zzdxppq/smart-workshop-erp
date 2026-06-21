<template>
  <div v-loading="loading">
    <h2>生产看板视图</h2>
    <p>看板视图：可视化生产进度</p>
    <el-row :gutter="12">
      <el-col :span="6"><el-card>在制: {{ stats?.wip ?? '—' }}</el-card></el-col>
      <el-col :span="6"><el-card>完工: {{ stats?.finished ?? '—' }}</el-card></el-col>
      <el-col :span="6"><el-card>不良: {{ stats?.defect ?? '—' }}</el-card></el-col>
      <el-col :span="6"><el-card>稼动率: {{ stats?.oee ?? '—' }}%</el-card></el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useDashboardStore } from '@/stores/dashboard'
import { useDashboardStat } from '@/composables/useDashboardData'

const dashboardStore = useDashboardStore()
const { data: stats, loading, load } = useDashboardStat(() => dashboardStore.loadProductionStats())

onMounted(load)
</script>
