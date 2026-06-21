<template>
  <div>
    <h2>账龄分析</h2>
    <el-form :inline="true" class="erp-filter-bar">
      <el-form-item label="类型">
        <el-select
          v-model="type"
          clearable
          placeholder="全部"
          style="width: 140px"
          @change="onSearch"
        >
          <el-option label="应收" value="RECEIVABLE" />
          <el-option label="应付" value="PAYABLE" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="onSearch">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" class="erp-table" :data="items" stripe>
      <el-table-column prop="bucket" label="账龄段" min-width="120" />
      <el-table-column label="金额" width="120" align="right">
        <template #default="{ row }">
          <span class="erp-num-highlight">{{ formatAmount(row.amount) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="count" label="笔数" width="80" align="right" />
      <el-table-column prop="percentage" label="占比" width="100">
        <template #default="{ row }">
          {{ row.percentage }}%
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="$router.push(`/finance/aging-detail/${row.id}`)">明细</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      :page-sizes="ERP_PAGE_SIZES"
      :layout="ERP_PAGINATION_LAYOUT"
      background
      class="erp-pagination"
      @current-change="onPageChange"
      @size-change="onSearch"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useFinanceStore } from '@/stores/finance'
import { usePagedList } from '@/composables/usePagedList'
import { ERP_PAGE_SIZES, ERP_PAGINATION_LAYOUT } from '@/constants/pagination'

const financeStore = useFinanceStore()
const type = ref<string | undefined>()

function formatAmount(v: unknown) {
  const n = Number(v ?? 0)
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const { items, loading, pageNum, pageSize, total, reload } = usePagedList<any>((params) =>
  financeStore.listAgings({
    type: type.value || 'RECEIVABLE',
    ...params,
  }),
)

function onSearch() {
  pageNum.value = 1
  reload({ type: type.value || 'RECEIVABLE' })
}
function onPageChange() {
  reload({ type: type.value || 'RECEIVABLE' })
}

onMounted(onSearch)
</script>
