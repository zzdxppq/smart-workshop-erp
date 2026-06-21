-- ======================================================================
-- V98 · 一级菜单排序 + 工作台子菜单统一「驾驶舱」命名
-- 顺序：工作台、销售、工程、生产、采购、仓储、品质、财务、人事、管理
-- 2026-06-21
-- ======================================================================

USE `cnc_platform`;

UPDATE `sys_menu` SET `sort` = 10 WHERE `menu_code` = 'mod.dashboard';
UPDATE `sys_menu` SET `sort` = 20 WHERE `menu_code` = 'mod.sales';
UPDATE `sys_menu` SET `sort` = 30 WHERE `menu_code` = 'mod.engineering';
UPDATE `sys_menu` SET `sort` = 40 WHERE `menu_code` = 'mod.production';
UPDATE `sys_menu` SET `sort` = 50 WHERE `menu_code` = 'mod.sourcing';
UPDATE `sys_menu` SET `sort` = 55 WHERE `menu_code` = 'mod.warehouse';
UPDATE `sys_menu` SET `sort` = 60 WHERE `menu_code` = 'mod.quality';
UPDATE `sys_menu` SET `sort` = 70 WHERE `menu_code` = 'mod.finance';
UPDATE `sys_menu` SET `sort` = 80 WHERE `menu_code` = 'mod.hr';
UPDATE `sys_menu` SET `sort` = 90 WHERE `menu_code` = 'mod.admin';

UPDATE `sys_menu` SET `menu_name` = '总览驾驶舱'   WHERE `menu_code` = 'dash.index';
UPDATE `sys_menu` SET `menu_name` = '生产驾驶舱'   WHERE `menu_code` = 'dash.production';
UPDATE `sys_menu` SET `menu_name` = '工程师驾驶舱' WHERE `menu_code` = 'dash.engineer';
UPDATE `sys_menu` SET `menu_name` = '仓管驾驶舱'   WHERE `menu_code` = 'dash.warehouse';
UPDATE `sys_menu` SET `menu_name` = '绩效驾驶舱'   WHERE `menu_code` = 'dash.performance';
UPDATE `sys_menu` SET `menu_name` = '多维度驾驶舱' WHERE `menu_code` = 'dash.multi';
UPDATE `sys_menu` SET `menu_name` = '总经理驾驶舱' WHERE `menu_code` = 'dash.alerts';

-- 兼容旧 seed：mod.material 若仍存在则隐藏或合并排序
UPDATE `sys_menu` SET `sort` = 35, `menu_name` = '工程' WHERE `menu_code` = 'mod.material' AND `menu_type` = 'MODULE';
