<template>
  <div v-loading="loading">
    <h2>委外状态详情</h2>
    <el-card v-if="state">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="委外单号">{{ state.outsourceNo }}</el-descriptions-item>
        <el-descriptions-item label="当前状态">
          <ErpStatusTag :status="state.status" />
        </el-descriptions-item>
        <el-descriptions-item label="工单号">{{ state.workorderNo }}</el-descriptions-item>
        <el-descriptions-item label="供应商 ID">{{ state.supplierId }}</el-descriptions-item>
        <el-descriptions-item label="料号">{{ state.materialCode }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ state.updatedAt }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
    <el-button style="margin-top: 16px" @click="$router.back()">返回</el-button>
  </div>
</template>

<script setup lang="ts">
import { useBaseStore } from '@/stores/_base'
import { useDetailLoad } from '@/composables/useDetailLoad'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'

const { data: state, loading } = useDetailLoad<any>((id) =>
  useBaseStore().api.get(`/outsource-states/${id}`),
)
</script>
