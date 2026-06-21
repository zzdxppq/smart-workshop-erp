-- ======================================================================
-- V87 · 报价与订单协同设计 V2.1 · 菜单权限（精简版 V2）
-- 2026-06-20
--
-- 设计原则：职责驱动、任务导向、最小必要
-- 整改内容：
-- 1. 工程师：9个菜单精简为5个（订单工程转化/报价工艺确认/工程数据分开）
-- 2. 生管：14个菜单精简为6个（排产中心/生产工作台合并）
-- 3. 品质：6个菜单精简为5个（检验工作台合并）
-- 4. 仓管：菜单结构优化（仓储总览/库存管理合并）
-- 5. 工程师：移除排产计划（归生管），移除工程转化（仅工程师）
-- 6. 物料条码统一归仓管
-- ======================================================================

USE `cnc_platform`;

-- ======================================================================
-- 一、新增菜单
-- ======================================================================

-- ----- 1. 工程师模块新增菜单 -----

-- 订单工程转化（高优先级）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(411, 4, 'eng.order-conversion', '订单工程转化', '/engineering/order-conversion', 'MENU', 1);

-- 报价工艺确认（中优先级）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(412, 4, 'eng.quote-confirmation', '报价工艺确认', '/engineering/quote-confirmation', 'MENU', 2);

-- 工程数据（图纸+料号查询合并）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(413, 4, 'eng.data', '工程数据', '/engineering/data', 'MENU', 3);

-- 工艺任务（查看个人任务排期）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(414, 4, 'eng.my-tasks', '工艺任务', '/engineering/my-tasks', 'MENU', 4);

-- ----- 2. 生管模块新增/重组菜单 -----

-- 生产工作台（总览+委外面板+逾期预警合并）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(311, 3, 'prod.workbench', '生产工作台', '/production/workbench', 'MENU', 1);

-- 排产中心（待转产+排产甘特+工序分配合并）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(312, 3, 'prod.scheduling', '排产中心', '/production/scheduling', 'MENU', 2);

-- 工单管理（工单+详情合并）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(313, 3, 'prod.workorder-mgr', '工单管理', '/production/workorder-mgr', 'MENU', 3);

-- 委外管理（委外列表+委外下单合并）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(314, 3, 'prod.outsource-mgr', '委外管理', '/production/outsource-mgr', 'MENU', 4);

-- 设备管理（设备台账+机台状态合并）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(315, 3, 'prod.machine-mgr', '设备管理', '/production/machine-mgr', 'MENU', 5);

-- ----- 3. 品质模块重组菜单 -----

-- 检验工作台（IQC/IPQC/OQC合并）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(511, 5, 'qc.workbench', '检验工作台', '/quality/workbench', 'MENU', 1);

-- ----- 4. 仓管模块重组菜单 -----

-- 仓储总览（多仓库总览+库存预警合并）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(1011, 10, 'wh.overview', '仓储总览', '/warehouse/overview', 'MENU', 1);

-- 库存管理（库存+批次+库位合并）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(1012, 10, 'wh.inventory-mgr', '库存管理', '/warehouse/inventory-mgr', 'MENU', 2);

-- ======================================================================
-- 二、角色权限分配
-- ======================================================================

-- ----- 工程师权限 -----

-- 订单工程转化：仅工程师
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT r.id, m.id, 'view'
FROM `sys_role` r, `sys_menu` m
WHERE r.role_code IN ('ENGINEER', 'ADMIN', 'SYS_ADMIN')
  AND m.menu_code = 'eng.order-conversion';

-- 报价工艺确认：仅工程师
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT r.id, m.id, 'view'
FROM `sys_role` r, `sys_menu` m
WHERE r.role_code IN ('ENGINEER', 'ADMIN', 'SYS_ADMIN')
  AND m.menu_code = 'eng.quote-confirmation';

-- 工程数据：仅工程师（图纸+料号查询）
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT r.id, m.id, 'view'
FROM `sys_role` r, `sys_menu` m
WHERE r.role_code IN ('ENGINEER', 'ADMIN', 'SYS_ADMIN')
  AND m.menu_code = 'eng.data';

