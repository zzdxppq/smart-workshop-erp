<template>
  <div>
    <h2>批次列表</h2>
    <el-form :inline="true" style="margin-bottom: 12px">
      <el-form-item label="料号">
        <el-input v-model="materialCode" clearable @keyup.enter="onSearch" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="onSearch">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="items" stripe border>
      <el-table-column prop="batchNo" label="批次号" min-width="140" />
      <el-table-column prop="materialCode" label="料号" min-width="120" />
      <el-table-column prop="supplierName" label="供应商" min-width="120" />
      <el-table-column prop="quantity" label="数量" width="80">
        <template #default="{ row }">{{ row.quantity ?? row.qty }}</template>
      </el-table-column>
      <el-table-column prop="qualityStatus" label="质量状态" width="100" />
      <el-table-column prop="arrivedAt" label="到货时间" min-width="160">
        <template #default="{ row }">{{ row.arrivedAt ?? row.receivedAt ?? '—' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="$router.push(`/warehouse/batch-trace/${row.batchNo}`)">追溯</el-button>
        </template>
      </el-table-column>
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
const materialCode = ref('WL-0001')

const { items, loading, pageNum, pageSize, total, reload } = usePagedList<any>((params) =>
  warehouseStore.listBatchesFefo({ materialCode: materialCode.value || undefined, ...params }),
)

function onSearch() {
  pageNum.value = 1
  reload()
}
function onPageChange() {
  reload()
}

onMounted(onSearch)
</script>
