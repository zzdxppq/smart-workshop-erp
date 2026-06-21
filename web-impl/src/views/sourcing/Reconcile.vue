<template>
  <ErpPageShell title="月度对账中心" description="四步系统内流程 · 不含线下动作（V1.3.7 红线）。">
    <el-alert type="warning" :closable="false" title="对账为系统内四步流程，不含线下操作" style="margin-bottom: 12px" />
    <el-form :inline="true" class="erp-filter-bar">
      <el-form-item label="期间">
        <el-date-picker v-model="period" type="month" placeholder="选择月份" value-format="YYYY-MM" @change="onSearch" />
      </el-form-item>
      <el-form-item label="供应商">
        <el-input v-model="vendorKeyword" placeholder="供应商名称/编码" clearable @keyup.enter="onSearch" />
      </el-form-item>
      <el-form-item>
        <el-button class="erp-btn-secondary" :loading="loading" @click="onSearch">查询</el-button>
        <el-button type="primary" class="erp-btn-primary" @click="goCreate">新建对账单</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" class="erp-table" :data="items" stripe>
      <el-table-column prop="reconcileNo" label="对账单号" min-width="140" />
      <el-table-column prop="period" label="期间" width="100" />
      <el-table-column prop="vendorName" label="供应商" min-width="120" />
      <el-table-column prop="totalAmount" label="对账金额" width="120" align="right">
        <template #default="{ row }">
          <MoneyAmount :value="row.totalAmount" />
        </template>
      </el-table-column>
      <el-table-column label="当前步骤" width="120">
        <template #default="{ row }">
          <ErpStatusTag :status="row.step" />
        </template>
      </el-table-column>
      <el-table-column label="厂商确认" width="90" align="center">
        <template #default="{ row }">
          <ErpStatusTag :status="row.vendorSigned ? 'PASS' : 'PENDING'" />
        </template>
      </el-table-column>
      <el-table-column label="财务审核" width="90" align="center">
        <template #default="{ row }">
          <ErpStatusTag :status="row.financeSigned ? 'APPROVED' : 'PENDING'" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" class="erp-btn-ghost" @click="$router.push(`/sourcing/reconcile-detail/${row.id}`)">详情</el-button>
          <el-button
            v-if="canSign(row.step)"
            size="small"
            class="erp-btn-secondary"
            @click="$router.push(`/sourcing/reconcile-signature/${row.id}`)"
          >签字</el-button>
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
import { useRouter } from 'vue-router'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import { useSourcingStore } from '@/stores/sourcing'
import { usePagedList } from '@/composables/usePagedList'

const router = useRouter()
const sourcingStore = useSourcingStore()
const period = ref<string>('2026-06')
const vendorKeyword = ref('')

const { items, loading, pageNum, pageSize, total, reload } = usePagedList<any>((params) =>
  sourcingStore.listReconciles({
    period: period.value,
    vendor: vendorKeyword.value || undefined,
    ...params,
  }),
)

function filters() {
  return { period: period.value, vendor: vendorKeyword.value || undefined }
}

function canSign(step: string) {
  return step === 'VENDOR_CONFIRM' || step === 'FINANCE_AUDIT'
}

function onSearch() {
  pageNum.value = 1
  reload(filters())
}
function onPageChange() {
  reload(filters())
}
function goCreate() {
  router.push('/sourcing/reconcile-create')
}

onMounted(onSearch)
</script>
