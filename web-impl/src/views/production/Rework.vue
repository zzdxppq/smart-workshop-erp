<template>
  <div>
    <h2>返修单</h2>
    <el-form :inline="true" style="margin-bottom: 12px">
      <el-form-item label="返修单号">
        <el-input v-model="keyword" clearable @keyup.enter="onSearch" />
      </el-form-item>
      <el-form-item label="优先级">
        <el-select v-model="priority" clearable placeholder="全部" @change="onSearch">
          <el-option label="低" value="LOW" />
          <el-option label="中" value="NORMAL" />
          <el-option label="高" value="HIGH" />
          <el-option label="紧急" value="URGENT" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="onSearch">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="items" stripe border>
      <el-table-column prop="reworkNo" label="返修单号" min-width="140" />
      <el-table-column prop="workorderNo" label="工单" min-width="130" />
      <el-table-column prop="defectNo" label="不良单" min-width="130" />
      <el-table-column prop="reason" label="原因" min-width="120" />
      <el-table-column label="优先级" width="90">
        <template #default="{ row }">
          <ErpStatusTag :status="row.priority" :label="priorityLabel(row.priority)" />
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><ErpStatusTag :status="row.status" /></template>
      </el-table-column>
      <el-table-column prop="assignee" label="处理人" width="90" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="$router.push(`/sourcing/rework-detail/${row.id}`)">详情</el-button>
          <el-button v-if="row.priority === 'URGENT'" size="small" type="danger" @click="$router.push('/sourcing/rework-alert')">告警</el-button>
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
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'
import { priorityLabel } from '@/utils/statusLabels'

const keyword = ref('')
const priority = ref('')

const { items, loading, pageNum, pageSize, total, reload } = usePagedList<any>((params) =>
  useBaseStore().api.get('/reworks', {
    params: {
      keyword: keyword.value || undefined,
      priority: priority.value || undefined,
      ...params,
    },
  }),
)

function onSearch() {
  pageNum.value = 1
  reload({ keyword: keyword.value || undefined, priority: priority.value || undefined })
}
function onPageChange() {
  reload({ keyword: keyword.value || undefined, priority: priority.value || undefined })
}

onMounted(onSearch)
</script>
