<template>
  <ErpPageShell title="询比价工作台" description="新建询价 → 录入回传报价 → 比价定标 → 一键转采购单/委外单。">
    <el-form :inline="true" class="erp-filter-bar">
      <el-form-item label="询价单号">
        <el-input v-model="keyword" clearable @keyup.enter="onSearch" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="status" clearable placeholder="全部" @change="onSearch">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="询价中" value="QUOTING" />
          <el-option label="已比价" value="COMPARED" />
          <el-option label="已定标" value="AWARDED" />
          <el-option label="已关闭" value="CLOSED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button class="erp-btn-secondary" :loading="loading" @click="onSearch">查询</el-button>
        <el-button type="primary" class="erp-btn-primary" @click="$router.push('/sourcing/rfq-create')">新建询价</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" class="erp-table" :data="items" stripe>
      <el-table-column prop="rfqNo" label="询价单号" min-width="140" />
      <el-table-column prop="prNo" label="来源 PR" min-width="120" />
      <el-table-column prop="materialCode" label="料号" min-width="110" />
      <el-table-column prop="qty" label="数量" width="80" align="right">
        <template #default="{ row }">
          <span class="erp-num-highlight">{{ row.qty ?? '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="quoteCount" label="报价数" width="88" align="right">
        <template #default="{ row }">
          <span class="erp-num-highlight">{{ row.quoteCount ?? 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="转单状态" min-width="150">
        <template #default="{ row }">{{ convertLabel(row) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <ErpStatusTag :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" class="erp-btn-ghost" @click="$router.push(`/sourcing/rfq-detail/${row.id}`)">详情</el-button>
          <el-button
            v-if="row.status === 'QUOTING' || row.status === 'COMPARED'"
            size="small"
            class="erp-btn-secondary"
            @click="$router.push(`/sourcing/rfq-compare/${row.id}`)"
          >比价</el-button>
          <el-button
            v-if="row.status === 'QUOTING' || row.status === 'COMPARED'"
            size="small"
            type="success"
            class="erp-btn-success"
            @click="$router.push(`/sourcing/rfq-award/${row.id}`)"
          >定标</el-button>
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
const status = ref('')

const { items, loading, pageNum, pageSize, total, reload } = usePagedList<any>((params) =>
  sourcingStore.listRfqs({
    keyword: keyword.value || undefined,
    status: status.value || undefined,
    ...params,
  }),
)

function convertLabel(row: Record<string, unknown>) {
  const cs = row.convertStatus as string | undefined
  if (cs === 'PO_CONVERTED') return `已转 PO ${row.convertedOrderNo ?? ''}`
  if (cs === 'OUTSOURCE_CONVERTED') return `已转委外 ${row.convertedOrderNo ?? ''}`
  return '未转单'
}

function onSearch() {
  pageNum.value = 1
  reload({ keyword: keyword.value || undefined, status: status.value || undefined })
}
function onPageChange() {
  reload({ keyword: keyword.value || undefined, status: status.value || undefined })
}

onMounted(onSearch)
</script>
