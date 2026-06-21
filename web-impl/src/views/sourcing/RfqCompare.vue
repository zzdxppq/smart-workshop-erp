<template>
  <div v-loading="loading">
    <h2>RFQ 比价</h2>
    <el-card v-if="comparison">
      <h3>比价结果</h3>
      <el-table :data="comparison.quotes || []" stripe border>
        <el-table-column prop="vendorName" label="供应商" />
        <el-table-column prop="unitPrice" label="单价" />
        <el-table-column prop="totalPrice" label="总价" />
        <el-table-column prop="leadTime" label="交期" />
        <el-table-column prop="quality" label="质量" />
        <el-table-column prop="score" label="综合得分">
          <template #default="{ row }">
            <el-progress :percentage="row.score" />
          </template>
        </el-table-column>
        <el-table-column label="推荐" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.recommended" type="success">推荐</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-button style="margin-top: 16px" @click="$router.back()">返回</el-button>
  </div>
</template>

<script setup lang="ts">
import { useSourcingStore } from '@/stores/sourcing'
import { useDetailLoad } from '@/composables/useDetailLoad'

const sourcingStore = useSourcingStore()
const { data: comparison, loading } = useDetailLoad<any>((id) => sourcingStore.compareRfq(id))
</script>