-- 工艺任务：仅工程师
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT r.id, m.id, 'view'
FROM `sys_role` r, `sys_menu` m
WHERE r.role_code IN ('ENGINEER', 'ADMIN', 'SYS_ADMIN')
  AND m.menu_code = 'eng.my-tasks';

-- 报价单：工程师可填写工艺
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT r.id, m.id, 'view'
FROM `sys_role` r, `sys_menu` m
WHERE r.role_code IN ('ENGINEER', 'ADMIN', 'SYS_ADMIN')
  AND m.menu_code = 'sales.quotes';

-- 销售订单：工程师可查看订单详情
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT r.id, m.id, 'view'
FROM `sys_role` r, `sys_menu` m
WHERE r.role_code IN ('ENGINEER', 'ADMIN', 'SYS_ADMIN')
  AND m.menu_code = 'sales.orders';

-- 工单：工程师可查看工序工艺
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT r.id, m.id, 'view'
FROM `sys_role` r, `sys_menu` m
WHERE r.role_code IN ('ENGINEER', 'ADMIN', 'SYS_ADMIN')
  AND m.menu_code = 'prod.workorders';

-- ----- 生管权限 -----

-- 生产工作台：仅生管
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT r.id, m.id, 'view'
FROM `sys_role` r, `sys_menu` m
WHERE r.role_code IN ('PROD_MGR', 'PRODUCTION_MANAGER', 'ADMIN', 'SYS_ADMIN')
  AND m.menu_code = 'prod.workbench';

-- 排产中心：仅生管
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT r.id, m.id, 'view'
FROM `sys_role` r, `sys_menu` m
WHERE r.role_code IN ('PROD_MGR', 'PRODUCTION_MANAGER', 'ADMIN', 'SYS_ADMIN')
  AND m.menu_code = 'prod.scheduling';

-- 工单管理：仅生管
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT r.id, m.id, 'view'
FROM `sys_role` r, `sys_menu` m
WHERE r.role_code IN ('PROD_MGR', 'PRODUCTION_MANAGER', 'ADMIN', 'SYS_ADMIN')
  AND m.menu_code = 'prod.workorder-mgr';

-- 委外管理：仅生管
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT r.id, m.id, 'view'
FROM `sys_role` r, `sys_menu` m
WHERE r.role_code IN ('PROD_MGR', 'PRODUCTION_MANAGER', 'ADMIN', 'SYS_ADMIN')
  AND m.menu_code = 'prod.outsource-mgr';

-- 设备管理：仅生管
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT r.id, m.id, 'view'
FROM `sys_role` r, `sys_menu` m
WHERE r.role_code IN ('PROD_MGR', 'PRODUCTION_MANAGER', 'ADMIN', 'SYS_ADMIN')
  AND m.menu_code = 'prod.machine-mgr';

-- MRP中心：生管保留
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT r.id, m.id, 'view'
FROM `sys_role` r, `sys_menu` m
WHERE r.role_code IN ('PROD_MGR', 'PRODUCTION_MANAGER', 'ADMIN', 'SYS_ADMIN')
  AND m.menu_code = 'prod.mrp';

-- ----- 品质权限 -----

-- 检验工作台：仅品质
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT r.id, m.id, 'view'
FROM `sys_role` r, `sys_menu` m
WHERE r.role_code IN ('QC', 'QUALITY_MANAGER', 'ADMIN', 'SYS_ADMIN')
  AND m.menu_code = 'qc.workbench';

-- FA首件：品质保留
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT r.id, m.id, 'view'
FROM `sys_role` r, `sys_menu` m
WHERE r.role_code IN ('QC', 'QUALITY_MANAGER', 'ADMIN', 'SYS_ADMIN')
  AND m.menu_code = 'qc.fa';

-- 三次元：品质保留
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT r.id, m.id, 'view'
FROM `sys_role` r, `sys_menu` m
WHERE r.role_code IN ('QC', 'QUALITY_MANAGER', 'ADMIN', 'SYS_ADMIN')
  AND m.menu_code = 'qc.cmm';

