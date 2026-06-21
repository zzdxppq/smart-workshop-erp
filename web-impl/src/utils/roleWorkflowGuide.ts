/**
 * 全链路流程角色职责指南（V2.1 · 各角色菜单权限一览表）
 */

export interface WorkflowStep {
  step: string
  action: string
  route?: string
}

export interface RoleWorkflowGuide {
  roleKey: string
  roleLabel: string
  goal: string
  endpoint: 'PC' | 'APP' | 'PC+APP'
  steps: WorkflowStep[]
  dashboardPath: string
  quickRoutes: { label: string; path: string }[]
}

export const ROLE_PRIORITY = [
  'GM',
  'SALES', 'SALES_MGR', 'SALES_MANAGER',
  'PROD_MGR', 'PRODUCTION_MANAGER', 'PROD_PLANNER',
  'BUYER', 'PURCHASER', 'PURCHASER_LEAD', 'PROCUREMENT_MANAGER',
  'WAREHOUSE', 'WAREHOUSE_LEAD',
  'QC',
  'ENGINEER',
  'OPERATOR',
  'FINANCE',
  'HR',
] as const

const GUIDES: Record<string, RoleWorkflowGuide> = {
  SALES: {
    roleKey: 'SALES',
    roleLabel: '业务员',
    goal: '报价获客（可上传新图号）→ 工程师定义工艺算价 → 多级审批 → 客户确认后从图纸库建销售订单（提交即生效、自动生成料号）→ 合同回款',
    endpoint: 'PC',
    dashboardPath: '/dashboard/sales',
    quickRoutes: [
      { label: '新建报价', path: '/sales/quotes/new' },
      { label: '新建销售订单', path: '/sales/orders/new' },
      { label: '报价单', path: '/sales/quotes' },
      { label: '销售订单', path: '/sales/orders' },
      { label: '客户档案', path: '/sales/customers' },
      { label: '合同回款', path: '/sales/contracts' },
    ],
    steps: [
      { step: '①', action: '维护客户档案与客户保护期', route: '/sales/customers' },
      { step: '②', action: '新建报价：上传/选图号，提交工程师定义工艺与算价（报价阶段不填交期）', route: '/sales/quotes/new' },
      { step: '③', action: '工程师完成后提交审批；通过后导出 PDF / 发送客户邮箱', route: '/sales/quotes/approval' },
      { step: '④', action: '客户确认后新建销售订单：仅从图纸库选图，填交期提交（无需审批，自动生成料号）', route: '/sales/orders/new' },
      { step: '⑤', action: '跟踪已生效订单、合同回款与利润', route: '/sales/contracts' },
    ],
  },
  PROD_MGR: {
    roleKey: 'PROD_MGR',
    roleLabel: '生管',
    goal: '排产 · 管工单 · 管委外 · 管物料 · 管设备',
    endpoint: 'PC',
    dashboardPath: '/dashboard/production',
    quickRoutes: [
      { label: '生产工作台', path: '/production/workbench' },
      { label: '排产中心', path: '/production/scheduling' },
      { label: '工单管理', path: '/production/workorder-mgr' },
      { label: '委外管理', path: '/production/outsource-mgr' },
      { label: 'MRP中心', path: '/production/mrp' },
      { label: '设备管理', path: '/production/machine-mgr' },
    ],
    steps: [
      { step: '①', action: '生产工作台：总览、委外面板、逾期预警', route: '/production/workbench' },
      { step: '②', action: '排产中心：待转产订单、甘特排产、工序分配', route: '/production/scheduling' },
      { step: '③', action: '工单全生命周期管理', route: '/production/workorder-mgr' },
      { step: '④', action: '委外列表与委外下单协同', route: '/production/outsource-mgr' },
      { step: '⑤', action: 'MRP 缺料分析与采购申请', route: '/production/mrp' },
    ],
  },
  BUYER: {
    roleKey: 'BUYER',
    roleLabel: '采购员',
    goal: '接收缺料清单+待委外清单 → 询比价 → 创建采购单/委外单 → 到货跟踪',
    endpoint: 'PC',
    dashboardPath: '/dashboard/procurement',
    quickRoutes: [
      { label: '询比价工作台', path: '/sourcing/rfq' },
      { label: '采购转单', path: '/sourcing/purchase-transfer' },
      { label: '委外转单', path: '/sourcing/outsub-order' },
      { label: '到货提醒', path: '/sourcing/incoming' },
      { label: '厂商资料', path: '/sourcing/vendors' },
    ],
    steps: [
      { step: '①', action: 'MRP 缺料清单转采购单', route: '/sourcing/purchase-transfer' },
      { step: '②', action: '询比价、定标', route: '/sourcing/rfq' },
      { step: '③', action: '生管推送待委外清单 → 选厂商下单', route: '/sourcing/outsub-order' },
      { step: '④', action: '到货提醒与委外对账', route: '/sourcing/incoming' },
    ],
  },
  WAREHOUSE: {
    roleKey: 'WAREHOUSE',
    roleLabel: '仓管员',
    goal: '收货 · 入库 · 出库 · 库存管理 · 条码打印',
    endpoint: 'PC+APP',
    dashboardPath: '/dashboard/warehouse',
    quickRoutes: [
      { label: '仓储总览', path: '/warehouse/overview' },
      { label: '库存管理', path: '/warehouse/inventory-mgr' },
      { label: '入库单', path: '/warehouse/inbound' },
      { label: '出库单', path: '/warehouse/outbound' },
      { label: '物料条码', path: '/material/barcode-list' },
    ],
    steps: [
      { step: '①', action: '仓储总览与库存预警', route: '/warehouse/overview' },
      { step: '②', action: '执行入库并打印标签', route: '/warehouse/inbound' },
      { step: '③', action: '扫码出库与批次管理', route: '/warehouse/outbound' },
      { step: '④', action: '物料条码生成与打印', route: '/material/barcode-list' },
    ],
  },
  QC: {
    roleKey: 'QC',
    roleLabel: '品检员',
    goal: '来料检(IQC) · 过程检(IPQC) · 成品检(OQC) · FA首件 · 三次元 · 不良品处理',
    endpoint: 'PC+APP',
    dashboardPath: '/dashboard/quality',
    quickRoutes: [
      { label: '检验工作台', path: '/quality/workbench' },
      { label: 'FA首件', path: '/quality/fa' },
      { label: '三次元', path: '/quality/cmm' },
      { label: '不良品', path: '/quality/defect' },
      { label: '提货检', path: '/quality/pickup' },
    ],
    steps: [
      { step: '①', action: '检验工作台：IQC/IPQC/OQC 三合一', route: '/quality/workbench' },
      { step: '②', action: 'FA 首件与 CMM 三次元检测', route: '/quality/fa' },
      { step: '③', action: '不良品记录与处置（返工/报废/让步）', route: '/quality/defect' },
    ],
  },
  ENGINEER: {
    roleKey: 'ENGINEER',
    roleLabel: '工程师',
    goal: '报价阶段：定义工艺路线与工时、完成算价；订单阶段：收到已生效订单待办，细化工艺参数与 BOM，提交后订单进入待转产池',
    endpoint: 'PC',
    dashboardPath: '/dashboard/engineer',
    quickRoutes: [
      { label: '工程师驾驶舱', path: '/dashboard/engineer' },
      { label: '报价工艺定义', path: '/engineering/quote-confirmation' },
      { label: '订单工程转化', path: '/engineering/order-conversion' },
      { label: '工程数据', path: '/engineering/data' },
      { label: '待办任务中心', path: '/engineering/my-tasks' },
    ],
    steps: [
      { step: '①', action: '报价待办：为每条明细定义工艺路线、预估工时并完成算价', route: '/engineering/quote-confirmation' },
      { step: '②', action: '订单待办：销售订单提交后收到通知，细化工艺参数、编制 BOM', route: '/engineering/order-conversion' },
      { step: '③', action: '提交工程转化 → 订单状态待转产，生管转工单（工程师环节结束）', route: '/production/pending-production' },
    ],
  },
  GM: {
    roleKey: 'GM',
    roleLabel: '总经理',
    goal: '除管理后台外全部业务菜单 · 大额审批 · 经营看板与告警',
    endpoint: 'PC',
    dashboardPath: '/dashboard/alerts',
    quickRoutes: [
      { label: '总经理驾驶舱', path: '/dashboard/alerts' },
      { label: '多维度看板', path: '/dashboard/multi' },
      { label: '利润分析', path: '/finance/profit' },
    ],
    steps: [
      { step: '①', action: '报价/采购/付款大额审批', route: '/dashboard/alerts' },
      { step: '②', action: '销售/生产/财务/品质驾驶舱看数', route: '/dashboard/multi' },
      { step: '③', action: '利润与经营告警处理', route: '/finance/profit' },
    ],
  },
  FINANCE: {
    roleKey: 'FINANCE',
    roleLabel: '财务',
    goal: '应收应付 · 成本核算 · 利润分析 · 付款审批 · 料号成本',
    endpoint: 'PC',
    dashboardPath: '/dashboard/finance',
    quickRoutes: [
      { label: '应收应付', path: '/finance/receivable-payable' },
      { label: '成本核算', path: '/finance/cost' },
      { label: '利润分析', path: '/finance/profit' },
      { label: '付款审批', path: '/finance/payment-approval' },
    ],
    steps: [
      { step: '①', action: '应收应付与账龄管理', route: '/finance/receivable-payable' },
      { step: '②', action: '工单成本核算', route: '/finance/cost' },
      { step: '③', action: '>10万付款双签审批', route: '/finance/payment-approval' },
    ],
  },
  OPERATOR: {
    roleKey: 'OPERATOR',
    roleLabel: '操作工',
    goal: '扫码开工 · 扫码报工 · 扫码过站 · 查看个人绩效（PC 仅绩效看板）',
    endpoint: 'APP',
    dashboardPath: '/dashboard/performance-board',
    quickRoutes: [{ label: '绩效看板', path: '/dashboard/performance-board' }],
    steps: [
      { step: 'APP', action: '扫工单码开工、报工、过站', route: '/production/workorders' },
      { step: 'PC', action: '绩效看板：产量、合格率、工时利用率、考核分', route: '/dashboard/performance-board' },
    ],
  },
  HR: {
    roleKey: 'HR',
    roleLabel: '人事',
    goal: '员工档案 · 系统账号 · 考勤 · 薪酬核算 · 绩效 · 招聘',
    endpoint: 'PC',
    dashboardPath: '/dashboard/index',
    quickRoutes: [
      { label: '员工列表', path: '/hr/employees' },
      { label: '薪酬核算', path: '/hr/salary' },
      { label: '绩效管理', path: '/hr/performance' },
    ],
    steps: [
      { step: '①', action: '员工档案与岗位字典维护', route: '/hr/employees' },
      { step: '②', action: '考勤月报与薪酬核算', route: '/hr/salary' },
      { step: '③', action: '绩效考核与招聘管理', route: '/hr/performance' },
    ],
  },
}

