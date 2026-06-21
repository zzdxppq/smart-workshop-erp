<template>
  <div v-loading="loading">
    <h2>库位详情</h2>
    <el-descriptions v-if="location" :column="2" border>
      <el-descriptions-item label="库位编码">{{ location.locationCode }}</el-descriptions-item>
      <el-descriptions-item label="所属仓库">{{ location.warehouse }}</el-descriptions-item>
      <el-descriptions-item label="库区">{{ location.zone }}</el-descriptions-item>
      <el-descriptions-item label="库位">{{ location.position }}</el-descriptions-item>
      <el-descriptions-item label="库容">{{ location.capacity }}</el-descriptions-item>
    </el-descriptions>
    <el-button style="margin-top: 16px" @click="$router.back()">返回</el-button>
  </div>
</template>

<script setup lang="ts">
import { useWarehouseStore } from '@/stores/warehouse'
import { useParamLoad } from '@/composables/useDetailLoad'

const warehouseStore = useWarehouseStore()
const { data: location, loading } = useParamLoad<any>('code', (code) => warehouseStore.getLocation(code))
</script>
