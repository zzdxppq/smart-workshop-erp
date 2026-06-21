<template>
  <div>
    <h2>提货检</h2>
    <el-form :inline="true" style="margin-bottom: 12px">
      <el-form-item label="提货单号">
        <el-input v-model="keyword" clearable @keyup.enter="onSearch" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="onSearch">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="items" stripe border>
      <el-table-column prop="pickupNo" label="提货单号" min-width="140" />
      <el-table-column prop="customer" label="客户" min-width="120" />
      <el-table-column prop="qty" label="数量" width="80" />
      <el-table-column label="结果" width="100">
        <template #default="{ row }">
          <el-tag :type="row.result === 'PASS' ? 'success' : row.result === 'FAIL' ? 'danger' : 'warning'">{{ row.result === 'PASS' ? '合格' : row.result === 'FAIL' ? '不合格' : row.result === 'PENDING' ? '待检' : row.result }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="时间" min-width="160" />
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="$router.push(`/quality/pickup-inspect/${row.id}`)">检验</el-button>
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
import { useQualityStore } from '@/stores/quality'
import { usePagedList } from '@/composables/usePagedList'

const qualityStore = useQualityStore()
const keyword = ref('')

const { items, loading, pageNum, pageSize, total, reload } = usePagedList<any>((params) =>
  qualityStore.listPickups({ keyword: keyword.value || undefined, ...params }),
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
