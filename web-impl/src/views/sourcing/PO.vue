<template>
  <ErpPageShell title="采购订单" description="由采购申请或询比价定标生成 · 不可独立新建。">
    <el-form :inline="true" class="erp-filter-bar">
      <el-form-item label="采购单号">
        <el-input v-model="keyword" clearable placeholder="PO..." @keyup.enter="onSearch" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="status" clearable placeholder="全部" @change="onSearch">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="已确认" value="CONFIRMED" />
          <el-option label="已关闭" value="CLOSED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button class="erp-btn-secondary" :loading="loading" @click="onSearch">查询</el-button>
        <el-button type="primary" class="erp-btn-primary" @click="$router.push('/sourcing/purchase-transfer')">采购转单</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" class="erp-table" :data="items" stripe>
      <el-table-column prop="poNo" label="采购单号" min-width="140" />
      <el-table-column prop="prNo" label="来源 PR" min-width="130" />
      <el-table-column prop="workorderNo" label="关联工单" min-width="130" />
      <el-table-column prop="vendorName" label="供应商" min-width="120" />
      <el-table-column label="来源" width="100">
        <template #default="{ row }">{{ sourceLabel(row.sourceType) }}</template>
      </el-table-column>
      <el-table-column prop="totalAmount" label="金额" width="120" align="right">
        <template #default="{ row }">
          <MoneyAmount :value="row.totalAmount" />
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <ErpStatusTag :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column prop="deliveryDate" label="交期" width="120" />
      <el-table-column prop="createdAt" label="创建时间" min-width="160" />
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button size="small" class="erp-btn-ghost" @click="$router.push(`/sourcing/po-detail/${row.id}`)">详情</el-button>
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
  </ErpPageShell>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ERP_PAGE_SIZES, ERP_PAGINATION_LAYOUT } from '@/constants/pagination'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import { useSourcingStore } from '@/stores/sourcing'
import { usePagedList } from '@/composables/usePagedList'

const sourcingStore = useSourcingStore()
const keyword = ref('')
const status = ref<string>()

const { items, loading, pageNum, pageSize, total, reload } = usePagedList<any>((params) =>
  sourcingStore.listPos({ keyword: keyword.value, status: status.value || undefined, ...params }),
)

function onSearch() {
  pageNum.value = 1
  reload({ keyword: keyword.value, status: status.value || undefined })
}

function onPageChange() {
  reload({ keyword: keyword.value, status: status.value || undefined })
}

function sourceLabel(t?: string) {
  return ({ FROM_MRP: 'MRP', NO_ORDER: '无订单', FROM_ORDER: '订单' })[t ?? ''] ?? t ?? '—'
}

onMounted(onSearch)
</script>
