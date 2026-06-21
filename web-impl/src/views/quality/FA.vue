<template>
  <ErpPageShell title="FA 首件检验" description="首件放行状态 · 双签流程">
    <el-form :inline="true" class="erp-filter-bar">
      <el-form-item label="首件单号">
        <el-input v-model="keyword" clearable @keyup.enter="onSearch" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="status" clearable placeholder="全部" style="width: 140px">
          <el-option label="待检验" value="PENDING_INSPECT" />
          <el-option label="检验中" value="INSPECTING" />
          <el-option label="待双签" value="PENDING_SIGN" />
          <el-option label="已通过" value="PASSED" />
          <el-option label="已驳回" value="FAILED" />
          <el-option label="返工中" value="REWORK" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button class="erp-btn-secondary" :loading="loading" @click="onSearch">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" class="erp-table" :data="items" stripe>
      <el-table-column prop="faNo" label="首件单号" min-width="140" />
      <el-table-column prop="workOrderNo" label="工单号" min-width="130" />
      <el-table-column prop="processName" label="工序" min-width="90" />
      <el-table-column prop="materialCode" label="料号" min-width="110" />
      <el-table-column prop="inspector" label="品检员" width="90" />
      <el-table-column prop="engineer" label="工程师" width="90" />
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }">
          <ErpStatusTag :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column prop="result" label="判定" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.result === 'PASSED'" type="success">通过</el-tag>
          <el-tag v-else-if="row.result === 'FAILED'" type="danger">不合格</el-tag>
          <el-tag v-else type="info">-</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="reworkCount" label="返工次数" width="90">
        <template #default="{ row }">
          <span v-if="row.reworkCount > 0" class="rework-count">{{ row.reworkCount }}次</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button size="small" class="erp-btn-ghost" @click="$router.push(`/quality/fa-detail/${row.id}`)">
            {{ row.status === 'PENDING_INSPECT' || row.status === 'PENDING_SIGN' || row.status === 'REWORK' ? '处理' : '详情' }}
          </el-button>
          <el-button size="small" class="erp-btn-success" @click="$router.push(`/quality/fa-report/${row.id}`)">
            报告
          </el-button>
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
const status = ref('')

const { items, loading, pageNum, pageSize, total, reload } = usePagedList<any>((params) =>
  qualityStore.listFas({ keyword: keyword.value || undefined, status: status.value || undefined, ...params }),
)

function onSearch() {
  pageNum.value = 1
  reload({ keyword: keyword.value || undefined, status: status.value || undefined })
}
function onPageChange() {
  reload({ keyword: keyword.value || undefined, status: status.value || undefined })
}

onMounted(onSearch)
</script>

<style scoped>
.rework-count {
  color: #e6a23c;
  font-weight: 600;
}
</style>
