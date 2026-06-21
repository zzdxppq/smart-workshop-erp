-- V1.3.7 · Story 1.32 · 采购·询比价 (FR-8-1)
-- 迁移：crm_rfq 询价单 + crm_rfq_vendor 询价厂商 + crm_rfq_quote 厂商报价
-- 3 P1 修补：询价单唯一 / 厂商报价必填 / 选最低不超预算 / 中标自动触发 PO
-- 模板：RF{yyyyMMdd}{seq:4}

USE `cnc_business`;

-- 询价单（RFQ）
CREATE TABLE IF NOT EXISTS `crm_rfq` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `rfq_no` VARCHAR(32) NOT NULL COMMENT 'RF{yyyyMMdd}{seq:4}',
  `title` VARCHAR(255) NOT NULL COMMENT '询价标题',
  `material_id` BIGINT NOT NULL,
  `material_code` VARCHAR(64) DEFAULT NULL,
  `material_name` VARCHAR(255) DEFAULT NULL,
  `qty` DECIMAL(12,2) NOT NULL COMMENT '需求数量',
  `unit` VARCHAR(16) DEFAULT NULL COMMENT '单位',
  `budget_amount` DECIMAL(14,2) NOT NULL COMMENT '预算金额 · P1 修补 3 校验',
  `required_date` DATE DEFAULT NULL COMMENT '需求到货日',
  `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/QUOTING/COMPARED/AWARDED/CLOSED',
  `awarded_vendor_id` BIGINT DEFAULT NULL,
  `awarded_vendor_name` VARCHAR(128) DEFAULT NULL,
  `awarded_quote_id` BIGINT DEFAULT NULL,
  `awarded_amount` DECIMAL(14,2) DEFAULT NULL,
  `purchase_order_id` BIGINT DEFAULT NULL COMMENT 'P1 修补 4 中标自动触发 PO',
  `purchase_order_no` VARCHAR(32) DEFAULT NULL,
  `winner_mode` VARCHAR(16) DEFAULT 'LOWEST' COMMENT 'LOWEST（最低价）/WEIGHTED（加权评分）',
  `created_by` BIGINT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_rfq_no` (`rfq_no`),
  KEY `idx_rfq_material` (`material_id`),
  KEY `idx_rfq_status` (`status`),
  KEY `idx_rfq_awarded_vendor` (`awarded_vendor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='询价单（V1.3.7 Story 1.32 FR-8-1）';

-- 询价-厂商（询价单关联的 3+ 候选厂商）
CREATE TABLE IF NOT EXISTS `crm_rfq_vendor` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `rfq_id` BIGINT NOT NULL,
  `vendor_id` BIGINT NOT NULL COMMENT '厂商 ID（来自系统厂商库）',
  `vendor_name` VARCHAR(128) NOT NULL,
  `vendor_code` VARCHAR(64) DEFAULT NULL,
  `contact_name` VARCHAR(64) DEFAULT NULL,
  `contact_phone` VARCHAR(32) DEFAULT NULL,
  `invited_at` DATETIME DEFAULT NULL,
  `quote_status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/QUOTED/NO_QUOTE/REJECTED',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_rfq_vendor` (`rfq_id`, `vendor_id`),
  KEY `idx_rfq_vendor_vendor` (`vendor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='询价-厂商关联（每 RFQ ≥ 3 厂商）';

-- 厂商报价
CREATE TABLE IF NOT EXISTS `crm_rfq_quote` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `rfq_id` BIGINT NOT NULL,
  `rfq_vendor_id` BIGINT NOT NULL,
  `vendor_id` BIGINT NOT NULL,
  `unit_price` DECIMAL(14,4) NOT NULL COMMENT '单价 · P1 修补 2 必填',
  `total_amount` DECIMAL(14,2) NOT NULL COMMENT '总报价',
  `lead_time_days` INT DEFAULT NULL COMMENT '交货周期（天）',
  `valid_until` DATE DEFAULT NULL COMMENT '报价有效期',
  `payment_terms` VARCHAR(255) DEFAULT NULL COMMENT '付款条件',
  `quality_score` DECIMAL(3,2) DEFAULT NULL COMMENT '质量评分 0-5（P1 修补 4 加权）',
  `delivery_score` DECIMAL(3,2) DEFAULT NULL COMMENT '交付评分 0-5',
  `service_score` DECIMAL(3,2) DEFAULT NULL COMMENT '服务评分 0-5',
  `weighted_score` DECIMAL(5,2) DEFAULT NULL COMMENT '加权总分（price 50% + quality 20% + delivery 20% + service 10%）',
  `is_awarded` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否中标',
  `remark` VARCHAR(500) DEFAULT NULL,
  `submitted_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `submitted_by` BIGINT DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_rfq_quote_vendor` (`rfq_id`, `vendor_id`),
  KEY `idx_rfq_quote_rfq` (`rfq_id`),
  KEY `idx_rfq_quote_awarded` (`is_awarded`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='厂商报价（每厂商 1 报价）';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
