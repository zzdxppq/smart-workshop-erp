<template>
  <ErpPageShell title="出库单" description="领料/发货/委外出库">
    <el-alert type="info" :closable="false" show-icon title="PC 端出库单管理；现场扫码出库请使用仓管 APP。" style="margin-bottom: 16px" />
    <el-table v-loading="loading" :data="items" stripe border>
      <el-table-column prop="outboundNo" label="出库单号" min-width="140" />
      <el-table-column prop="materialCode" label="料号" min-width="120" />
      <el-table-column prop="qty" label="数量" width="80" align="right" />
      <el-table-column prop="outboundType" label="类型" width="100" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><ErpStatusTag :status="row.status" /></template>
      </el-table-column>
    </el-table>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'
import { useBaseStore } from '@/stores/_base'
import { unwrapResult } from '@/utils/apiPage'

const api = useBaseStore().api
const loading = ref(false)
const items = ref<any[]>([])

onMounted(async () => {
  loading.value = true
  try {
    const data = unwrapResult<any>(await api.get('/warehouses/outbound', { params: { page: 0, size: 20 } }))
    items.value = data?.list ?? []
  } catch {
    items.value = []
  } finally {
    loading.value = false
  }
})
</script>
