-- ============================================================
-- V3__system_params.sql · V1.3.7 Story 1.3
-- ============================================================
-- 目的：Story 1.3 系统参数/数据字典/HR Feign 真实集成/性能与安全闭环
--     创建 5 张新表（sys_dict_type / sys_param / sys_change_log / sys_global_threshold / sys_audit_log_archive）
-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- ============================================================

USE `cnc_platform`;

-- ---------- 1. sys_dict_type（字典类型字典） ----------
DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `type_code` VARCHAR(50) NOT NULL COMMENT '字典类型编码（6 类：MATERIAL_CATEGORY/PROCESS_TYPE/SURFACE_TREATMENT/WORK_SHIFT/WAREHOUSE/CURRENCY）',
  `type_name` VARCHAR(100) NOT NULL COMMENT '字典类型名称',
  `description` VARCHAR(500) DEFAULT NULL,
  `is_builtin` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否内置（1=不可删 0=可删）',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_type_code` (`type_code`)
) ENGINE=InnoDB COMMENT='字典类型字典（V1.3.7 Story 1.3）';

-- ---------- 2. sys_param（系统参数） ----------
DROP TABLE IF EXISTS `sys_param`;
CREATE TABLE `sys_param` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `param_key` VARCHAR(100) NOT NULL COMMENT '参数键（dot.case）',
  `param_value` VARCHAR(1000) NOT NULL COMMENT '参数值',
  `param_group` VARCHAR(50) NOT NULL COMMENT 'BIZ_DOC_NO/PRINT_TEMPLATE/APP_CACHE_TTL',
  `description` VARCHAR(500) DEFAULT NULL,
  `updated_by` BIGINT DEFAULT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_param_key` (`param_key`),
  KEY `idx_param_group` (`param_group`)
) ENGINE=InnoDB COMMENT='系统参数（V1.3.7 Story 1.3 · FR-1-3-2）';

-- ---------- 3. sys_change_log（变更日志） ----------
DROP TABLE IF EXISTS `sys_change_log`;
CREATE TABLE `sys_change_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `entity` VARCHAR(50) NOT NULL COMMENT 'threshold/dict/param/...',
  `entity_id` BIGINT DEFAULT NULL COMMENT '业务实体 ID',
  `operation` VARCHAR(20) NOT NULL COMMENT 'CREATE/UPDATE/DELETE',
  `before_value` TEXT,
  `after_value` TEXT,
  `changed_by` BIGINT DEFAULT NULL,
  `changed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_entity_entity_id` (`entity`, `entity_id`),
  KEY `idx_changed_by` (`changed_by`),
  KEY `idx_changed_at` (`changed_at`)
) ENGINE=InnoDB COMMENT='变更日志（V1.3.7 Story 1.3 · FR-1-3-3 · V1.3.7 红线 5）';

-- ---------- 4. sys_global_threshold（金额阈值全局） ----------
DROP TABLE IF EXISTS `sys_global_threshold`;
CREATE TABLE `sys_global_threshold` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `biz_type` VARCHAR(50) NOT NULL COMMENT 'QUOTE/ORDER/PURCHASE/PAYMENT',
  `role_code` VARCHAR(50) NOT NULL COMMENT 'salesperson/dept_manager/gm',
  `threshold` DECIMAL(15,2) DEFAULT NULL COMMENT '金额阈值（NULL=无限额）',
  `currency` VARCHAR(10) NOT NULL DEFAULT 'CNY',
  `effective_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_biz_type_role` (`biz_type`, `role_code`)
) ENGINE=InnoDB COMMENT='金额阈值全局（V1.3.7 Story 1.3 · FR-1-3-3 · 双轨：Nacos 优先）';

