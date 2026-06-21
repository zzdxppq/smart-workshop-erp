-- V101 · 订单收款已合并至「应收账款」，撤销冗余菜单（若曾执行旧版 V101）
USE `cnc_platform`;

DELETE FROM `sys_role_permission` WHERE `menu_id` = 709;
DELETE FROM `sys_menu` WHERE `menu_code` = 'fin.order-receipts';
