<template>
  <div>
    <h2>合同回款</h2>
    <p class="page-hint">按 PRD FR-2-4：每条记录对应一张销售订单；销售维护回款计划，收款由财务在「应收账款」登记、销售仅可查看。</p>
    <el-form :inline="true" style="margin-bottom: 12px">
      <el-form-item label="客户/订单号">
        <el-input v-model="keyword" clearable placeholder="订单号、客户、联系人、电话" @keyup.enter="onSearch" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="onSearch">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="items" stripe border>
      <el-table-column prop="orderNo" label="订单号" min-width="150">
        <template #default="{ row }">
          <router-link
            v-if="row.orderId"
            :to="`/sales/orders/${row.orderId}`"
            class="erp-link"
          >{{ row.orderNo ?? row.contractNo }}</router-link>
          <span v-else>{{ row.orderNo ?? row.contractNo ?? '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="customerName" label="客户" min-width="120" />
      <el-table-column prop="contactName" label="联系人" width="100" />
      <el-table-column prop="contactPhone" label="联系电话" width="120" />
      <el-table-column prop="amount" label="订单金额" width="140">
        <template #default="{ row }">
          <MoneyAmount :model-value="Number(row.amount ?? 0)" display-only />
        </template>
      </el-table-column>
      <el-table-column prop="receivedAmount" label="已回款" width="120">
        <template #default="{ row }">
          <span class="erp-num-highlight">{{ row.receivedAmount ?? 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="未回款" width="120">
        <template #default="{ row }">
          <span :class="(row.amount ?? 0) - (row.receivedAmount ?? 0) > 0 ? 'erp-num-warn' : 'erp-num-highlight'">
            {{ (row.amount ?? 0) - (row.receivedAmount ?? 0) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="回款进度" width="120">
        <template #default="{ row }">
          <el-progress
            :percentage="row.amount ? Math.round((Number(row.receivedAmount ?? 0) / Number(row.amount)) * 100) : 0"
            :color="progressColor(row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="毛利率" width="90">
        <template #default="{ row }">
          <span v-if="row.marginRate != null">{{ formatRate(row.marginRate) }}</span>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <ErpStatusTag :status="row.status" :label="statusText(row.status)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.id" size="small" link @click="$router.push(`/sales/contracts/${row.id}/payment-plan`)">回款计划</el-button>
          <el-button v-if="row.id" size="small" link @click="$router.push(`/sales/contracts/${row.id}/payment-reg`)">查看收款</el-button>
          <el-button v-if="row.id" size="small" link @click="$router.push(`/sales/contracts/${row.id}/profit`)">利润</el-button>
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
      style="margin-top: 12px"
      @current-change="onPageChange"
      @size-change="onSearch"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ERP_PAGE_SIZES, ERP_PAGINATION_LAYOUT } from '@/constants/pagination'
import { useBaseStore } from '@/stores/_base'
import { usePagedList } from '@/composables/usePagedList'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'
import MoneyAmount from '@/components/erp/MoneyAmount.vue'

const keyword = ref('')

const { items, loading, pageNum, pageSize, total, reload } = usePagedList<any>((params) =>
  useBaseStore().api.get('/contracts', {
    params: { keyword: keyword.value || undefined, ...params },
  }),
)

function statusText(status?: string): string {
  const map: Record<string, string> = {
    DRAFT: '草稿',
    PENDING: '待回款',
    PARTIAL: '部分回款',
    RECEIVED: '已回齐',
    OVERDUE: '已逾期',
    CLOSED: '已关闭',
  }
  return status ? (map[status] ?? status) : ''
}

function progressColor(row: Record<string, unknown>): string {
  const amount = Number(row.amount ?? 0)
  const received = Number(row.receivedAmount ?? 0)
  if (amount === 0) return '#e6a23c'
  const pct = (received / amount) * 100
  if (pct >= 100) return '#67c23a'
  if (pct >= 50) return '#409eff'
  return '#e6a23c'
}

function formatRate(rate: unknown) {
  const n = Number(rate)
  if (Number.isNaN(n)) return '—'
  return `${(n <= 1 ? n * 100 : n).toFixed(1)}%`
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

<style scoped>
.page-hint {
  margin: 0 0 12px;
  font-size: 13px;
  color: var(--erp-text-muted);
}
.erp-link {
  color: var(--erp-color-primary);
  text-decoration: none;
}
.erp-link:hover {
  text-decoration: underline;
}
</style>
