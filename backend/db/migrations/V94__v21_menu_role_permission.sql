-- ======================================================================
-- V94 · 员工岗位字典 EMPLOYEE_POSITION + 各角色菜单权限一览表 V2.1
-- 2026-06-21 · Architect agent
--
-- 目标：
--   1) EMPLOYEE_POSITION 字典（12 条 POS-*）
--   2) 按 V2.1 角色菜单权限一览表初始化 sys_menu + sys_role_permission
--      - 12 角色 + 55+ 菜单
--      - SYS_ADMIN 全量
--      - 9 业务角色（SALES/SALES_MGR/PROD_MGR/ENGINEER/QC/BUYER/FINANCE/HR/WAREHOUSE）按 V2.1 表
--      - PROCUREMENT_MANAGER 与 BUYER 共享菜单（含 src.no-order 等）
--      - OPERATOR 仅 PC 端 1 个（绩效看板）
--      - CUSTOMER_VISITOR 仅访客进度查询
--      - SALES/SALES_MGR 同步扩 sales.quote-templates (V2.1 新增)
-- ======================================================================

USE `cnc_platform`;

-- ---------- 1. 字典类型 ----------
INSERT INTO `sys_dict_type` (`type_code`, `type_name`, `description`, `is_builtin`) VALUES
('EMPLOYEE_POSITION', '员工岗位', '员工岗位字典：岗位编码=POS-*；remark=JSON{dept,machine_types}', 1)
ON DUPLICATE KEY UPDATE `type_name` = VALUES(`type_name`),
                                `description` = VALUES(`description`),
                                `is_builtin` = VALUES(`is_builtin`);

-- ---------- 2. 12 条岗位字典 ----------
INSERT IGNORE INTO `sys_dict` (`dict_type`, `dict_code`, `dict_label`, `sort`, `status`, `remark`, `created_at`, `updated_at`) VALUES
('EMPLOYEE_POSITION', 'POS-CNC',       'CNC 操作工',    1,  'ACTIVE', JSON_OBJECT('dept','生产部','machine_types','CNC加工中心'),             NOW(), NOW()),
('EMPLOYEE_POSITION', 'POS-LATHE',     '车床操作工',    2,  'ACTIVE', JSON_OBJECT('dept','生产部','machine_types','数控车床'),               NOW(), NOW()),
('EMPLOYEE_POSITION', 'POS-MILL',      '铣床操作工',    3,  'ACTIVE', JSON_OBJECT('dept','生产部','machine_types','铣床'),                   NOW(), NOW()),
('EMPLOYEE_POSITION', 'POS-GRIND',     '磨床操作工',    4,  'ACTIVE', JSON_OBJECT('dept','生产部','machine_types','精密磨床'),               NOW(), NOW()),
('EMPLOYEE_POSITION', 'POS-EDM',       '电火花操作工',  5,  'ACTIVE', JSON_OBJECT('dept','生产部','machine_types','电火花'),                 NOW(), NOW()),
('EMPLOYEE_POSITION', 'POS-QC',        '品质检验员',    6,  'ACTIVE', JSON_OBJECT('dept','品质部','machine_types','三次元/测量设备'),         NOW(), NOW()),
('EMPLOYEE_POSITION', 'POS-WAREHOUSE', '仓管员',        7,  'ACTIVE', JSON_OBJECT('dept','仓储部','machine_types',''),                       NOW(), NOW()),
('EMPLOYEE_POSITION', 'POS-ENG',       '工程师',        8,  'ACTIVE', JSON_OBJECT('dept','工程部','machine_types',''),                       NOW(), NOW()),
('EMPLOYEE_POSITION', 'POS-SALES',     '业务员',        9,  'ACTIVE', JSON_OBJECT('dept','销售部','machine_types',''),                       NOW(), NOW()),
('EMPLOYEE_POSITION', 'POS-PURCHASE',  '采购员',       10,  'ACTIVE', JSON_OBJECT('dept','采购部','machine_types',''),                       NOW(), NOW()),
('EMPLOYEE_POSITION', 'POS-HR',        '人事专员',     11,  'ACTIVE', JSON_OBJECT('dept','人事部','machine_types',''),                       NOW(), NOW()),
('EMPLOYEE_POSITION', 'POS-MGR',       '管理人员',     12,  'ACTIVE', JSON_OBJECT('dept','生产部','machine_types',''),                       NOW(), NOW());

-- ---------- 3. sys_role 用户角色绑定：确保 admin → SYS_ADMIN ----------
INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(1, 1)   -- admin → SYS_ADMIN（V2.1 全量权限）
ON DUPLICATE KEY UPDATE `user_id` = VALUES(`user_id`);

