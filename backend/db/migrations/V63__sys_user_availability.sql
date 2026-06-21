-- V63 · 审批跳过请假：platform 本地维护用户可用性（由 erp-business HR 模块 Feign 同步）
USE `cnc_platform`;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'cnc_platform' AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'availability_status');
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE sys_user ADD COLUMN availability_status VARCHAR(16) NOT NULL DEFAULT ''ON_DUTY'' COMMENT ''ON_DUTY/ON_LEAVE/ON_TRIP/RESIGNED'' AFTER status, ADD COLUMN leave_no VARCHAR(64) NULL COMMENT ''请假单号'' AFTER availability_status',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
