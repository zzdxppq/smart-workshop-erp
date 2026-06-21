-- V1.3.7 · Story 1.37 · 财务·成本核算 (FR-9-2)
-- 迁移：crm_cost_accounting 成本核算 + crm_cost_segment 5 段成本
-- 3 P1 修补：5 段自动归集 / 成本非负 / 偏差率统计
-- 模板：CA{yyyyMMdd}{seq:4}

USE `cnc_business`;

-- 成本核算单（按订单/工单/委外单聚合）
CREATE TABLE IF NOT EXISTS `crm_cost_accounting` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `cost_no` VARCHAR(32) NOT NULL COMMENT 'CA{yyyyMMdd}{seq:4}',
  `ref_type` VARCHAR(16) NOT NULL COMMENT 'ORDER(1.6) / WORKORDER(1.15) / OUTSOURCE(1.18)',
  `ref_id` BIGINT NOT NULL,
  `ref_no` VARCHAR(32) NOT NULL,
  `material_id` BIGINT DEFAULT NULL,
  `material_code` VARCHAR(64) DEFAULT NULL,
  `material_name` VARCHAR(255) DEFAULT NULL,
  `qty` DECIMAL(12,2) NOT NULL DEFAULT 1,
  `unit_cost` DECIMAL(14,4) NOT NULL DEFAULT 0 COMMENT '单位成本',
  `total_cost` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '总成本 = sum(5 段)',
  `standard_cost` DECIMAL(14,2) DEFAULT NULL COMMENT '标准成本（参考）',
  `variance` DECIMAL(14,2) DEFAULT NULL COMMENT '偏差 = total - standard',
  `variance_rate` DECIMAL(8,4) DEFAULT NULL COMMENT '偏差率 %',
  `status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/CONFIRMED',
  `cost_date` DATE NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_cost_ref` (`ref_type`, `ref_id`),
  KEY `idx_cost_date` (`cost_date`),
  KEY `idx_cost_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成本核算单（V1.3.7 Story 1.37 FR-9-2）';

-- 5 段成本（材料/加工/委外/管理/折旧）
CREATE TABLE IF NOT EXISTS `crm_cost_segment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `cost_id` BIGINT NOT NULL,
  `segment_code` VARCHAR(16) NOT NULL COMMENT 'MATERIAL/PROCESS/OUTSOURCE/MANAGE/DEPRECIATION',
  `segment_name` VARCHAR(64) NOT NULL COMMENT '材料/加工/委外/管理/折旧',
  `amount` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '本段成本 · P1 修补 2 非负',
  `source` VARCHAR(32) DEFAULT NULL COMMENT '来源 1.9/1.10/1.17/1.26',
  `remark` VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_cost_segment` (`cost_id`, `segment_code`),
  KEY `idx_segment_code` (`segment_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='5 段成本明细（V1.3.7 Story 1.37 FR-9-2）';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
