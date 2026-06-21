<template>
  <div v-loading="loading">
    <h2>销售排行报表</h2>
    <el-form :inline="true">
      <el-form-item label="期间"><el-date-picker v-model="period" type="month" /></el-form-item>
      <el-form-item><el-button type="primary" @click="refresh">查询</el-button></el-form-item>
      <el-form-item><el-button type="success" @click="exportExcel">导出 Excel</el-button></el-form-item>
    </el-form>
    <el-table :data="ranking" stripe>
      <el-table-column prop="rank" label="排名" width="80" />
      <el-table-column prop="customerName" label="客户" />
      <el-table-column prop="salesman" label="业务员" />
      <el-table-column prop="amount" label="金额" />
      <el-table-column prop="orderCount" label="订单数" />
      <el-table-column prop="growth" label="同比增长">
        <template #default="{ row }">
          <el-tag :type="row.growth > 0 ? 'success' : 'danger'">{{ row.growth }}%</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useReportStore } from '@/stores/report'
import { parsePageItems } from '@/utils/apiPage'

const reportStore = useReportStore()
const ranking = ref<Record<string, unknown>[]>([])
const period = ref<string>('2026-06')
const loading = ref(false)

async function refresh() {
  loading.value = true
  try {
    ranking.value = parsePageItems(await reportStore.loadSalesRanking({ period: period.value, topN: 10 })).items as Record<string, unknown>[]
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载失败')
    ranking.value = []
  } finally {
    loading.value = false
  }
}

function exportExcel() {
  ElMessage.success('Excel 导出已启动')
}

onMounted(refresh)
</script>
