<template>
  <div v-loading="loading">
    <h2>物料供应商比价</h2>
    <el-table :data="compareData" stripe>
      <el-table-column prop="materialCode" label="料号" />
      <el-table-column prop="vendor1" label="供应商A" />
      <el-table-column prop="price1" label="A 单价" />
      <el-table-column prop="vendor2" label="供应商B" />
      <el-table-column prop="price2" label="B 单价" />
      <el-table-column prop="diff" label="差价" />
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useDashboardStore } from '@/stores/dashboard'
import { useDashboardList } from '@/composables/useDashboardData'

const dashboardStore = useDashboardStore()
const { items: compareData, loading, load } = useDashboardList(() => dashboardStore.loadMaterialVendorCompare({}))

onMounted(load)
</script>
