<template>
  <ErpPageShell title="库存与安全库存" description="当前库存水位可视化 · 低于下限红色预警 · 高于上限橙色提示。">
    <el-form :inline="true" class="erp-filter-bar">
      <el-form-item label="料号">
        <el-input v-model="keyword" clearable placeholder="WL-..." @keyup.enter="onSearch" />
      </el-form-item>
      <el-form-item>
        <el-button class="erp-btn-secondary" :loading="loading" @click="onSearch">查询</el-button>
        <el-button class="erp-btn-ghost" @click="$router.push('/warehouse/inventory-alert')">查看预警</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" class="erp-table" :data="items" stripe>
      <el-table-column prop="materialCode" label="料号" min-width="130" />
      <el-table-column prop="materialName" label="物料名称" min-width="140" />
      <el-table-column label="库存水位" width="160">
        <template #default="{ row }">
          <ErpStockLevel :current="row.currentQty" :min="row.minQty" :max="row.maxQty" />
        </template>
      </el-table-column>
      <el-table-column prop="minQty" label="安全下限" width="100" align="right">
        <template #default="{ row }">
          <span class="erp-num-highlight">{{ row.minQty ?? '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="maxQty" label="安全上限" width="100" align="right">
        <template #default="{ row }">
          <span class="erp-num-highlight">{{ row.maxQty ?? '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="reorderQty" label="补货量" width="90" align="right">
        <template #default="{ row }">
          <span class="erp-num-highlight">{{ row.reorderQty ?? '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <ErpStatusTag :status="stockStatus(row)" />
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      class="erp-pagination"
      @current-change="onPageChange"
      @size-change="onSearch"
    />
  </ErpPageShell>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import { useInventoryStore } from '@/stores/inventory'
import { usePagedList } from '@/composables/usePagedList'

const inventoryStore = useInventoryStore()
const keyword = ref('')

const { items, loading, pageNum, pageSize, total, reload } = usePagedList<any>((params) =>
  inventoryStore.listSafetyConfigs({ keyword: keyword.value || undefined, ...params }),
)

function stockStatus(row: { currentQty?: number; minQty?: number; maxQty?: number }) {
  const cur = Number(row.currentQty ?? 0)
  const min = Number(row.minQty ?? 0)
  const max = Number(row.maxQty ?? 0)
  if (cur < min) return 'FAIL'
  if (max > 0 && cur > max) return 'WARN'
  return 'PASS'
}

function onSearch() {
  pageNum.value = 1
  reload({ keyword: keyword.value || undefined })
}
function onPageChange() {
  reload({ keyword: keyword.value || undefined })
}

onMounted(onSearch)
</script>
