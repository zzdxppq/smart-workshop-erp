<template>
  <div>
    <h2>应收账款</h2>
    <el-alert type="info" :closable="false" title="作业人员仅可查看本人相关金额" />
    <el-form :inline="true" class="erp-filter-bar" style="margin-top: 12px">
      <el-form-item label="客户">
        <el-input v-model="customer" clearable placeholder="全部" @keyup.enter="onSearch" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="onSearch">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" class="erp-table" :data="items" stripe>
      <el-table-column prop="receivableNo" label="应收单号" min-width="140" />
      <el-table-column prop="customerName" label="客户" min-width="120" />
      <el-table-column prop="contractNo" label="销售订单号" min-width="150" />
      <el-table-column label="金额" width="120" align="right">
        <template #default="{ row }">
          <MoneyAmount :model-value="Number(row.amount ?? 0)" display-only />
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
          <el-button size="small" @click="$router.push(`/finance/receivable-detail/${row.id}`)">详情</el-button>
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
import MoneyAmount from '@/components/erp/MoneyAmount.vue'
import { useFinanceStore } from '@/stores/finance'
import { usePagedList } from '@/composables/usePagedList'
import { ERP_PAGE_SIZES, ERP_PAGINATION_LAYOUT } from '@/constants/pagination'
import { financeStatusLabel, financeStatusTagType } from '@/utils/statusLabels'

const financeStore = useFinanceStore()
const customer = ref('')

const { items, loading, pageNum, pageSize, total, reload } = usePagedList<any>((params) =>
  financeStore.listReceivables({ customer: customer.value || undefined, ...params }),
)

function onSearch() {
  pageNum.value = 1
  reload({ customer: customer.value || undefined })
}
function onPageChange() {
  reload({ customer: customer.value || undefined })
}

onMounted(onSearch)
</script>
