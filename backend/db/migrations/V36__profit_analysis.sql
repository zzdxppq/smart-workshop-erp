-- V1.3.7 · Story 1.39 · 财务·利润分析 (FR-9-4)
-- 迁移：crm_profit_analysis 利润分析单
-- 4 P1 修补：利润 = 收入 - 5 段成本 / 利润率 -100% ~ +∞ / 跨订单+成本 跨模块 / PDF 1h 缓存
-- 模板：PA{yyyyMMdd}{seq:4}

USE `cnc_business`;

-- 利润分析单（按 SETTLED 订单聚合）
CREATE TABLE IF NOT EXISTS `crm_profit_analysis` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `profit_no` VARCHAR(32) NOT NULL COMMENT 'PA{yyyyMMdd}{seq:4}',
  `order_id` BIGINT NOT NULL COMMENT '关联 1.6 SETTLED 订单',
  `order_no` VARCHAR(32) NOT NULL,
  `customer_id` BIGINT NOT NULL,
  `customer_name` VARCHAR(128) NOT NULL,
  `product_id` BIGINT DEFAULT NULL,
  `product_code` VARCHAR(64) DEFAULT NULL,
  `product_name` VARCHAR(255) DEFAULT NULL,
  `revenue` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '订单收入（不含税）',
  `cost_id` BIGINT DEFAULT NULL COMMENT '关联 1.37 5 段成本核算单',
  `cost_no` VARCHAR(32) DEFAULT NULL,
  `total_cost` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '5 段总成本',
  `profit` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT 'P1 修补 1：利润 = 收入 - 5 段成本',
  `profit_rate` DECIMAL(8,4) NOT NULL DEFAULT 0 COMMENT 'P1 修补 2：利润率 -100% ~ +∞',
  `alert_level` VARCHAR(16) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/WARNING/CRITICAL',
  `settled_date` DATE NOT NULL COMMENT '订单 SETTLED 日期',
  `analysis_month` VARCHAR(7) NOT NULL COMMENT 'yyyy-MM 月份',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_profit_order` (`order_id`),
  KEY `idx_profit_customer` (`customer_id`),
  KEY `idx_profit_month` (`analysis_month`),
  KEY `idx_profit_alert` (`alert_level`),
  KEY `idx_profit_settled` (`settled_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='利润分析单（V1.3.7 Story 1.39 FR-9-4）';

-- V94 · mock 清理：利润分析 seed 已移至 init_data.sql（E11-S3 客户利润汇总）
