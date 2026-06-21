-- V1.3.7 · Story 1.38 · 财务·回款控制 (FR-9-3)
-- 迁移：crm_payment_plan 回款计划 + crm_payment_alert 逾期告警
-- 3 P1 修补：回款金额 ≤ 订单金额 / 提前 3 天 ALERT / 逾期 ALERT_CRITICAL / 跨 1.36 应收
-- 模板：PP{yyyyMMdd}{seq:4}

USE `cnc_business`;

-- 回款计划（订单 SETTLED 触发）
CREATE TABLE IF NOT EXISTS `crm_payment_plan` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `plan_no` VARCHAR(32) NOT NULL COMMENT 'PP{yyyyMMdd}{seq:4}',
  `customer_id` BIGINT NOT NULL,
  `customer_name` VARCHAR(128) DEFAULT NULL,
  `order_id` BIGINT NOT NULL COMMENT '关联 1.6 订单',
  `order_no` VARCHAR(32) NOT NULL,
  `receivable_id` BIGINT DEFAULT NULL COMMENT '关联 1.36 应收',
  `receivable_no` VARCHAR(32) DEFAULT NULL,
  `total_amount` DECIMAL(14,2) NOT NULL COMMENT '订单金额',
  `planned_amount` DECIMAL(14,2) NOT NULL COMMENT '计划回款金额 · P1 修补 1 ≤ 订单金额',
  `paid_amount` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '已回款',
  `planned_date` DATE NOT NULL COMMENT '计划回款日',
  `alert_level` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/ALERT（提前 3 天）/ALERT_CRITICAL（逾期）/PAID',
  `paid_at` DATETIME DEFAULT NULL,
  `paid_by` BIGINT DEFAULT NULL,
  `remark` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_plan_order` (`order_id`),
  KEY `idx_plan_customer` (`customer_id`),
  KEY `idx_plan_level` (`alert_level`),
  KEY `idx_plan_planned_date` (`planned_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回款计划（V1.3.7 Story 1.38 FR-9-3）';

-- 逾期告警
CREATE TABLE IF NOT EXISTS `crm_payment_alert` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `plan_id` BIGINT NOT NULL,
  `alert_level` VARCHAR(20) NOT NULL COMMENT 'ALERT（提前 3 天）/ALERT_CRITICAL（逾期）',
  `alert_message` VARCHAR(500) DEFAULT NULL,
  `days_to_due` INT DEFAULT NULL COMMENT '距离到期天数（负数=逾期）',
  `notified_at` DATETIME DEFAULT NULL,
  `notified_channel` VARCHAR(16) DEFAULT NULL COMMENT 'EMAIL/SMS/INAPP',
  `acknowledged` TINYINT(1) NOT NULL DEFAULT 0,
  `acknowledged_by` BIGINT DEFAULT NULL,
  `acknowledged_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_alert_plan` (`plan_id`),
  KEY `idx_alert_level` (`alert_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回款告警（V1.3.7 Story 1.38 FR-9-3）';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
