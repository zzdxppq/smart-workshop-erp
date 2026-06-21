-- ======================================================================
-- V95 · 报价流程 V2.1 整改
-- 客户图号、成本项目录、报价明细扩展
-- 2026-06-21
-- ======================================================================

USE `cnc_business`;

SET @db := DATABASE();

-- 1) 图纸：客户图号 + 单件重量
SET @add_customer_drawing_no := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_drawing ADD COLUMN customer_drawing_no VARCHAR(128) DEFAULT NULL COMMENT ''客户图号（PDF文件名）'' AFTER drawing_no',
        'SELECT ''skip crm_drawing.customer_drawing_no'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_drawing' AND COLUMN_NAME = 'customer_drawing_no'
);
PREPARE _stmt FROM @add_customer_drawing_no;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

SET @add_unit_weight := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_drawing ADD COLUMN unit_weight DECIMAL(12,4) DEFAULT NULL COMMENT ''单件重量(kg)'' AFTER spec_size',
        'SELECT ''skip crm_drawing.unit_weight'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_drawing' AND COLUMN_NAME = 'unit_weight'
);
PREPARE _stmt FROM @add_unit_weight;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

SET @idx_customer_drawing := (
    SELECT IF(COUNT(*) = 0,
        'CREATE INDEX idx_drawing_customer_no ON crm_drawing (customer_drawing_no)',
        'SELECT ''skip idx_drawing_customer_no'' AS note')
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_drawing' AND INDEX_NAME = 'idx_drawing_customer_no'
);
PREPARE _stmt FROM @idx_customer_drawing;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

-- 2) 报价明细：客户图号、图纸ID、单件重量、产品名称
SET @add_qi_customer_drawing := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_quote_item ADD COLUMN customer_drawing_no VARCHAR(128) DEFAULT NULL COMMENT ''客户图号'' AFTER drawing_no',
        'SELECT ''skip crm_quote_item.customer_drawing_no'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_quote_item' AND COLUMN_NAME = 'customer_drawing_no'
);
PREPARE _stmt FROM @add_qi_customer_drawing;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

SET @add_qi_drawing_id := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_quote_item ADD COLUMN drawing_id BIGINT DEFAULT NULL COMMENT ''关联图纸ID'' AFTER customer_drawing_no',
        'SELECT ''skip crm_quote_item.drawing_id'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_quote_item' AND COLUMN_NAME = 'drawing_id'
);
PREPARE _stmt FROM @add_qi_drawing_id;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

SET @add_qi_unit_weight := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_quote_item ADD COLUMN unit_weight DECIMAL(12,4) DEFAULT NULL COMMENT ''单件重量(kg)'' AFTER spec',
        'SELECT ''skip crm_quote_item.unit_weight'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_quote_item' AND COLUMN_NAME = 'unit_weight'
);
PREPARE _stmt FROM @add_qi_unit_weight;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

SET @add_qi_product_name := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_quote_item ADD COLUMN product_name VARCHAR(128) DEFAULT NULL COMMENT ''产品名称'' AFTER drawing_id',
        'SELECT ''skip crm_quote_item.product_name'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_quote_item' AND COLUMN_NAME = 'product_name'
);
PREPARE _stmt FROM @add_qi_product_name;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

-- 3) 报价主表：客户需求描述（整体 comment 已有，增加 engineer_completed 标记）
SET @add_engineer_completed := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_quote ADD COLUMN engineer_completed TINYINT(1) DEFAULT 0 COMMENT ''工程师是否已完成全部明细工艺'' AFTER current_node',
        'SELECT ''skip crm_quote.engineer_completed'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_quote' AND COLUMN_NAME = 'engineer_completed'
);
PREPARE _stmt FROM @add_engineer_completed;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

