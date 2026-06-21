<template>
  <div>
    <h2>委外供应商切换</h2>
    <el-alert type="warning" :closable="false" title="供应商切换需生管+采购双向确认" />
    <el-form :inline="true" style="margin-top: 12px">
      <el-form-item label="委外单号">
        <el-input v-model="keyword" clearable @keyup.enter="onSearch" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="onSearch">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="items" stripe border>
      <el-table-column prop="switchNo" label="切换单号" min-width="140" />
      <el-table-column prop="outsourceNo" label="委外单号" min-width="130" />
      <el-table-column prop="oldVendor" label="原供应商" min-width="120" />
      <el-table-column prop="newVendor" label="新供应商" min-width="120" />
      <el-table-column prop="reason" label="原因" min-width="120" />
      <el-table-column label="生管确认" width="90">
        <template #default="{ row }">
          <el-icon v-if="row.prodConfirmed" color="green"><Check /></el-icon>
          <el-icon v-else color="gray"><Clock /></el-icon>
        </template>
      </el-table-column>
      <el-table-column label="采购确认" width="90">
        <template #default="{ row }">
          <el-icon v-if="row.purchConfirmed" color="green"><Check /></el-icon>
          <el-icon v-else color="gray"><Clock /></el-icon>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="$router.push(`/production/switch-detail/${row.id}`)">详情</el-button>
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
import { ref, onMounted } from 'vue'
import { Check, Clock } from '@element-plus/icons-vue'
import { useBaseStore } from '@/stores/_base'
import { usePagedList } from '@/composables/usePagedList'

const keyword = ref('')

const { items, loading, pageNum, pageSize, total, reload } = usePagedList<any>((params) =>
  useBaseStore().api.get('/outsource-switches', {
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
