-- V70 · 图纸物料编码与 crm_material 对齐 + 生管/工程师条码菜单权限
USE `cnc_business`;

-- V94 · mock 清理：图纸→物料补建逻辑依赖演示图纸，已移至 init_data.sql

USE `cnc_platform`;

-- 生管、工程师可访问「物料条码」列表（与图纸页「查看全部条码」一致）
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT r.role_id, m.id, 'view'
FROM (SELECT 5 AS role_id UNION SELECT 6) r
CROSS JOIN `sys_menu` m
WHERE m.menu_code = 'mat.barcode' AND m.status = 'ACTIVE';
