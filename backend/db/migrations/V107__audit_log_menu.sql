-- V107 · 审计日志菜单：系统管理分组下新增「审计日志」菜单项
USE `cnc_platform`;

-- 新增审计日志菜单（挂在 adm 用户管理同级）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`)
VALUES (913, 9, 'adm.audit-logs', '审计日志', '/admin/audit-logs', 'MENU', 1);

-- SYS_ADMIN 角色默认授予审计日志权限（INSERT IGNORE 不影响现网）
-- sys_role_permission 表结构：(role_id, menu_id, action)，无 permission 列
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 1, m.id, 'view'
FROM `sys_menu` m
WHERE m.menu_code = 'adm.audit-logs';
