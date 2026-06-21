-- V68: 修复菜单权限 - 只读
-- 问题1：SALES 角色通过 LIKE '/sales/%' 自动获得了 /sales/quotes/approval 权限
-- 问题2：BUYER 角色通过 LIKE '/sourcing/%' 自动获得了 /sourcing/approval-route 权限

USE `cnc_platform`;

-- 1. 修复报价审批权限 - 从 SALES 角色移除
DELETE FROM sys_role_permission
WHERE role_id = 2 AND menu_id = (SELECT id FROM sys_menu WHERE path = '/sales/quotes/approval');

-- 2. 修复采购审批路由权限 - 从 BUYER 角色移除（应该只有 PURCHASER_LEAD、PROCUREMENT_MANAGER、GM）
DELETE FROM sys_role_permission
WHERE role_id = 9 AND menu_id = (SELECT id FROM sys_menu WHERE path = '/sourcing/approval-route');

-- 注意：需要 DBA 执行以下 SQL 来确保权限正确：
-- INSERT IGNORE INTO sys_role_permission (role_id, menu_id, action)
-- SELECT 3, id, 'view' FROM sys_menu WHERE path = '/sales/quotes/approval';
--
-- INSERT IGNORE INTO sys_role_permission (role_id, menu_id, action)
-- SELECT 13, id, 'view' FROM sys_menu WHERE path = '/sourcing/approval-route';
