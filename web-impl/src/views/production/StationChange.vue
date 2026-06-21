<template>
  <div>
    <h2>过站流转</h2>
    <el-form :inline="true" style="margin-bottom: 12px">
      <el-form-item label="工单号" required>
        <el-input v-model="workorderNo" clearable placeholder="GD..." @keyup.enter="reload" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="reload">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="items" stripe border>
      <el-table-column prop="transferNo" label="过站单号" min-width="140" />
      <el-table-column prop="workorderNo" label="工单号" min-width="130" />
      <el-table-column prop="fromStepNo" label="源工序" width="90" />
      <el-table-column prop="toStepNo" label="目标工序" width="100" />
      <el-table-column prop="fromEquipmentId" label="源机台" width="100" />
      <el-table-column prop="toEquipmentId" label="目标机台" width="100" />
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useProductionScanStore } from '@/stores/productionScan'
import { parsePageItems } from '@/utils/apiPage'

const scanStore = useProductionScanStore()
const workorderNo = ref('')
const items = ref<any[]>([])
const loading = ref(false)

async function reload() {
  if (!workorderNo.value.trim()) {
    ElMessage.warning('请输入工单号')
    return
  }
  loading.value = true
  try {
    const r = await scanStore.getStationHistory(workorderNo.value.trim())
    items.value = parsePageItems(r).items
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载失败')
    items.value = []
  } finally {
    loading.value = false
  }
}
</script>
