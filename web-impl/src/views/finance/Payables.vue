<template>
  <div>
    <h2>应付账款</h2>
    <el-form :inline="true" style="margin-bottom: 12px">
      <el-form-item label="供应商">
        <el-input v-model="vendor" clearable @keyup.enter="onSearch" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="onSearch">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="items" stripe border>
      <el-table-column prop="payableNo" label="应付单号" min-width="140" />
      <el-table-column prop="vendorName" label="供应商" min-width="120" />
      <el-table-column prop="poNo" label="采购单号" min-width="130" />
      <el-table-column prop="amount" label="金额" width="120" align="right">
        <template #default="{ row }">
          <span class="erp-num-highlight">{{ formatAmount(row.amount) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="dueDate" label="到期日" width="120" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="financeStatusTagType(row.status)">{{ financeStatusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="$router.push(`/finance/payable-detail/${row.id}`)">详情</el-button>
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
import { financeStatusLabel, financeStatusTagType } from '@/utils/statusLabels'

const financeStore = useFinanceStore()
const vendor = ref('')

function formatAmount(v: unknown) {
  const n = Number(v ?? 0)
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const { items, loading, pageNum, pageSize, total, reload } = usePagedList<any>((params) =>
  financeStore.listPayables({ vendor: vendor.value || undefined, ...params }),
)

function onSearch() {
  pageNum.value = 1
  reload({ vendor: vendor.value || undefined })
}
function onPageChange() {
  reload({ vendor: vendor.value || undefined })
}

onMounted(onSearch)
</script>
