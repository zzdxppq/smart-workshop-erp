-- V1.3.7 · Story 1.27 · 委外质检 (FR-6-7)
-- 迁移：crm_outsource_quality 委外工序质检单 + crm_outsource_quality_item FA/CMM 检验项目 + crm_outsource_quality_defect 不良项
-- 3 P1 修补：检验项目必填 / 严重度分级 / 不良率 > 10% 告警
-- 模板：OQ{yyyyMMdd}{seq:4}

USE `cnc_business`;

-- 委外工序质检单（区别于 7 品质的来料/过程/成品检）
CREATE TABLE IF NOT EXISTS `crm_outsource_quality` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `quality_no` VARCHAR(32) NOT NULL COMMENT 'OQ{yyyyMMdd}{seq:4}',
  `outsource_id` BIGINT NOT NULL,
  `outsource_no` VARCHAR(32) NOT NULL,
  `process_name` VARCHAR(64) NOT NULL COMMENT '工序名称（区别于 1.25 来料）',
  `supplier_id` BIGINT NOT NULL,
  `supplier_name` VARCHAR(128) DEFAULT NULL,
  `inspect_type` VARCHAR(16) NOT NULL DEFAULT 'FA' COMMENT 'FA（首件）/CMM（三次元）',
  `inspect_qty` INT NOT NULL DEFAULT 1,
  `passed_qty` INT NOT NULL DEFAULT 0,
  `failed_qty` INT NOT NULL DEFAULT 0,
  `defect_rate` DECIMAL(5,2) DEFAULT NULL,
  `alerted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '不良率 > 10% 告警',
  `result` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PASSED/FAILED/CONDITIONAL',
  `inspector_user_id` BIGINT DEFAULT NULL,
  `inspected_at` DATETIME DEFAULT NULL,
  `remark` VARCHAR(500) DEFAULT NULL,
  `created_by` BIGINT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_quality_no` (`quality_no`),
  KEY `idx_quality_outsource_id` (`outsource_id`),
  KEY `idx_quality_process` (`process_name`),
  KEY `idx_quality_inspect_type` (`inspect_type`),
  KEY `idx_quality_result` (`result`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='委外工序质检单（V1.3.7 Story 1.27 FR-6-7）';

-- 检验项目（FA/CMM · 必填）
CREATE TABLE IF NOT EXISTS `crm_outsource_quality_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `quality_id` BIGINT NOT NULL,
  `item_type` VARCHAR(16) NOT NULL DEFAULT 'FA' COMMENT 'FA/CMM',
  `item_name` VARCHAR(128) NOT NULL,
  `standard` VARCHAR(500) DEFAULT NULL,
  `measured_value` VARCHAR(128) DEFAULT NULL,
  `tolerance` VARCHAR(64) DEFAULT NULL COMMENT 'CMM 专用：±0.05 mm',
  `passed` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_quality_item_quality_id` (`quality_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='委外检验项目（FA/CMM）';

-- 不良项（严重度分级）
CREATE TABLE IF NOT EXISTS `crm_outsource_quality_defect` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `quality_id` BIGINT NOT NULL,
  `item_id` BIGINT DEFAULT NULL,
  `defect_type` VARCHAR(64) NOT NULL,
  `severity` VARCHAR(16) NOT NULL DEFAULT 'MINOR',
  `qty` INT NOT NULL DEFAULT 1,
  `description` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_quality_defect_quality_id` (`quality_id`),
  KEY `idx_quality_defect_severity` (`severity`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='委外不良项（严重度分级）';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
