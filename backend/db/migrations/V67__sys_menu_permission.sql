-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
USE `cnc_platform`;

CREATE TABLE IF NOT EXISTS `sys_menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_id` BIGINT DEFAULT NULL COMMENT '父菜单 ID',
  `menu_code` VARCHAR(64) NOT NULL COMMENT '菜单编码',
  `menu_name` VARCHAR(100) NOT NULL COMMENT '菜单名称',
  `path` VARCHAR(255) NOT NULL COMMENT '前端路由 path',
  `menu_type` VARCHAR(20) NOT NULL DEFAULT 'MENU' COMMENT 'MODULE/MENU/ROUTE',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `icon` VARCHAR(64) DEFAULT NULL COMMENT '图标',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_menu_code` (`menu_code`),
  UNIQUE KEY `uniq_menu_path` (`path`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统菜单';

DELETE FROM `sys_role_permission`;
DELETE FROM `sys_menu`;

-- ---------- 顶级模块 ----------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`, `icon`) VALUES
(1, NULL, 'mod.dashboard',   '工作台', '/dashboard',   'MODULE', 10, 'HomeFilled'),
(2, NULL, 'mod.sales',       '销售',   '/sales',       'MODULE', 20, 'Money'),
(3, NULL, 'mod.production',  '生产',   '/production',  'MODULE', 30, 'Tools'),
(4, NULL, 'mod.material',    '物料',   '/material',    'MODULE', 40, 'Goods'),
(5, NULL, 'mod.quality',     '品质',   '/quality',     'MODULE', 50, 'Medal'),
(6, NULL, 'mod.sourcing',    '采购',   '/sourcing',    'MODULE', 60, 'ShoppingCart'),
(7, NULL, 'mod.finance',     '财务',   '/finance',     'MODULE', 70, 'CreditCard'),
(8, NULL, 'mod.hr',          '人事',   '/hr',          'MODULE', 80, 'User'),
(9, NULL, 'mod.admin',       '管理',   '/admin',       'MODULE', 90, 'Setting'),
(10, NULL, 'mod.warehouse',  '仓储',   '/warehouse',   'MODULE', 45, 'Box');

-- ---------- 工作台 ----------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(101, 1, 'dash.index',        '总览',       '/dashboard/index',        'MENU', 1),
(102, 1, 'dash.production',   '生产工作台', '/dashboard/production',   'MENU', 2),
(103, 1, 'dash.sales',        '销售驾驶舱', '/dashboard/sales',        'MENU', 3),
(104, 1, 'dash.finance',      '财务驾驶舱', '/dashboard/finance',      'MENU', 4),
(105, 1, 'dash.quality',      '品质驾驶舱', '/dashboard/quality',      'MENU', 5),
(106, 1, 'dash.outsource',    '委外驾驶舱', '/dashboard/outsource',    'MENU', 6),
(107, 1, 'dash.procurement',  '采购驾驶舱', '/dashboard/procurement',  'MENU', 7),
(108, 1, 'dash.engineer',     '工程师工作台','/dashboard/engineer',    'MENU', 8),
(109, 1, 'dash.warehouse',    '仓管工作台', '/dashboard/warehouse',    'MENU', 9),
(110, 1, 'dash.alerts',       '总经理告警', '/dashboard/alerts',       'MENU', 10),
(111, 1, 'dash.multi',        '多维度看板', '/dashboard/multi',        'MENU', 11);

-- ---------- 销售 ----------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(201, 2, 'sales.customers',    '客户档案', '/sales/customers',           'MENU', 1),
(202, 2, 'sales.protection',   '客户保护', '/sales/customer/protection', 'MENU', 2),
(203, 2, 'sales.quotes',       '报价单',   '/sales/quotes',              'MENU', 3),
(204, 2, 'sales.quote-approval','报价审批','/sales/quotes/approval',     'MENU', 4),
(205, 2, 'sales.orders',       '销售订单', '/sales/orders',              'MENU', 5),
(206, 2, 'sales.contracts',    '合同回款', '/sales/contracts',           'MENU', 6);

-- ---------- 生产 ----------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(301, 3, 'prod.workorders',    '工单',       '/production/workorders',          'MENU', 1),
(302, 3, 'prod.pending',       '待转产订单', '/production/pending-production',  'MENU', 2),
(303, 3, 'prod.schedule',      '排产看板',   '/production/schedule',            'MENU', 3),
(304, 3, 'prod.gantt',         '排产甘特',   '/production/schedule-gantt',        'MENU', 4),
(305, 3, 'prod.mrp',           'MRP 中心',   '/production/mrp',                 'MENU', 5),
(306, 3, 'prod.outsource',     '委外列表',   '/production/outsource',           'MENU', 6),
(307, 3, 'prod.allocation',    '工序分配',   '/production/allocation',          'MENU', 7),
(308, 3, 'prod.outsub-panel',  '委外面板',   '/production/outsub-panel',        'MENU', 8),
(309, 3, 'prod.machines',      '设备机台',   '/production/machines',            'MENU', 9);

