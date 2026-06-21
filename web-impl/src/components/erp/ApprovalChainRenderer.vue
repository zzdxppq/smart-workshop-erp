<template>
  <div class="approval-chain">
    <div
      v-for="(node, idx) in nodes"
      :key="node.id ?? idx"
      class="chain-node"
      :class="statusClass(node.status)"
    >
      <div class="node-circle">
        <el-icon v-if="node.status === 'APPROVED'"><Check /></el-icon>
        <el-icon v-else-if="node.status === 'REJECTED'"><Close /></el-icon>
        <span v-else>{{ idx + 1 }}</span>
      </div>
      <div class="node-body">
        <div class="node-title">{{ node.title }}</div>
        <div v-if="node.assignee" class="node-assignee">{{ node.assignee }}</div>
        <div v-if="node.time" class="node-time">{{ node.time }}</div>
      </div>
      <div v-if="idx < nodes.length - 1" class="chain-arrow">→</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Check, Close } from '@element-plus/icons-vue'

export interface ApprovalNode {
  id?: string | number
  title: string
  assignee?: string
  time?: string
  status?: 'PENDING' | 'APPROVED' | 'REJECTED' | 'SKIPPED'
}

defineProps<{
  nodes: ApprovalNode[]
}>()

function statusClass(status?: string) {
  if (status === 'APPROVED') return 'is-approved'
  if (status === 'REJECTED') return 'is-rejected'
  if (status === 'SKIPPED') return 'is-skipped'
  return 'is-pending'
}
</script>

<style scoped>
.approval-chain {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: 8px;
}
.chain-node {
  display: flex;
  align-items: center;
  gap: 8px;
}
.node-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  border: 2px solid var(--erp-border);
}
.is-pending .node-circle { border-color: var(--erp-color-warning); color: var(--erp-color-warning); }
.is-approved .node-circle { border-color: var(--erp-color-success); background: var(--erp-color-success); color: #fff; }
.is-rejected .node-circle { border-color: var(--erp-color-danger); background: var(--erp-color-danger); color: #fff; }
.is-skipped .node-circle { opacity: 0.5; }
.node-title { font-weight: 500; }
.node-assignee, .node-time { font-size: 12px; color: var(--erp-text-muted); }
.chain-arrow { color: var(--erp-text-muted); font-size: 18px; padding: 0 4px; }
</style>
