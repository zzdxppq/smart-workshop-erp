<template>
  <div v-loading="loading">
    <h2>工单看板详情</h2>
    <el-card v-if="wo">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="工单号">{{ wo.workorderNo }}</el-descriptions-item>
        <el-descriptions-item label="料号">{{ wo.materialCode }}</el-descriptions-item>
        <el-descriptions-item label="数量">{{ wo.qty }}</el-descriptions-item>
        <el-descriptions-item label="完工数">{{ wo.finishedQty }}</el-descriptions-item>
        <el-descriptions-item label="进度">{{ wo.progress }}%</el-descriptions-item>
        <el-descriptions-item label="状态"><ErpStatusTag :status="wo.status" /></el-descriptions-item>
      </el-descriptions>
    </el-card>
    <h3 style="margin-top: 16px">工序进度</h3>
    <el-table :data="wo?.steps || []" stripe>
      <el-table-column prop="stepNo" label="工序号" />
      <el-table-column prop="stepName" label="工序" />
      <el-table-column label="状态">
        <template #default="{ row }"><ErpStatusTag :status="row.status" /></template>
      </el-table-column>
      <el-table-column prop="operator" label="操作员" />
      <el-table-column prop="startTime" label="开始" />
      <el-table-column prop="endTime" label="结束" />
    </el-table>
    <el-button style="margin-top: 16px" @click="$router.back()">返回</el-button>
  </div>
</template>

<script setup lang="ts">
import { useBaseStore } from '@/stores/_base'
import { useDetailLoad } from '@/composables/useDetailLoad'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'

const { data: wo, loading } = useDetailLoad<{
  workorderNo?: string
  materialCode?: string
  qty?: number
  finishedQty?: number
  progress?: number
  status?: string
  steps?: Record<string, unknown>[]
}>((id) =>
  useBaseStore().api.get(`/dashboard/production/workorder/${id}`)
)
</script>
