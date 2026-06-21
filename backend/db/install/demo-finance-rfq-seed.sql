-- ============================================================
-- 演示数据补全：成本核算 / 询价详情 / 订单时间线
-- 前置：db/init.sql 或 demo-flow-seed.sql 已导入
-- 用途：财务成本列表、询价详情、订单时间线全链路展示
-- ============================================================

SET NAMES utf8mb4;
USE `cnc_business`;

-- ---------- 1. 订单时间线（演示订单 7001 + 通用订单 5） ----------
DELETE FROM `crm_order_history` WHERE `order_id` IN (5, 7001);

INSERT INTO `crm_order_history` (`order_id`, `operation`, `before_json`, `after_json`, `changed_by`, `changed_at`) VALUES
(7001, 'CREATE',        NULL, '{"status":"DRAFT"}',      2, '2026-06-15 09:00:00'),
(7001, 'CONFIRM',       NULL, '{"status":"CONFIRMED"}',  2, '2026-06-15 09:30:00'),
(7001, 'APPROVE',       NULL, '{"status":"PRODUCING"}',  3, '2026-06-15 10:00:00'),
(7001, 'CONVERT_PROD',  NULL, '{"productionOrderNo":"GD-20260615-0001"}', 5, '2026-06-15 11:00:00'),
(5,    'CREATE',        NULL, '{"status":"DRAFT"}',      2, '2026-06-10 08:00:00'),
(5,    'CONFIRM',       NULL, '{"status":"CONFIRMED"}',  2, '2026-06-10 09:00:00'),
(5,    'APPROVE',       NULL, '{"status":"PRODUCING"}',  3, '2026-06-10 10:00:00');

-- ---------- 2. 2026-06 成本核算（工单维度，含 5 段成本） ----------
DELETE FROM `crm_cost_segment` WHERE `cost_id` IN (
  SELECT id FROM (SELECT id FROM `crm_cost_accounting` WHERE `cost_no` LIKE 'CA-DEMO-202606-%') t
);
DELETE FROM `crm_cost_accounting` WHERE `cost_no` LIKE 'CA-DEMO-202606-%';

INSERT INTO `crm_cost_accounting`
  (`cost_no`, `ref_type`, `ref_id`, `ref_no`, `material_id`, `material_code`, `material_name`, `qty`, `unit_cost`, `total_cost`, `standard_cost`, `variance`, `variance_rate`, `status`, `cost_date`) VALUES
('CA-DEMO-202606-0001', 'WORKORDER', 9001, 'GD-20260615-0001', 2001, 'CP-DEMO-001', '演示壳体 A', 50.00,  860.0000, 43000.00, 40000.00,  3000.00,  7.5000, 'CONFIRMED', '2026-06-15'),
('CA-DEMO-202606-0002', 'WORKORDER', 9002, 'GD-20260615-0002', 2002, 'CP-DEMO-002', '演示法兰 B', 30.00,  920.0000, 27600.00, 25000.00,  2600.00, 10.4000, 'CONFIRMED', '2026-06-15'),
('CA-DEMO-202606-0003', 'WORKORDER', 6001, 'GD20260501-0001',  2001, 'M-AUTO-PART-001', '汽车配件 001', 50.00, 900.0000, 45000.00, 40000.00, 5000.00, 12.5000, 'CONFIRMED', '2026-06-01'),
('CA-DEMO-202606-0004', 'WORKORDER', 6002, 'GD20260502-0002',  2002, 'M-MACH-PART-002', '机械配件 002', 80.00, 750.0000, 60000.00, 58000.00, 2000.00,  3.4483, 'CONFIRMED', '2026-06-08'),
('CA-DEMO-202606-0005', 'ORDER',     7001, 'XS20260615-0001',  2003, 'DWG-DEMO-001',    '45#钢 φ120×80', 100.00, 850.0000, 85000.00, 82000.00, 3000.00,  3.6585, 'CONFIRMED', '2026-06-15'),
('CA-DEMO-202606-0006', 'WORKORDER', 6003, 'GD20260510-0003',  2003, 'M-HARD-PART-003', '五金配件 003', 60.00, 520.0000, 31200.00, 30000.00, 1200.00,  4.0000, 'DRAFT',     '2026-06-12'),
('CA-DEMO-202606-0007', 'OUTSOURCE', 7001, 'WW20260515-0001',  2002, 'M-MACH-PART-002', '机械配件 002', 80.00, 1200.0000, 96000.00, 100000.00, -4000.00, -4.0000, 'CONFIRMED', '2026-06-18');

