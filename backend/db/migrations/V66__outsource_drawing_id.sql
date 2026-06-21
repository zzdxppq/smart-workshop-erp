-- V1.3.8 · 委外单关联加工图纸（Epic 3 · 采购下单前确认图纸）
USE `cnc_business`;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'cnc_business' AND TABLE_NAME = 'crm_outsource_order' AND COLUMN_NAME = 'drawing_id');
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE crm_outsource_order ADD COLUMN drawing_id BIGINT NULL COMMENT ''加工图纸 ID（crm_drawing.id）'' AFTER material_code, ADD INDEX idx_outsource_drawing (drawing_id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_prod = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'cnc_production' AND TABLE_NAME = 'crm_outsource_order' AND COLUMN_NAME = 'drawing_id');
SET @sql_prod = IF(@col_prod = 0,
  'ALTER TABLE cnc_production.crm_outsource_order ADD COLUMN drawing_id BIGINT NULL COMMENT ''加工图纸 ID（crm_drawing.id）'' AFTER material_code, ADD INDEX idx_outsource_drawing (drawing_id)',
  'SELECT 1');
PREPARE stmt2 FROM @sql_prod; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;