const ROLE_ALIASES: Record<string, keyof typeof GUIDES> = {
  SALES_MGR: 'SALES',
  SALES_MANAGER: 'SALES',
  PRODUCTION_MANAGER: 'PROD_MGR',
  PROD_PLANNER: 'PROD_MGR',
  PURCHASER: 'BUYER',
  PURCHASER_LEAD: 'BUYER',
  PROCUREMENT_MANAGER: 'BUYER',
  WAREHOUSE_LEAD: 'WAREHOUSE',
}

export function resolveRoleGuide(userRoles: string[]): RoleWorkflowGuide | null {
  const normalized = userRoles.map((r) => r.toUpperCase())
  for (const role of ROLE_PRIORITY) {
    if (!normalized.includes(role)) continue
    const key = ROLE_ALIASES[role] ?? role
    if (GUIDES[key]) return GUIDES[key]
  }
  return null
}

const DASHBOARD_PATH_GUIDE: Record<string, string> = {
  '/dashboard/sales': 'SALES',
  '/dashboard/procurement': 'BUYER',
  '/dashboard/production': 'PROD_MGR',
  '/dashboard/finance': 'FINANCE',
  '/dashboard/quality': 'QC',
  '/dashboard/outsource': 'BUYER',
  '/dashboard/engineer': 'ENGINEER',
  '/dashboard/warehouse': 'WAREHOUSE',
  '/dashboard/alerts': 'GM',
  '/dashboard/multi': 'GM',
  '/dashboard/performance-board': 'OPERATOR',
}

export function resolveGuideForDashboard(
  path: string,
  userRoles: string[],
  roleKey?: string,
): RoleWorkflowGuide | null {
  if (roleKey && GUIDES[roleKey]) return GUIDES[roleKey]
  const guideKey = DASHBOARD_PATH_GUIDE[path]
  if (guideKey && GUIDES[guideKey]) return GUIDES[guideKey]
  return resolveRoleGuide(userRoles)
}

export function resolveDashboardPath(userRoles: string[]): string {
  return resolveRoleGuide(userRoles)?.dashboardPath ?? '/dashboard/index'
}

export function getAllRoleGuides(): RoleWorkflowGuide[] {
  return Object.values(GUIDES)
}
