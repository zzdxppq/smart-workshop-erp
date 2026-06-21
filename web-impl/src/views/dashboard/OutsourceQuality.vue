<template>
  <div v-loading="loading">
    <h2>委外品质</h2>
    <el-card v-if="quality">
      <h3>委外检合格率</h3>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="送检">{{ quality.inspected }}</el-descriptions-item>
        <el-descriptions-item label="合格">{{ quality.passed }}</el-descriptions-item>
        <el-descriptions-item label="合格率">{{ quality.passRate }}%</el-descriptions-item>
        <el-descriptions-item label="退货数">{{ quality.rejected }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useDashboardStore } from '@/stores/dashboard'
import { useDashboardStat } from '@/composables/useDashboardData'

const dashboardStore = useDashboardStore()
const { data: quality, loading, load } = useDashboardStat(() => dashboardStore.loadOutsourceQuality())

onMounted(load)
</script>
