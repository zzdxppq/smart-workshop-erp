-- ======================================================================
-- V83 · 报价与订单协同设计 V2.1 · Phase 1
-- 料号生成 + 订单状态机改造
-- 2026-06-20
-- ======================================================================

USE `cnc_business`;

-- 1) 订单明细表新增 material_no 字段（料号，订单提交时生成）
SET @db := DATABASE();

SET @add_material_no := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_order_item ADD COLUMN material_no VARCHAR(64) NULL COMMENT ''物料编码（料号），订单提交时生成'' AFTER drawing_no',
        'SELECT ''skip crm_order_item.material_no'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_order_item' AND COLUMN_NAME = 'material_no'
);
PREPARE _stmt FROM @add_material_no;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

-- 2) 订单明细表新增 source_quotation_detail_id 字段（来源报价明细行ID）
SET @add_quote_detail_id := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_order_item ADD COLUMN source_quotation_detail_id BIGINT NULL COMMENT ''来源报价明细行ID'' AFTER material_no',
        'SELECT ''skip crm_order_item.source_quotation_detail_id'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_order_item' AND COLUMN_NAME = 'source_quotation_detail_id'
);
PREPARE _stmt FROM @add_quote_detail_id;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

-- 3) 订单主表新增 source_quotation_id 字段（来源报价单ID）
SET @add_quote_id := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_order ADD COLUMN source_quotation_id BIGINT NULL COMMENT ''来源报价单ID'' AFTER quote_id',
        'SELECT ''skip crm_order.source_quotation_id'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_order' AND COLUMN_NAME = 'source_quotation_id'
);
PREPARE _stmt FROM @add_quote_id;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

-- 4) 订单主表新增 detailed_process 字段（工程师细化的详细工艺）
SET @add_detailed_process := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_order ADD COLUMN detailed_process JSON NULL COMMENT ''工程师细化的详细工艺'' AFTER source_quotation_id',
        'SELECT ''skip crm_order.detailed_process'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_order' AND COLUMN_NAME = 'detailed_process'
);
PREPARE _stmt FROM @add_detailed_process;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

-- 5) 订单主表新增 bom_data 字段（工程师编制的BOM数据）
SET @add_bom_data := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_order ADD COLUMN bom_data JSON NULL COMMENT ''工程师编制的BOM数据'' AFTER detailed_process',
        'SELECT ''skip crm_order.bom_data'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_order' AND COLUMN_NAME = 'bom_data'
);
PREPARE _stmt FROM @add_bom_data;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

-- 6) 物料主数据新增 drawing_no 字段（关联的图号）
USE `cnc_business`;
SET @add_drawing_no := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_material ADD COLUMN drawing_no VARCHAR(64) NULL COMMENT ''关联的图号（DWG-）'' AFTER process_id',
        'SELECT ''skip crm_material.drawing_no'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_material' AND COLUMN_NAME = 'drawing_no'
);
PREPARE _stmt FROM @add_drawing_no;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

-- 7) 物料主数据新增 generated_from_order 字段（首次生成该料号的销售订单号）
SET @add_gen_order := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_material ADD COLUMN generated_from_order VARCHAR(32) NULL COMMENT ''首次生成该料号的销售订单号'' AFTER drawing_no',
        'SELECT ''skip crm_material.generated_from_order'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_material' AND COLUMN_NAME = 'generated_from_order'
);
PREPARE _stmt FROM @add_gen_order;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

-- 8) 为 material_no 和 drawing_no 添加索引
SET @idx_material_no := (
    SELECT IF(COUNT(*) = 0,
        'CREATE INDEX idx_order_item_material_no ON crm_order_item (material_no)',
        'SELECT ''skip idx_order_item_material_no'' AS note')
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_order_item' AND INDEX_NAME = 'idx_order_item_material_no'
);
PREPARE _stmt FROM @idx_material_no;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

SET @idx_material_drawing_no := (
    SELECT IF(COUNT(*) = 0,
        'CREATE INDEX idx_material_drawing_no ON crm_material (drawing_no)',
        'SELECT ''skip idx_material_drawing_no'' AS note')
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_material' AND INDEX_NAME = 'idx_material_drawing_no'
);
PREPARE _stmt FROM @idx_material_drawing_no;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

-- 9) 工单表新增关联字段
USE `cnc_business`;

SET @add_sales_order_id := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_workorder ADD COLUMN sales_order_id BIGINT NULL COMMENT ''关联销售订单ID'' AFTER production_order_no',
        'SELECT ''skip crm_workorder.sales_order_id'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_workorder' AND COLUMN_NAME = 'sales_order_id'
);
PREPARE _stmt FROM @add_sales_order_id;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

SET @add_workorder_material_no := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_workorder ADD COLUMN material_no VARCHAR(64) NULL COMMENT ''关联料号'' AFTER sales_order_id',
        'SELECT ''skip crm_workorder.material_no'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_workorder' AND COLUMN_NAME = 'material_no'
);
PREPARE _stmt FROM @add_workorder_material_no;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

SET @add_bom_id := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_workorder ADD COLUMN bom_id BIGINT NULL COMMENT ''关联BOM ID'' AFTER material_no',
        'SELECT ''skip crm_workorder.bom_id'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_workorder' AND COLUMN_NAME = 'bom_id'
);
PREPARE _stmt FROM @add_bom_id;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

SET @add_process_route_id := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_workorder ADD COLUMN process_route_id BIGINT NULL COMMENT ''关联详细工艺ID'' AFTER bom_id',
        'SELECT ''skip crm_workorder.process_route_id'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_workorder' AND COLUMN_NAME = 'process_route_id'
);
PREPARE _stmt FROM @add_process_route_id;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

SET @add_planned_start := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_workorder ADD COLUMN planned_start DATETIME NULL COMMENT ''生管排产计划开始'' AFTER process_route_id',
        'SELECT ''skip crm_workorder.planned_start'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_workorder' AND COLUMN_NAME = 'planned_start'
);
PREPARE _stmt FROM @add_planned_start;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

SET @add_planned_end := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_workorder ADD COLUMN planned_end DATETIME NULL COMMENT ''生管排产计划结束'' AFTER planned_start',
        'SELECT ''skip crm_workorder.planned_end'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_workorder' AND COLUMN_NAME = 'planned_end'
);
PREPARE _stmt FROM @add_planned_end;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

-- 10) 状态机字典更新：新增 APPROVED/PROCESSING/PENDING_PRODUCTION 状态
USE `cnc_platform`;

-- 订单状态字典（如果存在则更新，不存在则插入）
INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('ORDER_STATUS', 'APPROVED', '已生效', 15, '订单提交后直接生效（无需审批）', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label), remark = VALUES(remark);

INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('ORDER_STATUS', 'PROCESSING', '工程转化中', 16, '工程师正在细化工艺和BOM', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label), remark = VALUES(remark);

INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('ORDER_STATUS', 'PENDING_PRODUCTION', '待转产', 17, '工程转化完成，等待生管转工单', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label), remark = VALUES(remark);

-- 更新现有 CONFIRMED 状态说明
UPDATE sys_dict SET remark = '已确认（已审批，可手动确认）' WHERE dict_type = 'ORDER_STATUS' AND dict_code = 'CONFIRMED';
