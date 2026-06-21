/** 图纸 / 条码 / 物料等业务状态码 → 中文 */
export const DRAWING_STATUS: Record<string, string> = {
  DRAFT: '草稿',
  RELEASED: '已发布',
  PUBLISHED: '已发布',
  CONVERTED: '已转化',
  ARCHIVED: '已归档',
  OBSOLETE: '已作废',
}

export const BARCODE_STATUS: Record<string, string> = {
  ACTIVE: '有效',
  DISCARDED: '已作废',
  INACTIVE: '无效',
}

export const STOCKTAKE_STATUS: Record<string, string> = {
  DRAFT: '草稿',
  COUNTING: '盘点中',
  CLOSED: '已关闭',
}

export const VENDOR_STATUS: Record<string, string> = {
  ACTIVE: '合作中',
  INACTIVE: '已停用',
  SUSPENDED: '已暂停',
}

export const CUSTOMER_STATUS: Record<string, string> = {
  ACTIVE: '正常',
  INACTIVE: '停用',
  BLACKLIST: '黑名单',
}

export const CUSTOMER_TYPE_LABELS: Record<string, string> = {
  VIP: 'VIP 客户',
  NORMAL: '普通客户',
  STRATEGIC: '战略客户',
}

export const SCAN_TYPE_LABELS: Record<string, string> = {
  INBOUND: '入库',
  OUTBOUND: '出库',
  TRANSFER: '移库',
  ADJUST: '调整',
  REPORT: '报工',
}

export const SOURCE_TYPE_LABELS: Record<string, string> = {
  NO_ORDER: '无订单采购',
  OUTSOURCE: '委外询价',
  PURCHASE: '采购',
  SALES: '销售',
  MRP: 'MRP',
}

export const RFQ_STATUS: Record<string, string> = {
  DRAFT: '草稿',
  QUOTING: '询价中',
  COMPARED: '已比价',
  AWARDED: '已定标',
  CLOSED: '已关闭',
}

export const PR_STATUS: Record<string, string> = {
  PENDING: '待处理',
  PARTIAL: '部分转单',
  CONVERTED: '已转单',
}

export const FINANCE_STATUS_LABELS: Record<string, string> = {
  PAID: '已结清',
  UNPAID: '未结清',
  PARTIAL: '部分付款',
  PARTIAL_PAID: '部分付款',
  OVERDUE: '已逾期',
  OPEN: '待付款',
  PENDING: '待处理',
  APPROVED: '已审批',
  DRAFT: '草稿',
  CLOSED: '已关闭',
  CANCELLED: '已取消',
}

/** 全局业务状态 → 中文（列表/详情统一） */
export const COMMON_STATUS_LABELS: Record<string, string> = {
  ...DRAWING_STATUS,
  ...BARCODE_STATUS,
  ...STOCKTAKE_STATUS,
  ...RFQ_STATUS,
  ...FINANCE_STATUS_LABELS,
  ...PR_STATUS,
  // 报价 / 订单
  DRAFT: '草稿',
  PENDING_ENG: '待工程师',
  PENDING_APPROVAL: '待审批',
  SUBMITTED: '已提交',
  PROCESSING: '工程转化中',
  PENDING_PRODUCTION: '待转产',
  IN_PRODUCTION: '已转工单',
  CONFIRMED: '已确认',
  PO_CONVERTED: '已转采购单',
  OUTSOURCE_CONVERTED: '已转委外',
  // BOM / 工艺
  // 工单 / 生产
  SCHEDULED: '已排产',
  IN_PROGRESS: '生产中',
  PRODUCING: '生产中',
  COMPLETED: '已完工',
  FINISHED: '已完成',
  REPORTED: '已报工',
  SETTLED: '已结算',
  PARTIAL_SHIPPED: '部分发货',
  // 委外
  CREATE: '建单',
  VENDOR_CONFIRM: '厂商确认',
  FINANCE_AUDIT: '财务审核',
  SIGN: '双方签',
  SHIPPED: '已发货',
  ACCEPTED: '已接单',
  SENT: '已发送',
  INSPECTED: '已检验',
  REWORK: '返修中',
  PENDING_SHIP: '待发货',
  SHIPPING: '送货中',
  PENDING_INSPECTION: '待检',
  INSPECTING: '质检中',
  QUALIFIED_STORAGE: '待入库',
  STORED: '已入库',
  REPAIR_REQUESTED: '待返修',
  NOTIFIED_REPAIR: '已通知返修',
  // 质检
  PASSED: '合格',
  FAILED: '不合格',
  CONCESSION: '让步接收',
  PASS: '合格',
  FAIL: '不合格',
  // V2.1 FA 状态
  PENDING_INSPECT: '待检验',
  PENDING_SIGN: '待双签',
  // V2.1 处置状态
  APPROVED: '已生效',
  RESOLVED: '已处理',
  // 设备
  RUNNING: '运行中',
  IDLE: '空闲',
  BUSY: '忙碌',
  MAINTENANCE: '维修中',
  FAULT: '故障',
  // 人事 / 账号
  ACTIVE: '在职',
  INACTIVE: '离职',
  NORMAL: '正常',
  LATE: '迟到',
  ABSENT: '缺勤',
  ENABLED: '启用',
  DISABLED: '禁用',
  // 招聘
  OPEN: '招聘中',
  // 告警 / 级别
  INFO: '信息',
  WARN: '警告',
  WARNING: '警告',
  ERROR: '错误',
  CRITICAL: '严重',
  MAJOR: '严重',
  MINOR: '轻微',
  // MRP / 任务
  SUCCESS: '成功',
  FAILURE: '失败',
  QUEUED: '排队中',
  PRINTED: '已打印',
  RUNNING_TASK: '执行中',
  // 返修优先级（若误作 status 展示）
  LOW: '低',
  HIGH: '高',
  URGENT: '紧急',
  // 其他
  REJECTED: '已驳回',
  ARRIVED: '已到货',
  BLACKLIST: '黑名单',
  VIP: 'VIP 客户',
  NO_ORDER: '无订单采购',
  INBOUND: '入库',
  OUTBOUND: '出库',
  INHOUSE: '自制',
  OUTSOURCE: '委外',
  ONLINE: '在线',
  OFFLINE: '离线',
  UNKNOWN: '未知',
}

