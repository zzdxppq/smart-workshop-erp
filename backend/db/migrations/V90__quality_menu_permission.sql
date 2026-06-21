-- ======================================================================
-- V90 · 品质专项增强 · 菜单与权限
-- 2026-06-20
--
-- 新增品质菜单：
-- 1. 质检工作台 /quality/workbench
-- 2. FA 首件检验 /quality/fa-inspection
-- 3. 不良品处理 /quality/defect-disposal
-- 4. CMM 测量 /quality/cmm-inspection
-- ======================================================================

USE `cnc_platform`;

-- 1) 新增品质菜单
INSERT IGNORE INTO sys_menu (menu_code, parent_id, menu_name, path, sort, icon, status)
SELECT 'quality.workbench', 500, '质检工作台', '/quality/workbench', 1, 'el-icon-monitor', 'ACTIVE'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'quality.workbench');

INSERT IGNORE INTO sys_menu (menu_code, parent_id, menu_name, path, sort, icon, status)
SELECT 'quality.fa-inspection', 500, 'FA 首件检验', '/quality/fa-inspection', 2, 'el-icon-document-checked', 'ACTIVE'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'quality.fa-inspection');

INSERT IGNORE INTO sys_menu (menu_code, parent_id, menu_name, path, sort, icon, status)
SELECT 'quality.defect-disposal', 500, '不良品处置', '/quality/defect-disposal', 3, 'el-icon-warning', 'ACTIVE'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'quality.defect-disposal');

INSERT IGNORE INTO sys_menu (menu_code, parent_id, menu_name, path, sort, icon, status)
SELECT 'quality.cmm-inspection', 500, 'CMM 测量', '/quality/cmm-inspection', 4, 'el-icon-data-analysis', 'ACTIVE'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'quality.cmm-inspection');

-- 2) 更新品质(QC)角色权限：只保留这4个菜单
DELETE rp FROM sys_role_permission rp
INNER JOIN sys_role r ON rp.role_id = r.id
INNER JOIN sys_menu m ON rp.menu_id = m.id
WHERE r.role_code = 'QC'
AND m.menu_code NOT IN ('quality.workbench', 'quality.fa-inspection', 'quality.defect-disposal', 'quality.cmm-inspection');

-- 3) 为 QC 角色添加新菜单权限
INSERT IGNORE INTO sys_role_permission (role_id, menu_id, action)
SELECT r.id, m.id, '*'
FROM sys_role r
CROSS JOIN sys_menu m
WHERE r.role_code = 'QC'
AND m.menu_code IN ('quality.workbench', 'quality.fa-inspection', 'quality.defect-disposal', 'quality.cmm-inspection');
