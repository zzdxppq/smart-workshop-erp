import type { ApprovalNode } from '@/components/erp/ApprovalChainRenderer.vue'

/** PRD / QUOTE_FLOW：业务员 → 部门经理(5-20万) → 总经理+财务(>20万) */
export function quoteApprovalPreviewNodes(amount: number, currentNode = 1): ApprovalNode[] {
  const done = currentNode >= 99
  if (amount < 50000) {
    return [{ title: '业务员', status: done ? 'APPROVED' : 'PENDING' }]
  }
  if (amount < 200000) {
    return [
      { title: '业务员', status: 'APPROVED' },
      { title: '部门经理', status: done ? 'APPROVED' : 'PENDING' },
    ]
  }
  const gmDone = currentNode >= 2 || done
  return [
    { title: '总经理', status: gmDone ? 'APPROVED' : 'PENDING' },
    { title: '财务总监', status: done ? 'APPROVED' : gmDone ? 'PENDING' : 'SKIPPED' },
  ]
}
