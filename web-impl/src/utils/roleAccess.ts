/**
 * 模块/路由角色访问控制（与 init.sql sys_role 对齐）
 */

export const ADMIN_ROLES = ['SYS_ADMIN', 'ADMIN'] as const

const G = (...roles: string[]) => [...roles, ...ADMIN_ROLES]

/** 常用角色组 */
export const ROLE_GROUPS = {
  SALES: G('SALES', 'SALES_MGR', 'SALES_MANAGER', 'GM'),
  BUYER: G('BUYER', 'PURCHASER', 'PURCHASER_LEAD', 'PROCUREMENT_MANAGER', 'GM'),
  WAREHOUSE: G('WAREHOUSE', 'WAREHOUSE_LEAD', 'BUYER', 'GM'),
  PRODUCTION: G('PROD_MGR', 'PRODUCTION_MANAGER', 'ENGINEER', 'OPERATOR', 'QC', 'GM'),
  QUALITY: G('QC', 'PROD_MGR', 'PRODUCTION_MANAGER', 'WAREHOUSE', 'GM'),
  FINANCE: G('FINANCE', 'GM'),
  HR: G('HR', 'GM'),
  GM_ONLY: G('GM', 'PROCUREMENT_MANAGER'),
} as const

/** 顶级菜单 → 可访问角色（null = 全员） */
export const MODULE_ROLES: Record<string, string[] | null> = {
  '/dashboard': null,
  '/warehouse': G('WAREHOUSE', 'WAREHOUSE_LEAD', 'BUYER', 'PROD_MGR', 'PRODUCTION_MANAGER', 'QC', 'GM'),
  '/sales': ROLE_GROUPS.SALES,
  '/production': G('PROD_MGR', 'PRODUCTION_MANAGER', 'OPERATOR', 'QC', 'GM'),  // 工程师不在生产模块菜单中（5.1 整改：仅工程数据）
  '/engineering': G('ENGINEER', 'GM'),
  '/material': G('ENGINEER', 'GM'),  // legacy 深链（BOM/条码详情等）
  '/quality': ROLE_GROUPS.QUALITY,
  '/sourcing': G('BUYER', 'PURCHASER', 'PURCHASER_LEAD', 'PROCUREMENT_MANAGER', 'WAREHOUSE', 'GM'),
  '/finance': ROLE_GROUPS.FINANCE,
  '/hr': ROLE_GROUPS.HR,
  '/admin': [...ADMIN_ROLES],
}

/**
 * 子菜单/路由级 RBAC（精确路径优先）
 * null = 继承模块级 canAccessModule
 */