-- 4) 报价成本项目录（范本基础数据）
CREATE TABLE IF NOT EXISTS `crm_quote_cost_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `item_code` VARCHAR(32) NOT NULL COMMENT '成本项编码',
    `item_name` VARCHAR(64) NOT NULL COMMENT '成本项名称',
    `billing_method` VARCHAR(16) NOT NULL COMMENT 'BY_WEIGHT/BY_HOUR/BY_AREA/BY_PERCENT',
    `unit` VARCHAR(16) DEFAULT NULL COMMENT '单位 kg/h/㎡/%',
    `unit_price` DECIMAL(18,4) DEFAULT NULL COMMENT '单价（百分比项可为空）',
    `profit_margin` DECIMAL(8,4) NOT NULL DEFAULT 0.15 COMMENT '利润率',
    `process_code` VARCHAR(32) DEFAULT NULL COMMENT '关联工序编码（工时类）',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `is_active` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uniq_cost_item_code` (`item_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报价成本项目录（V2.1）';

-- 5) 种子：默认成本项（幂等）
INSERT INTO crm_quote_cost_item (item_code, item_name, billing_method, unit, unit_price, profit_margin, process_code, sort_order)
SELECT * FROM (
    SELECT 'MATERIAL' AS item_code, '原材料' AS item_name, 'BY_WEIGHT' AS billing_method, 'kg' AS unit, 28.0000 AS unit_price, 0.1500 AS profit_margin, NULL AS process_code, 1 AS sort_order
    UNION SELECT 'LATHE', '车床', 'BY_HOUR', 'h', 80.0000, 0.1800, 'LATHE', 2
    UNION SELECT 'CNC', 'CNC', 'BY_HOUR', 'h', 120.0000, 0.2000, 'CNC', 3
    UNION SELECT 'EDM', '放电', 'BY_HOUR', 'h', 90.0000, 0.2000, 'EDM', 4
    UNION SELECT 'WEDM', '线切割', 'BY_HOUR', 'h', 60.0000, 0.1500, 'WEDM', 5
    UNION SELECT 'ANODIZE', '阳极氧化', 'BY_AREA', '㎡', 45.0000, 0.2500, NULL, 6
    UNION SELECT 'SOLID_SOLUTION', '固溶处理', 'BY_AREA', '㎡', 35.0000, 0.1500, NULL, 7
    UNION SELECT 'FORMING', '整形', 'BY_AREA', '㎡', 30.0000, 0.1200, NULL, 8
    UNION SELECT 'EXPRESS', '快递运送', 'BY_WEIGHT', 'kg', 5.0000, 0.1000, NULL, 9
    UNION SELECT 'SGNA', '营管销', 'BY_PERCENT', '%', NULL, 0.0800, NULL, 10
) AS seed
WHERE NOT EXISTS (SELECT 1 FROM crm_quote_cost_item LIMIT 1);

-- 6) 表处面积分项（报价明细）
SET @add_anodize_area := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_quote_item ADD COLUMN anodize_area DECIMAL(12,4) DEFAULT NULL COMMENT ''阳极氧化面积(㎡)'' AFTER surface_area',
        'SELECT ''skip crm_quote_item.anodize_area'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_quote_item' AND COLUMN_NAME = 'anodize_area'
);
PREPARE _stmt FROM @add_anodize_area;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

SET @add_solid_area := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_quote_item ADD COLUMN solid_solution_area DECIMAL(12,4) DEFAULT NULL COMMENT ''固溶面积(㎡)'' AFTER anodize_area',
        'SELECT ''skip crm_quote_item.solid_solution_area'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_quote_item' AND COLUMN_NAME = 'solid_solution_area'
);
PREPARE _stmt FROM @add_solid_area;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

SET @add_forming_area := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_quote_item ADD COLUMN forming_area DECIMAL(12,4) DEFAULT NULL COMMENT ''整形面积(㎡)'' AFTER solid_solution_area',
        'SELECT ''skip crm_quote_item.forming_area'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_quote_item' AND COLUMN_NAME = 'forming_area'
);
PREPARE _stmt FROM @add_forming_area;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;
