<template>
  <div>
    <h2>生产扫码历史</h2>
    <el-form :inline="true" style="margin-bottom: 12px">
      <el-form-item label="工单号" required>
        <el-input v-model="workorderNo" clearable placeholder="GD..." @keyup.enter="reload" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="reload">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="items" stripe border>
      <el-table-column prop="scanNo" label="扫码单号" min-width="140" />
      <el-table-column prop="workorderNo" label="工单号" min-width="130" />
      <el-table-column prop="scanType" label="类型" width="90" />
      <el-table-column prop="qty" label="数量" width="80" />
      <el-table-column prop="stepNo" label="工序号" width="90" />
      <el-table-column prop="scannedAt" label="扫码时间" min-width="160" />
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
    const r = await scanStore.getScanHistory(workorderNo.value.trim())
    items.value = parsePageItems(r).items
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载失败')
    items.value = []
  } finally {
    loading.value = false
  }
}
</script>