-- 不良品：品质保留
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT r.id, m.id, 'view'
FROM `sys_role` r, `sys_menu` m
WHERE r.role_code IN ('QC', 'QUALITY_MANAGER', 'ADMIN', 'SYS_ADMIN')
  AND m.menu_code = 'qc.defect';

-- 提货检：品质保留
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT r.id, m.id, 'view'
FROM `sys_role` r, `sys_menu` m
WHERE r.role_code IN ('QC', 'QUALITY_MANAGER', 'ADMIN', 'SYS_ADMIN')
  AND m.menu_code = 'qc.pickup';

-- ----- 仓管权限 -----

-- 仓储总览：仅仓管
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT r.id, m.id, 'view'
FROM `sys_role` r, `sys_menu` m
WHERE r.role_code IN ('WAREHOUSE', 'WAREHOUSE_KEEPER', 'ADMIN', 'SYS_ADMIN')
  AND m.menu_code = 'wh.overview';

-- 库存管理：仅仓管
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT r.id, m.id, 'view'
FROM `sys_role` r, `sys_menu` m
WHERE r.role_code IN ('WAREHOUSE', 'WAREHOUSE_KEEPER', 'ADMIN', 'SYS_ADMIN')
  AND m.menu_code = 'wh.inventory-mgr';

-- 物料条码：仅仓管（统一归属）
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT r.id, m.id, 'view'
FROM `sys_role` r, `sys_menu` m
WHERE r.role_code IN ('WAREHOUSE', 'WAREHOUSE_KEEPER', 'ADMIN', 'SYS_ADMIN')
  AND m.menu_code = 'mat.barcode';

-- ======================================================================
-- 三、权限清理：移除不合理的权限授予
-- ======================================================================

-- 从生管移除旧菜单权限（精简后不再需要）
-- 待转产订单（合并入排产中心）
DELETE FROM `sys_role_permission`
WHERE `menu_id` = (SELECT id FROM `sys_menu` WHERE `menu_code` = 'prod.pending')
  AND `role_id` IN (SELECT id FROM `sys_role` WHERE `role_code` IN ('PROD_MGR', 'PRODUCTION_MANAGER'));

-- 工单（合并入工单管理）
DELETE FROM `sys_role_permission`
WHERE `menu_id` = (SELECT id FROM `sys_menu` WHERE `menu_code` = 'prod.workorders')
  AND `role_id` IN (SELECT id FROM `sys_role` WHERE `role_code` IN ('PROD_MGR', 'PRODUCTION_MANAGER'));

-- 委外列表（合并入委外管理）
DELETE FROM `sys_role_permission`
WHERE `menu_id` = (SELECT id FROM `sys_menu` WHERE `menu_code` = 'prod.outsource')
  AND `role_id` IN (SELECT id FROM `sys_role` WHERE `role_code` IN ('PROD_MGR', 'PRODUCTION_MANAGER'));

-- 设备机台（合并入设备管理）
DELETE FROM `sys_role_permission`
WHERE `menu_id` = (SELECT id FROM `sys_menu` WHERE `menu_code` = 'prod.machines')
  AND `role_id` IN (SELECT id FROM `sys_role` WHERE `role_code` IN ('PROD_MGR', 'PRODUCTION_MANAGER'));

-- 排产看板（合并入排产中心）
DELETE FROM `sys_role_permission`
WHERE `menu_id` = (SELECT id FROM `sys_menu` WHERE `menu_code` = 'prod.schedule')
  AND `role_id` IN (SELECT id FROM `sys_role` WHERE `role_code` IN ('PROD_MGR', 'PRODUCTION_MANAGER'));

-- 排产甘特（合并入排产中心）
DELETE FROM `sys_role_permission`
WHERE `menu_id` = (SELECT id FROM `sys_menu` WHERE `menu_code` = 'prod.gantt')
  AND `role_id` IN (SELECT id FROM `sys_role` WHERE `role_code` IN ('PROD_MGR', 'PRODUCTION_MANAGER'));

-- 工序分配（合并入排产中心）
DELETE FROM `sys_role_permission`
WHERE `menu_id` = (SELECT id FROM `sys_menu` WHERE `menu_code` = 'prod.allocation')
  AND `role_id` IN (SELECT id FROM `sys_role` WHERE `role_code` IN ('PROD_MGR', 'PRODUCTION_MANAGER'));

