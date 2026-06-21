<template>
  <div v-loading="loading">
    <h2>应付单详情</h2>
    <el-card v-if="pay">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="应付单号">{{ pay.payableNo }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ pay.vendorName }}</el-descriptions-item>
        <el-descriptions-item label="采购单号">{{ pay.poNo }}</el-descriptions-item>
        <el-descriptions-item label="金额">{{ pay.amount }}</el-descriptions-item>
        <el-descriptions-item label="到期日">{{ pay.dueDate }}</el-descriptions-item>
        <el-descriptions-item label="状态"><ErpStatusTag :status="pay.status" /></el-descriptions-item>
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
const { data: pay, loading } = useDetailLoad<any>((id) => financeStore.getPayable(id))
</script>
