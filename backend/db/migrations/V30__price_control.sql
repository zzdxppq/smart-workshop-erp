-- V1.3.7 · Story 1.33 · 采购·价格控制 (FR-8-2)
-- 迁移：crm_price_control 物料限价 + crm_price_history 历史价
-- 3 P1 修补：价格上限非负 / 偏差率 ≥ 20% ALERTED / 唯一索引 (material_id, vendor_id)
-- 模板：PL{yyyyMMdd}{seq:4}

USE `cnc_business`;

-- 物料采购限价（按物料 + 厂商维度）
CREATE TABLE IF NOT EXISTS `crm_price_control` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `control_no` VARCHAR(32) NOT NULL COMMENT 'PL{yyyyMMdd}{seq:4}',
  `material_id` BIGINT NOT NULL,
  `material_code` VARCHAR(64) DEFAULT NULL,
  `material_name` VARCHAR(255) DEFAULT NULL,
  `vendor_id` BIGINT DEFAULT NULL COMMENT 'NULL 表示通用限价；指定厂商表示该厂商专项限价',
  `vendor_name` VARCHAR(128) DEFAULT NULL,
  `price_limit` DECIMAL(14,4) NOT NULL COMMENT '采购价上限 · P1 修补 1 非负',
  `currency` VARCHAR(8) NOT NULL DEFAULT 'CNY',
  `effective_date` DATE NOT NULL COMMENT '生效日',
  `expiry_date` DATE DEFAULT NULL COMMENT '失效日',
  `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/EXPIRED/INACTIVE',
  `set_by` BIGINT NOT NULL,
  `set_by_name` VARCHAR(64) DEFAULT NULL,
  `remark` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_control_material_vendor` (`material_id`, `vendor_id`, `effective_date`),
  KEY `idx_control_material` (`material_id`),
  KEY `idx_control_status` (`status`),
  KEY `idx_control_effective` (`effective_date`, `expiry_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物料采购限价（V1.3.7 Story 1.33 FR-8-2）';

-- 历史价（过去 3 个月内的实际采购价）
CREATE TABLE IF NOT EXISTS `crm_price_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `material_id` BIGINT NOT NULL,
  `material_code` VARCHAR(64) DEFAULT NULL,
  `vendor_id` BIGINT NOT NULL,
  `vendor_name` VARCHAR(128) DEFAULT NULL,
  `unit_price` DECIMAL(14,4) NOT NULL,
  `qty` DECIMAL(12,2) DEFAULT NULL,
  `total_amount` DECIMAL(14,2) DEFAULT NULL,
  `source_type` VARCHAR(16) NOT NULL DEFAULT 'PO' COMMENT 'PO/RFQ_QUOTE/MANUAL',
  `source_no` VARCHAR(32) DEFAULT NULL,
  `purchased_at` DATE NOT NULL,
  `created_by` BIGINT DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_price_history_material` (`material_id`),
  KEY `idx_price_history_vendor` (`vendor_id`),
  KEY `idx_price_history_purchased_at` (`purchased_at`),
  KEY `idx_price_history_material_vendor` (`material_id`, `vendor_id`, `purchased_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购历史价（V1.3.7 Story 1.33 FR-8-2）';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
