<template>
  <div>
    <h2>扫码历史</h2>
    <el-form :inline="true" style="margin-bottom: 12px">
      <el-form-item label="条码/物料">
        <el-input v-model="keyword" clearable @keyup.enter="onSearch" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="onSearch">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="items" stripe border>
      <el-table-column prop="scanNo" label="扫码单号" min-width="140" />
      <el-table-column prop="scanType" label="类型" width="90" />
      <el-table-column prop="barcodeNo" label="条码号" min-width="140" />
      <el-table-column prop="materialCode" label="料号" min-width="120" />
      <el-table-column prop="locationCode" label="库位" width="100" />
      <el-table-column prop="qty" label="数量" width="80" />
      <el-table-column prop="syncStatus" label="同步状态" width="100" />
      <el-table-column prop="scannedAt" label="扫码时间" min-width="160" />
    </el-table>
    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      style="margin-top: 12px"
      @current-change="onPageChange"
      @size-change="onSearch"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useWarehouseStore } from '@/stores/warehouse'
import { usePagedList } from '@/composables/usePagedList'

const warehouseStore = useWarehouseStore()
const keyword = ref('')

const { items, loading, pageNum, pageSize, total, reload } = usePagedList<any>((params) =>
  warehouseStore.listScans({ keyword: keyword.value || undefined, ...params }),
)

function onSearch() {
  pageNum.value = 1
  reload({ keyword: keyword.value || undefined })
}
function onPageChange() {
  reload({ keyword: keyword.value || undefined })
}

onMounted(onSearch)
</script>
