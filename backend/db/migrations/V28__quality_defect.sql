-- V1.3.7 · Story 1.31 · 品质·不良品处理 (FR-7-4)
-- 迁移：crm_quality_defect 不良品单 + crm_quality_defect_history 处理历史 + crm_quality_defect_action 处理动作
-- 3 P1 修补：3 动作（返工/报废/让步接收）/ 责任部门必填 / 成本非负
-- 模板：QD{yyyyMMdd}{seq:4}

USE `cnc_business`;

-- 不良品单（8D 报告）
CREATE TABLE IF NOT EXISTS `crm_quality_defect` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `defect_no` VARCHAR(32) NOT NULL COMMENT 'QD{yyyyMMdd}{seq:4}',
  `source_type` VARCHAR(16) NOT NULL COMMENT 'INTERNAL（厂内）/OUTSOURCE（委外）',
  `source_id` BIGINT DEFAULT NULL COMMENT '来源单 ID（IQC/IPQC/OQC/FA/CMM 单 ID）',
  `source_no` VARCHAR(32) DEFAULT NULL,
  `defect_type` VARCHAR(64) NOT NULL,
  `severity` VARCHAR(16) NOT NULL DEFAULT 'MAJOR' COMMENT 'MINOR/MAJOR/CRITICAL',
  `qty` INT NOT NULL DEFAULT 1,
  `material_id` BIGINT DEFAULT NULL,
  `material_code` VARCHAR(64) DEFAULT NULL,
  `work_order_id` BIGINT DEFAULT NULL,
  `work_order_no` VARCHAR(32) DEFAULT NULL,
  `d1_team` VARCHAR(255) DEFAULT NULL COMMENT '8D D1 团队组建',
  `d4_root_cause` TEXT DEFAULT NULL COMMENT '8D D4 根本原因',
  `d5_action` TEXT DEFAULT NULL COMMENT '8D D5 永久对策',
  `d8_closure` TEXT DEFAULT NULL COMMENT '8D D8 关闭',
  `defect_rate_ppm` DECIMAL(10,2) DEFAULT NULL COMMENT 'PPM 不良率',
  `total_qty` INT DEFAULT NULL COMMENT '总生产数量',
  `status` VARCHAR(20) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/IN_PROGRESS/RESOLVED/CLOSED',
  `result` VARCHAR(20) DEFAULT NULL COMMENT 'REWORK/SCRAP/CONCESSION/RETURN',
  `responsible_dept` VARCHAR(64) DEFAULT NULL COMMENT '责任部门 · P1 修补 2 必填',
  `cost_amount` DECIMAL(12,2) DEFAULT NULL COMMENT '成本 · P1 修补 3 非负',
  `created_by` BIGINT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_defect_no` (`defect_no`),
  KEY `idx_defect_source` (`source_type`, `source_id`),
  KEY `idx_defect_status` (`status`),
  KEY `idx_defect_result` (`result`),
  KEY `idx_defect_severity` (`severity`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='不良品单（V1.3.7 Story 1.31 FR-7-4 · 8D 报告）';

-- 不良处理历史（时序）
CREATE TABLE IF NOT EXISTS `crm_quality_defect_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `defect_id` BIGINT NOT NULL,
  `from_status` VARCHAR(20) DEFAULT NULL,
  `to_status` VARCHAR(20) NOT NULL,
  `operator_user_id` BIGINT DEFAULT NULL,
  `operator_name` VARCHAR(64) DEFAULT NULL,
  `comment` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_defect_history_defect_id` (`defect_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='不良处理历史';

-- 不良处理动作（3 选 1）
CREATE TABLE IF NOT EXISTS `crm_quality_defect_action` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `defect_id` BIGINT NOT NULL,
  `action_type` VARCHAR(16) NOT NULL COMMENT 'REWORK（返工）/SCRAP（报废）/CONCESSION（让步接收） · P1 修补 1',
  `qty` INT NOT NULL DEFAULT 1,
  `responsible_dept` VARCHAR(64) DEFAULT NULL COMMENT '责任部门 · P1 修补 2 必填',
  `cost_amount` DECIMAL(12,2) DEFAULT NULL COMMENT '成本 · P1 修补 3 非负',
  `executed_at` DATETIME DEFAULT NULL,
  `executor_user_id` BIGINT DEFAULT NULL,
  `remark` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_defect_action_defect_id` (`defect_id`),
  KEY `idx_defect_action_type` (`action_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='不良处理动作（3 选 1）';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
