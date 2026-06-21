/** 业务状态码 → 中文展示 */
export const RFQ_STATUS: Record<string, string> = {
  DRAFT: '草稿',
  QUOTING: '询价中',
  COMPARED: '已比价',
  AWARDED: '已定标',
  CLOSED: '已关闭',
}

export const EVENT_LEVEL: Record<string, string> = {
  CRITICAL: '严重',
  WARN: '警告',
  ERROR: '错误',
  INFO: '信息',
}

export const INVENTORY_ALERT_STATUS: Record<string, string> = {
  OPEN: '待处理',
  RESOLVED: '已解决',
  ARCHIVED: '已归档',
}

export const INVENTORY_ALERT_STATUS_TAG: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
  OPEN: 'warning',
  RESOLVED: 'success',
  ARCHIVED: 'info',
}

export const RECEIVABLE_STATUS: Record<string, string> = {
  OPEN: '未结清',
  PARTIAL: '部分收款',
  CLOSED: '已结清',
  OVERDUE: '已逾期',
  PAID: '已结清',
}

export function dictLabel(map: Record<string, string>, code?: string | null): string {
  if (!code) return '—'
  return map[code] ?? code
}

export function dictTagType(
  map: Record<string, 'success' | 'warning' | 'danger' | 'info'>,
  code?: string | null,
): 'success' | 'warning' | 'danger' | 'info' {
  if (!code) return 'info'
  return map[code] ?? 'info'
}

export const RFQ_STATUS_TAG: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
  DRAFT: 'info',
  QUOTING: 'warning',
  COMPARED: 'info',
  AWARDED: 'success',
  CLOSED: 'info',
}

export const EVENT_LEVEL_TAG: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
  CRITICAL: 'danger',
  WARN: 'warning',
  INFO: 'info',
}
