-- V1.3.7 · Story 1.35 · 采购·来料质检 (FR-8-4)
-- 迁移：crm_purchase_incoming_inspection 来料质检单 + crm_purchase_incoming_item 检验项
-- 3 P1 修补：单一 163 邮箱（AD-3）/ 抽样 AQL / 不良率 > 10% 阻断入库 / 跨 1.32 PO 关联
-- 模板：PI{yyyyMMdd}{seq:4}

USE `cnc_business`;

-- 采购来料质检单
CREATE TABLE IF NOT EXISTS `crm_purchase_incoming_inspection` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `inspection_no` VARCHAR(32) NOT NULL COMMENT 'PI{yyyyMMdd}{seq:4}',
  `incoming_id` BIGINT DEFAULT NULL COMMENT '关联 1.34 实际到货',
  `po_id` BIGINT NOT NULL,
  `po_no` VARCHAR(32) NOT NULL,
  `vendor_id` BIGINT DEFAULT NULL,
  `vendor_name` VARCHAR(128) DEFAULT NULL,
  `material_id` BIGINT NOT NULL,
  `material_code` VARCHAR(64) DEFAULT NULL,
  `material_name` VARCHAR(255) DEFAULT NULL,
  `batch_no` VARCHAR(32) DEFAULT NULL COMMENT '关联 1.12 扫码批次',
  `inspector_id` BIGINT NOT NULL COMMENT '质检员 user_id',
  `inspector_name` VARCHAR(64) DEFAULT NULL,
  `sample_size` INT NOT NULL DEFAULT 0 COMMENT '抽样数',
  `sample_pass` INT NOT NULL DEFAULT 0 COMMENT '抽样合格数',
  `sample_fail` INT NOT NULL DEFAULT 0 COMMENT '抽样不合格数',
  `defect_rate` DECIMAL(5,2) DEFAULT NULL COMMENT '不良率 % · P1 修补 3 > 10% 阻断',
  `aql_level` VARCHAR(16) NOT NULL DEFAULT 'II' COMMENT 'AQL 等级 · P1 修补 2 I/II/III',
  `result` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PASS/REJECT',
  `notify_email` VARCHAR(128) NOT NULL DEFAULT 'inspect@btsheng-163.com' COMMENT 'P1 修补 1 单一 163 邮箱 AD-3',
  `inspected_at` DATETIME DEFAULT NULL,
  `remark` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_inspection_po_material` (`po_id`, `material_id`),
  KEY `idx_inspection_po` (`po_id`),
  KEY `idx_inspection_result` (`result`),
  KEY `idx_inspection_material` (`material_id`),
  KEY `idx_inspection_inspected_at` (`inspected_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购来料质检单（V1.3.7 Story 1.35 FR-8-4）';

-- 采购来料质检检验项
CREATE TABLE IF NOT EXISTS `crm_purchase_incoming_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `inspection_id` BIGINT NOT NULL,
  `seq_no` INT NOT NULL DEFAULT 1,
  `check_item` VARCHAR(128) NOT NULL COMMENT '检验项目（外观/尺寸/材质/性能等）',
  `standard` VARCHAR(500) DEFAULT NULL COMMENT '判定标准',
  `sample_qty` INT NOT NULL DEFAULT 1,
  `pass_qty` INT NOT NULL DEFAULT 0,
  `fail_qty` INT NOT NULL DEFAULT 0,
  `is_critical` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '关键项 1 票否决',
  `result` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PASS/FAIL/PENDING',
  `remark` VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_item_inspection` (`inspection_id`),
  KEY `idx_item_result` (`result`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购来料质检检验项（V1.3.7 Story 1.35 FR-8-4）';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