-- 委外面板（合并入生产工作台）
DELETE FROM `sys_role_permission`
WHERE `menu_id` = (SELECT id FROM `sys_menu` WHERE `menu_code` = 'prod.outsub-panel')
  AND `role_id` IN (SELECT id FROM `sys_role` WHERE `role_code` IN ('PROD_MGR', 'PRODUCTION_MANAGER'));

-- 从工程师移除旧菜单权限
-- 工程转化（改为eng.order-conversion）
DELETE FROM `sys_role_permission`
WHERE `menu_id` = (SELECT id FROM `sys_menu` WHERE `menu_code` = 'mat.engineering-workbench')
  AND `role_id` IN (SELECT id FROM `sys_role` WHERE `role_code` IN ('ENGINEER'));

-- 注意：dash.engineer（工作台）保留给工程师，不需要删除

-- 从品质移除旧菜单权限
-- 来料/过程/成品检（合并入检验工作台）
DELETE FROM `sys_role_permission`
WHERE `menu_id` = (SELECT id FROM `sys_menu` WHERE `menu_code` = 'qc.inspection')
  AND `role_id` IN (SELECT id FROM `sys_role` WHERE `role_code` IN ('QC', 'QUALITY_MANAGER'));

-- 委外检（删除）
DELETE FROM `sys_role_permission`
WHERE `menu_id` = (SELECT id FROM `sys_menu` WHERE `menu_code` = 'qc.outsource')
  AND `role_id` IN (SELECT id FROM `sys_role` WHERE `role_code` IN ('QC', 'QUALITY_MANAGER'));

-- 从仓管移除旧菜单权限
-- 多仓库总览（合并入仓储总览）
DELETE FROM `sys_role_permission`
WHERE `menu_id` = (SELECT id FROM `sys_menu` WHERE `menu_code` = 'wh.index')
  AND `role_id` IN (SELECT id FROM `sys_role` WHERE `role_code` IN ('WAREHOUSE', 'WAREHOUSE_KEEPER'));

-- 库存（合并入库存管理）
DELETE FROM `sys_role_permission`
WHERE `menu_id` = (SELECT id FROM `sys_menu` WHERE `menu_code` = 'wh.inventory')
  AND `role_id` IN (SELECT id FROM `sys_role` WHERE `role_code` IN ('WAREHOUSE', 'WAREHOUSE_KEEPER'));

-- 批次列表（合并入库存管理）
DELETE FROM `sys_role_permission`
WHERE `menu_id` = (SELECT id FROM `sys_menu` WHERE `menu_code` = 'wh.batches')
  AND `role_id` IN (SELECT id FROM `sys_role` WHERE `role_code` IN ('WAREHOUSE', 'WAREHOUSE_KEEPER'));

-- 库位树（合并入库存管理）
DELETE FROM `sys_role_permission`
WHERE `menu_id` = (SELECT id FROM `sys_menu` WHERE `menu_code` = 'wh.locations')
  AND `role_id` IN (SELECT id FROM `sys_role` WHERE `role_code` IN ('WAREHOUSE', 'WAREHOUSE_KEEPER'));

-- 库存预警（合并入仓储总览）
DELETE FROM `sys_role_permission`
WHERE `menu_id` = (SELECT id FROM `sys_menu` WHERE `menu_code` = 'wh.alert')
  AND `role_id` IN (SELECT id FROM `sys_role` WHERE `role_code` IN ('WAREHOUSE', 'WAREHOUSE_KEEPER'));

-- ======================================================================
-- 四、V2.1 新增报价范本菜单（独立，不受精简影响）
-- ======================================================================

-- 报价范本：销售、工程师可见
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(207, 2, 'sales.quote-templates', '报价范本', '/sales/quote-templates', 'MENU', 3);

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT r.id, m.id, 'view'
FROM `sys_role` r, `sys_menu` m
WHERE r.role_code IN ('SALES', 'ENGINEER', 'ADMIN', 'SYS_ADMIN')
  AND m.menu_code = 'sales.quote-templates';
