-- ======================================================================
-- V97 · 销售订单流程 V2.1 整改
-- 订单明细扩展：客户图号、图纸ID、产品名称、单件重量、工艺路线预览
-- 2026-06-21
-- ======================================================================

USE `cnc_business`;

SET @db := DATABASE();

SET @add_oi_customer_drawing := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_order_item ADD COLUMN customer_drawing_no VARCHAR(128) DEFAULT NULL COMMENT ''客户图号'' AFTER drawing_no',
        'SELECT ''skip crm_order_item.customer_drawing_no'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_order_item' AND COLUMN_NAME = 'customer_drawing_no'
);
PREPARE _stmt FROM @add_oi_customer_drawing;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

SET @add_oi_drawing_id := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_order_item ADD COLUMN drawing_id BIGINT DEFAULT NULL COMMENT ''关联图纸ID'' AFTER customer_drawing_no',
        'SELECT ''skip crm_order_item.drawing_id'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_order_item' AND COLUMN_NAME = 'drawing_id'
);
PREPARE _stmt FROM @add_oi_drawing_id;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

SET @add_oi_product_name := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_order_item ADD COLUMN product_name VARCHAR(128) DEFAULT NULL COMMENT ''产品名称'' AFTER drawing_id',
        'SELECT ''skip crm_order_item.product_name'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_order_item' AND COLUMN_NAME = 'product_name'
);
PREPARE _stmt FROM @add_oi_product_name;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

SET @add_oi_unit_weight := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_order_item ADD COLUMN unit_weight DECIMAL(12,4) DEFAULT NULL COMMENT ''单件重量(kg)'' AFTER spec',
        'SELECT ''skip crm_order_item.unit_weight'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_order_item' AND COLUMN_NAME = 'unit_weight'
);
PREPARE _stmt FROM @add_oi_unit_weight;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

SET @add_oi_process_route := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_order_item ADD COLUMN process_route VARCHAR(500) DEFAULT NULL COMMENT ''工艺路线预览'' AFTER product_name',
        'SELECT ''skip crm_order_item.process_route'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_order_item' AND COLUMN_NAME = 'process_route'
);
PREPARE _stmt FROM @add_oi_process_route;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

SET @idx_oi_drawing_id := (
    SELECT IF(COUNT(*) = 0,
        'CREATE INDEX idx_order_item_drawing_id ON crm_order_item (drawing_id)',
        'SELECT ''skip idx_order_item_drawing_id'' AS note')
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_order_item' AND INDEX_NAME = 'idx_order_item_drawing_id'
);
PREPARE _stmt FROM @idx_oi_drawing_id;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;
