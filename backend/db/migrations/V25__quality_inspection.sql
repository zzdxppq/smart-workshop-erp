-- V1.3.7 · Story 1.28 · 品质·来料/过程/成品检 (FR-7-1)
-- 迁移：crm_quality_inspection 3 检单 + crm_quality_inspection_item 检验项目 + crm_quality_sample AQL 抽样
-- 3 P1 修补：抽样规则 AQL / 检验项目必填 / 严重度 4 级（INFO/WARN/ERROR/CRITICAL）
-- 模板：QI{yyyyMMdd}{seq:4}

USE `cnc_business`;

-- 来料/过程/成品检单（IQC/IPQC/OQC）
CREATE TABLE IF NOT EXISTS `crm_quality_inspection` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `inspection_no` VARCHAR(32) NOT NULL COMMENT 'QI{yyyyMMdd}{seq:4}',
  `inspect_type` VARCHAR(16) NOT NULL COMMENT 'IQC（来料）/IPQC（过程）/OQC（成品）',
  `material_id` BIGINT DEFAULT NULL COMMENT '物料 ID（IQC 必填）',
  `material_code` VARCHAR(64) DEFAULT NULL,
  `material_name` VARCHAR(128) DEFAULT NULL,
  `work_order_id` BIGINT DEFAULT NULL COMMENT '工单 ID（IPQC 必填）',
  `work_order_no` VARCHAR(32) DEFAULT NULL,
  `process_name` VARCHAR(64) DEFAULT NULL COMMENT '工序名称（IPQC 必填）',
  `batch_no` VARCHAR(32) DEFAULT NULL,
  `lot_size` INT NOT NULL DEFAULT 0 COMMENT '批量',
  `sample_size` INT NOT NULL DEFAULT 0 COMMENT '抽样量',
  `sample_rule` VARCHAR(64) DEFAULT 'AQL-1.0' COMMENT '抽样规则 AQL',
  `aql_level` VARCHAR(16) DEFAULT '1.0' COMMENT 'AQL 等级 0.65/1.0/1.5/2.5/4.0',
  `inspect_qty` INT NOT NULL DEFAULT 0,
  `passed_qty` INT NOT NULL DEFAULT 0,
  `failed_qty` INT NOT NULL DEFAULT 0,
  `defect_rate` DECIMAL(5,2) DEFAULT NULL,
  `result` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PASSED/FAILED/CONDITIONAL',
  `inspector_user_id` BIGINT DEFAULT NULL,
  `inspected_at` DATETIME DEFAULT NULL,
  `max_severity` VARCHAR(16) DEFAULT NULL COMMENT '最高严重度 INFO/WARN/ERROR/CRITICAL',
  `trigger_rework` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '触发返修（IQC 不通过）',
  `trigger_stockin` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '触发入库（OQC 通过）',
  `remark` VARCHAR(500) DEFAULT NULL,
  `created_by` BIGINT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_inspection_no` (`inspection_no`),
  KEY `idx_inspection_type` (`inspect_type`),
  KEY `idx_inspection_material` (`material_id`),
  KEY `idx_inspection_workorder` (`work_order_id`),
  KEY `idx_inspection_result` (`result`),
  KEY `idx_inspection_severity` (`max_severity`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='来料/过程/成品检单（V1.3.7 Story 1.28 FR-7-1）';

-- 检验项目（必填）
CREATE TABLE IF NOT EXISTS `crm_quality_inspection_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `inspection_id` BIGINT NOT NULL,
  `item_name` VARCHAR(128) NOT NULL COMMENT '检验项目名称（必填）',
  `standard` VARCHAR(500) DEFAULT NULL COMMENT '判定标准',
  `measured_value` VARCHAR(128) DEFAULT NULL,
  `severity` VARCHAR(16) NOT NULL DEFAULT 'INFO' COMMENT 'INFO/WARN/ERROR/CRITICAL',
  `passed` TINYINT(1) NOT NULL DEFAULT 0,
  `defect_desc` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_inspection_item_inspection_id` (`inspection_id`),
  KEY `idx_inspection_item_severity` (`severity`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='来料/过程/成品检项目（V1.3.7 P1 修补 2 · 必填）';

-- AQL 抽样记录
CREATE TABLE IF NOT EXISTS `crm_quality_sample` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `inspection_id` BIGINT NOT NULL,
  `sample_no` VARCHAR(32) NOT NULL COMMENT '样本编号 S{seq:3}',
  `item_id` BIGINT DEFAULT NULL,
  `sample_qty` INT NOT NULL DEFAULT 1,
  `defect_qty` INT NOT NULL DEFAULT 0,
  `aql_passed` TINYINT(1) NOT NULL DEFAULT 0,
  `remark` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_sample_no` (`sample_no`),
  KEY `idx_sample_inspection_id` (`inspection_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AQL 抽样记录（V1.3.7 P1 修补 1）';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
