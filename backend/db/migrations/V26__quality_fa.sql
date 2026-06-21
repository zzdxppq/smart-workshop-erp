-- V1.3.7 · Story 1.29 · 品质·FA 首件 (FR-7-2)
-- 迁移：crm_quality_fa FA 首件单 + crm_quality_fa_item FA 8 维度检验项目
-- 3 P1 修补：FA 必检（开工前）/ 检验项目 8 维度 / 不合格阻断生产
-- 模板：QF{yyyyMMdd}{seq:4}

USE `cnc_business`;

-- FA 首件单（开工前必检）
CREATE TABLE IF NOT EXISTS `crm_quality_fa` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `fa_no` VARCHAR(32) NOT NULL COMMENT 'QF{yyyyMMdd}{seq:4}',
  `work_order_id` BIGINT NOT NULL,
  `work_order_no` VARCHAR(32) NOT NULL,
  `process_id` BIGINT NOT NULL,
  `process_name` VARCHAR(64) NOT NULL,
  `operator_user_id` BIGINT DEFAULT NULL,
  `inspect_qty` INT NOT NULL DEFAULT 1 COMMENT '首件数量 默认 1',
  `result` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PASSED/FAILED',
  `locked` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '不合格锁定工序',
  `pdf_url` VARCHAR(255) DEFAULT NULL COMMENT '首件 PDF 报告',
  `inspector_user_id` BIGINT DEFAULT NULL,
  `inspected_at` DATETIME DEFAULT NULL,
  `remark` VARCHAR(500) DEFAULT NULL,
  `created_by` BIGINT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_fa_no` (`fa_no`),
  KEY `idx_fa_work_order_id` (`work_order_id`),
  KEY `idx_fa_process_id` (`process_id`),
  KEY `idx_fa_result` (`result`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='FA 首件单（V1.3.7 Story 1.29 FR-7-2 · 开工前必检）';

-- FA 8 维度检验项目
CREATE TABLE IF NOT EXISTS `crm_quality_fa_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `fa_id` BIGINT NOT NULL,
  `dimension` VARCHAR(32) NOT NULL COMMENT '尺寸/形位/粗糙度/硬度/材质/外观/装配/性能 8 维度',
  `item_name` VARCHAR(128) NOT NULL,
  `standard` VARCHAR(500) DEFAULT NULL,
  `measured_value` VARCHAR(128) DEFAULT NULL,
  `tolerance` VARCHAR(64) DEFAULT NULL,
  `passed` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_fa_item_fa_id` (`fa_id`),
  KEY `idx_fa_item_dimension` (`dimension`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='FA 8 维度检验项目（V1.3.7 P1 修补 2）';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
