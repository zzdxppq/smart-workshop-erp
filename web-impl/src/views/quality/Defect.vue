<template>
  <ErpPageShell title="不良品处置" description="8D报告 · 处置状态跟踪">
    <el-form :inline="true" class="erp-filter-bar">
      <el-form-item label="不良单号">
        <el-input v-model="keyword" clearable @keyup.enter="onSearch" />
      </el-form-item>
      <el-form-item label="严重度">
        <el-select v-model="severity" clearable placeholder="全部" style="width: 120px">
          <el-option label="轻微" value="MINOR" />
          <el-option label="严重" value="MAJOR" />
          <el-option label="致命" value="CRITICAL" />
        </el-select>
      </el-form-item>
      <el-form-item label="处置状态">
        <el-select v-model="dispositionStatus" clearable placeholder="全部" style="width: 120px">
          <el-option label="待处置" value="PENDING" />
          <el-option label="已批准" value="APPROVED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button class="erp-btn-secondary" :loading="loading" @click="onSearch">查询</el-button>
        <el-button type="primary" @click="$router.push('/quality/defect-create')">新增不良品</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" class="erp-table" :data="items" stripe>
      <el-table-column prop="defectNo" label="不良单号" min-width="140" />
      <el-table-column prop="workOrderNo" label="工单" min-width="130" />
      <el-table-column prop="defectType" label="不良类型" min-width="120" />
      <el-table-column prop="causeCategory" label="原因分类" min-width="100">
        <template #default="{ row }">
          <el-tag v-if="row.causeCategory" size="small">{{ row.causeCategory }}</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="qty" label="数量" width="70" align="right">
        <template #default="{ row }">
          <span class="erp-num-highlight">{{ row.qty ?? '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="severity" label="严重度" width="80">
        <template #default="{ row }">
          <ErpStatusTag :status="row.severity" />
        </template>
      </el-table-column>
      <el-table-column prop="result" label="处置方式" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.result === 'REWORK'" type="warning">返工</el-tag>
          <el-tag v-else-if="row.result === 'SCRAP'" type="danger">报废</el-tag>
          <el-tag v-else-if="row.result === 'CONCESSION'" type="info">让步</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="dispositionStatus" label="处置状态" width="100">
        <template #default="{ row }">
          <ErpStatusTag :status="row.dispositionStatus" />
        </template>
      </el-table-column>
      <el-table-column prop="responsibleDept" label="责任部门" width="90" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button size="small" class="erp-btn-ghost" @click="$router.push(`/quality/defect-detail/${row.id}`)">
            {{ row.dispositionStatus === 'PENDING' ? '处置' : '详情' }}
          </el-button>
          <el-button size="small" class="erp-btn-secondary" @click="$router.push('/quality/defect-report')">
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
const severity = ref('')
const dispositionStatus = ref('')

const { items, loading, pageNum, pageSize, total, reload } = usePagedList<any>((params) =>
  qualityStore.listDefects({
    keyword: keyword.value || undefined,
    severity: severity.value || undefined,
    dispositionStatus: dispositionStatus.value || undefined,
    ...params,
  }),
)

function filters() {
  return {
    keyword: keyword.value || undefined,
    severity: severity.value || undefined,
    dispositionStatus: dispositionStatus.value || undefined,
  }
}

function onSearch() {
  pageNum.value = 1
  reload(filters())
}
function onPageChange() {
  reload(filters())
}

onMounted(onSearch)
</script>
