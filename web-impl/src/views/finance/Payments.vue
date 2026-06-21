<template>
  <div>
    <h2>付款管理</h2>
    <el-form :inline="true" style="margin-bottom: 12px">
      <el-form-item label="供应商">
        <el-input v-model="vendor" clearable @keyup.enter="onSearch" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="status" clearable placeholder="全部" @change="onSearch">
          <el-option label="待审" value="PENDING" />
          <el-option label="已审" value="APPROVED" />
          <el-option label="已付" value="PAID" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="onSearch">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="items" stripe border>
      <el-table-column prop="paymentNo" label="付款单号" min-width="140" />
      <el-table-column prop="vendorName" label="供应商" min-width="120" />
      <el-table-column prop="payableNo" label="应付单" min-width="130" />
      <el-table-column label="金额" width="140">
        <template #default="{ row }">
          <MoneyAmount :model-value="Number(row.amount ?? 0)" display-only />
        </template>
      </el-table-column>
      <el-table-column label="审批" width="180">
        <template #default="{ row }">
          <ApprovalChainRenderer v-if="row.status === 'PENDING'" :nodes="[{ title: '财务', status: 'PENDING' }]" />
          <el-tag v-else type="success">已审</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <ErpStatusTag :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="$router.push(`/finance/payment-detail/${row.id}`)">详情</el-button>
          <el-button v-if="row.status === 'PENDING'" size="small" type="success" @click="approve(row.id)">审批</el-button>
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
import { ElMessage } from 'element-plus'
import { useFinanceStore } from '@/stores/finance'
import { usePagedList } from '@/composables/usePagedList'
import { ERP_PAGE_SIZES, ERP_PAGINATION_LAYOUT } from '@/constants/pagination'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'

const financeStore = useFinanceStore()
const vendor = ref('')
const status = ref('')

const { items, loading, pageNum, pageSize, total, reload } = usePagedList<any>((params) =>
  financeStore.listPayments({
    vendor: vendor.value || undefined,
    status: status.value || undefined,
    ...params,
  }),
)

function filters() {
  return { vendor: vendor.value || undefined, status: status.value || undefined }
}

function onSearch() {
  pageNum.value = 1
  reload(filters())
}
function onPageChange() {
  reload(filters())
}

async function approve(id: number) {
  try {
    await financeStore.approvePayment(id)
    ElMessage.success('已审批')
    onPageChange()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '审批失败')
  }
}

onMounted(onSearch)
</script>
