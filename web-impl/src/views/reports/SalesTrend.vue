<template>
  <div v-loading="loading">
    <h2>销售趋势报表</h2>
    <el-form :inline="true">
      <el-form-item label="开始"><el-date-picker v-model="startDate" type="date" /></el-form-item>
      <el-form-item label="结束"><el-date-picker v-model="endDate" type="date" /></el-form-item>
      <el-form-item><el-button type="primary" @click="refresh">查询</el-button></el-form-item>
    </el-form>
    <el-table :data="trend" stripe>
      <el-table-column prop="period" label="期间" />
      <el-table-column prop="revenue" label="销售额" />
      <el-table-column prop="cost" label="成本" />
      <el-table-column prop="profit" label="利润" />
      <el-table-column prop="orderCount" label="订单数" />
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useReportStore } from '@/stores/report'
import { parsePageItems } from '@/utils/apiPage'

const reportStore = useReportStore()
const trend = ref<Record<string, unknown>[]>([])
const startDate = ref<string>('2026-01-01')
const endDate = ref<string>('2026-06-30')
const loading = ref(false)

async function refresh() {
  loading.value = true
  try {
    trend.value = parsePageItems(await reportStore.loadSalesTrend({ startDate: startDate.value, endDate: endDate.value })).items as Record<string, unknown>[]
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载失败')
    trend.value = []
  } finally {
    loading.value = false
  }
}
onMounted(refresh)
</script>
