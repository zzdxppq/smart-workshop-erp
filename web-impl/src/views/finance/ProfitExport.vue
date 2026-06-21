<template>
  <div>
    <h2>利润表导出</h2>
    <el-form label-width="120px">
      <el-form-item label="期间" required>
        <el-date-picker v-model="period" type="month" />
      </el-form-item>
      <el-form-item label="格式">
        <el-select v-model="format">
          <el-option label="Excel (XLSX)" value="XLSX" />
          <el-option label="CSV" value="CSV" />
          <el-option label="PDF" value="PDF" />
        </el-select>
      </el-form-item>
      <el-form-item label="粒度">
        <el-select v-model="granularity">
          <el-option label="产品" value="PRODUCT" />
          <el-option label="客户" value="CUSTOMER" />
          <el-option label="业务员" value="SALESMAN" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="exportData">导出</el-button>
        <el-button @click="$router.back()">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useFinanceStore } from '@/stores/finance'
import { unwrapResult } from '@/utils/apiPage'

const financeStore = useFinanceStore()
const period = ref<string>('2026-06')
const format = ref<'XLSX' | 'CSV' | 'PDF'>('XLSX')
const granularity = ref<'PRODUCT' | 'CUSTOMER' | 'SALESMAN'>('PRODUCT')

async function exportData() {
  try {
    const exported = unwrapResult<{ fileName?: string }>(await financeStore.exportProfit({
      period: period.value,
      format: format.value,
      granularity: granularity.value,
    }))
    ElMessage.success(`导出已启动：${exported.fileName ?? '文件已生成'}`)
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '导出失败')
  }
}
</script>