-- ---------- 物料 ----------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(401, 4, 'mat.drawings',   '图纸',     '/material/drawings',          'MENU', 1),
(402, 4, 'mat.lookup',     '料号查询', '/material/lookup',            'MENU', 2),
(403, 4, 'mat.barcode',    '物料条码', '/material/barcode-list',      'MENU', 3),
(404, 4, 'mat.category',   '物料分类', '/material/material-category', 'MENU', 4),
(405, 4, 'mat.boms',       'BOM',      '/material/boms',                'MENU', 5),
(406, 4, 'mat.process',    '工艺库',   '/material/process',             'MENU', 6),
(407, 4, 'mat.cost',       '料号成本', '/material/cost-aggregator',   'MENU', 7);

-- ---------- 品质 ----------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(501, 5, 'qc.inspection',  '来料/过程/成品检', '/quality/inspection',          'MENU', 1),
(502, 5, 'qc.fa',          'FA 首件',          '/quality/fa',                  'MENU', 2),
(503, 5, 'qc.cmm',         '三次元',           '/quality/cmm',                 'MENU', 3),
(504, 5, 'qc.defect',      '不良品',           '/quality/defect',              'MENU', 4),
(505, 5, 'qc.pickup',      '提货检',           '/quality/pickup',              'MENU', 5),
(506, 5, 'qc.outsource',   '委外检',           '/quality/outsource-inspection','MENU', 6);

-- ---------- 采购 ----------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(601, 6, 'src.rfq',        '询价比价 RFQ', '/sourcing/rfq',              'MENU', 1),
(602, 6, 'src.po',         '采购订单 PO',  '/sourcing/po',               'MENU', 2),
(603, 6, 'src.incoming',   '到货回执',     '/sourcing/incoming',           'MENU', 3),
(604, 6, 'src.no-order',   '无单采购',     '/sourcing/no-order-purchase',  'MENU', 4),
(605, 6, 'src.approval',   '审批路由',     '/sourcing/approval-route',     'MENU', 5),
(606, 6, 'src.reconcile',  '月度对账',     '/sourcing/reconcile',          'MENU', 6),
(607, 6, 'src.outsub',     '委外下单',     '/sourcing/outsub-order',       'MENU', 7),
(608, 6, 'src.rework',     '返修协同',     '/sourcing/rework',             'MENU', 8),
(609, 6, 'src.vendors',    '厂商资料',     '/sourcing/vendors',            'MENU', 9);

-- ---------- 财务 ----------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(701, 7, 'fin.receivables', '应收',     '/finance/receivables', 'MENU', 1),
(702, 7, 'fin.payables',    '应付',     '/finance/payables',    'MENU', 2),
(703, 7, 'fin.aging',       '账龄',     '/finance/aging',       'MENU', 3),
(704, 7, 'fin.cost',        '成本',     '/finance/cost',        'MENU', 4),
(705, 7, 'fin.payments',    '付款',     '/finance/payments',    'MENU', 5),
(706, 7, 'fin.profit',      '利润分析', '/finance/profit',      'MENU', 6),
(707, 7, 'fin.scans',       '签字扫描', '/finance/signed-scans','MENU', 7),
(708, 7, 'fin.gm-summary',  '总经理汇总','/finance/gm-summary', 'MENU', 8);

-- ---------- 人事 ----------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(801, 8, 'hr.employees',   '员工档案', '/hr/employees',   'MENU', 1),
(802, 8, 'hr.accounts',    '账号管理', '/hr/accounts',    'MENU', 2),
(803, 8, 'hr.attendance',  '考勤',     '/hr/attendance',  'MENU', 3),
(804, 8, 'hr.payroll',     '薪资',     '/hr/payroll',     'MENU', 4);

