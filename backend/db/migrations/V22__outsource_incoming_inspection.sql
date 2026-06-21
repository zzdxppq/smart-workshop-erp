-- V1.3.7 · Story 1.25 · 委外来料质检 (FR-6-5)
-- 迁移：crm_outsource_incoming_inspection 来料质检单 + crm_outsource_incoming_item 检验项目 + crm_outsource_incoming_defect 不良项
-- 3 P1 修补：单一 163 邮箱（V1.3.7 AD-3）/ 检验项目必填 / 严重度分级
-- 模板：OI{yyyyMMdd}{seq:4}

USE `cnc_business`;

-- 来料质检单
CREATE TABLE IF NOT EXISTS `crm_outsource_incoming_inspection` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `inspection_no` VARCHAR(32) NOT NULL COMMENT 'OI{yyyyMMdd}{seq:4}',
  `outsource_id` BIGINT NOT NULL COMMENT '关联委外单主键 ID',
  `outsource_no` VARCHAR(32) NOT NULL,
  `supplier_id` BIGINT NOT NULL,
  `supplier_name` VARCHAR(128) DEFAULT NULL,
  `material_code` VARCHAR(32) DEFAULT NULL,
  `inspect_qty` INT NOT NULL DEFAULT 0 COMMENT '送检数量',
  `passed_qty` INT NOT NULL DEFAULT 0 COMMENT '合格数',
  `failed_qty` INT NOT NULL DEFAULT 0 COMMENT '不合格数',
  `defect_rate` DECIMAL(5,2) DEFAULT NULL COMMENT '不良率 %',
  `result` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PASSED/FAILED/CONDITIONAL',
  `inspector_user_id` BIGINT DEFAULT NULL,
  `inspected_at` DATETIME DEFAULT NULL,
  `notify_email` VARCHAR(128) DEFAULT NULL COMMENT '通知邮箱（V1.3.7 AD-3 · 单一 163 邮箱）',
  `remark` VARCHAR(500) DEFAULT NULL,
  `created_by` BIGINT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_inspection_no` (`inspection_no`),
  KEY `idx_incoming_outsource_id` (`outsource_id`),
  KEY `idx_incoming_outsource_no` (`outsource_no`),
  KEY `idx_incoming_result` (`result`),
  KEY `idx_incoming_supplier` (`supplier_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='委外来料质检单（V1.3.7 Story 1.25 FR-6-5）';

-- 检验项目（必填 · P1 修补 2）
CREATE TABLE IF NOT EXISTS `crm_outsource_incoming_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `inspection_id` BIGINT NOT NULL,
  `item_name` VARCHAR(128) NOT NULL COMMENT '检验项目名称（必填）',
  `standard` VARCHAR(500) DEFAULT NULL COMMENT '检验标准',
  `measured_value` VARCHAR(128) DEFAULT NULL COMMENT '实测值',
  `passed` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '0/1',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_incoming_item_inspection` (`inspection_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='来料检验项目（必填）';

-- 不良项（严重度分级 · P1 修补 3）
CREATE TABLE IF NOT EXISTS `crm_outsource_incoming_defect` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `inspection_id` BIGINT NOT NULL,
  `item_id` BIGINT DEFAULT NULL,
  `defect_type` VARCHAR(64) NOT NULL COMMENT '不良类型',
  `severity` VARCHAR(16) NOT NULL DEFAULT 'MINOR' COMMENT 'MINOR/MAJOR/CRITICAL（严重度分级）',
  `qty` INT NOT NULL DEFAULT 1 COMMENT '不良数量',
  `description` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_incoming_defect_inspection` (`inspection_id`),
  KEY `idx_incoming_defect_severity` (`severity`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='来料不良项（严重度分级）';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
