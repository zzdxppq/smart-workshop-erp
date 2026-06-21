<template>
  <div>
    <h2>物料库存</h2>
    <el-form :inline="true" style="margin-bottom: 12px">
      <el-form-item label="料号">
        <el-input v-model="keyword" clearable @keyup.enter="onSearch" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="onSearch">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="items" stripe border>
      <el-table-column prop="materialCode" label="料号" min-width="130" />
      <el-table-column prop="materialName" label="物料名称" min-width="140" />
      <el-table-column prop="qty" label="库存" width="100" />
      <el-table-column prop="unit" label="单位" width="80" />
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

const keyword = ref('')

const { items, loading, pageNum, pageSize, total, reload } = usePagedList<any>((params) =>
  useBaseStore().api.get('/material/inventory', {
    params: { keyword: keyword.value || undefined, ...params },
  }),
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
