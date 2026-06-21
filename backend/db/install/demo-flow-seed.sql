-- ============================================================
-- 昆山佰泰胜 ERP · 演示流程 Mock 数据（可重复导入）
-- 前置：已执行 db/init.sql + db/migrations/V3~V58
-- 用途：Web 登录 / 报价 / 工单 / 扫码三码 / 生产工作台 全链路体验
-- 条码规范：GD- / LZ- / SB-（见 web-impl/docs/barcode-prefix.md）
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------- 1. 平台用户与角色（每角色 1 人 · 密码 123456） ----------
USE `cnc_platform`;

INSERT INTO `sys_role` (`id`, `role_code`, `role_name`, `data_scope`, `status`) VALUES
(12, 'OPERATOR', '操作工', 'SELF', 'ACTIVE'),
(13, 'PROCUREMENT_MANAGER', '采购主管', 'DEPT', 'ACTIVE'),
(14, 'CUSTOMER_VISITOR', '客户现场演示', 'CUSTOM', 'ACTIVE')
ON DUPLICATE KEY UPDATE `role_name` = VALUES(`role_name`);

INSERT INTO `sys_user` (`id`, `username`, `password_hash`, `real_name`, `email`, `status`) VALUES
(1,  'admin',               '$2a$12$wulkj1kVO3eGc8v9ckGWEeE.Toh5NX/XL8CRtCWbdbgAlxCP1LtIe', '系统管理员', 'admin@cnc.local',               'ACTIVE'),
(2,  'sales',               '$2a$12$wulkj1kVO3eGc8v9ckGWEeE.Toh5NX/XL8CRtCWbdbgAlxCP1LtIe', '业务员',     'sales@cnc.local',               'ACTIVE'),
(3,  'sales_mgr',           '$2a$12$wulkj1kVO3eGc8v9ckGWEeE.Toh5NX/XL8CRtCWbdbgAlxCP1LtIe', '销售经理',   'sales_mgr@cnc.local',           'ACTIVE'),
(4,  'gm',                  '$2a$12$wulkj1kVO3eGc8v9ckGWEeE.Toh5NX/XL8CRtCWbdbgAlxCP1LtIe', '总经理',     'gm@cnc.local',                  'ACTIVE'),
(5,  'prod_mgr',            '$2a$12$wulkj1kVO3eGc8v9ckGWEeE.Toh5NX/XL8CRtCWbdbgAlxCP1LtIe', '生管',       'prod_mgr@cnc.local',            'ACTIVE'),
(6,  'engineer',            '$2a$12$wulkj1kVO3eGc8v9ckGWEeE.Toh5NX/XL8CRtCWbdbgAlxCP1LtIe', '工程师',     'engineer@cnc.local',            'ACTIVE'),
(7,  'warehouse',           '$2a$12$wulkj1kVO3eGc8v9ckGWEeE.Toh5NX/XL8CRtCWbdbgAlxCP1LtIe', '仓管',       'warehouse@cnc.local',           'ACTIVE'),
(8,  'qc',                  '$2a$12$wulkj1kVO3eGc8v9ckGWEeE.Toh5NX/XL8CRtCWbdbgAlxCP1LtIe', '品检',       'qc@cnc.local',                  'ACTIVE'),
(9,  'buyer',               '$2a$12$wulkj1kVO3eGc8v9ckGWEeE.Toh5NX/XL8CRtCWbdbgAlxCP1LtIe', '采购',       'buyer@cnc.local',               'ACTIVE'),
(10, 'finance',             '$2a$12$wulkj1kVO3eGc8v9ckGWEeE.Toh5NX/XL8CRtCWbdbgAlxCP1LtIe', '财务',       'finance@cnc.local',             'ACTIVE'),
(11, 'hr',                  '$2a$12$wulkj1kVO3eGc8v9ckGWEeE.Toh5NX/XL8CRtCWbdbgAlxCP1LtIe', '人事',       'hr@cnc.local',                  'ACTIVE'),
(12, 'procurement_manager', '$2a$12$wulkj1kVO3eGc8v9ckGWEeE.Toh5NX/XL8CRtCWbdbgAlxCP1LtIe', '采购主管',   'procurement_manager@cnc.local', 'ACTIVE'),
(13, 'operator',            '$2a$12$wulkj1kVO3eGc8v9ckGWEeE.Toh5NX/XL8CRtCWbdbgAlxCP1LtIe', '操作工',     'operator@cnc.local',            'ACTIVE'),
(14, 'visitor_demo',        '$2a$12$wulkj1kVO3eGc8v9ckGWEeE.Toh5NX/XL8CRtCWbdbgAlxCP1LtIe', '客户演示账号', 'visitor_demo@cnc.local',        'ACTIVE')
ON DUPLICATE KEY UPDATE `password_hash` = VALUES(`password_hash`), `real_name` = VALUES(`real_name`), `updated_at` = CURRENT_TIMESTAMP;

INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(1, 1), (2, 2), (3, 3), (4, 4), (5, 5), (6, 6), (7, 7), (8, 8), (9, 9), (10, 10), (11, 11), (12, 13), (13, 12), (14, 14)
ON DUPLICATE KEY UPDATE `user_id` = VALUES(`user_id`);

INSERT INTO `sys_dept` (`id`, `parent_id`, `dept_name`, `sort`, `status`) VALUES
(10, NULL, '生产一部', 1, 'ACTIVE'),
(11, NULL, '销售部', 2, 'ACTIVE')
ON DUPLICATE KEY UPDATE `dept_name` = VALUES(`dept_name`);

INSERT INTO `sys_dict` (`id`, `dict_type`, `dict_code`, `dict_label`, `sort`, `status`) VALUES
(1001, 'CUSTOMER_STATUS', 'C-DEMO-001', '昆山演示客户有限公司', 100, 'ACTIVE')
ON DUPLICATE KEY UPDATE `dict_label` = VALUES(`dict_label`);

-- ---------- 2. 业务 · 扫码演示工单（GD-/LZ-/SB-） ----------
USE `cnc_business`;

DELETE FROM `crm_production_scan` WHERE workorder_no IN ('GD-20260615-0001','GD-20260615-0002');
DELETE FROM `crm_production_report` WHERE workorder_no IN ('GD-20260615-0001','GD-20260615-0002');
DELETE FROM `crm_workorder_step` WHERE workorder_id IN (9001, 9002);
DELETE FROM `crm_production_schedule` WHERE workorder_id IN (9001, 9002);
DELETE FROM `crm_workorder` WHERE id IN (9001, 9002);

INSERT INTO `crm_workorder`
(`id`, `workorder_no`, `material_code`, `product_name`, `qty`, `priority`, `status`, `scheduled_start`, `equipment_id`, `equipment_type`, `estimated_hours`, `owner_user_id`, `dept_id`) VALUES
(9001, 'GD-20260615-0001', 'CP-DEMO-001', '演示壳体 A（扫码三码）', 50, 1, 'SCHEDULED', NOW(), 1, 'CNC', 8.0, 5, 10),
(9002, 'GD-20260615-0002', 'CP-DEMO-002', '演示法兰 B（在产）',     30, 3, 'IN_PROGRESS', NOW(), 2, 'LATHE', 6.0, 5, 10);

INSERT INTO `crm_workorder_step` (`workorder_id`, `step_no`, `step_name`, `equipment_type`, `estimated_minutes`, `status`) VALUES
(9001, 1, 'CNC 粗加工', 'CNC',   120, 'PENDING'),
(9001, 2, '精车',       'LATHE',  90, 'PENDING'),
(9002, 1, '铣齿',       'MILLING', 100, 'IN_PROGRESS');

INSERT INTO `crm_production_schedule`
(`schedule_no`, `workorder_id`, `equipment_id`, `equipment_type`, `plan_start`, `plan_end`, `status`) VALUES
('SCH-DEMO-0001', 9001, 1, 'CNC',   DATE_ADD(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 9 HOUR), 'PLANNED'),
('SCH-DEMO-0002', 9002, 2, 'LATHE', NOW(), DATE_ADD(NOW(), INTERVAL 6 HOUR), 'IN_PROGRESS');

-- ---------- 3. 生产工作台看板 ----------
DELETE FROM `crm_dashboard_production` WHERE snapshot_no LIKE 'DS-DEMO-%';

INSERT INTO `crm_dashboard_production`
(`snapshot_no`, `workorder_no`, `product_name`, `workorder_status`, `qty_planned`, `qty_completed`, `progress`, `alert_type`, `alert_message`, `snapshot_at`) VALUES
('DS-DEMO-0001', 'GD-20260615-0001', '演示壳体 A', 'PENDING',     50,  0,  0.00, NULL,       NULL,                 NOW()),
('DS-DEMO-0002', 'GD-20260615-0002', '演示法兰 B', 'IN_PROGRESS', 30, 12, 40.00, NULL,       NULL,                 NOW()),
('DS-DEMO-0003', 'GD-20260610-0003', '历史工单 C', 'INSPECT',     80, 80, 100.00, NULL,      NULL,                 NOW()),
('DS-DEMO-0004', NULL, NULL, NULL, 0, 0, 0.00, 'WARN',     'CNC-02 负荷 92%',              NOW()),
('DS-DEMO-0005', NULL, NULL, NULL, 0, 0, 0.00, 'CRITICAL', '工单 GD-20260615-0001 待开工', NOW());

