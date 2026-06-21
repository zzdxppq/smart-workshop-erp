<template>
  <div>
    <h2>委外检视图</h2>
    <el-form :inline="true" style="margin-bottom: 12px">
      <el-form-item label="委外单/物料">
        <el-input v-model="keyword" clearable @keyup.enter="onSearch" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="onSearch">查询</el-button>
        <el-button @click="$router.push('/quality/inspection')">返回品质检验</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="items" stripe border>
      <el-table-column prop="inspectionNo" label="委外检单号" min-width="140" />
      <el-table-column prop="outsourceNo" label="委外单号" min-width="130" />
      <el-table-column prop="vendorName" label="委外厂" min-width="120" />
      <el-table-column prop="materialCode" label="料号" min-width="110" />
      <el-table-column prop="qty" label="数量" width="80" />
      <el-table-column label="结果" width="100">
        <template #default="{ row }">
          <el-tag :type="row.result === 'PASS' ? 'success' : 'danger'">{{ row.result === 'PASS' ? '合格' : row.result === 'FAIL' ? '不合格' : row.result }}</el-tag>
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
  qualityStore.listOutsourceInspections({ keyword: keyword.value || undefined, ...params }),
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
