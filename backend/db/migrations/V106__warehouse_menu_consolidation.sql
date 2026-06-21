-- V106 · 仓储菜单合并：库存查询+批次列表+库存 → 库存管理（Tab）
USE `cnc_platform`;

-- 删除旧冗余菜单（合并到库存管理）
DELETE rp FROM `sys_role_permission` rp
INNER JOIN `sys_menu` m ON m.id = rp.menu_id
WHERE m.`menu_code` IN ('wh.stock-query', 'wh.batches', 'wh.inventory');
DELETE FROM `sys_menu`
WHERE `menu_code` IN ('wh.stock-query', 'wh.batches', 'wh.inventory');

-- 更新库存预警分组（独立分组）
UPDATE `sys_menu` SET `menu_name` = '库存预警' WHERE `menu_code` = 'wh.alert';

-- 排序调整：库存管理=3，库存预警=4，盘点单=5
UPDATE `sys_menu` SET `sort` = 3 WHERE `menu_code` = 'wh.inventory-mgr';
UPDATE `sys_menu` SET `sort` = 4 WHERE `menu_code` = 'wh.alert';
UPDATE `sys_menu` SET `sort` = 5 WHERE `menu_code` = 'wh.stocktake';
