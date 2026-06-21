<template>
  <div>
    <h2>工作流配置</h2>

    <el-alert type="info" :closable="false" style="margin-bottom: 16px">
      <template #title>
        <strong>功能说明</strong>
      </template>
      <ul style="margin: 8px 0 0 0; padding-left: 18px; font-size: 13px; line-height: 1.8">
        <li>工作流配置用于管理报价单、销售订单、采购订单、委外订单等单据的审批流程。</li>
        <li>每条工作流定义一组审批节点（如部门经理 → GM → 归档），满足条件时自动流转。</li>
        <li>金额阈值决定审批路由：小于阈值可跳过高层审批，直接由部门经理通过。</li>
        <li>当前页面为只读展示，管理工作流请前往各业务模块的审批页面。</li>
      </ul>
    </el-alert>

    <el-table v-loading="loading" :data="items" stripe border @row-click="openFlow">
      <el-table-column prop="name" label="名称" min-width="160" />
      <el-table-column prop="workflowCode" label="编码" min-width="160" />
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
        名称: {{ selected?.name ?? '-' }} · 编码: {{ selected?.workflowCode ?? '-' }} · 状态: <ErpStatusTag v-if="selected" :status="String(selected.status ?? '')" />
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
    { title: (row.name as string) || (row.workflowCode as string) || '审批节点', status: row.status === 'ACTIVE' ? 'PENDING' : 'APPROVED' },
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
