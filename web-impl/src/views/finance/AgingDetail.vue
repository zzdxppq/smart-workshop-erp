<template>
  <div v-loading="loading">
    <h2>账龄明细</h2>
    <el-card v-if="aging">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="账龄段">{{ aging.bucket }}</el-descriptions-item>
        <el-descriptions-item label="金额">{{ aging.amount }}</el-descriptions-item>
        <el-descriptions-item label="笔数">{{ aging.count }}</el-descriptions-item>
        <el-descriptions-item label="占比">{{ aging.percentage }}%</el-descriptions-item>
      </el-descriptions>
    </el-card>
    <h3 style="margin-top: 16px">账龄内单据</h3>
    <el-table :data="aging?.bills || []" stripe border>
      <el-table-column prop="billNo" label="单据号" />
      <el-table-column prop="partyName" label="对方" />
      <el-table-column prop="amount" label="金额" />
      <el-table-column prop="dueDate" label="到期日" />
      <el-table-column prop="overdueDays" label="逾期天数" />
    </el-table>
    <el-button style="margin-top: 16px" @click="$router.back()">返回</el-button>
  </div>
</template>

<script setup lang="ts">
import { useFinanceStore } from '@/stores/finance'
import { useDetailLoad } from '@/composables/useDetailLoad'

const financeStore = useFinanceStore()
const { data: aging, loading } = useDetailLoad<any>((id) => financeStore.getAging(id))
</script>
