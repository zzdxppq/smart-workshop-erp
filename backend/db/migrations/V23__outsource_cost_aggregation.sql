-- V1.3.7 · Story 1.26 · 工序/整单委外成本归集 (FR-6-6)
-- 迁移：crm_outsource_cost_aggregation 委外成本归集（5 段成本自动聚合）
-- 3 P1 修补：5 段成本自动聚合 / 成本非负 / 偏差率统计
-- 5 段：MATERIAL/LABOR/MACHINE/OVERHEAD/OUTSOURCE

USE `cnc_business`;

-- 委外成本归集（按委外单 × 物料 × 工序）
CREATE TABLE IF NOT EXISTS `crm_outsource_cost_aggregation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `outsource_id` BIGINT NOT NULL COMMENT '关联委外单主键 ID',
  `outsource_no` VARCHAR(32) NOT NULL,
  `material_code` VARCHAR(32) NOT NULL,
  `process_name` VARCHAR(64) DEFAULT NULL,
  `cost_material` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '材料成本（5 段 1）',
  `cost_labor` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '人工成本（5 段 2）',
  `cost_machine` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '设备成本（5 段 3）',
  `cost_overhead` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '管理成本（5 段 4）',
  `cost_outsource` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '委外成本（5 段 5）',
  `cost_total` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '总成本 = 5 段累加',
  `budget_cost` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '预算成本（用于偏差率）',
  `deviation_pct` DECIMAL(5,2) DEFAULT NULL COMMENT '偏差率 %',
  `deviation_level` VARCHAR(16) DEFAULT NULL COMMENT 'WITHIN/WARN/OVER',
  `aggregation_scope` VARCHAR(16) NOT NULL DEFAULT 'STEP' COMMENT 'STEP/PROCESS/WHOLE',
  `remark` VARCHAR(500) DEFAULT NULL,
  `created_by` BIGINT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_outsource_material_process` (`outsource_id`, `material_code`, `process_name`, `aggregation_scope`),
  KEY `idx_cost_agg_outsource_id` (`outsource_id`),
  KEY `idx_cost_agg_material` (`material_code`),
  KEY `idx_cost_agg_scope` (`aggregation_scope`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='委外成本归集（V1.3.7 Story 1.26 FR-6-6）';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
