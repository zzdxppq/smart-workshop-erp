-- V75 · 补全 OPERATOR 操作工角色（Flyway 路径遗漏 · 仅 demo 种子有）
--
-- 背景：云端 sys_role 有 PROCUREMENT_MANAGER / CUSTOMER_VISITOR（V52/V72），
--       但 OPERATOR 只在 demo-flow-seed 中，未走 Flyway，导致 APP 操作工无角色。
-- 修复：插入 OPERATOR + 菜单权限（演示账号见 init_data.sql）

USE `cnc_platform`;

-- 1. 角色（不固定 id，避免与已有 PROCUREMENT_MANAGER 冲突）
INSERT IGNORE INTO `sys_role` (`role_code`, `role_name`, `data_scope`, `status`)
VALUES ('OPERATOR', '操作工', 'SELF', 'ACTIVE');

-- 2. 菜单权限（按 role_code 关联，对齐 V67 / sys-menu-permission-seed）
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT r.id, m.id, 'view'
FROM `sys_role` r
JOIN `sys_menu` m ON m.status = 'ACTIVE'
WHERE r.role_code = 'OPERATOR'
  AND (
    m.path IN ('/dashboard', '/dashboard/index', '/dashboard/production', '/dashboard/performance-board', '/production')
    OR m.path IN ('/production/workorders', '/production/schedule', '/dashboard/performance-board')
  );

-- V94 · mock 清理：operator 演示账号已移至 init_data.sql
