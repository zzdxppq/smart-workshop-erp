-- V1.3.7 · Story 1.40 · 财务·料号成本聚合视图 (FR-9-5 V1.3.4 新增 · P0)
-- 迁移：crm_material_cost_aggregation 物料 × 5 段成本自动聚合视图
-- 4 P1 修补：5 段严格 V1.3.4 标准 / 物料编码唯一 / 趋势 12 月 / 厂商对比
-- 模板：MC{yyyyMMdd}{seq:4}

USE `cnc_business`;

-- 物料成本聚合视图（按物料编码 × 月份维度聚合 5 段成本）
CREATE TABLE IF NOT EXISTS `crm_material_cost_aggregation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `agg_no` VARCHAR(32) NOT NULL COMMENT 'MC{yyyyMMdd}{seq:4}',
  `material_id` BIGINT NOT NULL,
  `material_code` VARCHAR(64) NOT NULL COMMENT 'P1 修补 2：物料编码唯一',
  `material_name` VARCHAR(255) NOT NULL,
  `agg_month` VARCHAR(7) NOT NULL COMMENT 'yyyy-MM',
  `vendor_id` BIGINT DEFAULT NULL,
  `vendor_name` VARCHAR(128) DEFAULT NULL,
  `qty` DECIMAL(14,2) NOT NULL DEFAULT 0,
  `material_amount` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT 'P1 修补 1：5 段严格 V1.3.4 标准 · 原材料',
  `process_amount` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '加工',
  `outsource_amount` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '委外',
  `manage_amount` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '管理',
  `depreciation_amount` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '折旧',
  `total_cost` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '5 段总成本',
  `unit_cost` DECIMAL(14,4) NOT NULL DEFAULT 0,
  `cost_sources` VARCHAR(255) DEFAULT NULL COMMENT '来源 BOM 1.9 + 工艺 1.10 + 工单 1.15 + 委外 1.18/1.26 + 库存 1.14',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_material_month_vendor` (`material_code`, `agg_month`, `vendor_id`),
  KEY `idx_material_code` (`material_code`),
  KEY `idx_agg_month` (`agg_month`),
  KEY `idx_vendor` (`vendor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='料号成本聚合视图（V1.3.7 Story 1.40 FR-9-5 V1.3.4 强化）';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
