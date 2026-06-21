-- V108 · 客户图号审批状态：PENDING（待审批）/ APPROVED（已审批，可建单）
USE `cnc_business`;

SET @col = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'crm_drawing'
    AND COLUMN_NAME = 'quote_approval_status'
);
SET @sql = IF(
  @col = 0,
  'ALTER TABLE `crm_drawing` ADD COLUMN `quote_approval_status` VARCHAR(16) DEFAULT NULL COMMENT ''PENDING（待审批）/ APPROVED（已审批，可建单）'' AFTER `is_new`',
  'SELECT ''skip crm_drawing.quote_approval_status'' AS note'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
