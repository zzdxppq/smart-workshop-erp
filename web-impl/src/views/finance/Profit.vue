<template>
  <div>
    <h2>利润分析</h2>
    <el-form :inline="true">
      <el-form-item label="期间">
        <el-date-picker v-model="period" type="month" value-format="YYYY-MM" @change="onSearch" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="onSearch">查询</el-button>
        <el-button type="success" @click="$router.push('/finance/profit-export')">导出</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="items" stripe border style="margin-top: 12px">
      <el-table-column prop="productName" label="产品" min-width="140" />
      <el-table-column prop="revenue" label="收入" width="110" />
      <el-table-column prop="cost" label="成本" width="110" />
      <el-table-column prop="grossProfit" label="毛利" width="110" />
      <el-table-column prop="netProfit" label="净利" width="110" />
      <el-table-column prop="margin" label="利润率" width="100">
        <template #default="{ row }">
          <span :class="marginClass(row.margin)">{{ row.margin }}%</span>
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
import type { OrderProfit } from '@/api/generated/models/OrderProfit'

const financeStore = useFinanceStore()
const period = ref<string>('2026-06')

const { items, loading, pageNum, pageSize, total, reload } = usePagedList<OrderProfit>((params) =>
  financeStore.listProfits({ period: period.value, ...params }),
)

function onSearch() {
  pageNum.value = 1
  reload({ period: period.value })
}
function onPageChange() {
  reload({ period: period.value })
}

function marginClass(margin?: number) {
  const m = Number(margin ?? 0)
  if (m < 0) return 'margin-critical'
  if (m < 5) return 'margin-danger'
  if (m < 10) return 'margin-warning'
  return ''
}

onMounted(onSearch)
</script>

<style scoped>
.margin-warning { color: #bf8700; font-weight: 600; }
.margin-danger { color: #cf222e; font-weight: 600; }
.margin-critical { color: #cf222e; font-weight: 700; background: #ffebe9; padding: 0 4px; }
</style>
