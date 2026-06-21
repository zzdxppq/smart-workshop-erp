<template>
  <div v-loading="loading">
    <h2>物料价格看板</h2>
    <el-table :data="priceData" stripe>
      <el-table-column prop="materialCode" label="料号" />
      <el-table-column prop="materialName" label="物料名称" />
      <el-table-column prop="price" label="当前价" />
      <el-table-column prop="change" label="环比" />
      <el-table-column prop="supplierName" label="供应商" />
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useDashboardStore } from '@/stores/dashboard'
import { useDashboardList } from '@/composables/useDashboardData'

const dashboardStore = useDashboardStore()
const { items: priceData, loading, load } = useDashboardList(() => dashboardStore.loadMaterialPrice({}))

onMounted(load)
</script>
