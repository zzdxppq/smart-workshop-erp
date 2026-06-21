-- ============================================================
-- V4__crm_quote.sql · V1.3.7 Story 1.5
-- ============================================================
-- 目的：Story 1.5 报价与多级审批
--     创建 3 张新表（crm_quote / crm_quote_item / crm_quote_history）
-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- ============================================================

USE `cnc_business`;

-- ---------- 1. crm_quote 报价单 ----------
DROP TABLE IF EXISTS `crm_quote`;
CREATE TABLE `crm_quote` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `quote_no` VARCHAR(30) NOT NULL COMMENT 'BJ+YYYYMMDD+NNNN (例 BJ20260611-0001)',
  `customer_id` BIGINT NOT NULL COMMENT '客户 ID (sys_dict?type=CUSTOMER_STATUS 查 BLACKLIST)',
  `customer_name` VARCHAR(200) NOT NULL,
  `owner_user_id` BIGINT NOT NULL,
  `dept_id` BIGINT NOT NULL,
  `currency` VARCHAR(10) NOT NULL DEFAULT 'CNY',
  `total_amount` DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT '由 items 自动计算(只读)',
  `delivery_date` DATE NOT NULL,
  `is_fa` TINYINT(1) NOT NULL DEFAULT 0,
  `is_new` TINYINT(1) NOT NULL DEFAULT 0,
  `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/SUBMITTED/APPROVED/REJECTED/CONVERTED',
  `current_node` INT DEFAULT 1 COMMENT '当前审批节点 (1/2/3)',
  `comment` VARCHAR(1000) DEFAULT NULL,
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_quote_no` (`quote_no`),
  KEY `idx_customer_id` (`customer_id`),
  KEY `idx_owner_user_id` (`owner_user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB COMMENT='报价单 (V1.3.7 Story 1.5)';

-- ---------- 2. crm_quote_item 报价明细 ----------
DROP TABLE IF EXISTS `crm_quote_item`;
CREATE TABLE `crm_quote_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `quote_id` BIGINT NOT NULL,
  `drawing_no` VARCHAR(50) NOT NULL COMMENT '图号',
  `material` VARCHAR(50) NOT NULL,
  `spec` VARCHAR(200) DEFAULT NULL,
  `quantity` INT NOT NULL,
  `unit_price` DECIMAL(15,2) NOT NULL,
  `amount` DECIMAL(15,2) NOT NULL COMMENT '= quantity * unit_price',
  `is_fa` TINYINT(1) NOT NULL DEFAULT 0,
  `is_new` TINYINT(1) NOT NULL DEFAULT 0,
  `sort` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_quote_id_sort` (`quote_id`, `sort`),
  CONSTRAINT `fk_quote_item_quote` FOREIGN KEY (`quote_id`) REFERENCES `crm_quote` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='报价明细 (V1.3.7 Story 1.5)';

-- ---------- 3. crm_quote_history 报价变更历史 ----------
DROP TABLE IF EXISTS `crm_quote_history`;
CREATE TABLE `crm_quote_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `quote_id` BIGINT NOT NULL,
  `operation` VARCHAR(20) NOT NULL COMMENT 'CREATE/UPDATE/SUBMIT/APPROVE/REJECT/CONVERT/PDF_DOWNLOAD',
  `before_json` TEXT,
  `after_json` TEXT,
  `changed_by` BIGINT NOT NULL,
  `changed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_quote_id` (`quote_id`),
  KEY `idx_changed_by` (`changed_by`),
  KEY `idx_changed_at` (`changed_at`)
) ENGINE=InnoDB COMMENT='报价变更历史 (V1.3.7 Story 1.5 · 红线 5)';

-- ---------- 4. CUSTOMER_STATUS 字典类型（黑名单） ----------
USE `cnc_platform`;

INSERT INTO `sys_dict_type` (`type_code`, `type_name`, `description`, `is_builtin`) VALUES
('CUSTOMER_STATUS', '客户状态', 'NORMAL / BLACKLIST（黑名单直接驳回 40902）', 1)
ON DUPLICATE KEY UPDATE `type_name` = VALUES(`type_name`);

-- V94 · mock 清理：CUSTOMER_STATUS 演示客户已移至 init_data.sql（保留字典类型）

-- 5 条 UI 红线检查
SELECT 'V4__crm_quote.sql 迁移完成' AS message,
       (SELECT COUNT(*) FROM sys_dict WHERE dict_type = 'CUSTOMER_STATUS') AS customer_status_count;
