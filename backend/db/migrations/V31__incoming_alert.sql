-- V1.3.7 · Story 1.34 · 采购·到货提醒 (FR-8-3)
-- 迁移：crm_incoming_alert 到货提醒 + crm_incoming 实际到货
-- 3 P1 修补：预估到货日必填 / 提前 3 天 ALERT / 逾期 ALERT_CRITICAL / 唯一索引 (po_id, material_id)
-- 模板：IA{yyyyMMdd}{seq:4}

USE `cnc_business`;

-- 到货提醒（PO 创建时生成）
CREATE TABLE IF NOT EXISTS `crm_incoming_alert` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `alert_no` VARCHAR(32) NOT NULL COMMENT 'IA{yyyyMMdd}{seq:4}',
  `po_id` BIGINT NOT NULL,
  `po_no` VARCHAR(32) NOT NULL,
  `vendor_id` BIGINT DEFAULT NULL,
  `vendor_name` VARCHAR(128) DEFAULT NULL,
  `material_id` BIGINT NOT NULL,
  `material_code` VARCHAR(64) DEFAULT NULL,
  `material_name` VARCHAR(255) DEFAULT NULL,
  `qty` DECIMAL(12,2) NOT NULL,
  `unit` VARCHAR(16) DEFAULT NULL,
  `expected_date` DATE NOT NULL COMMENT '预估到货日 · P1 修补 1 必填',
  `alert_level` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/ALERT（提前 3 天）/ALERT_CRITICAL（逾期）/ARRIVED',
  `alert_message` VARCHAR(500) DEFAULT NULL,
  `arrived_qty` DECIMAL(12,2) DEFAULT NULL,
  `arrived_at` DATETIME DEFAULT NULL,
  `arrived_by` BIGINT DEFAULT NULL,
  `reminded_at` DATETIME DEFAULT NULL,
  `reminded_count` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_alert_po_material` (`po_id`, `material_id`),
  KEY `idx_alert_po` (`po_id`),
  KEY `idx_alert_level` (`alert_level`),
  KEY `idx_alert_expected_date` (`expected_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='到货提醒（V1.3.7 Story 1.34 FR-8-3）';

-- 实际到货（扫码入库记录）
CREATE TABLE IF NOT EXISTS `crm_incoming` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `incoming_no` VARCHAR(32) NOT NULL COMMENT 'IN{yyyyMMdd}{seq:4}',
  `alert_id` BIGINT NOT NULL,
  `po_id` BIGINT NOT NULL,
  `po_no` VARCHAR(32) NOT NULL,
  `vendor_id` BIGINT DEFAULT NULL,
  `vendor_name` VARCHAR(128) DEFAULT NULL,
  `material_id` BIGINT NOT NULL,
  `material_code` VARCHAR(64) DEFAULT NULL,
  `material_name` VARCHAR(255) DEFAULT NULL,
  `arrived_qty` DECIMAL(12,2) NOT NULL,
  `expected_qty` DECIMAL(12,2) NOT NULL,
  `unit` VARCHAR(16) DEFAULT NULL,
  `arrived_at` DATETIME NOT NULL,
  `arrived_by` BIGINT NOT NULL,
  `quality_status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PASS/FAIL',
  `scan_batch_no` VARCHAR(32) DEFAULT NULL COMMENT '关联 1.12 扫码批次号',
  `remark` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_incoming_alert` (`alert_id`),
  KEY `idx_incoming_po` (`po_id`),
  KEY `idx_incoming_material` (`material_id`),
  KEY `idx_incoming_arrived_at` (`arrived_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实际到货（V1.3.7 Story 1.34 FR-8-3）';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
