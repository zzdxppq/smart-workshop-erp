<template>
  <div v-loading="loading">
    <h2>供应商切换详情</h2>
    <el-card v-if="sw">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="切换单号">{{ sw.switchNo }}</el-descriptions-item>
        <el-descriptions-item label="委外单号">{{ sw.outsourceNo }}</el-descriptions-item>
        <el-descriptions-item label="原供应商">{{ sw.oldVendor }}</el-descriptions-item>
        <el-descriptions-item label="新供应商">{{ sw.newVendor }}</el-descriptions-item>
        <el-descriptions-item label="原因" :span="2">{{ sw.reason }}</el-descriptions-item>
        <el-descriptions-item label="生管确认">{{ sw.prodConfirmed ? '已确认' : '未确认' }}</el-descriptions-item>
        <el-descriptions-item label="采购确认">{{ sw.purchConfirmed ? '已确认' : '未确认' }}</el-descriptions-item>
        <el-descriptions-item label="状态"><ErpStatusTag :status="sw.status" /></el-descriptions-item>
      </el-descriptions>
    </el-card>
    <div style="margin-top: 16px">
      <el-button
        type="primary"
        :disabled="!sw || sw.prodConfirmed"
        :loading="confirmingProd"
        @click="confirmProd"
      >生管确认</el-button>
      <el-button
        type="success"
        :disabled="!sw || sw.purchConfirmed"
        :loading="confirmingPurch"
        @click="confirmPurch"
      >采购确认</el-button>
      <el-button @click="$router.back()">返回</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useBaseStore } from '@/stores/_base'
import { useDetailLoad } from '@/composables/useDetailLoad'
import { unwrapResult } from '@/utils/apiPage'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'

const route = useRoute()
const confirmingProd = ref(false)
const confirmingPurch = ref(false)

const { data: sw, loading } = useDetailLoad<any>((id) =>
  useBaseStore().api.get(`/outsource-switches/${id}`),
)

async function confirmProd() {
  confirmingProd.value = true
  try {
    const r = await useBaseStore().api.post(`/outsource-switches/${route.params.id}/confirm-prod`)
    sw.value = unwrapResult(r)
    ElMessage.success('生管已确认')
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '确认失败')
  } finally {
    confirmingProd.value = false
  }
}

async function confirmPurch() {
  confirmingPurch.value = true
  try {
    const r = await useBaseStore().api.post(`/outsource-switches/${route.params.id}/confirm-purch`)
    sw.value = unwrapResult(r)
    ElMessage.success('采购已确认')
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '确认失败')
  } finally {
    confirmingPurch.value = false
  }
}
</script>
