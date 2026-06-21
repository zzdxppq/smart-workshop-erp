-- V69 · V1.3.9 标签模板 + 双模式打印菜单（管理后台）
USE `cnc_platform`;

INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(913, 9, 'adm.label-templates', '标签模板', '/admin/label-templates', 'MENU', 6),
(914, 9, 'adm.label-print',     '标签打印', '/admin/label-print',     'MENU', 7),
(915, 9, 'adm.print-logs',      '打印历史', '/admin/print-logs',      'MENU', 8)
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`),
  `path` = VALUES(`path`),
  `sort` = VALUES(`sort`);

UPDATE `sys_menu` SET `sort` = 5 WHERE `menu_code` = 'adm.printers';

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 1, `id`, 'view' FROM `sys_menu` WHERE `menu_code` IN ('adm.label-templates', 'adm.label-print', 'adm.print-logs');

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT r.role_id, m.id, 'view'
FROM (SELECT 9 AS role_id UNION SELECT 7 UNION SELECT 5) r
CROSS JOIN `sys_menu` m
WHERE m.menu_code = 'adm.label-print';