INSERT INTO `crm_cost_segment` (`cost_id`, `segment_code`, `segment_name`, `amount`, `source`, `remark`)
SELECT c.id, 'MATERIAL', '材料', 18000.00, '1.9', '6061 铝板' FROM `crm_cost_accounting` c WHERE c.cost_no = 'CA-DEMO-202606-0001'
UNION ALL SELECT c.id, 'PROCESS', '加工', 15000.00, '1.10', 'CNC 精加工' FROM `crm_cost_accounting` c WHERE c.cost_no = 'CA-DEMO-202606-0001'
UNION ALL SELECT c.id, 'OUTSOURCE', '委外', 5000.00, '1.26', '表面处理' FROM `crm_cost_accounting` c WHERE c.cost_no = 'CA-DEMO-202606-0001'
UNION ALL SELECT c.id, 'MANAGE', '管理', 3000.00, '1.17', '管理分摊' FROM `crm_cost_accounting` c WHERE c.cost_no = 'CA-DEMO-202606-0001'
UNION ALL SELECT c.id, 'DEPRECIATION', '折旧', 2000.00, '1.9', '设备折旧' FROM `crm_cost_accounting` c WHERE c.cost_no = 'CA-DEMO-202606-0001'
UNION ALL SELECT c.id, 'MATERIAL', '材料', 12000.00, '1.9', '45# 圆钢' FROM `crm_cost_accounting` c WHERE c.cost_no = 'CA-DEMO-202606-0002'
UNION ALL SELECT c.id, 'PROCESS', '加工', 10000.00, '1.10', '铣齿' FROM `crm_cost_accounting` c WHERE c.cost_no = 'CA-DEMO-202606-0002'
UNION ALL SELECT c.id, 'OUTSOURCE', '委外', 2000.00, '1.26', '委外热处理' FROM `crm_cost_accounting` c WHERE c.cost_no = 'CA-DEMO-202606-0002'
UNION ALL SELECT c.id, 'MANAGE', '管理', 2000.00, '1.17', '管理分摊' FROM `crm_cost_accounting` c WHERE c.cost_no = 'CA-DEMO-202606-0002'
UNION ALL SELECT c.id, 'DEPRECIATION', '折旧', 1600.00, '1.9', '设备折旧' FROM `crm_cost_accounting` c WHERE c.cost_no = 'CA-DEMO-202606-0002'
UNION ALL SELECT c.id, 'MATERIAL', '材料', 20000.00, '1.9', '半成品' FROM `crm_cost_accounting` c WHERE c.cost_no = 'CA-DEMO-202606-0003'
UNION ALL SELECT c.id, 'PROCESS', '加工', 15000.00, '1.10', 'CNC' FROM `crm_cost_accounting` c WHERE c.cost_no = 'CA-DEMO-202606-0003'
UNION ALL SELECT c.id, 'OUTSOURCE', '委外', 5000.00, '1.26', '委外电镀' FROM `crm_cost_accounting` c WHERE c.cost_no = 'CA-DEMO-202606-0003'
UNION ALL SELECT c.id, 'MANAGE', '管理', 3000.00, '1.17', '管理分摊' FROM `crm_cost_accounting` c WHERE c.cost_no = 'CA-DEMO-202606-0003'
UNION ALL SELECT c.id, 'DEPRECIATION', '折旧', 2000.00, '1.9', '设备折旧' FROM `crm_cost_accounting` c WHERE c.cost_no = 'CA-DEMO-202606-0003'
UNION ALL SELECT c.id, 'MATERIAL', '材料', 28000.00, '1.9', '45# 钢' FROM `crm_cost_accounting` c WHERE c.cost_no = 'CA-DEMO-202606-0004'
UNION ALL SELECT c.id, 'PROCESS', '加工', 20000.00, '1.10', '车铣复合' FROM `crm_cost_accounting` c WHERE c.cost_no = 'CA-DEMO-202606-0004'
UNION ALL SELECT c.id, 'OUTSOURCE', '委外', 0.00, '1.26', '无委外' FROM `crm_cost_accounting` c WHERE c.cost_no = 'CA-DEMO-202606-0004'
UNION ALL SELECT c.id, 'MANAGE', '管理', 8000.00, '1.17', '管理分摊' FROM `crm_cost_accounting` c WHERE c.cost_no = 'CA-DEMO-202606-0004'
UNION ALL SELECT c.id, 'DEPRECIATION', '折旧', 4000.00, '1.9', '设备折旧' FROM `crm_cost_accounting` c WHERE c.cost_no = 'CA-DEMO-202606-0004'
UNION ALL SELECT c.id, 'MATERIAL', '材料', 40000.00, '1.9', '45#钢毛坯' FROM `crm_cost_accounting` c WHERE c.cost_no = 'CA-DEMO-202606-0005'
UNION ALL SELECT c.id, 'PROCESS', '加工', 25000.00, '1.10', 'CNC+精车' FROM `crm_cost_accounting` c WHERE c.cost_no = 'CA-DEMO-202606-0005'
UNION ALL SELECT c.id, 'OUTSOURCE', '委外', 10000.00, '1.26', '委外热处理' FROM `crm_cost_accounting` c WHERE c.cost_no = 'CA-DEMO-202606-0005'
UNION ALL SELECT c.id, 'MANAGE', '管理', 6000.00, '1.17', '管理分摊' FROM `crm_cost_accounting` c WHERE c.cost_no = 'CA-DEMO-202606-0005'
UNION ALL SELECT c.id, 'DEPRECIATION', '折旧', 4000.00, '1.9', '设备折旧' FROM `crm_cost_accounting` c WHERE c.cost_no = 'CA-DEMO-202606-0005'
UNION ALL SELECT c.id, 'MATERIAL', '材料', 14000.00, '1.9', 'Q235' FROM `crm_cost_accounting` c WHERE c.cost_no = 'CA-DEMO-202606-0006'
UNION ALL SELECT c.id, 'PROCESS', '加工', 10000.00, '1.10', '机加工' FROM `crm_cost_accounting` c WHERE c.cost_no = 'CA-DEMO-202606-0006'
UNION ALL SELECT c.id, 'OUTSOURCE', '委外', 2000.00, '1.26', '委外' FROM `crm_cost_accounting` c WHERE c.cost_no = 'CA-DEMO-202606-0006'
UNION ALL SELECT c.id, 'MANAGE', '管理', 3200.00, '1.17', '管理分摊' FROM `crm_cost_accounting` c WHERE c.cost_no = 'CA-DEMO-202606-0006'
UNION ALL SELECT c.id, 'DEPRECIATION', '折旧', 2000.00, '1.9', '设备折旧' FROM `crm_cost_accounting` c WHERE c.cost_no = 'CA-DEMO-202606-0006'
UNION ALL SELECT c.id, 'MATERIAL', '材料', 30000.00, '1.9', '原料' FROM `crm_cost_accounting` c WHERE c.cost_no = 'CA-DEMO-202606-0007'
UNION ALL SELECT c.id, 'PROCESS', '加工', 20000.00, '1.10', '前道加工' FROM `crm_cost_accounting` c WHERE c.cost_no = 'CA-DEMO-202606-0007'
UNION ALL SELECT c.id, 'OUTSOURCE', '委外', 40000.00, '1.26', '委外热处理' FROM `crm_cost_accounting` c WHERE c.cost_no = 'CA-DEMO-202606-0007'
UNION ALL SELECT c.id, 'MANAGE', '管理', 4000.00, '1.17', '管理分摊' FROM `crm_cost_accounting` c WHERE c.cost_no = 'CA-DEMO-202606-0007'
UNION ALL SELECT c.id, 'DEPRECIATION', '折旧', 2000.00, '1.9', '设备折旧' FROM `crm_cost_accounting` c WHERE c.cost_no = 'CA-DEMO-202606-0007';

