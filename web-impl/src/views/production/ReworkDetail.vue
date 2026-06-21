<template>
  <div v-loading="loading">
    <h2>返修单详情</h2>
    <el-card v-if="rework">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="返修单号">{{ rework.reworkNo }}</el-descriptions-item>
        <el-descriptions-item label="工单号">{{ rework.workorderNo }}</el-descriptions-item>
        <el-descriptions-item label="不良单号">{{ rework.defectNo }}</el-descriptions-item>
        <el-descriptions-item label="优先级"><ErpStatusTag :status="rework.priority" :label="priorityLabel(rework.priority)" /></el-descriptions-item>
        <el-descriptions-item label="状态"><ErpStatusTag :status="rework.status" /></el-descriptions-item>
        <el-descriptions-item label="处理人">{{ rework.assignee }}</el-descriptions-item>
        <el-descriptions-item label="原因" :span="2">{{ rework.reason }}</el-descriptions-item>
        <el-descriptions-item label="处理记录" :span="2">{{ rework.processLog }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
    <el-button style="margin-top: 16px" @click="$router.back()">返回</el-button>
  </div>
</template>

<script setup lang="ts">
import { useBaseStore } from '@/stores/_base'
import { useDetailLoad } from '@/composables/useDetailLoad'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'
import { priorityLabel } from '@/utils/statusLabels'

const { data: rework, loading } = useDetailLoad<any>((id) =>
  useBaseStore().api.get(`/reworks/detail/${id}`),
)
</script>
