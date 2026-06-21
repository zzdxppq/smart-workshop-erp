<template>
  <div>
    <h2>工作流</h2>
    <el-table v-loading="loading" :data="items" stripe border @row-click="openFlow">
      <el-table-column prop="name" label="名称" min-width="140" />
      <el-table-column prop="code" label="编码" min-width="120" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><ErpStatusTag :status="row.status" /></template>
      </el-table-column>
      <el-table-column label="审批链预览" min-width="200">
        <template #default="{ row }">
          <ApprovalChainRenderer :nodes="previewNodes(row)" />
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

    <el-drawer v-model="drawerOpen" title="工作流节点" size="480px">
      <ApprovalChainRenderer v-if="selected" :nodes="previewNodes(selected)" />
      <p style="margin-top: 12px; color: var(--erp-text-muted); font-size: 13px">
        编码: {{ selected?.code }} · 状态: <ErpStatusTag v-if="selected" :status="String(selected.status ?? '')" />
      </p>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useBaseStore } from '@/stores/_base'
import { usePagedList } from '@/composables/usePagedList'
import type { ApprovalNode } from '@/components/erp/ApprovalChainRenderer.vue'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'

const drawerOpen = ref(false)
const selected = ref<Record<string, unknown> | null>(null)

const { items, loading, pageNum, pageSize, total, reload } = usePagedList<any>((params) =>
  useBaseStore().api.get('/admin/workflows', { params }),
)

function previewNodes(row: Record<string, unknown>): ApprovalNode[] {
  return [
    { title: '提交', status: 'APPROVED' },
    { title: row.name as string || '审批节点', status: row.status === 'ACTIVE' ? 'PENDING' : 'APPROVED' },
    { title: '归档', status: 'PENDING' },
  ]
}

function openFlow(row: Record<string, unknown>) {
  selected.value = row
  drawerOpen.value = true
}

function onSearch() {
  pageNum.value = 1
  reload()
}
function onPageChange() {
  reload()
}

onMounted(onSearch)
</script>