-- ---------- 4. 报价 → 订单演示（业务员 5 分钟流程） ----------
DELETE FROM `crm_quote_item` WHERE quote_id = 8001;
DELETE FROM `crm_quote` WHERE id = 8001;
DELETE FROM `crm_order_item` WHERE order_id = 7001;
DELETE FROM `crm_order` WHERE id = 7001;

INSERT INTO `crm_quote`
(`id`, `quote_no`, `customer_id`, `customer_name`, `owner_user_id`, `dept_id`, `total_amount`, `delivery_date`, `status`, `current_node`) VALUES
(8001, 'BJ20260615-0001', 1001, '昆山演示客户有限公司', 2, 11, 128000.00, DATE_ADD(CURDATE(), INTERVAL 30 DAY), 'SUBMITTED', 2);

INSERT INTO `crm_quote_item`
(`quote_id`, `drawing_no`, `material`, `spec`, `quantity`, `unit_price`, `amount`, `sort`) VALUES
(8001, 'DWG-DEMO-001', '45#钢', 'φ120×80', 100, 1280.00, 128000.00, 1);

INSERT INTO `crm_order`
(`id`, `order_no`, `quote_id`, `customer_id`, `customer_name`, `owner_user_id`, `dept_id`, `total_amount`, `delivery_date`, `status`, `production_order_no`) VALUES
(7001, 'XS20260615-0001', 8001, 1001, '昆山演示客户有限公司', 2, 11, 128000.00, DATE_ADD(CURDATE(), INTERVAL 30 DAY), 'CONFIRMED', NULL);

INSERT INTO `crm_order_item`
(`order_id`, `drawing_no`, `material`, `quantity`, `unit_price`, `amount`, `sort`) VALUES
(7001, 'DWG-DEMO-001', '45#钢', 100, 1280.00, 128000.00, 1);

-- ---------- 5. 物料条码（仓储扫码 WL-） ----------
DELETE FROM `crm_material_barcode` WHERE barcode_no LIKE 'WL-DEMO-%';

INSERT IGNORE INTO `crm_material` (`material_code`, `material_name`, `spec`, `unit`, `category_id`, `cost_material`, `cost_labor`, `cost_machine`, `cost_overhead`, `cost_outsource`, `cost_total`) VALUES
('CP-DEMO-001', '演示壳体 A（扫码三码）', 'φ120×80', 'PCS', 1, 800.00, 40.00, 20.00, 0.00, 0.00, 860.00),
('CP-DEMO-002', '演示法兰 B（在产）', '标准件', 'PCS', 1, 700.00, 30.00, 15.00, 0.00, 0.00, 745.00),
('RM-STEEL-45', '45#圆钢', 'Φ120', 'KG', 1, 12.00, 3.00, 2.00, 1.00, 0.00, 18.00),
('RM-CAST-HT',  'HT250 铸件', '标准件', 'PCS', 1, 45.00, 8.00, 5.00, 2.00, 0.00, 60.00)
ON DUPLICATE KEY UPDATE `material_name` = VALUES(`material_name`), `spec` = VALUES(`spec`);

INSERT INTO `crm_material_barcode` (`barcode_no`, `material_code`, `spec`, `batch_no`, `qty`, `status`, `generated_by`) VALUES
('WL-DEMO-STEEL-001', 'RM-STEEL-45', '45#圆钢 Φ120', 'B20260615', 500, 'ACTIVE', 1),
('WL-DEMO-CAST-001',  'RM-CAST-HT',  'HT250 铸件',   'B20260616', 200, 'ACTIVE', 1)
ON DUPLICATE KEY UPDATE `qty` = VALUES(`qty`);

SET FOREIGN_KEY_CHECKS = 1;

-- ---------- 体验账号 ----------
SELECT '=== 演示账号（密码均为 123456）===' AS info;
SELECT u.username, u.real_name, r.role_code
FROM cnc_platform.sys_user u
JOIN cnc_platform.sys_user_role ur ON u.id = ur.user_id
JOIN cnc_platform.sys_role r ON ur.role_id = r.id
WHERE u.id BETWEEN 1 AND 12
ORDER BY u.id;
SELECT '=== 扫码三码口令 ===' AS info;
SELECT 'GD-20260615-0001 → LZ-GD001-P01 → SB-CNC-001' AS scan_flow;
