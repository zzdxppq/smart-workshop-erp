<template>
  <ErpPageShell title="库存查询" description="按料号 / 库位 / 批次多维检索">
    <el-form :inline="true" class="erp-filter-bar">
      <el-form-item label="料号">
        <el-input v-model="materialCode" clearable @keyup.enter="reload" />
      </el-form-item>
      <el-form-item label="库位">
        <el-input v-model="locationCode" clearable @keyup.enter="reload" />
      </el-form-item>
      <el-form-item label="批次">
        <el-input v-model="batchNo" clearable @keyup.enter="reload" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="reload">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="items" stripe border>
      <el-table-column prop="materialCode" label="料号" min-width="120" />
      <el-table-column prop="locationCode" label="库位" min-width="120" />
      <el-table-column prop="batchNo" label="批次" min-width="140" />
      <el-table-column prop="barcodeNo" label="条码号" min-width="200" />
      <el-table-column prop="qty" label="在库数量" width="100" align="right" />
    </el-table>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import { useWarehouseStore } from '@/stores/warehouse'
import { parsePageItems } from '@/utils/apiPage'

const warehouseStore = useWarehouseStore()
const materialCode = ref('')
const locationCode = ref('')
const batchNo = ref('')
const loading = ref(false)
const items = ref<any[]>([])

async function reload() {
  loading.value = true
  try {
    const r = await warehouseStore.listBatchesFefo({
      materialCode: materialCode.value || undefined,
      batchNo: batchNo.value || undefined,
      pageNum: 1,
      pageSize: 50,
    })
    const { items: list } = parsePageItems(r)
    items.value = list
  } finally {
    loading.value = false
  }
}

onMounted(reload)
</script>
