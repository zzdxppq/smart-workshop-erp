<template>
  <div v-loading="loading">
    <h2>薪酬单详情</h2>
    <el-card v-if="pr">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="员工">{{ pr.empName }}</el-descriptions-item>
        <el-descriptions-item label="期间">{{ pr.period }}</el-descriptions-item>
        <el-descriptions-item label="基本工资">{{ pr.base }}</el-descriptions-item>
        <el-descriptions-item label="加班">{{ pr.overtime }}</el-descriptions-item>
        <el-descriptions-item label="奖金">{{ pr.bonus }}</el-descriptions-item>
        <el-descriptions-item label="扣款">{{ pr.deduction }}</el-descriptions-item>
        <el-descriptions-item label="实发">{{ pr.net }}</el-descriptions-item>
        <el-descriptions-item label="状态"><ErpStatusTag :status="pr.status" /></el-descriptions-item>
      </el-descriptions>
    </el-card>
    <div style="margin-top: 16px">
      <el-button v-if="pr && pr.status !== 'APPROVED'" type="primary" :loading="submitting" @click="approve">审批</el-button>
      <el-button @click="$router.back()">返回</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useHrStore } from '@/stores/hr'
import { useDetailLoad } from '@/composables/useDetailLoad'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'

const hrStore = useHrStore()
const submitting = ref(false)

const { data: pr, loading, load, id } = useDetailLoad<any>((pid) => hrStore.getPayroll(pid))

async function approve() {
  submitting.value = true
  try {
    await hrStore.approvePayroll(id())
    ElMessage.success('已审批')
    await load()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '审批失败')
  } finally {
    submitting.value = false
  }
}
</script>
