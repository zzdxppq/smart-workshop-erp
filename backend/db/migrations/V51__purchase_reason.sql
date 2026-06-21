-- ============================================================
-- V1.3.8 Story 4.1 · V51__purchase_reason.sql
-- 无订单采购模式：sys_dict PURCHASE_REASON（PO 字段已由 V49 建表包含）
-- ============================================================

USE `cnc_platform`;

INSERT IGNORE INTO sys_dict (dict_type, dict_code, dict_label, sort, status) VALUES
  ('PURCHASE_REASON', 'URGENT_REPLENISH', '紧急补料', 1, 'ACTIVE'),
  ('PURCHASE_REASON', 'CUSTOMER_ADD',     '客户加单', 2, 'ACTIVE'),
  ('PURCHASE_REASON', 'STOCK_SWAP',       '库存置换', 3, 'ACTIVE'),
  ('PURCHASE_REASON', 'OTHER',            '其他',     4, 'ACTIVE');

USE `cnc_business`;

-- V49 已含 source_type / purchase_reason · 此处仅幂等补列（旧库升级路径）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_purchase_order' AND COLUMN_NAME = 'source_type');
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE crm_purchase_order ADD COLUMN source_type VARCHAR(20) NOT NULL DEFAULT ''FROM_ORDER'' COMMENT ''FROM_ORDER/FROM_MRP/NO_ORDER'', ADD COLUMN purchase_reason VARCHAR(30) NULL COMMENT ''采购理由'', ADD KEY idx_source_type (source_type), ADD KEY idx_purchase_reason (purchase_reason)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
