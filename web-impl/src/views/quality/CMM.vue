<template>
  <ErpPageShell title="CMM 三次元检测" description="精密尺寸检测 · 设备/物料关联。">
    <el-form :inline="true" class="erp-filter-bar">
      <el-form-item label="单号">
        <el-input v-model="keyword" clearable @keyup.enter="onSearch" />
      </el-form-item>
      <el-form-item>
        <el-button class="erp-btn-secondary" :loading="loading" @click="onSearch">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" class="erp-table" :data="items" stripe>
      <el-table-column prop="cmmNo" label="检测单号" min-width="140" />
      <el-table-column prop="deviceId" label="设备号" width="100" />
      <el-table-column prop="materialCode" label="料号" min-width="110" />
      <el-table-column prop="inspector" label="检验员" width="90" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <ErpStatusTag :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column prop="measuredAt" label="检测时间" min-width="160" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" class="erp-btn-ghost" @click="$router.push(`/quality/cmm-detail/${row.id}`)">详情</el-button>
          <el-button size="small" type="success" class="erp-btn-success" @click="$router.push(`/quality/cmm-report/${row.id}`)">报告</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      class="erp-pagination"
      @current-change="onPageChange"
      @size-change="onSearch"
    />
  </ErpPageShell>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import { useQualityStore } from '@/stores/quality'
import { usePagedList } from '@/composables/usePagedList'

const qualityStore = useQualityStore()
const keyword = ref('')

const { items, loading, pageNum, pageSize, total, reload } = usePagedList<any>((params) =>
  qualityStore.listCmms({ keyword: keyword.value || undefined, ...params }),
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
