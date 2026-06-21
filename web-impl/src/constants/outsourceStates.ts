/** 委外 7 状态机（与 erp-production OutsourceStateMachineService 对齐） */
export const OUTSOURCE_STATES = [
  'DRAFT',
  'SENT',
  'ACCEPTED',
  'IN_PRODUCTION',
  'INSPECTED',
  'COMPLETED',
  'REWORK',
  'CLOSED',
  'REJECTED',
] as const

export type OutsourceStateCode = (typeof OUTSOURCE_STATES)[number]

export const OUTSOURCE_STATE_LABELS: Record<string, string> = {
  DRAFT: '草稿',
  SENT: '已发送',
  ACCEPTED: '已接单',
  IN_PRODUCTION: '生产中',
  INSPECTED: '已检验',
  COMPLETED: '已完成',
  REWORK: '返修中',
  CLOSED: '已关闭',
  REJECTED: '已拒收',
  // PRD 业务态（列表 API 可能返回）
  PENDING_SHIP: '待发货',
  SHIPPING: '送货中',
  PENDING_INSPECTION: '待检',
  INSPECTING: '质检中',
  QUALIFIED_STORAGE: '待入库',
  STORED: '已入库',
  REPAIR_REQUESTED: '待返修',
  NOTIFIED_REPAIR: '已通知返修',
}

export function outsourceStateLabel(code?: string) {
  if (!code) return '—'
  return OUTSOURCE_STATE_LABELS[code] ?? code
}

export function outsourceStateTagType(code?: string): 'info' | 'warning' | 'success' | 'danger' {
  if (!code) return 'info'
  if (code === 'REWORK' || code === 'REPAIR_REQUESTED' || code === 'NOTIFIED_REPAIR') return 'danger'
  if (code === 'PENDING_INSPECTION' || code === 'INSPECTING' || code === 'SENT' || code === 'SHIPPING') return 'warning'
  if (code === 'COMPLETED' || code === 'STORED' || code === 'CLOSED') return 'success'
  return 'info'
}

/** 面板分组（进行中态优先展示） */
export const OUTSOURCE_PANEL_GROUPS: { key: string; label: string; states: string[] }[] = [
  { key: 'ship', label: '待发货/送货', states: ['PENDING_SHIP', 'SHIPPING', 'DRAFT', 'SENT'] },
  { key: 'inspect', label: '待检/质检', states: ['PENDING_INSPECTION', 'INSPECTING', 'ACCEPTED', 'IN_PRODUCTION'] },
  { key: 'storage', label: '入库', states: ['QUALIFIED_STORAGE', 'INSPECTED', 'COMPLETED'] },
  { key: 'rework', label: '返修', states: ['REWORK', 'REPAIR_REQUESTED', 'NOTIFIED_REPAIR'] },
  { key: 'done', label: '已结案', states: ['STORED', 'CLOSED', 'REJECTED'] },
]