export const COMMON_STATUS_TONE: Record<string, string> = {
  DRAFT: 'muted',
  RELEASED: 'success',
  PUBLISHED: 'success',
  CONVERTED: 'success',
  ARCHIVED: 'warning',
  OBSOLETE: 'danger',
  ACTIVE: 'success',
  DISCARDED: 'muted',
  INACTIVE: 'muted',
  COUNTING: 'warning',
  CLOSED: 'success',
  QUOTING: 'warning',
  COMPARED: 'warning',
  AWARDED: 'success',
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
  SUBMITTED: 'primary',
  CONFIRMED: 'success',
  SCHEDULED: 'primary',
  IN_PROGRESS: 'primary',
  PRODUCING: 'primary',
  COMPLETED: 'success',
  FINISHED: 'success',
  CANCELLED: 'muted',
  SHIPPED: 'success',
  RUNNING: 'success',
  IDLE: 'muted',
  FAULT: 'danger',
  MAINTENANCE: 'warning',
  PASSED: 'success',
  PASS: 'success',
  FAILED: 'danger',
  FAIL: 'danger',
  CONCESSION: 'warning',
  PAID: 'success',
  UNPAID: 'warning',
  OVERDUE: 'danger',
  OPEN: 'warning',
  INFO: 'primary',
  WARN: 'warning',
  WARNING: 'warning',
  ERROR: 'danger',
  CRITICAL: 'danger',
  MAJOR: 'danger',
  MINOR: 'warning',
  SUCCESS: 'success',
  FAILURE: 'danger',
  URGENT: 'danger',
  HIGH: 'warning',
  REWORK: 'danger',
  INSPECTING: 'warning',
  // V2.1 FA 状态
  PENDING_INSPECT: 'primary',
  PENDING_SIGN: 'warning',
  RESOLVED: 'success',
}

export const DRAWING_STATUS_TAG: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
  DRAFT: 'info',
  RELEASED: 'success',
  PUBLISHED: 'success',
  CONVERTED: 'success',
  ARCHIVED: 'warning',
  OBSOLETE: 'danger',
}

export const BARCODE_STATUS_TAG: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
  ACTIVE: 'success',
  DISCARDED: 'info',
  INACTIVE: 'info',
}

export const STOCKTAKE_STATUS_TAG: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
  DRAFT: 'info',
  COUNTING: 'warning',
  CLOSED: 'success',
}

export function bizStatusLabel(code?: string | null, map: Record<string, string> = DRAWING_STATUS): string {
  return commonStatusLabel(code, map)
}

export function bizStatusTagType(
  code?: string | null,
  map: Record<string, 'success' | 'warning' | 'danger' | 'info'> = DRAWING_STATUS_TAG,
): 'success' | 'warning' | 'danger' | 'info' {
  if (!code) return 'info'
  const key = String(code).toUpperCase()
  return map[key] ?? 'info'
}

/** 已是中文则原样返回，否则查表 */
export function commonStatusLabel(code?: string | null, map?: Record<string, string>): string {
  if (code == null || code === '') return '—'
  const raw = String(code).trim()
  if (/[\u4e00-\u9fff]/.test(raw)) return raw
  const key = raw.toUpperCase()
  const merged = map ?? COMMON_STATUS_LABELS
  return merged[key] ?? COMMON_STATUS_LABELS[key] ?? raw
}

export function commonStatusTone(code?: string | null): string {
  if (!code) return 'muted'
  const raw = String(code)
  if (/[\u4e00-\u9fff]/.test(raw)) return 'muted'
  const key = raw.toUpperCase()
  return COMMON_STATUS_TONE[key] ?? 'muted'
}

export function vendorStatusLabel(code?: string | null): string {
  return commonStatusLabel(code, VENDOR_STATUS)
}

export function customerStatusLabel(code?: string | null): string {
  return commonStatusLabel(code, CUSTOMER_STATUS)
}

export function sourceTypeLabel(code?: string | null): string {
  return commonStatusLabel(code, SOURCE_TYPE_LABELS)
}

export function financeStatusLabel(code?: string) {
  return commonStatusLabel(code, FINANCE_STATUS_LABELS)
}

export function financeStatusTagType(code?: string): 'success' | 'warning' | 'danger' | 'info' {
  const key = String(code ?? '').toUpperCase()
  if (key === 'PAID' || key === 'APPROVED' || key === 'CLOSED') return 'success'
  if (key === 'OVERDUE' || key === 'CANCELLED') return 'danger'
  if (key === 'PARTIAL' || key === 'PARTIAL_PAID' || key === 'PENDING' || key === 'OPEN') return 'warning'
  return 'info'
}

export const PRIORITY_LABELS: Record<string, string> = {
  LOW: '低',
  NORMAL: '中',
  MEDIUM: '中',
  HIGH: '高',
  URGENT: '紧急',
}

export function priorityLabel(code?: string | null): string {
  if (!code) return '—'
  const key = String(code).toUpperCase()
  if (/[\u4e00-\u9fff]/.test(String(code))) return String(code)
  return PRIORITY_LABELS[key] ?? String(code)
}
