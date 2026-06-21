<template>
  <div>
    <h2>采购视图</h2>
    <el-alert type="warning" :closable="false" title="采购角色仅可选择厂商，不可变更工序归属" />
    <el-table v-loading="loading" :data="items" stripe border style="margin-top: 12px">
      <el-table-column prop="outsourceNo" label="委外单号" min-width="140" />
      <el-table-column prop="workorderNo" label="工单号" min-width="130" />
      <el-table-column prop="supplierName" label="供应商" min-width="120" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><ErpStatusTag :status="row.status" /></template>
      </el-table-column>
      <el-table-column prop="totalAmount" label="金额" width="110" />
    </el-table>
    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      style="margin-top: 12px"
      @current-change="onPageChange"
      @size-change="onSearch"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useOutsourceStore } from '@/stores/outsource'
import { usePagedList } from '@/composables/usePagedList'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'

const outsourceStore = useOutsourceStore()

const { items, loading, pageNum, pageSize, total, reload } = usePagedList<any>((params) =>
  outsourceStore.listOrders({ status: 'SENT', ...params }),
)

function onSearch() {
  pageNum.value = 1
  reload({ status: 'SENT' })
}
function onPageChange() {
  reload({ status: 'SENT' })
}

onMounted(onSearch)
</script>
