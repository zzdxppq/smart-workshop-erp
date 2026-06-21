-- V105 · 生产菜单结构调整：职责分组，删除冗余父级菜单
USE `cnc_platform`;

-- 删除旧的冗余父级菜单（已由 router menuGroup 替代）
-- prod.outsource-mgr：/production/outsource-mgr → /production/outsource（redirect 已设）
DELETE rp FROM `sys_role_permission` rp
INNER JOIN `sys_menu` m ON m.id = rp.menu_id
WHERE m.`menu_code` IN ('prod.outsource-mgr', 'prod.mrp');
DELETE FROM `sys_menu`
WHERE `menu_code` IN ('prod.outsource-mgr', 'prod.mrp');