export const SUBMENU_ROLES: Record<string, string[] | null> = {
  // 工作台（各驾驶舱按角色隔离；index 全员可见）
  '/dashboard/index': null,
  '/dashboard/production': ROLE_GROUPS.PRODUCTION,
  '/dashboard/sales': ROLE_GROUPS.SALES,
  '/dashboard/finance': ROLE_GROUPS.FINANCE,
  '/dashboard/quality': ROLE_GROUPS.QUALITY,
  '/dashboard/outsource': G('PROD_MGR', 'PRODUCTION_MANAGER', 'BUYER', 'PURCHASER', 'GM'),
  '/dashboard/procurement': ROLE_GROUPS.BUYER,
  '/dashboard/engineer': G('ENGINEER', 'PROD_MGR', 'PRODUCTION_MANAGER', 'GM'),
  '/dashboard/warehouse': G('WAREHOUSE', 'WAREHOUSE_LEAD', 'BUYER', 'GM'),
  '/dashboard/alerts': G('GM'),
  '/dashboard/multi': G('GM', 'PROD_MGR', 'PRODUCTION_MANAGER', 'FINANCE'),
  '/dashboard/performance-board': G('PROD_MGR', 'PRODUCTION_MANAGER', 'OPERATOR', 'GM'),

  // 客户现场演示
  '/visitor/progress': G('CUSTOMER_VISITOR', 'PROD_MGR', 'PRODUCTION_MANAGER', 'GM'),
  '/sales/customers': ROLE_GROUPS.SALES,
  '/sales/customer/protection': G('SALES', 'SALES_MGR', 'SALES_MANAGER', 'GM'),
  '/sales/quotes': ROLE_GROUPS.SALES,
  '/sales/quotes/approval': G('SALES_MGR', 'SALES_MANAGER', 'GM'),
  '/sales/orders': ROLE_GROUPS.SALES,
  '/sales/contracts': G('SALES', 'SALES_MGR', 'SALES_MANAGER', 'FINANCE', 'GM'),

  // 生产
  '/production/workorders': ROLE_GROUPS.PRODUCTION,
  '/production/workorder-create': G('PROD_MGR', 'PRODUCTION_MANAGER', 'GM'),
  '/production/pending-production': G('PROD_MGR', 'PRODUCTION_MANAGER', 'GM'),
  '/production/schedule': G('PROD_MGR', 'PRODUCTION_MANAGER', 'ENGINEER', 'GM'),
  '/production/schedule-gantt': G('PROD_MGR', 'PRODUCTION_MANAGER', 'ENGINEER', 'GM'),
  '/production/mrp': G('PROD_MGR', 'PRODUCTION_MANAGER', 'ENGINEER', 'GM'),
  '/production/outsource': G('PROD_MGR', 'PRODUCTION_MANAGER', 'BUYER', 'PURCHASER', 'GM'),
  '/production/machine-mgr': G('PROD_MGR', 'PRODUCTION_MANAGER', 'GM'),
  '/production/workbench': G('PROD_MGR', 'PRODUCTION_MANAGER', 'GM'),
  '/production/scheduling': G('PROD_MGR', 'PRODUCTION_MANAGER', 'GM'),
  '/production/workorder-mgr': G('PROD_MGR', 'PRODUCTION_MANAGER', 'GM'),
  '/production/outsource-mgr': G('PROD_MGR', 'PRODUCTION_MANAGER', 'GM'),
  '/production/allocation': G('PROD_MGR', 'PRODUCTION_MANAGER', 'GM'),
  '/finance/receivable-payable': ROLE_GROUPS.FINANCE,
  '/finance/payment-approval': ROLE_GROUPS.FINANCE,
  '/finance/material-cost': G('FINANCE', 'GM', 'PROD_MGR', 'PRODUCTION_MANAGER'),
  '/warehouse/overview': ROLE_GROUPS.WAREHOUSE,
  '/warehouse/inventory-mgr': ROLE_GROUPS.WAREHOUSE,
  '/hr/salary': G('HR', ...ADMIN_ROLES),
  '/production/rework': G('PROD_MGR', 'PRODUCTION_MANAGER', 'BUYER', 'PURCHASER', 'QC', 'GM'),
  '/production/outsub-order': ROLE_GROUPS.BUYER,

  // 工程数据（V2.1 · /engineering/*）
  '/engineering/order-conversion': G('ENGINEER', 'GM'),
  '/engineering/quote-confirmation': G('ENGINEER', 'GM'),
  '/engineering/data': G('ENGINEER', 'GM'),
  '/engineering/my-tasks': G('ENGINEER', 'GM'),

  // 物料 / 仓储（legacy 深链）
  '/material/drawings': G('ENGINEER', 'PROD_MGR', 'PRODUCTION_MANAGER', 'WAREHOUSE', 'BUYER', 'SALES', 'GM'),
  '/material/lookup': G('ENGINEER', 'PROD_MGR', 'PRODUCTION_MANAGER', 'WAREHOUSE', 'BUYER', 'SALES', 'GM'),
  '/material/barcode-list': G('WAREHOUSE', 'WAREHOUSE_LEAD', 'PROD_MGR', 'PRODUCTION_MANAGER', 'ENGINEER', 'GM'),
  '/material/product-route': G('ENGINEER', 'PROD_MGR', 'PRODUCTION_MANAGER', 'GM'),
  '/material/detail': G('ENGINEER', 'PROD_MGR', 'PRODUCTION_MANAGER', 'WAREHOUSE', 'BUYER', 'GM'),
  '/material/material-category': G('ENGINEER', 'PROD_MGR', 'PRODUCTION_MANAGER', 'GM'),
  '/material/boms': G('ENGINEER', 'PROD_MGR', 'PRODUCTION_MANAGER', 'GM'),
  '/material/boms/edit': G('ENGINEER', 'PROD_MGR', 'PRODUCTION_MANAGER', 'GM'),
  '/material/process': G('ENGINEER', 'PROD_MGR', 'PRODUCTION_MANAGER', 'GM'),
  '/material/cost-aggregator': G('FINANCE', 'GM', 'PROD_MGR', 'PRODUCTION_MANAGER'),
  '/material/index': G('ENGINEER', 'PROD_MGR', 'PRODUCTION_MANAGER', 'WAREHOUSE', 'BUYER', 'GM'),
  '/warehouse/index': ROLE_GROUPS.WAREHOUSE,
  '/warehouse/locations': ROLE_GROUPS.WAREHOUSE,
  '/warehouse/batches': ROLE_GROUPS.WAREHOUSE,
  '/warehouse/inventory': ROLE_GROUPS.WAREHOUSE,
  '/warehouse/inventory-alert': G('WAREHOUSE', 'WAREHOUSE_LEAD', 'BUYER', 'PROD_MGR', 'GM'),

  // 品质
  '/quality/workbench': ROLE_GROUPS.QUALITY,
  '/quality/inspection': ROLE_GROUPS.QUALITY,
  '/quality/inspection-create': ROLE_GROUPS.QUALITY,
  '/quality/inspection-template': G('QC', 'ENGINEER', 'PROD_MGR', 'PRODUCTION_MANAGER'),
  '/quality/fa': ROLE_GROUPS.QUALITY,
  '/quality/cmm': ROLE_GROUPS.QUALITY,
  '/quality/defect': ROLE_GROUPS.QUALITY,
  '/quality/pickup': ROLE_GROUPS.QUALITY,

  // 采购
  '/sourcing/rfq': ROLE_GROUPS.BUYER,
  '/sourcing/purchase-transfer': ROLE_GROUPS.BUYER,
  '/sourcing/po': ROLE_GROUPS.BUYER,
  '/sourcing/incoming': G('BUYER', 'PURCHASER', 'PURCHASER_LEAD', 'PROCUREMENT_MANAGER', 'WAREHOUSE', 'GM'),
  '/sourcing/no-order-purchase': ROLE_GROUPS.BUYER,
  '/sourcing/approval-route': G('BUYER', 'PURCHASER', 'PURCHASER_LEAD', 'PROCUREMENT_MANAGER', 'GM'),
  '/sourcing/reconcile': G('BUYER', 'PURCHASER', 'PURCHASER_LEAD', 'FINANCE', 'GM'),
  '/sourcing/outsub-order': ROLE_GROUPS.BUYER,
  '/sourcing/rework': G('BUYER', 'PURCHASER', 'QC', 'GM'),
  '/sourcing/rework-detail': G('BUYER', 'PURCHASER', 'QC', 'GM'),
  '/sourcing/rework-alert': G('BUYER', 'PURCHASER', 'QC', 'GM'),
  '/sourcing/vendors': G('BUYER', 'PURCHASER', 'PURCHASER_LEAD', 'FINANCE', 'GM'),

  // 财务
  '/finance/receivables': ROLE_GROUPS.FINANCE,
  '/finance/payables': ROLE_GROUPS.FINANCE,
  '/finance/aging': ROLE_GROUPS.FINANCE,
  '/finance/cost': ROLE_GROUPS.FINANCE,
  '/finance/payments': ROLE_GROUPS.FINANCE,
  '/finance/profit': G('FINANCE', 'GM', 'SALES_MGR', 'SALES_MANAGER'),
  '/finance/signed-scans': G('FINANCE', 'GM', 'BUYER', 'PURCHASER'),
  '/finance/gm-summary': ROLE_GROUPS.GM_ONLY,

  // 人事
  '/hr/employees': ROLE_GROUPS.HR,
  '/hr/accounts': ROLE_GROUPS.HR,
  '/hr/attendance': ROLE_GROUPS.HR,
  '/hr/payroll': ROLE_GROUPS.HR,

  // 管理
  '/admin/users': [...ADMIN_ROLES],
  '/admin/depts': [...ADMIN_ROLES],
  '/admin/workflows': [...ADMIN_ROLES],
  '/admin/dict': [...ADMIN_ROLES],
  '/admin/keyboard': [...ADMIN_ROLES],
  '/admin/printers': [...ADMIN_ROLES],
  '/admin/label-templates': [...ADMIN_ROLES],
  '/admin/label-print': G('BUYER', 'PURCHASER', 'WAREHOUSE', 'PROD_MGR', 'PRODUCTION_MANAGER', ...ADMIN_ROLES),
  '/admin/print-logs': G('GM', ...ADMIN_ROLES),
  '/admin/email-config': [...ADMIN_ROLES],
  '/admin/email-templates': [...ADMIN_ROLES],
  '/admin/field-encryption': G('GM', 'FINANCE', ...ADMIN_ROLES),
  '/admin/reports/workflow-stats': G('GM', ...ADMIN_ROLES),
  '/admin/reports/sales-ranking': G('GM', 'SALES_MGR', 'SALES_MANAGER', ...ADMIN_ROLES),
  '/admin/reports/sales-trend': G('GM', 'SALES_MGR', 'SALES_MANAGER', ...ADMIN_ROLES),
  '/admin/reports/customer-analysis': G('GM', 'FINANCE', ...ADMIN_ROLES),
}