-- ======================================================================
-- 4. sys_menu 全量重建（V2.1）
-- ======================================================================

DELETE FROM `sys_role_permission`;
DELETE FROM `sys_menu`;

-- 顶级模块（10 个）
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`, `icon`) VALUES
(1, NULL, 'mod.dashboard',   '工作台',     '/dashboard',  'MODULE', 10, 'HomeFilled'),
(2, NULL, 'mod.sales',       '销售',       '/sales',      'MODULE', 20, 'Money'),
(3, NULL, 'mod.production',  '生产',       '/production', 'MODULE', 30, 'Tools'),
(4, NULL, 'mod.engineering', '工程',   '/engineering', 'MODULE', 40, 'Goods'),
(5, NULL, 'mod.quality',     '品质',       '/quality',    'MODULE', 50, 'Medal'),
(6, NULL, 'mod.sourcing',    '采购',       '/sourcing',   'MODULE', 60, 'ShoppingCart'),
(7, NULL, 'mod.finance',     '财务',       '/finance',    'MODULE', 70, 'CreditCard'),
(8, NULL, 'mod.hr',          '人事',       '/hr',         'MODULE', 80, 'User'),
(9, NULL, 'mod.admin',       '管理',       '/admin',      'MODULE', 90, 'Setting'),
(10, NULL, 'mod.warehouse',  '仓储',       '/warehouse',  'MODULE', 45, 'Box');

-- 工作台子菜单（12 个：含 dash.performance 操作工业绩看板）
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(101, 1, 'dash.index',         '总览',         '/dashboard/index',                'MENU', 1),
(102, 1, 'dash.production',    '生产工作台',   '/dashboard/production',           'MENU', 2),
(103, 1, 'dash.sales',         '销售驾驶舱',   '/dashboard/sales',                'MENU', 3),
(104, 1, 'dash.finance',       '财务驾驶舱',   '/dashboard/finance',              'MENU', 4),
(105, 1, 'dash.quality',       '品质驾驶舱',   '/dashboard/quality',              'MENU', 5),
(106, 1, 'dash.outsource',     '委外驾驶舱',   '/dashboard/outsource',            'MENU', 6),
(107, 1, 'dash.procurement',   '采购驾驶舱',   '/dashboard/procurement',          'MENU', 7),
(108, 1, 'dash.engineer',      '工程师工作台', '/dashboard/engineer',             'MENU', 8),
(109, 1, 'dash.warehouse',     '仓管工作台',   '/dashboard/warehouse',            'MENU', 9),
(110, 1, 'dash.alerts',        '总经理告警',   '/dashboard/alerts',               'MENU', 10),
(111, 1, 'dash.multi',         '多维度看板',   '/dashboard/multi',                'MENU', 11),
(112, 1, 'dash.performance',   '绩效看板',     '/dashboard/performance-board',    'MENU', 12);

-- 销售子菜单（7 个，含 V2.1 新增 sales.quote-templates id=207）
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(201, 2, 'sales.customers',     '客户档案',  '/sales/customers',          'MENU', 1),
(202, 2, 'sales.protection',    '客户保护',  '/sales/customer/protection','MENU', 2),
(203, 2, 'sales.quotes',        '报价单',    '/sales/quotes',             'MENU', 3),
(204, 2, 'sales.quote-approval','报价审批',  '/sales/quotes/approval',    'MENU', 4),
(205, 2, 'sales.orders',        '销售订单',  '/sales/orders',             'MENU', 5),
(206, 2, 'sales.contracts',     '合同回款',  '/sales/contracts',          'MENU', 6),
(207, 2, 'sales.quote-templates','报价范本',  '/sales/quote-templates',    'MENU', 7);

-- 生产子菜单（6 个：V2.1 精简）
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(311, 3, 'prod.workbench',     '生产工作台',  '/production/workbench',     'MENU', 1),
(312, 3, 'prod.scheduling',    '排产中心',    '/production/scheduling',    'MENU', 2),
(313, 3, 'prod.workorder-mgr', '工单管理',    '/production/workorder-mgr', 'MENU', 3),
(314, 3, 'prod.outsource-mgr', '委外管理',    '/production/outsource-mgr', 'MENU', 4),
(305, 3, 'prod.mrp',           'MRP中心',     '/production/mrp',           'MENU', 5),
(315, 3, 'prod.machine-mgr',   '设备管理',    '/production/machine-mgr',   'MENU', 6);

-- 工程数据子菜单（4 个 · V2.1 工程师专属）
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(411, 4, 'eng.order-conversion',  '订单工程转化', '/engineering/order-conversion',  'MENU', 1),
(412, 4, 'eng.quote-confirmation','报价工艺定义', '/engineering/quote-confirmation','MENU', 2),
(413, 4, 'eng.data',              '图纸与料号',     '/engineering/data',              'MENU', 3),
(414, 4, 'eng.my-tasks',          '待办任务中心',   '/engineering/my-tasks',          'MENU', 4);

-- 品质子菜单（5 个：qc.workbench + qc.fa/cmm/defect/pickup）
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(511, 5, 'qc.workbench', '检验工作台', '/quality/workbench', 'MENU', 1),
(502, 5, 'qc.fa',        'FA首件',    '/quality/fa',        'MENU', 2),
(503, 5, 'qc.cmm',       '三次元',    '/quality/cmm',       'MENU', 3),
(504, 5, 'qc.defect',    '不良品',    '/quality/defect',    'MENU', 4),
(505, 5, 'qc.pickup',    '提货检',    '/quality/pickup',    'MENU', 5);

-- 采购子菜单（7 项 · 路径统一 /sourcing/*）
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(601,  6, 'src.rfq-workbench',      '询比价工作台',        '/sourcing/rfq',                   'MENU', 1),
(613,  6, 'src.pr-conversion',      '采购转单（MRP缺料）', '/sourcing/purchase-transfer',     'MENU', 2),
(606,  6, 'src.no-order',           '无订单采购',          '/sourcing/no-order-purchase',     'MENU', 3),
(614,  6, 'src.outsource-conversion','委外转单',            '/sourcing/outsub-order',          'MENU', 4),
(615,  6, 'src.delivery-reminder',  '到货提醒',            '/sourcing/incoming',              'MENU', 5),
(616,  6, 'src.outsource-reconcile','委外对账',            '/sourcing/reconcile',             'MENU', 6),
(617,  6, 'src.vendors',            '厂商资料',            '/sourcing/vendors',               'MENU', 7);

-- 财务子菜单（5 个：应收应付/成本核算/利润分析/付款审批/料号成本）
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(701, 7, 'fin.receivable-payable', '应收应付',  '/finance/receivable-payable', 'MENU', 1),
(704, 7, 'fin.cost',              '成本核算',  '/finance/cost',               'MENU', 2),
(706, 7, 'fin.profit',            '利润分析',  '/finance/profit',             'MENU', 3),
(705, 7, 'fin.payment-approval',  '付款审批',  '/finance/payment-approval',   'MENU', 4),
(618, 7, 'fin.material-cost',     '料号成本',  '/finance/material-cost',      'MENU', 5);

-- 人事子菜单（6 个：V2.1 员工列表/账号/考勤/薪酬/绩效/招聘）
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(801, 8, 'hr.employees',   '员工列表',  '/hr/employees',    'MENU', 1),
(802, 8, 'hr.accounts',    '系统账号',  '/hr/accounts',     'MENU', 2),
(803, 8, 'hr.attendance',  '考勤月报',  '/hr/attendance',   'MENU', 3),
(804, 8, 'hr.salary',      '薪酬核算',  '/hr/salary',       'MENU', 4),
(805, 8, 'hr.performance', '绩效管理',  '/hr/performance', 'MENU', 5),
(806, 8, 'hr.recruitment', '招聘管理',  '/hr/recruitment', 'MENU', 6);

-- 管理子菜单（6 个：用户/角色/工作流/参数/字典/审计）
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(901, 9, 'adm.users',      '用户管理',  '/admin/users',      'MENU', 1),
(902, 9, 'adm.roles',      '角色管理',  '/admin/roles',      'MENU', 2),
(903, 9, 'adm.workflows',  '工作流配置','/admin/workflows',  'MENU', 3),
(904, 9, 'adm.params',     '系统参数',  '/admin/params',     'MENU', 4),
(905, 9, 'adm.dict',       '数据字典',  '/admin/dict',       'MENU', 5),
(906, 9, 'adm.audit',      '操作日志',  '/admin/audit',      'MENU', 6);

-- 仓储子菜单（5 个：含物料条码 · V2.1 统一归仓管）
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(1011, 10, 'wh.overview',      '仓储总览',  '/warehouse/overview',     'MENU', 1),
(1012, 10, 'wh.inventory-mgr', '库存管理',  '/warehouse/inventory-mgr','MENU', 2),
(1006, 10, 'wh.inbound',       '入库单',    '/warehouse/inbound',      'MENU', 3),
(1007, 10, 'wh.outbound',      '出库单',    '/warehouse/outbound',     'MENU', 4),
(403,  10, 'mat.barcode',      '物料条码',  '/material/barcode-list',  'MENU', 5);

-- 客户演示（V2.1 · CUSTOMER_VISITOR 独立模块，无工作台）
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(15, NULL, 'mod.visitor',      '生产进度',     '/visitor',        'MODULE', 35, 'View'),
(1201, 15, 'visitor.progress', '生产进度查询', '/visitor/progress', 'MENU', 1);

-- ======================================================================
-- 5. sys_role_permission 12 角色权限分配（V2.1）
-- ======================================================================

-- SYS_ADMIN（id=1）全量菜单 view 权限
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 1, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE';

-- SALES（id=2）：工作台总览 + 销售模块 7 项（含 V2.1 新增 quote-templates）
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 2, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND (
  `path` IN ('/dashboard','/dashboard/index')
  OR `menu_code` IN ('sales.customers','sales.protection','sales.quotes','sales.quote-approval','sales.orders','sales.contracts','sales.quote-templates')
);

-- SALES_MGR（id=3）：SALES 全集 + 报价审批 + 工作台销售驾驶舱
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 3, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND (
  `path` IN ('/dashboard','/dashboard/index','/dashboard/sales')
  OR `menu_code` IN ('sales.customers','sales.protection','sales.quotes','sales.quote-approval','sales.orders','sales.contracts','sales.quote-templates')
);

-- GM（id=4）：除管理后台外全部菜单（V2.1 一览表 §九）
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 4, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND (
  `id` <> 9
  AND `path` NOT LIKE '/admin/%'
  AND `menu_code` NOT LIKE 'adm.%'
);

-- PROD_MGR（id=5）：工作台 + 生产 6 项
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 5, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND (
  `path` IN ('/dashboard','/dashboard/index')
  OR `menu_code` IN ('prod.workbench','prod.scheduling','prod.workorder-mgr','prod.outsource-mgr','prod.mrp','prod.machine-mgr')
);

-- ENGINEER（id=6）：工程师工作台 + 报价工艺定义 + 订单工程转化 + 图纸与料号 + 待办任务中心
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 6, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND (
  `path` IN ('/dashboard','/dashboard/index','/dashboard/engineer','/engineering')
  OR `menu_code` IN ('eng.order-conversion','eng.quote-confirmation','eng.data','eng.my-tasks','sales.quotes')
);

-- QC（id=8）：工作台总览+品质驾驶舱+品质 5 项
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 8, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND (
  `path` IN ('/dashboard','/dashboard/index','/dashboard/quality')
  OR `menu_code` IN ('qc.workbench','qc.fa','qc.cmm','qc.defect','qc.pickup')
);

-- BUYER（id=9）：工作台 + 采购 7 项
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 9, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND (
  `path` IN ('/dashboard','/dashboard/index','/dashboard/procurement','/sourcing')
  OR `menu_code` IN ('src.rfq-workbench','src.pr-conversion','src.no-order','src.outsource-conversion','src.delivery-reminder','src.outsource-reconcile','src.vendors')
);

-- PROCUREMENT_MANAGER（id=13）：与 BUYER 相同
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 13, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND (
  `path` IN ('/dashboard','/dashboard/index','/dashboard/procurement','/sourcing')
  OR `menu_code` IN ('src.rfq-workbench','src.pr-conversion','src.no-order','src.outsource-conversion','src.delivery-reminder','src.outsource-reconcile','src.vendors')
);

-- FINANCE（id=10）：工作台 + 财务 5 项
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 10, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND (
  `path` IN ('/dashboard','/dashboard/index','/dashboard/finance')
  OR `menu_code` IN ('fin.receivable-payable','fin.cost','fin.profit','fin.payment-approval','fin.material-cost')
);

-- HR（id=11）：工作台总览+人事 6 项
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 11, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND (
  `path` IN ('/dashboard','/dashboard/index')
  OR `menu_code` IN ('hr.employees','hr.accounts','hr.attendance','hr.salary','hr.performance','hr.recruitment')
);

-- WAREHOUSE（id=7）：工作台 + 仓储 5 项（含物料条码）
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 7, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND (
  `path` IN ('/dashboard','/dashboard/index','/dashboard/warehouse','/warehouse')
  OR `menu_code` IN ('wh.overview','wh.inventory-mgr','wh.inbound','wh.outbound','mat.barcode')
);

-- OPERATOR（id=12）：仅 PC 绩效看板（V2.1 §十）
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 12, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND (
  `path` IN ('/dashboard','/dashboard/performance-board')
  OR `menu_code` = 'dash.performance'
);

-- CUSTOMER_VISITOR（id=14）：仅生产进度查询（V2.1 §十一 · 无工作台）
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 14, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND (
  `menu_code` IN ('visitor.progress','mod.visitor')
);
