-- V1.3.9 · 工单关联销售订单（AC-5.1.1 订单转工单）
USE `cnc_business`;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'cnc_business' AND TABLE_NAME = 'crm_workorder' AND COLUMN_NAME = 'sales_order_id');
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE crm_workorder ADD COLUMN sales_order_id BIGINT NULL COMMENT ''销售订单ID'' AFTER remark, ADD COLUMN sales_order_no VARCHAR(32) NULL COMMENT ''销售订单号 XS'' AFTER sales_order_id, ADD INDEX idx_workorder_sales_order (sales_order_id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 同步至 cnc_production（V60 模式）
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_workorder` LIKE `cnc_business`.`crm_workorder`;

SET @col_prod = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'cnc_production' AND TABLE_NAME = 'crm_workorder' AND COLUMN_NAME = 'sales_order_id');
SET @sql_prod = IF(@col_prod = 0,
  'ALTER TABLE cnc_production.crm_workorder ADD COLUMN sales_order_id BIGINT NULL COMMENT ''销售订单ID'' AFTER remark, ADD COLUMN sales_order_no VARCHAR(32) NULL COMMENT ''销售订单号 XS'' AFTER sales_order_id, ADD INDEX idx_workorder_sales_order (sales_order_id)',
  'SELECT 1');
PREPARE stmt2 FROM @sql_prod; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;