-- ---------- 5. sys_audit_log_archive（审计日志归档表，5 年保留） ----------
DROP TABLE IF EXISTS `sys_audit_log_archive`;
CREATE TABLE `sys_audit_log_archive` (
  `id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `module` VARCHAR(50) NOT NULL,
  `action` VARCHAR(50) NOT NULL,
  `before_json` TEXT,
  `after_json` TEXT,
  `ip` VARCHAR(50),
  `ts` DATETIME NOT NULL,
  `archived_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_module_action_ts` (`user_id`, `module`, `action`, `ts`),
  KEY `idx_archived_at` (`archived_at`)
) ENGINE=InnoDB COMMENT='审计日志归档表（V1.3.7 Story 1.3 · 1 年保留 + 5 年归档 · 5 年后清理）';


-- ============================================================
-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- ============================================================

-- 1) 字典类型（6 类）
INSERT INTO `sys_dict_type` (`type_code`, `type_name`, `description`, `is_builtin`) VALUES
('MATERIAL_CATEGORY', '物料分类', '钢材/铝材/铜材/塑料/其他', 1),
('PROCESS_TYPE', '工序类型', 'CNC 加工/车削/铣削/磨削/表处', 1),
('SURFACE_TREATMENT', '表处类型', '阳极氧化/喷涂/电镀/抛光', 1),
('WORK_SHIFT', '班别', '早班/中班/晚班', 1),
('WAREHOUSE', '仓库', '原材料仓/半成品仓/成品仓/委外仓', 1),
('CURRENCY', '币种', 'CNY/USD/EUR/JPY', 1);

-- 清理 V1 遗留小写字典（utf8mb4_ci 下 process_type 与 PROCESS_TYPE 视为同一 type · CNC 会冲突）
DELETE FROM `sys_dict` WHERE BINARY `dict_type` IN ('process_type', 'material_category');

-- 2) 物料分类字典（5 项）
INSERT INTO `sys_dict` (`dict_type`, `dict_code`, `dict_label`, `sort`, `status`) VALUES
('MATERIAL_CATEGORY', 'STEEL', '钢材', 1, 'ACTIVE'),
('MATERIAL_CATEGORY', 'ALUMINUM', '铝材', 2, 'ACTIVE'),
('MATERIAL_CATEGORY', 'COPPER', '铜材', 3, 'ACTIVE'),
('MATERIAL_CATEGORY', 'PLASTIC', '塑料', 4, 'ACTIVE'),
('MATERIAL_CATEGORY', 'OTHER', '其他', 99, 'ACTIVE')
ON DUPLICATE KEY UPDATE
  `dict_type` = VALUES(`dict_type`),
  `dict_label` = VALUES(`dict_label`),
  `sort` = VALUES(`sort`),
  `status` = VALUES(`status`);

-- 3) 工序类型字典（5 项）
INSERT INTO `sys_dict` (`dict_type`, `dict_code`, `dict_label`, `sort`, `status`) VALUES
('PROCESS_TYPE', 'CNC', 'CNC 加工', 1, 'ACTIVE'),
('PROCESS_TYPE', 'TURNING', '车削', 2, 'ACTIVE'),
('PROCESS_TYPE', 'MILLING', '铣削', 3, 'ACTIVE'),
('PROCESS_TYPE', 'GRINDING', '磨削', 4, 'ACTIVE'),
('PROCESS_TYPE', 'SURFACE_TREATMENT', '表处', 99, 'ACTIVE')
ON DUPLICATE KEY UPDATE
  `dict_type` = VALUES(`dict_type`),
  `dict_label` = VALUES(`dict_label`),
  `sort` = VALUES(`sort`),
  `status` = VALUES(`status`);

-- 4) 表处类型字典（4 项）
INSERT INTO `sys_dict` (`dict_type`, `dict_code`, `dict_label`, `sort`, `status`) VALUES
('SURFACE_TREATMENT', 'ANODIZING', '阳极氧化', 1, 'ACTIVE'),
('SURFACE_TREATMENT', 'PAINTING', '喷涂', 2, 'ACTIVE'),
('SURFACE_TREATMENT', 'PLATING', '电镀', 3, 'ACTIVE'),
('SURFACE_TREATMENT', 'POLISHING', '抛光', 4, 'ACTIVE')
ON DUPLICATE KEY UPDATE
  `dict_type` = VALUES(`dict_type`),
  `dict_label` = VALUES(`dict_label`),
  `sort` = VALUES(`sort`),
  `status` = VALUES(`status`);

-- 5) 班别字典（3 项）
INSERT INTO `sys_dict` (`dict_type`, `dict_code`, `dict_label`, `sort`, `status`) VALUES
('WORK_SHIFT', 'MORNING', '早班 (08:00-16:00)', 1, 'ACTIVE'),
('WORK_SHIFT', 'NOON', '中班 (16:00-00:00)', 2, 'ACTIVE'),
('WORK_SHIFT', 'NIGHT', '晚班 (00:00-08:00)', 3, 'ACTIVE')
ON DUPLICATE KEY UPDATE
  `dict_type` = VALUES(`dict_type`),
  `dict_label` = VALUES(`dict_label`),
  `sort` = VALUES(`sort`),
  `status` = VALUES(`status`);

-- 6) 仓库字典（4 项）
INSERT INTO `sys_dict` (`dict_type`, `dict_code`, `dict_label`, `sort`, `status`) VALUES
('WAREHOUSE', 'RAW', '原材料仓', 1, 'ACTIVE'),
('WAREHOUSE', 'SEMI', '半成品仓', 2, 'ACTIVE'),
('WAREHOUSE', 'FINISHED', '成品仓', 3, 'ACTIVE'),
('WAREHOUSE', 'OUTSOURCE', '委外仓', 4, 'ACTIVE')
ON DUPLICATE KEY UPDATE
  `dict_type` = VALUES(`dict_type`),
  `dict_label` = VALUES(`dict_label`),
  `sort` = VALUES(`sort`),
  `status` = VALUES(`status`);

-- 7) 币种字典（4 项）
INSERT INTO `sys_dict` (`dict_type`, `dict_code`, `dict_label`, `sort`, `status`) VALUES
('CURRENCY', 'CNY', '人民币 (¥)', 1, 'ACTIVE'),
('CURRENCY', 'USD', '美元 ($)', 2, 'ACTIVE'),
('CURRENCY', 'EUR', '欧元 (€)', 3, 'ACTIVE'),
('CURRENCY', 'JPY', '日元 (¥)', 4, 'ACTIVE')
ON DUPLICATE KEY UPDATE
  `dict_type` = VALUES(`dict_type`),
  `dict_label` = VALUES(`dict_label`),
  `sort` = VALUES(`sort`),
  `status` = VALUES(`status`);


-- ============================================================
-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- ============================================================

-- 1) 单据编号规则（BIZ_DOC_NO · 4 类 × 4 段）
INSERT INTO `sys_param` (`param_key`, `param_value`, `param_group`, `description`) VALUES
('biz.doc-no.quote', 'QUOTE-{yyyyMMdd}-{seq:4}', 'BIZ_DOC_NO', '报价单编号：QUOTE-20260610-0001'),
('biz.doc-no.order', 'ORDER-{yyyyMMdd}-{seq:4}', 'BIZ_DOC_NO', '订单编号：ORDER-20260610-0001'),
('biz.doc-no.purchase', 'PO-{yyyyMMdd}-{seq:4}', 'BIZ_DOC_NO', '采购单编号：PO-20260610-0001'),
('biz.doc-no.payment', 'PAY-{yyyyMMdd}-{seq:4}', 'BIZ_DOC_NO', '付款单编号：PAY-20260610-0001'),
('biz.doc-no.seq-start', '1', 'BIZ_DOC_NO', '单据编号起始值（每日 0 点重置）'),
('biz.doc-no.seq-step', '1', 'BIZ_DOC_NO', '单据编号步长');

-- 2) 打印模板（PRINT_TEMPLATE · 2 项）
INSERT INTO `sys_param` (`param_key`, `param_value`, `param_group`, `description`) VALUES
('print.quote.default', 'QUOTE_TEMPLATE_V1', 'PRINT_TEMPLATE', '报价单默认打印模板'),
('print.order.default', 'ORDER_TEMPLATE_V1', 'PRINT_TEMPLATE', '订单默认打印模板');

-- 3) APP 端离线缓存时长（APP_CACHE_TTL · 1 项）
INSERT INTO `sys_param` (`param_key`, `param_value`, `param_group`, `description`) VALUES
('app.cache-ttl', 'PT24H', 'APP_CACHE_TTL', 'APP 端离线缓存时长（ISO 8601 Duration，默认 24h）');


-- ============================================================
-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- ============================================================

-- QUOTE 业务：业务员 5万 / 部门经理 20万 / 总经理 无限额
INSERT INTO `sys_global_threshold` (`biz_type`, `role_code`, `threshold`, `currency`) VALUES
('QUOTE', 'salesperson', 50000.00, 'CNY'),
('QUOTE', 'dept_manager', 200000.00, 'CNY'),
('QUOTE', 'gm', NULL, 'CNY');

-- ORDER 业务（同 QUOTE）
INSERT INTO `sys_global_threshold` (`biz_type`, `role_code`, `threshold`, `currency`) VALUES
('ORDER', 'salesperson', 50000.00, 'CNY'),
('ORDER', 'dept_manager', 200000.00, 'CNY'),
('ORDER', 'gm', NULL, 'CNY');

-- PURCHASE 业务：采购员 1万 / 部门经理 5万 / 总经理 无限额
INSERT INTO `sys_global_threshold` (`biz_type`, `role_code`, `threshold`, `currency`) VALUES
('PURCHASE', 'buyer', 10000.00, 'CNY'),
('PURCHASE', 'dept_manager', 50000.00, 'CNY'),
('PURCHASE', 'gm', NULL, 'CNY');

-- PAYMENT 业务：财务双签 10万（>10万 gm + 财务总监双签）
INSERT INTO `sys_global_threshold` (`biz_type`, `role_code`, `threshold`, `currency`) VALUES
('PAYMENT', 'finance', 100000.00, 'CNY'),
('PAYMENT', 'gm', NULL, 'CNY'),
('PAYMENT', 'finance_director', NULL, 'CNY');


-- ============================================================
-- 迁移完成
-- ============================================================
SELECT 'V3__system_params.sql 迁移完成' AS message,
       (SELECT COUNT(*) FROM `sys_dict_type`) AS dict_type_count,
       (SELECT COUNT(*) FROM `sys_dict`) AS dict_count,
       (SELECT COUNT(*) FROM `sys_param`) AS param_count,
       (SELECT COUNT(*) FROM `sys_global_threshold`) AS threshold_count;
