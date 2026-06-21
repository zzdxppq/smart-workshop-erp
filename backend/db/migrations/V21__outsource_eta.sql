-- V1.3.7 · Story 1.24 · 委外历史交期预估 (FR-6-4)
-- 迁移：crm_outsource_eta 预估交期 + crm_outsource_actual 实际交期
-- 3 P1 修补：偏差超 20% 自动告警 / 预估准确率 ≥ 80% / 预估必填
-- 模板：OE{yyyyMMdd}{seq:4}

USE `cnc_business`;

-- 预估交期（基于历史数据预测）
CREATE TABLE IF NOT EXISTS `crm_outsource_eta` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `eta_no` VARCHAR(32) NOT NULL COMMENT 'OE{yyyyMMdd}{seq:4}',
  `outsource_id` BIGINT NOT NULL COMMENT '关联委外单主键 ID',
  `outsource_no` VARCHAR(32) NOT NULL COMMENT '冗余委外单号',
  `supplier_id` BIGINT NOT NULL COMMENT '供应商 ID',
  `supplier_name` VARCHAR(128) DEFAULT NULL,
  `process_name` VARCHAR(64) DEFAULT NULL,
  `qty` INT NOT NULL DEFAULT 1 COMMENT '数量',
  `predicted_days` INT NOT NULL COMMENT '预估交期（天）',
  `predicted_delivery_date` DATE NOT NULL COMMENT '预估交付日期',
  `confidence` DECIMAL(5,2) NOT NULL DEFAULT 0.80 COMMENT '预估置信度 0~1（≥ 0.80 通过）',
  `base_samples` INT NOT NULL DEFAULT 0 COMMENT '基于历史样本数',
  `actual_delivery_date` DATE DEFAULT NULL COMMENT '实际交付日期（actual 表关联）',
  `deviation_pct` DECIMAL(5,2) DEFAULT NULL COMMENT '偏差率 %（actual - predicted）/ predicted × 100',
  `accuracy_passed` TINYINT(1) DEFAULT NULL COMMENT '是否通过 ≥ 80%',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PREDICTED' COMMENT 'PREDICTED/IN_PROGRESS/COMPLETED/OVERDUE/ALERTED',
  `created_by` BIGINT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_eta_no` (`eta_no`),
  KEY `idx_eta_outsource_id` (`outsource_id`),
  KEY `idx_eta_outsource_no` (`outsource_no`),
  KEY `idx_eta_supplier_id` (`supplier_id`),
  KEY `idx_eta_status` (`status`),
  KEY `idx_eta_predicted_date` (`predicted_delivery_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='委外预估交期（V1.3.7 Story 1.24 FR-6-4）';

-- 实际交期（仅记录历史完成实绩）
CREATE TABLE IF NOT EXISTS `crm_outsource_actual` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `eta_id` BIGINT DEFAULT NULL COMMENT '关联预估 ID',
  `outsource_id` BIGINT NOT NULL,
  `outsource_no` VARCHAR(32) NOT NULL,
  `supplier_id` BIGINT NOT NULL,
  `supplier_name` VARCHAR(128) DEFAULT NULL,
  `process_name` VARCHAR(64) DEFAULT NULL,
  `qty` INT NOT NULL DEFAULT 1,
  `promised_date` DATE NOT NULL COMMENT '承诺交期',
  `actual_date` DATE NOT NULL COMMENT '实际完成日',
  `actual_days` INT NOT NULL COMMENT '实际天数',
  `predicted_days` INT DEFAULT NULL COMMENT '预估天数（冗余）',
  `deviation_pct` DECIMAL(5,2) DEFAULT NULL COMMENT '偏差率',
  `on_time` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否准时 0/1',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_actual_outsource_id` (`outsource_id`),
  KEY `idx_actual_supplier_id` (`supplier_id`),
  KEY `idx_actual_actual_date` (`actual_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='委外实际交期历史（V1.3.7 Story 1.24 FR-6-4）';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
