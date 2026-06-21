-- ======================================================================
-- V74 · PRD V2.0 对齐整改（图纸/仓储菜单/条码/权限）
-- 2026-06-19
-- ======================================================================

USE `cnc_business`;

-- 1) 图纸：上传时不强制物料编码，增加材质/规格字段
--    幂等：init/V6 已含列时跳过 ADD（避免 1060 Duplicate column）
ALTER TABLE crm_drawing
    MODIFY COLUMN material_code VARCHAR(64) NULL COMMENT '料号，工程转化前可为空';

SET @db := DATABASE();

SET @add_material_grade := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_drawing ADD COLUMN material_grade VARCHAR(128) NULL COMMENT ''材质'' AFTER title',
        'SELECT ''skip crm_drawing.material_grade'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_drawing' AND COLUMN_NAME = 'material_grade'
);
PREPARE _stmt FROM @add_material_grade;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

SET @add_spec_size := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_drawing ADD COLUMN spec_size VARCHAR(256) NULL COMMENT ''规格尺寸'' AFTER material_grade',
        'SELECT ''skip crm_drawing.spec_size'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_drawing' AND COLUMN_NAME = 'spec_size'
);
PREPARE _stmt FROM @add_spec_size;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

-- 2) 标记不合规 BC 前缀条码为失效
UPDATE crm_material_barcode
SET status = 'DISCARDED', updated_at = NOW()
WHERE barcode_no REGEXP '^BC[0-9]{8}-[0-9]{4}$'
  AND status = 'ACTIVE';

UPDATE crm_material_barcode
SET status = 'DISCARDED', updated_at = NOW()
WHERE barcode_no LIKE 'WL-DEMO-%';

USE `cnc_platform`;

-- 3) 菜单：物料 → 工程数据；新增工艺路线维护、仓储入库/出库/盘点
UPDATE sys_menu SET menu_name = '工程数据' WHERE menu_code = 'mod.material';

INSERT INTO sys_menu (id, parent_id, menu_code, menu_name, path, menu_type, sort)
VALUES (408, 4, 'mat.product-route', '工艺路线维护', '/material/process-routes', 'MENU', 8)
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), path = VALUES(path);

INSERT INTO sys_menu (id, parent_id, menu_code, menu_name, path, menu_type, sort)
VALUES
    (1006, 10, 'wh.inbound',   '入库单',   '/warehouse/inbound',   'MENU', 6),
    (1007, 10, 'wh.outbound',  '出库单',   '/warehouse/outbound',  'MENU', 7),
    (1008, 10, 'wh.stock-query','库存查询','/warehouse/stock-query','MENU', 8),
    (1009, 10, 'wh.stocktake', '盘点单',   '/warehouse/stocktake', 'MENU', 9)
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), path = VALUES(path);

UPDATE sys_menu SET menu_name = '仓储总览' WHERE menu_code = 'wh.index';
UPDATE sys_menu SET sort = 1 WHERE menu_code = 'wh.inbound';
UPDATE sys_menu SET sort = 2 WHERE menu_code = 'wh.outbound';
UPDATE sys_menu SET sort = 3 WHERE menu_code = 'wh.stock-query';
UPDATE sys_menu SET sort = 4 WHERE menu_code = 'wh.batches';
UPDATE sys_menu SET sort = 5 WHERE menu_code = 'wh.inventory';
UPDATE sys_menu SET sort = 6 WHERE menu_code = 'wh.alert';
UPDATE sys_menu SET sort = 7 WHERE menu_code = 'wh.stocktake';

-- 4) 角色权限：工程数据仅工程师+管理员
DELETE rp FROM sys_role_permission rp
INNER JOIN sys_menu m ON rp.menu_id = m.id
WHERE rp.role_id IN (2, 3, 5, 7, 9, 10)
  AND (m.path LIKE '/material/%' OR m.menu_code = 'mod.material');

-- 工程师 + 管理员保留工程数据全量
INSERT INTO sys_role_permission (role_id, menu_id, action)
SELECT 6, id, 'view' FROM sys_menu WHERE status = 'ACTIVE' AND (
    path IN ('/material', '/dashboard', '/dashboard/index', '/dashboard/engineer', '/production')
    OR path LIKE '/material/%'
    OR path LIKE '/production/schedule%'
    OR path LIKE '/production/mrp%'
    OR path LIKE '/production/workorders%'
)
ON DUPLICATE KEY UPDATE action = VALUES(action);

-- 仓管：仓储全量
INSERT INTO sys_role_permission (role_id, menu_id, action)
SELECT 7, id, 'view' FROM sys_menu WHERE status = 'ACTIVE' AND (
    path IN ('/warehouse', '/dashboard', '/dashboard/index', '/dashboard/warehouse')
    OR path LIKE '/warehouse/%'
)
ON DUPLICATE KEY UPDATE action = VALUES(action);

-- 生管：仓储总览+库存查询+预警（只读）
INSERT INTO sys_role_permission (role_id, menu_id, action)
SELECT 5, id, 'view' FROM sys_menu WHERE status = 'ACTIVE' AND path IN (
    '/warehouse/index', '/warehouse/stock-query', '/warehouse/inventory', '/warehouse/inventory-alert'
)
ON DUPLICATE KEY UPDATE action = VALUES(action);

-- 采购：仓储总览+入库单+预警
INSERT INTO sys_role_permission (role_id, menu_id, action)
SELECT 9, id, 'view' FROM sys_menu WHERE status = 'ACTIVE' AND path IN (
    '/warehouse/index', '/warehouse/inbound', '/warehouse/inventory-alert'
)
ON DUPLICATE KEY UPDATE action = VALUES(action);

-- 财务：仓储总览+库存查询
INSERT INTO sys_role_permission (role_id, menu_id, action)
SELECT 10, id, 'view' FROM sys_menu WHERE status = 'ACTIVE' AND path IN (
    '/warehouse/index', '/warehouse/stock-query', '/warehouse/inventory'
)
ON DUPLICATE KEY UPDATE action = VALUES(action);

-- 品质：仓储总览+批次列表
INSERT INTO sys_role_permission (role_id, menu_id, action)
SELECT 8, id, 'view' FROM sys_menu WHERE status = 'ACTIVE' AND path IN (
    '/warehouse/index', '/warehouse/batches'
)
ON DUPLICATE KEY UPDATE action = VALUES(action);
