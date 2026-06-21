<template>
  <div v-loading="loading">
    <h2>批次追溯</h2>
    <el-card v-if="trace">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="批次号">{{ trace.batchNo }}</el-descriptions-item>
        <el-descriptions-item label="料号">{{ trace.materialCode }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ trace.supplierName }}</el-descriptions-item>
        <el-descriptions-item label="数量">{{ trace.qty }}</el-descriptions-item>
        <el-descriptions-item label="质量状态">{{ trace.qualityStatus }}</el-descriptions-item>
        <el-descriptions-item label="库位">{{ trace.locationCode }}</el-descriptions-item>
        <el-descriptions-item label="入库时间">{{ trace.receivedAt }}</el-descriptions-item>
      </el-descriptions>
      <h4>追溯链路</h4>
      <el-table :data="trace.traceSteps || []" size="small" stripe border>
        <el-table-column prop="stepName" label="节点" />
        <el-table-column prop="operatedAt" label="时间" />
        <el-table-column prop="location" label="库位" />
        <el-table-column prop="qty" label="数量" />
      </el-table>
    </el-card>
    <el-button style="margin-top: 16px" @click="$router.back()">返回</el-button>
  </div>
</template>

<script setup lang="ts">
import { useWarehouseStore } from '@/stores/warehouse'
import { useParamLoad } from '@/composables/useDetailLoad'

const warehouseStore = useWarehouseStore()
const { data: trace, loading } = useParamLoad<any>('batchNo', (batchNo) => warehouseStore.getBatchTrace(batchNo))
</script>