/** 前缀匹配（详情页等 hideInMenu 路由） */
const PREFIX_MENU_ROLES: Array<[string, string[]]> = [
  ['/sales/quotes', ROLE_GROUPS.SALES],
  ['/sales/orders', ROLE_GROUPS.SALES],
  ['/sales/contracts', G('SALES', 'SALES_MGR', 'SALES_MANAGER', 'FINANCE', 'GM')],
  ['/sourcing/', ROLE_GROUPS.BUYER],
  ['/production/workorder', ROLE_GROUPS.PRODUCTION],
  ['/production/outsource', G('PROD_MGR', 'PRODUCTION_MANAGER', 'BUYER', 'PURCHASER', 'GM')],
  ['/sourcing/rfq', ROLE_GROUPS.BUYER],
  ['/sourcing/purchase-transfer', ROLE_GROUPS.BUYER],
  ['/sourcing/po', ROLE_GROUPS.BUYER],
  ['/sourcing/incoming', G('BUYER', 'PURCHASER', 'WAREHOUSE', 'GM')],
  ['/sourcing/reconcile', G('BUYER', 'PURCHASER', 'FINANCE', 'GM')],
  ['/sourcing/rework', G('BUYER', 'PURCHASER', 'QC', 'GM')],
  ['/warehouse/', ROLE_GROUPS.WAREHOUSE],
  ['/engineering/', G('ENGINEER', 'GM')],
  ['/material/', G('ENGINEER', 'PROD_MGR', 'PRODUCTION_MANAGER', 'WAREHOUSE', 'BUYER', 'SALES', 'GM')],
  ['/admin/hr', G('HR', ...ADMIN_ROLES)],
  ['/hr', G('HR', ...ADMIN_ROLES)],
]

