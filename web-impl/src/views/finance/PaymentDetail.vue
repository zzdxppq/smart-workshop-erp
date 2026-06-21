<template>
  <div v-loading="loading">
    <h2>付款单详情</h2>
    <el-card v-if="pay">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="付款单号">{{ pay.paymentNo }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ pay.vendorName }}</el-descriptions-item>
        <el-descriptions-item label="应付单号">{{ pay.payableNo }}</el-descriptions-item>
        <el-descriptions-item label="金额">{{ pay.amount }}</el-descriptions-item>
        <el-descriptions-item label="付款方式">{{ pay.method }}</el-descriptions-item>
        <el-descriptions-item label="状态"><ErpStatusTag :status="pay.status" /></el-descriptions-item>
        <el-descriptions-item label="审批人">{{ pay.approver }}</el-descriptions-item>
        <el-descriptions-item label="审批时间">{{ pay.approvedAt }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
    <el-button style="margin-top: 16px" @click="$router.back()">返回</el-button>
  </div>
</template>

<script setup lang="ts">
import { useFinanceStore } from '@/stores/finance'
import { useDetailLoad } from '@/composables/useDetailLoad'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'

const financeStore = useFinanceStore()
const { data: pay, loading } = useDetailLoad<any>((id) => financeStore.getPayment(id))
</script>
