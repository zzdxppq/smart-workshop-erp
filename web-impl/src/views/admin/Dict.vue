<template>
  <div>
    <h2>数据字典</h2>
    <el-form :inline="true" style="margin-bottom: 12px">
      <el-form-item label="分类">
        <el-input v-model="category" clearable @keyup.enter="onSearch" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="onSearch">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="items" stripe border>
      <el-table-column prop="dictType" label="分类" min-width="160" />
      <el-table-column prop="dictCode" label="键" min-width="180" />
      <el-table-column prop="dictLabel" label="值" min-width="140" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><ErpStatusTag :status="row.status" /></template>
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
import { ref, onMounted } from 'vue'
import { useBaseStore } from '@/stores/_base'
import { usePagedList } from '@/composables/usePagedList'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'

const category = ref('')

const { items, loading, pageNum, pageSize, total, reload } = usePagedList<any>((params) =>
  useBaseStore().api.get('/dicts', {
    params: { type: category.value || undefined, ...params },
  }),
)

function onSearch() {
  pageNum.value = 1
  reload({ type: category.value || undefined })
}
function onPageChange() {
  reload({ type: category.value || undefined })
}

onMounted(onSearch)
</script>
