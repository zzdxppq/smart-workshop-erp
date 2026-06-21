-- V1.3.7 · Story 1.30 · 品质·CMM 三次元 (FR-7-3)
-- 迁移：crm_quality_cmm CMM 测量单 + crm_quality_cmm_point 测点数据
-- 3 P1 修补：CMM 测点 ≥ 3 / 偏差超差告警 / 报告 PDF 必存
-- 模板：QC{yyyyMMdd}{seq:4}

USE `cnc_business`;

-- CMM 三次元测量单
CREATE TABLE IF NOT EXISTS `crm_quality_cmm` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `cmm_no` VARCHAR(32) NOT NULL COMMENT 'QC{yyyyMMdd}{seq:4}',
  `work_order_id` BIGINT DEFAULT NULL,
  `work_order_no` VARCHAR(32) DEFAULT NULL,
  `drawing_no` VARCHAR(32) DEFAULT NULL,
  `part_name` VARCHAR(128) DEFAULT NULL,
  `point_count` INT NOT NULL DEFAULT 0 COMMENT '测点数量 · P1 修补 1：≥ 3',
  `cpk` DECIMAL(8,4) DEFAULT NULL COMMENT 'Cpk 过程能力指数',
  `pp` DECIMAL(8,4) DEFAULT NULL,
  `ppk` DECIMAL(8,4) DEFAULT NULL,
  `cp` DECIMAL(8,4) DEFAULT NULL,
  `max_deviation` DECIMAL(8,4) DEFAULT NULL COMMENT '最大偏差 mm',
  `deviation_alert` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '超差告警 · P1 修补 2',
  `result` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PASSED/FAILED',
  `pdf_url` VARCHAR(255) DEFAULT NULL COMMENT 'PDF 报告 · P1 修补 3：必存',
  `inspector_user_id` BIGINT DEFAULT NULL,
  `inspected_at` DATETIME DEFAULT NULL,
  `remark` VARCHAR(500) DEFAULT NULL,
  `created_by` BIGINT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_cmm_no` (`cmm_no`),
  KEY `idx_cmm_work_order_id` (`work_order_id`),
  KEY `idx_cmm_result` (`result`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CMM 三次元测量单（V1.3.7 Story 1.30 FR-7-3）';

-- CMM 测点
CREATE TABLE IF NOT EXISTS `crm_quality_cmm_point` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `cmm_id` BIGINT NOT NULL,
  `point_no` VARCHAR(16) NOT NULL COMMENT 'P1/P2/P3 ...',
  `axis` VARCHAR(8) NOT NULL DEFAULT 'X' COMMENT 'X/Y/Z',
  `nominal_value` DECIMAL(10,4) NOT NULL COMMENT '标称值',
  `measured_value` DECIMAL(10,4) NOT NULL COMMENT '实测值',
  `tolerance_upper` DECIMAL(10,4) DEFAULT NULL COMMENT '上偏差',
  `tolerance_lower` DECIMAL(10,4) DEFAULT NULL COMMENT '下偏差',
  `deviation` DECIMAL(10,4) DEFAULT NULL COMMENT '偏差 = 实测 - 标称',
  `passed` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_cmm_point_cmm_id` (`cmm_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CMM 测点（P1 修补 1 ≥ 3）';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
