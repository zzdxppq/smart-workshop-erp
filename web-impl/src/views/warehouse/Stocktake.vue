<template>
  <ErpPageShell title="盘点单" description="周期盘点 · 账实核对">
    <el-button type="primary" style="margin-bottom: 16px" @click="createStocktake">新建盘点单</el-button>
    <el-table v-loading="loading" :data="items" stripe border>
      <el-table-column prop="stocktakeNo" label="盘点单号" min-width="140" />
      <el-table-column prop="warehouseCode" label="仓库" width="100" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <ErpStatusTag :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" min-width="160" />
    </el-table>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'
import { useBaseStore } from '@/stores/_base'
import { unwrapResult } from '@/utils/apiPage'

const api = useBaseStore().api
const loading = ref(false)
const items = ref<any[]>([])

async function load() {
  loading.value = true
  try {
    const data = unwrapResult<any>(await api.get('/warehouses/stocktake'))
    items.value = data?.list ?? []
  } catch {
    items.value = []
  } finally {
    loading.value = false
  }
}

async function createStocktake() {
  try {
    await api.post('/warehouses/stocktake', { warehouseCode: 'WH-01' })
    ElMessage.success('盘点单已创建')
    await load()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '创建失败')
  }
}

onMounted(load)
</script>
