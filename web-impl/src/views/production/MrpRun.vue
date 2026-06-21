<template>
  <div>
    <h2>MRP 运算历史</h2>
    <el-table v-loading="loading" :data="items" stripe border>
      <el-table-column prop="runNo" label="运算号" min-width="140" />
      <el-table-column prop="runType" label="类型" width="100" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><ErpStatusTag :status="row.status" /></template>
      </el-table-column>
      <el-table-column prop="totalShortage" label="缺料" width="80" />
      <el-table-column prop="totalPurchaseSuggestion" label="建议采购" width="100" />
      <el-table-column prop="startedAt" label="开始时间" min-width="160" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="$router.push(`/production/mrp-result/${row.id}`)">结果</el-button>
          <el-button size="small" @click="$router.push(`/production/mrp-shortage/${row.id}`)">缺料</el-button>
        </template>
      </el-table-column>
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
import { useMrpStore } from '@/stores/mrp'
import { usePagedList } from '@/composables/usePagedList'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'

const mrpStore = useMrpStore()

const { items, loading, pageNum, pageSize, total, reload } = usePagedList<any>((params) =>
  mrpStore.listRuns(params),
)

function onSearch() {
  pageNum.value = 1
  reload()
}
function onPageChange() {
  reload()
}

onMounted(onSearch)
</script>