-- ---------- 管理 ----------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(901, 9, 'adm.users',           '用户管理',   '/admin/users',                  'MENU', 1),
(902, 9, 'adm.workflows',       '审批流',     '/admin/workflows',              'MENU', 2),
(903, 9, 'adm.dict',            '数据字典',   '/admin/dict',                   'MENU', 3),
(904, 9, 'adm.keyboard',        '快捷键',     '/admin/keyboard',               'MENU', 4),
(905, 9, 'adm.printers',        '打印机',     '/admin/printers',               'MENU', 5),
(906, 9, 'adm.email-config',    '邮件配置',   '/admin/email-config',           'MENU', 6),
(907, 9, 'adm.email-templates', '邮件模板',   '/admin/email-templates',        'MENU', 7),
(908, 9, 'adm.field-encrypt',   '字段加密',   '/admin/field-encryption',       'MENU', 8),
(909, 9, 'adm.rpt-workflow',    '审批统计',   '/admin/reports/workflow-stats', 'MENU', 9),
(910, 9, 'adm.rpt-ranking',     '销售龙虎榜', '/admin/reports/sales-ranking',  'MENU', 10),
(911, 9, 'adm.rpt-trend',       '销售趋势',   '/admin/reports/sales-trend',    'MENU', 11),
(912, 9, 'adm.rpt-customer',    '客户分析',   '/admin/reports/customer-analysis','MENU', 12);

-- ---------- 仓储 ----------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(1001, 10, 'wh.index',    '多仓库总览', '/warehouse/index',           'MENU', 1),
(1002, 10, 'wh.locations','库位树',     '/warehouse/locations',       'MENU', 2),
(1003, 10, 'wh.batches',  '批次列表',   '/warehouse/batches',         'MENU', 3),
(1004, 10, 'wh.inventory','库存',       '/warehouse/inventory',       'MENU', 4),
(1005, 10, 'wh.alert',    '库存预警',   '/warehouse/inventory-alert', 'MENU', 5);

-- ---------- 角色权限：SYS_ADMIN 全量 ----------
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 1, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE';

-- SALES 业务员
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 2, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND (
  `path` IN ('/dashboard','/dashboard/index','/dashboard/sales','/sales')
  OR `path` LIKE '/sales/%'
);

-- SALES_MGR 销售经理（同 SALES + 报价审批已有）
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 3, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND (
  `path` IN ('/dashboard','/dashboard/index','/dashboard/sales','/sales')
  OR `path` LIKE '/sales/%'
);

-- GM 总经理（除管理后台）
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 4, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND `path` NOT LIKE '/admin%';

-- PROD_MGR 生管
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 5, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND (
  `path` IN ('/dashboard','/dashboard/index','/dashboard/production','/production','/material')
  OR `path` LIKE '/dashboard/production%'
  OR `path` LIKE '/production/%'
  OR `path` IN ('/material/drawings','/material/lookup','/material/boms','/material/process','/material/material-category')
);

-- ENGINEER
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 6, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND (
  `path` IN ('/dashboard','/dashboard/index','/dashboard/engineer','/production','/material')
  OR `path` LIKE '/dashboard/engineer%'
  OR `path` LIKE '/production/schedule%'
  OR `path` LIKE '/production/mrp%'
  OR `path` LIKE '/production/workorders%'
  OR `path` LIKE '/material/%'
);

-- WAREHOUSE 仓管
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 7, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND (
  `path` IN ('/dashboard','/dashboard/index','/dashboard/warehouse','/warehouse','/material')
  OR `path` LIKE '/warehouse/%'
  OR `path` IN ('/material/lookup','/material/barcode-list')
);

-- QC 品检
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 8, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND (
  `path` IN ('/dashboard','/dashboard/index','/dashboard/quality','/quality')
  OR `path` LIKE '/quality/%'
);

-- BUYER 采购
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 9, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND (
  `path` IN ('/dashboard','/dashboard/index','/dashboard/procurement','/dashboard/outsource','/sourcing')
  OR `path` LIKE '/sourcing/%'
  OR `path` IN ('/production/outsub-panel')
);

-- FINANCE 财务
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 10, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND (
  `path` IN ('/dashboard','/dashboard/index','/dashboard/finance','/finance')
  OR `path` LIKE '/finance/%'
  OR `path` IN ('/sales/contracts')
);

-- HR 人事
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 11, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND (
  `path` IN ('/dashboard','/dashboard/index','/hr')
  OR `path` LIKE '/hr/%'
);

-- OPERATOR 操作工
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT r.id, m.id, 'view'
FROM `sys_role` r
JOIN `sys_menu` m ON m.status = 'ACTIVE'
WHERE r.role_code = 'OPERATOR'
  AND (
    m.path IN ('/dashboard','/dashboard/index','/dashboard/production','/production')
    OR m.path IN ('/production/workorders','/production/schedule')
  );

-- PROCUREMENT_MANAGER 采购主管
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT r.id, m.id, 'view'
FROM `sys_role` r
JOIN `sys_menu` m ON m.status = 'ACTIVE'
WHERE r.role_code = 'PROCUREMENT_MANAGER'
  AND (
    m.path IN ('/dashboard','/dashboard/index','/dashboard/procurement','/sourcing','/finance')
    OR m.path LIKE '/sourcing/%'
    OR m.path IN ('/finance/gm-summary')
  );
