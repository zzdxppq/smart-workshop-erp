<template>
  <div v-loading="loading">
    <h2>返修告警中心</h2>
    <el-alert type="error" :closable="false" title="紧急返修单 - 需立即处理" />
    <el-table :data="alerts" stripe>
      <el-table-column prop="reworkNo" label="返修单号" />
      <el-table-column prop="workorderNo" label="工单" />
      <el-table-column prop="reason" label="原因" />
      <el-table-column prop="alertedAt" label="告警时间" />
      <el-table-column label="状态">
        <template #default="{ row }">
          <ErpStatusTag :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button size="small" type="danger" @click="handleAlert(row.id)">处理告警</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useBaseStore } from '@/stores/_base'
import { parsePageItems } from '@/utils/apiPage'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'

const alerts = ref<Record<string, unknown>[]>([])
const loading = ref(false)

async function refresh() {
  loading.value = true
  try {
    alerts.value = parsePageItems(await useBaseStore().api.get('/reworks/alerts')).items as Record<string, unknown>[]
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载失败')
    alerts.value = []
  } finally {
    loading.value = false
  }
}

async function handleAlert(id: number) {
  try {
    await useBaseStore().api.post(`/reworks/alerts/${id}/ack`)
    ElMessage.success('告警已确认处理')
    await refresh()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '操作失败')
  }
}

onMounted(refresh)
</script>
