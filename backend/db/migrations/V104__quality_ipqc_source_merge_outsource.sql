-- V104 · 委外检合并至 IPQC：来源标识 + 工程菜单更名 + 清理委外检菜单
USE `cnc_platform`;

UPDATE `sys_menu` SET `menu_name` = '工程数据' WHERE `menu_code` = 'eng.data';

DELETE rp FROM `sys_role_permission` rp
INNER JOIN `sys_menu` m ON m.id = rp.menu_id
WHERE m.`menu_code` IN ('qc.outsource', 'quality.outsource-inspection');

DELETE FROM `sys_menu`
WHERE `menu_code` IN ('qc.outsource', 'quality.outsource-inspection');

USE `cnc_business`;

SET @col_inspect_source = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'crm_quality_inspection'
    AND COLUMN_NAME = 'inspect_source'
);
SET @sql_inspect_source = IF(
  @col_inspect_source = 0,
  'ALTER TABLE `crm_quality_inspection`
     ADD COLUMN `inspect_source` VARCHAR(16) NOT NULL DEFAULT ''INTERNAL''
       COMMENT ''INTERNAL（厂内）/OUTSOURCE（委外）'' AFTER `source_ref`',
  'SELECT ''skip crm_quality_inspection.inspect_source'' AS note'
);
PREPARE stmt FROM @sql_inspect_source;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_inspect_source = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'crm_quality_inspection'
    AND INDEX_NAME = 'idx_inspection_inspect_source'
);
SET @sql_idx = IF(
  @idx_inspect_source = 0,
  'CREATE INDEX `idx_inspection_inspect_source` ON `crm_quality_inspection` (`inspect_type`, `inspect_source`)',
  'SELECT ''skip idx_inspection_inspect_source'' AS note'
);
PREPARE stmt FROM @sql_idx;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