-- ---------- 3. 询价单演示（RFQ #1 三厂商完整报价） ----------
DELETE FROM `crm_rfq_quote` WHERE `rfq_id` = 1;
DELETE FROM `crm_rfq_vendor` WHERE `rfq_id` = 1;

INSERT INTO `crm_rfq` (`id`, `rfq_no`, `title`, `material_id`, `material_code`, `material_name`, `qty`, `unit`, `budget_amount`, `required_date`, `status`, `winner_mode`, `created_by`, `created_at`) VALUES
(1, 'RF20260612-0001', '6061 铝板询价', 1001, 'M-AL6061-PLATE', '6061 铝板 5mm', 500.00, 'KG', 50000.00, DATE_ADD(CURDATE(), INTERVAL 14 DAY), 'QUOTED', 'LOWEST', 9, '2026-06-12 08:00:00')
ON DUPLICATE KEY UPDATE
  `material_code` = VALUES(`material_code`), `material_name` = VALUES(`material_name`),
  `qty` = VALUES(`qty`), `status` = VALUES(`status`), `required_date` = VALUES(`required_date`);

INSERT INTO `crm_rfq_vendor` (`rfq_id`, `vendor_id`, `vendor_name`, `vendor_code`, `contact_name`, `contact_phone`, `invited_at`, `quote_status`) VALUES
(1, 901, '上海铝业',   'V-SH-AL',  '张经理', '13800001111', '2026-06-12 09:00:00', 'SUBMITTED'),
(1, 902, '江苏金属',   'V-JS-MT',  '李经理', '13800002222', '2026-06-12 09:00:00', 'SUBMITTED'),
(1, 903, '浙江精材',   'V-ZJ-JC',  '王经理', '13800003333', '2026-06-12 09:00:00', 'SUBMITTED');

