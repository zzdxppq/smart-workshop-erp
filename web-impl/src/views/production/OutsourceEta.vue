<template>
  <div>
    <h2>委外 ETA 预估</h2>
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
      <el-table-column prop="workorderNo" label="工单" min-width="130" />
      <el-table-column prop="supplierName" label="供应商" min-width="120" />
      <el-table-column prop="originalEta" label="原 ETA" width="120" />
      <el-table-column prop="predictedEta" label="预测 ETA" width="120" />
      <el-table-column label="风险" width="120">
        <template #default="{ row }">
          <el-tag :type="row.delayDays > 0 ? 'danger' : 'success'">
            {{ row.delayDays > 0 ? `延误 ${row.delayDays} 天` : '正常' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="$router.push('/production/eta-history')">历史</el-button>
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
import { useBaseStore } from '@/stores/_base'
import { usePagedList } from '@/composables/usePagedList'

const keyword = ref('')

const { items, loading, pageNum, pageSize, total, reload } = usePagedList<any>((params) =>
  useBaseStore().api.get('/outsource-eta', {
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
