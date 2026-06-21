-- V1.3.7 · Story 1.21 · 月度对账 (FR-6-1)
-- 迁移：crm_reconcile 月度对账单 + crm_reconcile_item 对账明细 + crm_reconcile_signature 厂商签字扫描件
-- V1.3.7 AD-2 红线：不含"线下"动作

USE `cnc_business`;

-- 月度对账单
CREATE TABLE IF NOT EXISTS `crm_reconcile` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `reconcile_no` VARCHAR(30) NOT NULL COMMENT 'RC{yyyyMM}{seq:4}',
  `vendor_id` BIGINT NOT NULL,
  `vendor_name` VARCHAR(200) NOT NULL,
  `period_year` INT NOT NULL,
  `period_month` INT NOT NULL,
  `total_amount` DECIMAL(15,2) NOT NULL DEFAULT 0.00,
  `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/VENDOR_CONFIRMED/BOTH_CONFIRMED/FINANCE_CONFIRMED/CLOSED',
  `current_step` INT DEFAULT 1 COMMENT '1-4 步',
  `is_locked` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '对账期锁定',
  `created_by` BIGINT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_reconcile_no` (`reconcile_no`),
  KEY `idx_vendor_id` (`vendor_id`),
  KEY `idx_period` (`period_year`, `period_month`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='月度对账单 (V1.3.7 AD-2 不含"线下")';

-- 对账明细
CREATE TABLE IF NOT EXISTS `crm_reconcile_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `reconcile_id` BIGINT NOT NULL,
  `outsource_order_id` BIGINT NOT NULL,
  `outsource_order_no` VARCHAR(30) NOT NULL,
  `item_name` VARCHAR(200) NOT NULL,
  `quantity` INT NOT NULL,
  `unit_price` DECIMAL(15,2) NOT NULL,
  `amount` DECIMAL(15,2) NOT NULL,
  `vendor_amount` DECIMAL(15,2) DEFAULT NULL COMMENT '厂商确认金额',
  `final_amount` DECIMAL(15,2) DEFAULT NULL COMMENT '最终对账金额',
  `is_consistent` TINYINT(1) DEFAULT NULL COMMENT '金额是否一致',
  `sort` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_reconcile_id` (`reconcile_id`),
  CONSTRAINT `fk_reconcile_item_reconcile` FOREIGN KEY (`reconcile_id`) REFERENCES `crm_reconcile` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对账明细';

-- 厂商签字扫描件 (AES-256-GCM 加密)
CREATE TABLE IF NOT EXISTS `crm_reconcile_signature` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `reconcile_id` BIGINT NOT NULL,
  `signer_user_id` BIGINT NOT NULL,
  `signer_name` VARCHAR(100) NOT NULL,
  `signature_image_path` VARCHAR(500) NOT NULL,
  `encrypted_data` TEXT NOT NULL COMMENT 'AES-256-GCM 加密后的扫描件',
  `iv` VARCHAR(64) NOT NULL COMMENT '12 字节 IV 唯一',
  `auth_tag` VARCHAR(64) NOT NULL COMMENT '128-bit GCM tag',
  `signed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_reconcile_id_sig` (`reconcile_id`),
  CONSTRAINT `fk_reconcile_sig_reconcile` FOREIGN KEY (`reconcile_id`) REFERENCES `crm_reconcile` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='厂商签字扫描件 (V1.3.6 加密)';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