INSERT INTO `crm_rfq_quote` (`rfq_id`, `rfq_vendor_id`, `vendor_id`, `unit_price`, `total_amount`, `lead_time_days`, `valid_until`, `payment_terms`, `quality_score`, `delivery_score`, `service_score`, `is_awarded`, `submitted_at`)
SELECT 1, v.id, v.vendor_id, q.unit_price, q.total_amount, q.lead_time_days, DATE_ADD(CURDATE(), INTERVAL 30 DAY), '月结30天', q.quality_score, 90, 88, q.is_awarded, q.submitted_at
FROM (
  SELECT 901 AS vendor_id, 540.00 AS unit_price, 270000.00 AS total_amount, 7 AS lead_time_days, 95 AS quality_score, 0 AS is_awarded, '2026-06-13 10:00:00' AS submitted_at
  UNION ALL SELECT 902, 560.00, 280000.00, 10, 92, 0, '2026-06-13 11:00:00'
  UNION ALL SELECT 903, 580.00, 290000.00, 5, 98, 1, '2026-06-13 12:00:00'
) q
JOIN `crm_rfq_vendor` v ON v.rfq_id = 1 AND v.vendor_id = q.vendor_id;

UPDATE `crm_rfq` SET `awarded_vendor_id` = 903, `awarded_vendor_name` = '浙江精材', `awarded_amount` = 290000.00 WHERE `id` = 1;

-- ---------- 4. 利润率预警阈值（E1-S3 系统参数） ----------
USE `cnc_platform`;

INSERT INTO `sys_param` (`param_key`, `param_value`, `param_group`, `description`) VALUES
('finance.profit.warning-rate', '10.00', 'FINANCE_PROFIT', '利润率预警黄线（%）：低于此值标黄'),
('finance.profit.critical-rate', '5.00', 'FINANCE_PROFIT', '利润率预警红线（%）：低于此值标红'),
('finance.profit.loss-rate', '0.00', 'FINANCE_PROFIT', '利润率亏损线（%）：低于此值深红强提示'),
('finance.profit.realtime.enabled', 'true', 'FINANCE_PROFIT', '加工中实时利润率预警开关'),
('finance.profit.realtime.interval-hours', '1', 'FINANCE_PROFIT', '实时预警重算间隔（小时）'),
('finance.profit.alert.dedup-hours', '24', 'FINANCE_PROFIT', '同一订单同一等级预警去重窗口（小时）')
ON DUPLICATE KEY UPDATE `param_value` = VALUES(`param_value`), `description` = VALUES(`description`);

SELECT '=== 演示数据已导入 ===' AS info;
SELECT COUNT(*) AS cost_rows FROM cnc_business.crm_cost_accounting WHERE cost_date >= '2026-06-01';
SELECT COUNT(*) AS rfq_quotes FROM cnc_business.crm_rfq_quote WHERE rfq_id = 1;