/** 路由 meta.roles 别名（前端 hasRole 兼容 DB 编码） */
const ROLE_ALIASES: Record<string, string[]> = {
  SYS_ADMIN: [...ADMIN_ROLES, 'WAREHOUSE', 'BUYER', 'PURCHASER', 'GM', 'FINANCE', 'SALES', 'QC', 'PROD_MGR', 'HR'],
  BUYER: ['PURCHASER', 'PROCUREMENT_MANAGER'],
  SALES_MGR: ['SALES_MANAGER'],
  PROD_MGR: ['PRODUCTION_MANAGER'],
  PRODUCTION_MANAGER: ['PROD_MGR'],
  PURCHASER: ['BUYER'],
  WAREHOUSE_LEAD: ['WAREHOUSE'],
}

export function normalizeRoles(roles: string[] | undefined | null): string[] {
  const set = new Set<string>()
  for (const r of roles ?? []) {
    if (!r) continue
    set.add(r)
    for (const alias of ROLE_ALIASES[r] ?? []) {
      set.add(alias)
    }
  }
  return [...set]
}

export function isAdmin(roles: string[]): boolean {
  const n = normalizeRoles(roles)
  return n.some((r) => (ADMIN_ROLES as readonly string[]).includes(r))
}

export function hasAnyRole(userRoles: string[] | undefined | null, required: string[]): boolean {
  const normalized = normalizeRoles(userRoles)
  if (isAdmin(normalized)) return true
  // 仅展开用户侧角色别名；勿展开 required 中的 SYS_ADMIN（其别名含全部业务角色会导致越权）
  return required.some((r) => normalized.includes(r))
}

export function canAccessModule(modulePath: string, userRoles: string[] | undefined | null): boolean {
  if (!(modulePath in MODULE_ROLES)) return false
  const allowed = MODULE_ROLES[modulePath]
  if (allowed === null) return true
  return hasAnyRole(userRoles, allowed)
}

export function canAccessPath(path: string, userRoles: string[] | undefined | null): boolean {
  const top = '/' + (path.split('/').filter(Boolean)[0] ?? '')
  return canAccessModule(top, userRoles)
}

/** 子菜单 / 详情页路由 RBAC */
export function canAccessRoute(
  path: string,
  userRoles: string[] | undefined | null,
  explicitRoles?: string[],
): boolean {
  if (explicitRoles?.length) {
    return hasAnyRole(userRoles, explicitRoles)
  }
  const normalized = path.replace(/\/+/g, '/').replace(/\/$/, '') || '/'

  const exact = SUBMENU_ROLES[normalized]
  if (exact !== undefined) {
    if (exact === null) return canAccessPath(normalized, userRoles)
    return hasAnyRole(userRoles, exact)
  }

  for (const [prefix, roles] of PREFIX_MENU_ROLES) {
    if (normalized === prefix || normalized.startsWith(`${prefix}/`) || normalized.startsWith(prefix)) {
      return hasAnyRole(userRoles, roles)
    }
  }

  return canAccessPath(normalized, userRoles)
}

const TEMPLATE_EDITOR_ROLES = G('ENGINEER', 'PROD_MGR', 'PRODUCTION_MANAGER')
const TEMPLATE_PUBLISHER_ROLES = G('PROD_MGR', 'PRODUCTION_MANAGER')

/** 工程师/品质主管可编辑检验模板 */
export function canEditInspectionTemplate(userRoles: string[] | undefined | null): boolean {
  return hasAnyRole(userRoles, TEMPLATE_EDITOR_ROLES)
}

/** 品质主管可发布/停用检验模板 */
export function canPublishInspectionTemplate(userRoles: string[] | undefined | null): boolean {
  return hasAnyRole(userRoles, TEMPLATE_PUBLISHER_ROLES)
}
