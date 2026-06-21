-- V109 · sys_workflow 增加 name 字段（V1.3.9 P0）
-- 背景：Workflows.vue 用 name/code 两列，但 sys_workflow 只有 workflow_code 无 name
USE `cnc_platform`;

SET @col = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_workflow'
    AND COLUMN_NAME = 'name'
);
SET @sql = IF(
  @col = 0,
  'ALTER TABLE `sys_workflow` ADD COLUMN `name` VARCHAR(100) DEFAULT NULL COMMENT ''工作流名称'' AFTER `id`',
  'SELECT ''skip'' AS note'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 补充 name 字段数据（基于 workflow_code 语义补充友好名称）
UPDATE `sys_workflow` SET `name` = '报价审批工作流' WHERE `workflow_code` = 'QUOTE_APPROVAL' AND (`name` IS NULL OR `name` = '');
UPDATE `sys_workflow` SET `name` = '销售订单审批工作流' WHERE `workflow_code` = 'SALES_ORDER_APPROVAL' AND (`name` IS NULL OR `name` = '');
UPDATE `sys_workflow` SET `name` = '采购订单审批工作流' WHERE `workflow_code` = 'PO_APPROVAL' AND (`name` IS NULL OR `name` = '');
UPDATE `sys_workflow` SET `name` = '委外订单审批工作流' WHERE `workflow_code` = 'OUTSOURCE_APPROVAL' AND (`name` IS NULL OR `name` = '');
