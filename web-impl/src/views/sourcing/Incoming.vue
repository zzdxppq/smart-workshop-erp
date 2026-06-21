<template>
  <div class="incoming-page">
    <h2>到货提醒</h2>
    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="到货登记请在 Android 仓管 APP 扫码完成；Web 端仅查看到货提醒与入库状态。"
      style="margin-bottom: 12px"
    />
    <el-form :inline="true" style="margin-bottom: 12px">
      <el-form-item label="到货单号">
        <el-input v-model="keyword" clearable @keyup.enter="onSearch" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="onSearch">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="items" stripe border>
      <el-table-column prop="alertNo" label="提醒单号" min-width="140">
        <template #default="{ row }">{{ row.alertNo || row.incomingNo }}</template>
      </el-table-column>
      <el-table-column prop="poNo" label="采购单号" min-width="130" />
      <el-table-column prop="vendorName" label="供应商" min-width="120" />
      <el-table-column prop="materialCode" label="料号" min-width="110" />
      <el-table-column prop="qty" label="数量" width="80" />
      <el-table-column label="预计到货" min-width="120">
        <template #default="{ row }">{{ row.expectedDate || row.arrivedAt }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.alertLevel === 'ARRIVED' || row.arrivedAt ? 'success' : 'warning'">
            {{ row.alertLevel === 'ARRIVED' || row.arrivedAt ? '已到货' : (row.alertLevel || '待到货') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="$router.push(`/sourcing/incoming-detail/${row.id}`)">详情</el-button>
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
import { useSourcingStore } from '@/stores/sourcing'
import { usePagedList } from '@/composables/usePagedList'

const sourcingStore = useSourcingStore()
const keyword = ref('')

const { items, loading, pageNum, pageSize, total, reload } = usePagedList<any>((params) =>
  sourcingStore.listIncoming({ keyword: keyword.value || undefined, ...params }),
)

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
.incoming-page { padding: 16px; }
</style>
