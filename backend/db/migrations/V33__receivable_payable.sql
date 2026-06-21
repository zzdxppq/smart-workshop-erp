-- V1.3.7 · Story 1.36 · 财务·应收应付 (FR-9-1)
-- 迁移：crm_receivable 应收 + crm_payable 应付 + crm_payment 收付款记录
-- 4 P1 修补：应收/应付金额非负 / 收付款金额 ≤ 未收/未付金额 / 账龄 4 段（30/60/90/90+）/ 跨订单/PO 关联
-- 模板：RV{yyyyMMdd}{seq:4}（应收）/ PV{yyyyMMdd}{seq:4}（应付）/ PM{yyyyMMdd}{seq:4}（收付款记录）

USE `cnc_business`;

-- 应收账款（客户欠款）
CREATE TABLE IF NOT EXISTS `crm_receivable` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `receivable_no` VARCHAR(32) NOT NULL COMMENT 'RV{yyyyMMdd}{seq:4}',
  `customer_id` BIGINT NOT NULL,
  `customer_name` VARCHAR(128) DEFAULT NULL,
  `order_id` BIGINT NOT NULL COMMENT '关联 1.6 销售订单',
  `order_no` VARCHAR(32) NOT NULL,
  `total_amount` DECIMAL(14,2) NOT NULL COMMENT '订单金额 · P1 修补 1 非负',
  `paid_amount` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '已收金额',
  `unpaid_amount` DECIMAL(14,2) NOT NULL COMMENT '未收金额',
  `due_date` DATE NOT NULL COMMENT '到期日',
  `aging_days` INT NOT NULL DEFAULT 0 COMMENT '账龄天数',
  `aging_bucket` VARCHAR(16) NOT NULL DEFAULT 'CURRENT' COMMENT '账龄段 CURRENT(0-30)/D30(30-60)/D60(60-90)/D90(90+) · P1 修补 3',
  `status` VARCHAR(16) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/PARTIAL/CLOSED/OVERDUE',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_receivable_order` (`order_id`),
  KEY `idx_receivable_customer` (`customer_id`),
  KEY `idx_receivable_status` (`status`),
  KEY `idx_receivable_due_date` (`due_date`),
  KEY `idx_receivable_aging_bucket` (`aging_bucket`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应收账款（V1.3.7 Story 1.36 FR-9-1）';

-- 应付账款（欠供应商）
CREATE TABLE IF NOT EXISTS `crm_payable` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `payable_no` VARCHAR(32) NOT NULL COMMENT 'PV{yyyyMMdd}{seq:4}',
  `vendor_id` BIGINT NOT NULL,
  `vendor_name` VARCHAR(128) DEFAULT NULL,
  `po_id` BIGINT NOT NULL COMMENT '关联 1.32 采购订单',
  `po_no` VARCHAR(32) NOT NULL,
  `total_amount` DECIMAL(14,2) NOT NULL COMMENT 'PO 金额 · P1 修补 1 非负',
  `paid_amount` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '已付金额',
  `unpaid_amount` DECIMAL(14,2) NOT NULL COMMENT '未付金额',
  `due_date` DATE NOT NULL COMMENT '到期日',
  `aging_days` INT NOT NULL DEFAULT 0,
  `aging_bucket` VARCHAR(16) NOT NULL DEFAULT 'CURRENT' COMMENT '账龄段',
  `status` VARCHAR(16) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/PARTIAL/CLOSED/OVERDUE',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_payable_po` (`po_id`),
  KEY `idx_payable_vendor` (`vendor_id`),
  KEY `idx_payable_status` (`status`),
  KEY `idx_payable_due_date` (`due_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应付账款（V1.3.7 Story 1.36 FR-9-1）';

-- 收付款记录
CREATE TABLE IF NOT EXISTS `crm_payment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `payment_no` VARCHAR(32) NOT NULL COMMENT 'PM{yyyyMMdd}{seq:4}',
  `type` VARCHAR(16) NOT NULL COMMENT 'RECEIPT(收客户)/PAYMENT(付供应商)',
  `ref_id` BIGINT NOT NULL COMMENT 'crm_receivable.id 或 crm_payable.id',
  `ref_no` VARCHAR(32) NOT NULL COMMENT 'RV/PV 单号',
  `amount` DECIMAL(14,2) NOT NULL COMMENT '本次收/付金额 · P1 修补 2 ≤ 未收/未付',
  `method` VARCHAR(16) NOT NULL DEFAULT 'BANK' COMMENT 'BANK/CASH/CHECK/WECHAT/ALIPAY',
  `paid_by` BIGINT NOT NULL,
  `paid_at` DATETIME NOT NULL,
  `remark` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_payment_no` (`payment_no`),
  KEY `idx_payment_type` (`type`),
  KEY `idx_payment_ref` (`ref_id`),
  KEY `idx_payment_paid_at` (`paid_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收付款记录（V1.3.7 Story 1.36 FR-9-1）';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
