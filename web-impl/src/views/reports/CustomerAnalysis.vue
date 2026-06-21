<template>
  <div v-loading="loading">
    <h2>客户分析报表</h2>
    <el-form :inline="true">
      <el-form-item label="期间"><el-date-picker v-model="period" type="month" /></el-form-item>
      <el-form-item><el-button type="primary" @click="refresh">查询</el-button></el-form-item>
    </el-form>
    <el-table :data="analysis" stripe>
      <el-table-column prop="customerName" label="客户" />
      <el-table-column prop="totalAmount" label="总金额" />
      <el-table-column prop="orderCount" label="订单数" />
      <el-table-column prop="avgAmount" label="客单价" />
      <el-table-column prop="lastOrderDate" label="最近下单" />
      <el-table-column label="类型">
        <template #default="{ row }">
          <ErpStatusTag :status="row.type" :label="commonStatusLabel(row.type, CUSTOMER_TYPE_LABELS)" />
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useReportStore } from '@/stores/report'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'
import { commonStatusLabel, CUSTOMER_TYPE_LABELS } from '@/utils/statusLabels'
import { parsePageItems } from '@/utils/apiPage'

const reportStore = useReportStore()
const analysis = ref<Record<string, unknown>[]>([])
const period = ref<string>('2026-06')
const loading = ref(false)

async function refresh() {
  loading.value = true
  try {
    analysis.value = parsePageItems(await reportStore.loadCustomerAnalysis({ period: period.value })).items as Record<string, unknown>[]
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载失败')
    analysis.value = []
  } finally {
    loading.value = false
  }
}
onMounted(refresh)
</script>
