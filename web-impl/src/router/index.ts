import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { canAccessRoute, hasAnyRole } from '@/utils/roleAccess'
import { canAccessRoute as canAccessRouteWithMenu } from '@/utils/menuAccess'
import { isSessionActive } from '@/utils/authSession'

/**
 * 7 大顶级菜单路由表
 * @see docs/ux-handoff.md §3.1
 */
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { public: true, title: '登录' },
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      // 1. 工作台
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/layouts/MenuLayout.vue'),
        redirect: '/dashboard/index',
        meta: { title: '工作台', icon: 'HomeFilled' },
        children: [
          { path: 'index', name: 'DashboardIndex', component: () => import('@/views/dashboard/Index.vue'), meta: { title: '总览驾驶舱' } },
          { path: 'production', name: 'ProductionDashboard', component: () => import('@/views/dashboard/Production.vue'), meta: { title: '生产驾驶舱' } },
          { path: 'sales', name: 'SalesDashboard', component: () => import('@/views/dashboard/Sales.vue'), meta: { title: '销售驾驶舱' } },
          { path: 'finance', name: 'FinanceDashboard', component: () => import('@/views/dashboard/DashboardFinance.vue'), meta: { title: '财务驾驶舱' } },
          { path: 'quality', name: 'QualityDashboard', component: () => import('@/views/dashboard/DashboardQuality.vue'), meta: { title: '品质驾驶舱' } },
          { path: 'outsource', name: 'OutsourceDashboard', component: () => import('@/views/dashboard/Outsource.vue'), meta: { title: '委外驾驶舱' } },
          { path: 'procurement', name: 'ProcurementDashboard', component: () => import('@/views/dashboard/DashboardProcurement.vue'), meta: { title: '采购驾驶舱' } },
          { path: 'engineer', name: 'EngineerDashboard', component: () => import('@/views/dashboard/DashboardEngineer.vue'), meta: { title: '工程师驾驶舱' } },
          { path: 'warehouse', name: 'WarehouseDashboard', component: () => import('@/views/dashboard/DashboardWarehouse.vue'), meta: { title: '仓管驾驶舱' } },
          { path: 'alerts', name: 'DashboardAlerts', component: () => import('@/views/dashboard/DashboardAlerts.vue'), meta: { title: '总经理驾驶舱' } },
          { path: 'multi', name: 'MultiDashboard', component: () => import('@/views/dashboard/MultiDashboard.vue'), meta: { title: '多维度驾驶舱' } },
          { path: 'performance-board', name: 'PerformanceBoard', component: () => import('@/views/dashboard/PerformanceBoard.vue'), meta: { title: '绩效驾驶舱' } },
        ],
      },
      // 1.5 仓储
      {
        path: 'warehouse',
        name: 'Warehouse',
        component: () => import('@/layouts/MenuLayout.vue'),
        redirect: '/warehouse/index',
        meta: { title: '仓储', icon: 'Box' },
        children: [
          { path: 'index',          name: 'WarehouseIndex',  component: () => import('@/views/warehouse/Index.vue'),         meta: { title: '仓储总览', menuGroup: '仓储总览' } },
          { path: 'inbound',        name: 'WarehouseInbound', component: () => import('@/views/warehouse/InboundOrders.vue'), meta: { title: '入库单', menuGroup: '出入库' } },
          { path: 'outbound',       name: 'WarehouseOutbound', component: () => import('@/views/warehouse/OutboundOrders.vue'), meta: { title: '出库单', menuGroup: '出入库' } },
          { path: 'stock-query',    name: 'StockQuery',      component: () => import('@/views/warehouse/StockQuery.vue'),     meta: { title: '库存查询', menuGroup: '库存批次' } },
          { path: 'locations',      name: 'Locations',       component: () => import('@/views/warehouse/Locations.vue'),     meta: { title: '库位树', menuGroup: '仓储总览', hideInMenu: true } },
          { path: 'location-detail/:code', name: 'LocationDetail', component: () => import('@/views/warehouse/LocationDetail.vue'), meta: { title: '库位详情', hideInMenu: true } },
          { path: 'batches',        name: 'BatchList',       component: () => import('@/views/warehouse/BatchList.vue'),      meta: { title: '批次列表', menuGroup: '库存批次' } },
          { path: 'batch-trace/:batchNo', name: 'BatchTrace', component: () => import('@/views/warehouse/BatchTrace.vue'),     meta: { title: '批次追溯', hideInMenu: true } },
          { path: 'inventory',      name: 'WarehouseInventory', component: () => import('@/views/warehouse/Inventory.vue'),      meta: { title: '库存', menuGroup: '库存批次' } },
          { path: 'inventory-alert',name: 'InventoryAlert',  component: () => import('@/views/warehouse/InventoryAlert.vue'), meta: { title: '库存预警', menuGroup: '库存批次' } },
          { path: 'stocktake',      name: 'Stocktake',       component: () => import('@/views/warehouse/Stocktake.vue'),       meta: { title: '盘点单', menuGroup: '库存批次' } },
          { path: 'alert-resolve/:id?', name: 'AlertResolve', component: () => import('@/views/warehouse/AlertResolve.vue'), meta: { title: '预警解决', hideInMenu: true } },
        ],
      },
      // 2. 销售
      {
        path: 'sales',
        name: 'Sales',
        component: () => import('@/layouts/MenuLayout.vue'),
        redirect: '/sales/customers',
        meta: { title: '销售', icon: 'Money' },
        children: [
          { path: 'customers',        name: 'Customers',  component: () => import('@/views/sales/Customers.vue'), meta: { title: '客户档案', menuGroup: '客户管理' } },
          { path: 'customers/:id',    name: 'CustomerDetail', component: () => import('@/views/sales/CustomerDetail.vue'), meta: { title: '客户详情' } },
          { path: 'customer/protection', name: 'CustomerProtection', component: () => import('@/views/sales/CustomerProtection.vue'), meta: { title: '客户保护', menuGroup: '客户管理' } },
          { path: 'quotes',           name: 'Quotes',     component: () => import('@/views/sales/Quotes.vue'),    meta: { title: '新建报价单', menuGroup: '报价订单' } },
          { path: 'quotes/new',       name: 'QuoteNew',   component: () => import('@/views/sales/QuoteForm.vue'), meta: { title: '新建报价', hideInMenu: true } },
          { path: 'quotes/approval',  name: 'QuoteApproval', component: () => import('@/views/sales/QuoteApproval.vue'), meta: { title: '报价审批', menuGroup: '报价订单', roles: ['SALES_MGR', 'SALES_MANAGER', 'GM'] } },
          { path: 'quote-templates', name: 'QuoteCostItems', component: () => import('@/views/sales/QuoteCostItems.vue'), meta: { title: '报价范本管理', hideInMenu: true } },
          { path: 'quotes/:id',       name: 'QuoteEdit',  component: () => import('@/views/sales/QuoteForm.vue'), meta: { title: '编辑报价', hideInMenu: true } },
          { path: 'orders',           name: 'Orders',     component: () => import('@/views/sales/Orders.vue'),    meta: { title: '新建销售订单', menuGroup: '销售订单' } },
          { path: 'orders/new',       name: 'OrderNew',   component: () => import('@/views/sales/OrderForm.vue'), meta: { title: '新建销售订单', hideInMenu: true } },
          { path: 'orders/:id/edit',  name: 'OrderEdit',  component: () => import('@/views/sales/OrderForm.vue'), meta: { title: '编辑销售订单', hideInMenu: true } },
          { path: 'orders/:id',       name: 'OrderDetail', component: () => import('@/views/sales/OrderDetail.vue'), meta: { title: '订单详情', hideInMenu: true } },
          { path: 'orders/:id/change', name: 'OrderChange', component: () => import('@/views/sales/OrderChange.vue'), meta: { title: '订单变更', hideInMenu: true } },
          { path: 'orders/:id/timeline', name: 'OrderTimeline', component: () => import('@/views/sales/OrderTimeline.vue'), meta: { title: '订单时间线', hideInMenu: true } },
          { path: 'contracts',        name: 'Contracts',  component: () => import('@/views/sales/Contracts.vue'), meta: { title: '合同回款', menuGroup: '合同回款' } },
          { path: 'contracts/:id/payment-plan', name: 'ContractPaymentPlan', component: () => import('@/views/sales/ContractSub.vue'), meta: { title: '回款计划' } },
          { path: 'contracts/:id/payment-reg', name: 'ContractPaymentReg', component: () => import('@/views/sales/ContractSub.vue'), meta: { title: '查看收款' } },
          { path: 'contracts/:id/profit', name: 'ContractProfit', component: () => import('@/views/sales/ContractSub.vue'), meta: { title: '订单利润' } },
        ],
      },
      // 3. 生产（含 V1.3.7 工序分配）
      {
        path: 'production',
        name: 'Production',
        component: () => import('@/layouts/MenuLayout.vue'),
        redirect: '/production/workorders',
        meta: { title: '生产', icon: 'Tools' },
        children: [
          { path: 'workorders',       name: 'Workorders',      component: () => import('@/views/production/Workorders.vue'),    meta: { title: '工单', menuGroup: '工单排产' } },
          { path: 'pending-production', name: 'PendingProduction', component: () => import('@/views/production/PendingProduction.vue'), meta: { title: '待转产订单', menuGroup: '工单排产' } },
          { path: 'pending-production-detail/:id', name: 'PendingProductionDetail', component: () => import('@/views/production/PendingProductionDetail.vue'), meta: { title: '待转产订单详情', hideInMenu: true } },
          { path: 'workorder-detail/:id', name: 'WorkorderDetail', component: () => import('@/views/production/WorkorderDetail.vue'), meta: { title: '工单详情', hideInMenu: true } },
          { path: 'workorder-create', name: 'WorkorderCreate', component: () => import('@/views/production/WorkorderCreate.vue'), meta: { title: '工单创建', hideInMenu: true } },
          { path: 'schedule',         name: 'Schedule',        component: () => import('@/views/production/Schedule.vue'),       meta: { title: '排产看板', menuGroup: '工单排产' } },
          { path: 'schedule-gantt',   name: 'ScheduleGantt',   component: () => import('@/views/production/ScheduleGantt.vue'), meta: { title: '排产甘特', menuGroup: '工单排产' } },
          { path: 'schedule-calendar',name: 'ScheduleCalendar',component: () => import('@/views/production/ScheduleCalendar.vue'), meta: { title: '排产日历', hideInMenu: true } },
          { path: 'workorder-steps/:id', name: 'WorkorderSteps', component: () => import('@/views/production/WorkorderSteps.vue'), meta: { title: '工单工序', hideInMenu: true } },
          { path: 'report-list',      name: 'ReportList',      component: () => import('@/views/production/ReportList.vue'),     meta: { title: '报工历史', hideInMenu: true } },
          { path: 'mrp',              name: 'MRP',             component: () => import('@/views/production/MRP.vue'),            meta: { title: 'MRP 中心', menuGroup: 'MRP委外' } },
          { path: 'mrp-run',          name: 'MrpRun',          component: () => import('@/views/production/MrpRun.vue'),         meta: { title: 'MRP 运算历史', hideInMenu: true } },
          { path: 'mrp-result/:runId',name: 'MrpResult',       component: () => import('@/views/production/MrpResult.vue'),      meta: { title: 'MRP 结果', hideInMenu: true } },
          { path: 'mrp-shortage/:runId', name: 'MrpShortage',  component: () => import('@/views/production/MrpShortage.vue'),    meta: { title: 'MRP 缺料', hideInMenu: true } },
          { path: 'outsource',        name: 'Outsource',       component: () => import('@/views/production/Outsource.vue'),      meta: { title: '委外列表', menuGroup: 'MRP委外' } },
          { path: 'outsource-detail/:outsourceNo', name: 'OutsourceDetail', component: () => import('@/views/production/OutsourceDetail.vue'), meta: { title: '委外详情', hideInMenu: true } },
          { path: 'outsource-create', name: 'OutsourceCreate', component: () => import('@/views/production/OutsourceCreate.vue'), meta: { title: '委外创建', hideInMenu: true, redline: 'no-vendor-dropdown' } },
          { path: 'outsource-purchase',name: 'OutsourcePurchase', component: () => import('@/views/production/OutsourcePurchaseView.vue'), meta: { title: '委外采购', hideInMenu: true, redline: 'no-process-decision-toggle' } },
          { path: 'allocation',       name: 'Allocation',      component: () => import('@/views/production/Allocation.vue'),     meta: { title: '工序分配', menuGroup: 'MRP委外', redline: 'no-vendor-dropdown' } },
          { path: 'outsub-order',     name: 'OutsubOrder',     redirect: '/sourcing/outsub-order' },
          { path: 'outsub-panel',     name: 'OutsubPanel',     component: () => import('@/views/production/OutsourceStateMachine.vue'), meta: { title: '委外面板', menuGroup: 'MRP委外' } },
          { path: 'state-history',    name: 'StateHistory',    component: () => import('@/views/production/StateHistory.vue'),    meta: { title: '委外状态历史', hideInMenu: true } },
          { path: 'rework',           name: 'Rework',          redirect: '/sourcing/rework' },
          { path: 'rework-detail/:id', name: 'ReworkDetail',   redirect: (to) => `/sourcing/rework-detail/${to.params.id}` },
          { path: 'rework-alert',     name: 'ReworkAlert',     redirect: '/sourcing/rework-alert' },
          { path: 'machines',         name: 'Machines',        component: () => import('@/views/production/Machines.vue'),       meta: { title: '设备机台', menuGroup: '设备' } },
          { path: 'machines/:id',     name: 'MachineDetail',   component: () => import('@/views/production/MachineDetail.vue'), meta: { title: '设备详情', hideInMenu: true } },
        ],
      },
      // 4. 工程数据（V2.1 · /engineering/*）
      {
        path: 'engineering',
        name: 'Engineering',
        component: () => import('@/layouts/MenuLayout.vue'),
        redirect: '/engineering/order-conversion',
        meta: { title: '工程', icon: 'Goods' },
        children: [
          {
            path: 'order-conversion',
            name: 'OrderConversion',
            component: () => import('@/views/engineering/OrderConversion.vue'),
            meta: { title: '订单工程转化' },
          },
          {
            path: 'quote-confirmation',
            name: 'QuoteConfirmation',
            component: () => import('@/views/engineering/QuoteConfirmation.vue'),
            meta: { title: '报价工艺定义' },
          },
          {
            path: 'data',
            name: 'EngineeringData',
            component: () => import('@/views/engineering/EngineeringData.vue'),
            meta: { title: '图纸与料号' },
          },
          {
            path: 'my-tasks',
            name: 'EngineeringMyTasks',
            component: () => import('@/views/engineering/MyTasks.vue'),
            meta: { title: '待办任务中心' },
          },
        ],
      },
      // 4b. 物料（legacy 深链：BOM/工艺/条码详情等 · 菜单已迁至 /engineering 与 /warehouse）
      {
        path: 'material',
        name: 'Material',
        component: () => import('@/layouts/MenuLayout.vue'),
        redirect: '/engineering/data',
        meta: { title: '工程数据', icon: 'Box', hideInMenu: true },
        children: [
          { path: 'drawings', redirect: (to) => ({ path: '/engineering/data', query: { ...to.query, tab: to.query.tab ?? 'drawings' } }) },
          { path: 'lookup', redirect: (to) => ({ path: '/engineering/data', query: { ...to.query, tab: 'drawings' } }) },
          { path: 'barcode-list',     name: 'BarcodeList',      component: () => import('@/views/material/BarcodeList.vue'),      meta: { title: '物料条码', menuPath: '/material/barcode-list', hideInMenu: true } },
          { path: 'barcode-generate', name: 'BarcodeGenerate',  component: () => import('@/views/material/BarcodeGenerate.vue'),  meta: { title: '条码生成', hideInMenu: true } },
          { path: 'barcode-detail',   name: 'BarcodeDetail',    component: () => import('@/views/material/BarcodeDetail.vue'),    meta: { title: '条码详情', hideInMenu: true } },
          { path: 'barcode-detail/:barcodeNo', name: 'BarcodeDetailByCode', component: () => import('@/views/material/BarcodeDetail.vue'), meta: { title: '条码详情', hideInMenu: true } },
          { path: 'barcode-print',    name: 'BarcodePrint',     component: () => import('@/views/material/BarcodePrint.vue'),     meta: { title: '条码打印', hideInMenu: true } },
          { path: 'material-category',name: 'MaterialCategory', component: () => import('@/views/material/MaterialCategory.vue'), meta: { title: '物料分类', hideInMenu: true } },
          { path: 'boms',             name: 'BOMs',             component: () => import('@/views/material/BOMs.vue'),             meta: { title: 'BOM', hideInMenu: true } },
          { path: 'boms/edit',        name: 'BomEdit',          component: () => import('@/views/material/BomEdit.vue'),         meta: { title: 'BOM 编辑', hideInMenu: true } },
          { path: 'process',          name: 'Process',          component: () => import('@/views/material/Process.vue'),          meta: { title: '工艺库', hideInMenu: true } },
          { path: 'process-routes',   name: 'ProcessRoutes',    component: () => import('@/views/material/ProcessRoutes.vue'),    meta: { title: '工艺路线维护', hideInMenu: true } },
          { path: 'product-route/:id?', name: 'ProductRoute',   component: () => import('@/views/material/ProductRoute.vue'),   meta: { title: '产品工艺路线', hideInMenu: true } },
          { path: 'inventory',        name: 'MaterialInventory', redirect: '/warehouse/inventory' },
          { path: 'wh-index',         redirect: '/warehouse/index', meta: { title: '多仓库总览', menuPath: '/warehouse/index' } },
          { path: 'wh-locations',     redirect: '/warehouse/locations', meta: { title: '库位树', menuPath: '/warehouse/locations' } },
          { path: 'wh-batches',       redirect: '/warehouse/batches', meta: { title: '批次列表', menuPath: '/warehouse/batches' } },
          { path: 'wh-inventory',     redirect: '/warehouse/inventory', meta: { title: '库存', menuPath: '/warehouse/inventory' } },
          { path: 'wh-alert',         redirect: '/warehouse/inventory-alert', meta: { title: '库存预警', menuPath: '/warehouse/inventory-alert' } },
          { path: 'cost-aggregator',  name: 'CostAggregator',   component: () => import('@/views/material/CostAggregator.vue'),   meta: { title: '料号成本', hideInMenu: true } },
          { path: 'detail/:id',       name: 'MaterialDetail',   component: () => import('@/views/v138/MaterialDetail.vue'),      meta: { title: '料号详情', hideInMenu: true } },
        ],
      },
      // 5. 品质
      {
        path: 'quality',
        name: 'Quality',
        component: () => import('@/layouts/MenuLayout.vue'),
        redirect: '/quality/inspection',
        meta: { title: '品质', icon: 'Medal' },
        children: [
          { path: 'inspection',       name: 'Inspection',     component: () => import('@/views/quality/Inspection.vue'),    meta: { title: '来料/过程/成品检', menuGroup: '常规检验' } },
          { path: 'inspection-create', name: 'InspectionCreate', component: () => import('@/views/quality/InspectionCreate.vue'), meta: { title: '新建检验单', hideInMenu: true } },
          { path: 'inspection-template', name: 'InspectionTemplate', component: () => import('@/views/quality/InspectionTemplate.vue'), meta: { title: '检验方案模板', menuGroup: '常规检验' } },
          { path: 'concession-approval', name: 'ConcessionApproval', component: () => import('@/views/quality/ConcessionApproval.vue'), meta: { title: '让步审批', menuGroup: '异常处置' } },
          { path: 'inspection-detail/:id', name: 'InspectionDetail', component: () => import('@/views/quality/InspectionDetail.vue'), meta: { title: '检验单详情' } },
          { path: 'inspection-report/:id', name: 'InspectionReport', component: () => import('@/views/quality/InspectionReport.vue'), meta: { title: '检验报告' } },
          { path: 'fa',               name: 'FA',             component: () => import('@/views/quality/FA.vue'),             meta: { title: 'FA 首件', menuGroup: '专项检验' } },
          { path: 'cmm',              name: 'CMM',            component: () => import('@/views/quality/CMM.vue'),            meta: { title: '三次元', menuGroup: '专项检验' } },
          { path: 'defect',           name: 'Defect',         component: () => import('@/views/quality/Defect.vue'),         meta: { title: '不良品', menuGroup: '异常处置' } },
          { path: 'fa-detail/:id',    name: 'FaDetail',       component: () => import('@/views/quality/FaDetail.vue'),       meta: { title: 'FA 详情' } },
          { path: 'fa-report/:id',    name: 'FaReport',       component: () => import('@/views/quality/FaReport.vue'),       meta: { title: 'FA 报告' } },
          { path: 'cmm-detail/:id',   name: 'CmmDetail',      component: () => import('@/views/quality/CmmDetail.vue'),      meta: { title: 'CMM 详情' } },
          { path: 'cmm-report/:id',   name: 'CmmReport',      component: () => import('@/views/quality/CmmReport.vue'),      meta: { title: 'CMM 报告' } },
          { path: 'defect-detail/:id', name: 'DefectDetail',  component: () => import('@/views/quality/DefectDetail.vue'),  meta: { title: '不良品详情' } },
          { path: 'defect-report', name: 'DefectReport',  component: () => import('@/views/quality/DefectReport.vue'),  meta: { title: '不良品报告' } },
          { path: 'pickup',           name: 'Pickup',         component: () => import('@/views/quality/Pickup.vue'),         meta: { title: '提货检', menuGroup: '异常处置' } },
          { path: 'pickup-inspect/:id', name: 'PickupInspect', component: () => import('@/views/quality/PickupInspect.vue'), meta: { title: '提货检验' } },
          { path: 'outsource-inspection', name: 'OutsourceInspection', component: () => import('@/views/quality/OutsourceInspection.vue'), meta: { title: '委外检', menuGroup: '常规检验' } },
        ],
      },
      // 6. 采购（含 V1.3.6/7 月度对账 · 不含"线下"）
      {
        path: 'sourcing',
        name: 'Sourcing',
        component: () => import('@/layouts/MenuLayout.vue'),
        redirect: '/sourcing/rfq',
        meta: { title: '采购', icon: 'ShoppingCart' },
        children: [
          { path: 'rfq',              name: 'RFQ',            component: () => import('@/views/sourcing/RFQ.vue'),           meta: { title: '询比价工作台', menuGroup: '询比价' } },
          { path: 'rfq-create',       name: 'RfqCreate',      component: () => import('@/views/sourcing/RfqCreate.vue'),      meta: { title: '新建询价', hideInMenu: true } },
          { path: 'rfq-detail/:id',   name: 'RfqDetail',      component: () => import('@/views/sourcing/RfqDetail.vue'),      meta: { title: '询价详情' } },
          { path: 'rfq-compare/:id',  name: 'RfqCompare',     component: () => import('@/views/sourcing/RfqCompare.vue'),     meta: { title: '询价比价' } },
          { path: 'rfq-award/:id',    name: 'RfqAward',       component: () => import('@/views/sourcing/RfqAward.vue'),       meta: { title: '询价定标' } },
          { path: 'purchase-transfer', name: 'PurchaseTransfer', component: () => import('@/views/sourcing/PurchaseTransfer.vue'), meta: { title: '采购转单', menuGroup: '采购执行' } },
          { path: 'po',               name: 'PO',             component: () => import('@/views/sourcing/PO.vue'),            meta: { title: '采购订单', menuGroup: '采购执行', hideInMenu: true } },
          { path: 'po-create',        name: 'PoCreate',       component: () => import('@/views/sourcing/PoCreate.vue'),       meta: { title: '新建采购单', hideInMenu: true } },
          { path: 'po-detail/:id',    name: 'PoDetail',       component: () => import('@/views/sourcing/PoDetail.vue'),       meta: { title: '采购单详情', hideInMenu: true } },
          { path: 'incoming',         name: 'Incoming',       component: () => import('@/views/sourcing/Incoming.vue'),      meta: { title: '到货提醒', menuGroup: '采购执行' } },
          { path: 'incoming-detail/:id', name: 'IncomingDetail', component: () => import('@/views/sourcing/IncomingDetail.vue'), meta: { title: '到货详情', hideInMenu: true } },
          { path: 'batch-incoming',   name: 'BatchIncoming',    component: () => import('@/views/v138/BatchIncoming.vue'),     meta: { title: '分批到货进度', hideInMenu: true } },
          { path: 'no-order-purchase', name: 'NoOrderPurchase', component: () => import('@/views/v138/NoOrderPurchase.vue'),  meta: { title: '无订单采购', menuGroup: '采购执行' } },
          { path: 'approval-route',   name: 'ProcurementApproval', component: () => import('@/views/v138/ProcurementApproval.vue'), meta: { title: '审批路由', menuGroup: '系统工具' } },
          { path: 'reconcile',        name: 'Reconcile',      component: () => import('@/views/sourcing/Reconcile.vue'),     meta: { title: '月度对账', menuGroup: '委外对账', redline: 'no-offline-actions' } },
          { path: 'outsub-order',     name: 'SourcingOutsubOrder', component: () => import('@/views/production/OutsubOrder.vue'), meta: { title: '委外转单', menuGroup: '委外执行', redline: 'no-process-decision-toggle' } },
          { path: 'rework',           name: 'SourcingRework',  component: () => import('@/views/production/Rework.vue'),         meta: { title: '返修单', menuGroup: '委外执行' } },
          { path: 'rework-detail/:id', name: 'SourcingReworkDetail', component: () => import('@/views/production/ReworkDetail.vue'), meta: { title: '返修单详情', hideInMenu: true } },
          { path: 'rework-alert',     name: 'SourcingReworkAlert', component: () => import('@/views/production/ReworkAlert.vue'), meta: { title: '返修告警', hideInMenu: true } },
          { path: 'reconcile-create', name: 'ReconcileCreate', component: () => import('@/views/sourcing/ReconcileCreate.vue'), meta: { title: '新建对账单', hideInMenu: true } },
          { path: 'reconcile-detail/:id', name: 'ReconcileDetail', component: () => import('@/views/sourcing/ReconcileDetail.vue'), meta: { title: '对账详情' } },
          { path: 'reconcile-signature/:id', name: 'ReconcileSignature', component: () => import('@/views/sourcing/ReconcileSignature.vue'), meta: { title: '对账签字', hideInMenu: true } },
          { path: 'vendors',          name: 'Vendors',        component: () => import('@/views/sourcing/Vendors.vue'),       meta: { title: '厂商资料', menuGroup: '厂商资料' } },
        ],
      },
      // 7. 财务
      {
        path: 'finance',
        name: 'Finance',
        component: () => import('@/layouts/MenuLayout.vue'),
        redirect: '/finance/receivables',
        meta: { title: '财务', icon: 'CreditCard' },
        children: [
          { path: 'receivables',      name: 'Receivables',  component: () => import('@/views/finance/Receivables.vue'),   meta: { title: '应收账款' } },
          { path: 'payables',         name: 'Payables',     component: () => import('@/views/finance/Payables.vue'),      meta: { title: '应付账款' } },
          { path: 'receivable-detail/:id', name: 'ReceivableDetail', component: () => import('@/views/finance/ReceivableDetail.vue'), meta: { title: '应收详情' } },
          { path: 'payable-detail/:id', name: 'PayableDetail', component: () => import('@/views/finance/PayableDetail.vue'), meta: { title: '应付详情' } },
          { path: 'aging',            name: 'Aging',        component: () => import('@/views/finance/Aging.vue'),         meta: { title: '账龄分析' } },
          { path: 'aging-detail/:id', name: 'AgingDetail',  component: () => import('@/views/finance/AgingDetail.vue'),   meta: { title: '账龄明细' } },
          { path: 'cost',             name: 'Cost',         component: () => import('@/views/finance/Cost.vue'),          meta: { title: '成本核算' } },
          { path: 'cost-detail/:id',  name: 'CostDetail',   component: () => import('@/views/finance/CostDetail.vue'),   meta: { title: '成本详情' } },
          { path: 'payments',         name: 'Payments',     component: () => import('@/views/finance/Payments.vue'),      meta: { title: '付款管理' } },
          { path: 'payment-detail/:id', name: 'PaymentDetail', component: () => import('@/views/finance/PaymentDetail.vue'), meta: { title: '付款详情' } },
          { path: 'profit',           name: 'Profit',       component: () => import('@/views/finance/Profit.vue'),        meta: { title: '利润分析' } },
          { path: 'profit-export',    name: 'ProfitExport', component: () => import('@/views/finance/ProfitExport.vue'), meta: { title: '利润导出', hideInMenu: true } },
          { path: 'signed-scans',     name: 'SignedScans',  component: () => import('@/views/finance/SignedScans.vue'),  meta: { title: '签字扫描件档案' } },
          { path: 'gm-summary',       name: 'GmSummary',    component: () => import('@/views/v138/GmSummary.vue'),       meta: { title: '总经理汇总', roles: ['GM', 'ADMIN'], hideInMenu: true } },
        ],
      },
      // 8. 人事（与财务并列 · PRD Epic 10）
      {
        path: 'hr',
        name: 'HR',
        component: () => import('@/layouts/HrLayout.vue'),
        redirect: '/hr/employees',
        meta: { title: '人事', icon: 'User' },
        children: [
          { path: 'employees', name: 'HREmployees', component: () => import('@/views/admin/HrEmployeeList.vue'), meta: { title: '员工列表', hideInMenu: true } },
          { path: 'accounts', name: 'HRAccounts', component: () => import('@/views/admin/HrSystemAccounts.vue'), meta: { title: '系统账号', hideInMenu: true } },
          { path: 'employee/:id', name: 'HREmployeeDetail', component: () => import('@/views/admin/HrEmployeeDetail.vue'), meta: { title: '员工详情', hideInMenu: true } },
          { path: 'attendance', name: 'HRAttendance', component: () => import('@/views/admin/HrAttendance.vue'), meta: { title: '考勤月报', hideInMenu: true } },
          { path: 'payroll', name: 'Payroll', component: () => import('@/views/admin/Payroll.vue'), meta: { title: '薪酬核算', hideInMenu: true } },
          { path: 'payroll/:id', name: 'PayrollDetail', component: () => import('@/views/admin/PayrollDetail.vue'), meta: { title: '薪酬详情', hideInMenu: true } },
          { path: 'performance', name: 'HRPerformance', component: () => import('@/views/admin/HrPerformance.vue'), meta: { title: '绩效管理', hideInMenu: true } },
          { path: 'recruitment', name: 'HRRecruitment', component: () => import('@/views/admin/HrRecruitment.vue'), meta: { title: '招聘管理', hideInMenu: true } },
        ],
      },
      // 9. 管理（含 V1.3.7 邮件配置）
      {
        path: 'admin',
        name: 'Admin',
        component: () => import('@/layouts/MenuLayout.vue'),
        redirect: '/admin/users',
        meta: { title: '管理', icon: 'Setting' },
        children: [
          { path: 'users',            name: 'Users',        component: () => import('@/views/admin/Users.vue'),         meta: { title: '用户/角色', menuGroup: '系统配置' } },
          { path: 'depts',            name: 'Depts',        component: () => import('@/views/admin/DeptList.vue'),      meta: { title: '部门管理', menuGroup: '系统配置' } },
          { path: 'workflows',        name: 'Workflows',    component: () => import('@/views/admin/Workflows.vue'),     meta: { title: '工作流', menuGroup: '系统配置' } },
          { path: 'dict',             name: 'Dict',         component: () => import('@/views/admin/Dict.vue'),          meta: { title: '字典', menuGroup: '系统配置' } },
          { path: 'keyboard',         name: 'KeyboardSettings', component: () => import('@/views/admin/KeyboardSettings.vue'), meta: { title: '快捷键设置', menuGroup: '系统配置' } },
          { path: 'printers',         name: 'Printers',     component: () => import('@/views/admin/Printers.vue'),      meta: { title: '打印机管理', menuGroup: '消息安全' } },
          { path: 'label-templates',  name: 'LabelTemplates', component: () => import('@/views/admin/LabelTemplates.vue'), meta: { title: '标签模板', menuGroup: '消息安全' } },
          { path: 'label-print',      name: 'LabelPrint',   component: () => import('@/views/admin/LabelPrint.vue'),   meta: { title: '标签打印', menuGroup: '消息安全' } },
          { path: 'print-logs',       name: 'PrintLogs',    component: () => import('@/views/admin/PrintLog.vue'),      meta: { title: '打印历史', menuGroup: '消息安全' } },
          { path: 'email-config',     name: 'EmailConfig',  component: () => import('@/views/admin/EmailConfig.vue'),   meta: { title: '邮件配置', menuGroup: '消息安全' } },
          { path: 'email-templates',  name: 'EmailTemplates', component: () => import('@/views/admin/EmailTemplates.vue'), meta: { title: '邮件模板', menuGroup: '消息安全' } },
          { path: 'field-encryption', name: 'FieldEncryption', component: () => import('@/views/admin/FieldEncryption.vue'), meta: { title: '字段加密', menuGroup: '消息安全' } },
          { path: 'reports/workflow-stats', name: 'AdminWorkflowStats', component: () => import('@/views/reports/WorkflowStats.vue'), meta: { title: '审批事件统计', roles: ['GM', 'ADMIN'], menuGroup: '报表' } },
          { path: 'reports/sales-ranking', name: 'AdminSalesRanking', component: () => import('@/views/reports/SalesRanking.vue'), meta: { title: '销售龙虎榜', menuGroup: '报表' } },
          { path: 'reports/sales-trend', name: 'AdminSalesTrend', component: () => import('@/views/reports/SalesTrend.vue'), meta: { title: '销售趋势', menuGroup: '报表' } },
          { path: 'reports/customer-analysis', name: 'AdminCustomerAnalysis', component: () => import('@/views/reports/CustomerAnalysis.vue'), meta: { title: '客户利润', menuGroup: '报表' } },
        ],
      },
    ],
  },
  { path: '/sales/quote/list', redirect: '/sales/quotes' },
  { path: '/sales/quote/:id', redirect: (to) => `/sales/quotes/${to.params.id}` },
  { path: '/sales/customer/list', redirect: '/sales/customers' },
  { path: '/sales/customer/:id', redirect: (to) => `/sales/customers/${to.params.id}` },
  // v138 旧路径兼容重定向
  { path: '/incoming/batch-v138', redirect: '/sourcing/batch-incoming' },
  { path: '/purchase/no-order-v138', redirect: '/sourcing/no-order-purchase' },
  // 旧 V2.1 采购菜单 path 兼容（已统一为 /sourcing）
  { path: '/purchase/rfq', redirect: '/sourcing/rfq' },
  { path: '/purchase/pr-conversion', redirect: '/sourcing/purchase-transfer' },
  { path: '/purchase/no-order', redirect: '/sourcing/no-order-purchase' },
  { path: '/purchase/outsource-conversion', redirect: '/sourcing/outsub-order' },
  { path: '/purchase/delivery-reminder', redirect: '/sourcing/incoming' },
  { path: '/purchase/outsource-reconcile', redirect: '/sourcing/reconcile' },
  { path: '/purchase/vendors', redirect: '/sourcing/vendors' },
  { path: '/purchase', redirect: '/sourcing/rfq' },
  // V2.1 菜单 path → 当前实现路由（兼容 sys_menu 授权路径）
  { path: '/production/workbench', redirect: '/dashboard/production' },
  { path: '/production/scheduling', redirect: '/production/schedule' },
  { path: '/production/workorder-mgr', redirect: '/production/workorders' },
  { path: '/production/outsource-mgr', redirect: '/production/outsource' },
  { path: '/production/machine-mgr', redirect: '/production/machines' },
  { path: '/quality/workbench', redirect: '/quality/inspection' },
  { path: '/finance/receivable-payable', redirect: '/finance/receivables' },
  { path: '/finance/payment-approval', redirect: '/finance/payments' },
  { path: '/finance/material-cost', redirect: '/material/cost-aggregator' },
  { path: '/warehouse/overview', redirect: '/warehouse/index' },
  { path: '/warehouse/inventory-mgr', redirect: '/warehouse/inventory' },
  { path: '/hr/salary', redirect: '/hr/payroll' },
  { path: '/admin/roles', redirect: '/admin/users' },
  { path: '/admin/params', redirect: '/admin/dict' },
  { path: '/admin/audit', redirect: '/admin/reports/workflow-stats' },
  { path: '/approval/route-v138', redirect: '/sourcing/approval-route' },
  { path: '/reports/gm-summary-v138', redirect: '/finance/gm-summary' },
  { path: '/materials/:id/detail', redirect: (to) => `/material/detail/${to.params.id}` },
  { path: '/material/drawings', redirect: (to) => ({ path: '/engineering/data', query: { ...to.query, tab: to.query.tab ?? 'drawings' } }) },
  { path: '/material/lookup', redirect: (to) => ({ path: '/engineering/data', query: { ...to.query, tab: 'drawings' } }) },
  { path: '/reports/workflow-stats', redirect: '/admin/reports/workflow-stats' },
  { path: '/reports/sales-ranking', redirect: '/admin/reports/sales-ranking' },
  { path: '/reports/sales-trend', redirect: '/admin/reports/sales-trend' },
  { path: '/reports/customer-analysis', redirect: '/admin/reports/customer-analysis' },
  // APP 专属功能（PRD FR-4-2 / FR-5-2 / FR-12-2）— Web 仅提示
  { path: '/warehouse/scan', redirect: { path: '/app-only', query: { feature: '扫码中心', from: '/warehouse/inventory' } } },
  { path: '/warehouse/scan-inbound', redirect: { path: '/app-only', query: { feature: '扫码入库', from: '/warehouse/inventory' } } },
  { path: '/warehouse/scan-outbound', redirect: { path: '/app-only', query: { feature: '扫码出库', from: '/warehouse/inventory' } } },
  { path: '/warehouse/scan-history', redirect: { path: '/app-only', query: { feature: '扫码历史', from: '/warehouse/inventory' } } },
  { path: '/production/scan-workorder', redirect: { path: '/app-only', query: { feature: '扫码三码', from: '/production/workorders' } } },
  { path: '/production/scan-history', redirect: { path: '/app-only', query: { feature: '生产扫码历史', from: '/production/workorders' } } },
  { path: '/production/station-change', redirect: { path: '/app-only', query: { feature: '过站流转', from: '/production/workorders' } } },
  { path: '/production/outsub-order', redirect: '/sourcing/outsub-order' },
  { path: '/production/rework', redirect: '/sourcing/rework' },
  { path: '/production/rework-detail/:id', redirect: (to) => `/sourcing/rework-detail/${to.params.id}` },
  { path: '/production/rework-alert', redirect: '/sourcing/rework-alert' },
  { path: '/admin/payroll-detail/:id', redirect: (to) => `/hr/payroll/${to.params.id}` },
  { path: '/admin/hr/employee-detail/:id', redirect: (to) => `/hr/employee/${to.params.id}` },
  { path: '/admin/hr/:pathMatch(.*)*', redirect: (to) => `/hr/${to.params.pathMatch}` },
  { path: '/admin/payroll', redirect: '/hr/payroll' },
  { path: '/admin/payroll/:id', redirect: (to) => `/hr/payroll/${to.params.id}` },
  { path: '/sourcing/incoming-create', redirect: { path: '/app-only', query: { feature: '登记到货', from: '/sourcing/incoming' } } },
  {
    path: '/visitor/progress',
    name: 'VisitorProgress',
    component: () => import('@/views/visitor/VisitorProgress.vue'),
    meta: { title: '生产进度查询', roles: ['CUSTOMER_VISITOR', 'ADMIN', 'GM', 'PROD_MGR', 'PRODUCTION_MANAGER'] },
  },
  {
    path: '/app-only',
    name: 'AppOnly',
    component: () => import('@/views/system/AppOnly.vue'),
    meta: { public: true, title: 'APP 功能' },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { public: true, title: '404' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// V1.3.7 鉴权守卫：JWT + 5 条红线
router.beforeEach((to, _from, next) => {
  const auth = useAuthStore()

  if (to.meta.public) {
    return next()
  }

  if (!auth.token) {
    return next({ name: 'Login', query: { redirect: to.fullPath } })
  }

  if (!isSessionActive(auth.token)) {
    auth.logout()
    return next({ name: 'Login', query: { redirect: to.fullPath, expired: '1' } })
  }

  // V1.4.0 · 客户演示账号登录后直达搜索页
  if (
    hasAnyRole(auth.userRoles, ['CUSTOMER_VISITOR'])
    && !hasAnyRole(auth.userRoles, ['ADMIN', 'GM', 'PROD_MGR', 'PRODUCTION_MANAGER'])
    && !to.path.startsWith('/visitor')
  ) {
    return next('/visitor/progress')
  }

  // V1.4.1：生管角色禁止直接访问销售订单详情页
  // 生管应通过 /production/pending-production-detail/:id 进入待转产订单详情
  if (
    hasAnyRole(auth.userRoles, ['PROD_MGR', 'PRODUCTION_MANAGER'])
    && /^\/sales\/orders\/\d+$/.test(to.path)
  ) {
    return next('/production/pending-production')
  }

  // V1.3.9 Sprint 13 Story 13.4：路由 meta roles 校验（GM/ADMIN only）
  // 防止 SALES 等角色绕过路由直达 · 端点 @PreAuthorize 为第二道防线
  const requiredRoles = (to.meta.roles as string[] | undefined) || []
  if (requiredRoles.length > 0) {
    const ok = hasAnyRole(auth.userRoles, requiredRoles)
    if (!ok) {
      return next({ name: 'Dashboard' })
    }
  } else if (!canAccessRouteWithMenu(to.path, auth.userRoles, auth.menuPaths)) {
    return next({ name: 'Dashboard' })
  }

  // TODO: V1.3.7 5 条红线验证
  // 1. 生管页面无厂商下拉框
  // 2. 采购页面无工序归属切换
  // 3. 对账页面无"采购带纸去厂商处"按钮
  // 4. 厂商接收邮箱为标准格式（发送渠道固定 163 SMTP）
  // 5. 消息中心无"短信发送"按钮

  next()
})

router.afterEach((to) => {
  const title = to.meta.title as string | undefined
  document.title = title ? `${title} · 佰泰胜 ERP` : '佰泰胜 ERP'
})

export default router
