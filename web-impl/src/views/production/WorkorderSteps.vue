<template>
  <div v-loading="loading">
    <h2>工单工序</h2>
    <el-table :data="steps" stripe border>
      <el-table-column prop="stepNo" label="工序号" width="90" />
      <el-table-column prop="stepName" label="工序名称" min-width="140" />
      <el-table-column prop="equipmentType" label="机台类型" min-width="120" />
      <el-table-column prop="estimatedMinutes" label="预计工时 (分钟)" width="130" />
      <el-table-column prop="actualMinutes" label="实际工时 (分钟)" width="130" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><ErpStatusTag :status="row.status" /></template>
      </el-table-column>
    </el-table>
    <el-button style="margin-top: 16px" @click="$router.back()">返回</el-button>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useWorkorderStore } from '@/stores/workorder'
import { parsePageItems } from '@/utils/apiPage'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'

const route = useRoute()
const workorderStore = useWorkorderStore()
const steps = ref<any[]>([])
const loading = ref(false)

onMounted(async () => {
  const id = Number(route.params.id)
  if (!id) {
    ElMessage.error('无效的工单 ID')
    return
  }
  loading.value = true
  try {
    const r = await workorderStore.listSteps(id)
    steps.value = parsePageItems(r).items
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载失败')
  } finally {
    loading.value = false
  }
})
</script>
