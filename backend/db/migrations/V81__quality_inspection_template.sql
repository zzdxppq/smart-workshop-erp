-- V1.3.9 · 检验方案模板 · DRAFT/ACTIVE/ARCHIVED + 检验项
USE `cnc_business`;

CREATE TABLE IF NOT EXISTS `crm_quality_inspection_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `template_no` VARCHAR(32) NOT NULL COMMENT 'QT{yyyyMMdd}{seq:4}',
  `template_name` VARCHAR(128) NOT NULL,
  `drawing_no_pattern` VARCHAR(64) DEFAULT NULL COMMENT '适用图号/模糊，如 DWG-2025%',
  `material_code` VARCHAR(64) DEFAULT NULL COMMENT '关联料号',
  `inspection_type` VARCHAR(16) DEFAULT NULL COMMENT 'INCOMING/IN_PROCESS/OUTGOING 或空=通用',
  `sample_ratio` DECIMAL(5,2) DEFAULT NULL COMMENT '抽检比例 %',
  `status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/ACTIVE/ARCHIVED',
  `version` INT NOT NULL DEFAULT 1,
  `remark` VARCHAR(500) DEFAULT NULL,
  `published_by` BIGINT DEFAULT NULL,
  `published_at` DATETIME DEFAULT NULL,
  `archived_by` BIGINT DEFAULT NULL,
  `archived_at` DATETIME DEFAULT NULL,
  `created_by` BIGINT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_template_no` (`template_no`),
  KEY `idx_template_status` (`status`),
  KEY `idx_template_drawing` (`drawing_no_pattern`),
  KEY `idx_template_material` (`material_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检验方案模板';

CREATE TABLE IF NOT EXISTS `crm_quality_inspection_template_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `template_id` BIGINT NOT NULL,
  `sort_order` INT NOT NULL DEFAULT 0,
  `item_name` VARCHAR(128) NOT NULL,
  `standard` VARCHAR(500) DEFAULT NULL,
  `tolerance_upper` VARCHAR(64) DEFAULT NULL,
  `tolerance_lower` VARCHAR(64) DEFAULT NULL,
  `severity` VARCHAR(16) NOT NULL DEFAULT 'INFO',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_template_item_template_id` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检验方案模板项';

-- V94 · mock 清理：检验模板演示 seed 已移至 init_data.sql
