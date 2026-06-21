-- V99 · 销售二级菜单命名调整；报价范本从默认菜单隐藏
UPDATE `sys_menu` SET `menu_name` = '新建报价单' WHERE `menu_code` = 'sales.quotes';
UPDATE `sys_menu` SET `menu_name` = '新建销售订单' WHERE `menu_code` = 'sales.orders';
UPDATE `sys_menu` SET `status` = 'INACTIVE' WHERE `menu_code` = 'sales.quote-templates';

-- 业务员(SALES)不可见报价审批
DELETE FROM `sys_role_permission`
WHERE `role_id` = 2
  AND `menu_id` = (SELECT `id` FROM (SELECT `id` FROM `sys_menu` WHERE `menu_code` = 'sales.quote-approval') t);
