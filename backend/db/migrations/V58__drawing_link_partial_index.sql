-- ============================================================
-- V1.3.9 Sprint 13 Story 13.3 · V58__drawing_link_partial_index.sql
-- crm_drawing_link 5 类 link 部分索引 + 业务表 material_code JOIN 索引
-- init.sql：在 V62 之后执行（crm_workorder_process 依赖 V62 建表）
-- ============================================================

USE `cnc_business`;

-- 1. crm_drawing_link 5 类 link 部分索引（MySQL 8 函数索引）
CREATE INDEX idx_drawing_link_order
    ON crm_drawing_link(
        (CASE WHEN biz_type = 'ORDER' THEN drawing_id END),
        (CASE WHEN biz_type = 'ORDER' THEN biz_id END)
    );

CREATE INDEX idx_drawing_link_po
    ON crm_drawing_link(
        (CASE WHEN biz_type = 'PO' THEN drawing_id END),
        (CASE WHEN biz_type = 'PO' THEN biz_id END)
    );

CREATE INDEX idx_drawing_link_incoming
    ON crm_drawing_link(
        (CASE WHEN biz_type = 'INCOMING' THEN drawing_id END),
        (CASE WHEN biz_type = 'INCOMING' THEN biz_id END)
    );

CREATE INDEX idx_drawing_link_inspection
    ON crm_drawing_link(
        (CASE WHEN biz_type = 'INSPECTION' THEN drawing_id END),
        (CASE WHEN biz_type = 'INSPECTION' THEN biz_id END)
    );

CREATE INDEX idx_drawing_link_process
    ON crm_drawing_link(
        (CASE WHEN biz_type = 'WORKORDER_PROCESS' THEN drawing_id END),
        (CASE WHEN biz_type = 'WORKORDER_PROCESS' THEN biz_id END)
    );

-- 2. 业务表 material_code JOIN 索引（真实表名 · 非 crm_incoming_order_item / crm_inspection_item）
CREATE INDEX idx_order_item_material_order
    ON crm_order_item(material, order_id);

CREATE INDEX idx_po_item_material_po
    ON crm_purchase_order_item(material_code, purchase_order_id);

CREATE INDEX idx_incoming_material_id
    ON crm_incoming(material_code, id);

CREATE INDEX idx_quality_inspection_material_id
    ON crm_quality_inspection(material_code, id);

CREATE INDEX idx_workorder_process_material
    ON crm_workorder_process(material_code);

ALTER TABLE crm_drawing_link
    COMMENT = 'V1.3.9 Sprint 12.1 创建 · Sprint 13.3 真实查询对接 · V58 加 5 部分索引 + material_code JOIN 索引';

SELECT 'V58__drawing_link_partial_index.sql 迁移完成' AS message,
       (SELECT COUNT(*) FROM information_schema.statistics
        WHERE table_schema = 'cnc_business'
          AND table_name = 'crm_drawing_link'
          AND index_name LIKE 'idx_drawing_link_%') AS partial_index_count,
       (SELECT COUNT(*) FROM information_schema.statistics
        WHERE table_schema = 'cnc_business'
          AND ((table_name = 'crm_order_item' AND index_name = 'idx_order_item_material_order')
            OR (table_name = 'crm_purchase_order_item' AND index_name = 'idx_po_item_material_po')
            OR (table_name = 'crm_incoming' AND index_name = 'idx_incoming_material_id')
            OR (table_name = 'crm_quality_inspection' AND index_name = 'idx_quality_inspection_material_id')
            OR (table_name = 'crm_workorder_process' AND index_name = 'idx_workorder_process_material'))) AS item_index_count;
