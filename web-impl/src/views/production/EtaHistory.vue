<template>
  <div>
    <h2>委外 ETA 历史趋势</h2>
    <el-form :inline="true" style="margin-bottom: 12px">
      <el-form-item label="委外单号">
        <el-input v-model="keyword" clearable @keyup.enter="onSearch" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="onSearch">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="items" stripe border>
      <el-table-column prop="outsourceNo" label="委外单号" min-width="130" />
      <el-table-column prop="predictedAt" label="预测时间" min-width="160" />
      <el-table-column prop="predictedEta" label="预测 ETA" width="120" />
      <el-table-column prop="actualEta" label="实际 ETA" width="120" />
      <el-table-column prop="algorithm" label="算法" width="100" />
      <el-table-column prop="accuracy" label="准确度" width="90" />
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
  useBaseStore().api.get('/outsource-eta/history-records', {
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
