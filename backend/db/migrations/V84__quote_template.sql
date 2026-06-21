-- ======================================================================
-- V84 · 报价与订单协同设计 V2.1 · Phase 2
-- 报价范本管理 + 工程师工艺字段
-- 2026-06-20
-- ======================================================================

USE `cnc_business`;

-- 1) 报价范本表
CREATE TABLE IF NOT EXISTS `crm_quote_template` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `template_no` VARCHAR(32) NOT NULL COMMENT '范本编号：MB-YYYYMMDD-NNNN',
    `template_name` VARCHAR(128) NOT NULL COMMENT '范本名称',
    `category` VARCHAR(32) DEFAULT NULL COMMENT '分类：机加工/钣金/组装等',
    `process_type` VARCHAR(32) DEFAULT NULL COMMENT '工艺类型：车床/CNC/放电/线割/表处等',
    `cost_material` DECIMAL(18,4) DEFAULT 0 COMMENT '材料成本（元/件）',
    `cost_labor` DECIMAL(18,4) DEFAULT 0 COMMENT '人工成本（元/小时）',
    `cost_machine` DECIMAL(18,4) DEFAULT 0 COMMENT '机台成本（元/小时）',
    `cost_overhead` DECIMAL(18,4) DEFAULT 0 COMMENT '管理费率（%）',
    `cost_outsource` DECIMAL(18,4) DEFAULT 0 COMMENT '委外费率（%）',
    `profit_margin` DECIMAL(8,4) DEFAULT 0.20 COMMENT '利润率（默认20%）',
    `billing_method` VARCHAR(16) DEFAULT 'BY_QUANTITY' COMMENT '计费方式：BY_QUANTITY/BY_WEIGHT/BY_AREA',
    `unit` VARCHAR(16) DEFAULT '件' COMMENT '计费单位',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注说明',
    `is_active` TINYINT(1) DEFAULT 1 COMMENT '是否启用（0否1是）',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uniq_template_no` (`template_no`),
    INDEX `idx_template_category` (`category`),
    INDEX `idx_template_process_type` (`process_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报价范本表（V2.1）';

-- 2) 报价范本明细表（工序明细）
CREATE TABLE IF NOT EXISTS `crm_quote_template_process` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `template_id` BIGINT NOT NULL COMMENT '范本ID',
    `sequence` INT NOT NULL DEFAULT 0 COMMENT '工序序号',
    `process_code` VARCHAR(32) DEFAULT NULL COMMENT '工序编码',
    `process_name` VARCHAR(64) NOT NULL COMMENT '工序名称',
    `machine_type` VARCHAR(64) DEFAULT NULL COMMENT '设备类型',
    `unit_time_minutes` INT DEFAULT 0 COMMENT '单位工时（分钟）',
    `cost_per_hour` DECIMAL(18,4) DEFAULT 0 COMMENT '每小时成本',
    `outsource_flag` TINYINT(1) DEFAULT 0 COMMENT '是否委外（0否1是）',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    INDEX `idx_template_process_template` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报价范本工序明细表（V2.1）';

-- 3) 报价明细新增工艺字段
SET @db := DATABASE();

SET @add_quote_process := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_quote_item ADD COLUMN process_summary JSON NULL COMMENT ''工艺汇总JSON'' AFTER drawing_no',
        'SELECT ''skip crm_quote_item.process_summary'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_quote_item' AND COLUMN_NAME = 'process_summary'
);
PREPARE _stmt FROM @add_quote_process;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

SET @add_quote_process_route := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_quote_item ADD COLUMN process_route VARCHAR(500) DEFAULT NULL COMMENT ''工艺路线（如：下料→CNC→热处理）'' AFTER process_summary',
        'SELECT ''skip crm_quote_item.process_route'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_quote_item' AND COLUMN_NAME = 'process_route'
);
PREPARE _stmt FROM @add_quote_process_route;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

SET @add_quote_total_hours := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_quote_item ADD COLUMN total_hours DECIMAL(10,2) DEFAULT 0 COMMENT ''总工时（小时）'' AFTER process_route',
        'SELECT ''skip crm_quote_item.total_hours'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_quote_item' AND COLUMN_NAME = 'total_hours'
);
PREPARE _stmt FROM @add_quote_total_hours;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

SET @add_quote_template_id := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_quote_item ADD COLUMN template_id BIGINT DEFAULT NULL COMMENT ''引用的范本ID'' AFTER total_hours',
        'SELECT ''skip crm_quote_item.template_id'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_quote_item' AND COLUMN_NAME = 'template_id'
);
PREPARE _stmt FROM @add_quote_template_id;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

-- 4) 报价明细新增表处面积字段
SET @add_quote_surface_area := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_quote_item ADD COLUMN surface_area DECIMAL(12,4) DEFAULT NULL COMMENT ''表处面积（cm²）'' AFTER template_id',
        'SELECT ''skip crm_quote_item.surface_area'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_quote_item' AND COLUMN_NAME = 'surface_area'
);
PREPARE _stmt FROM @add_quote_surface_area;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

-- 5) 字典：计费方式
USE `cnc_platform`;

INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('QUOTE_BILLING_METHOD', 'BY_QUANTITY', '按数量', 1, '按件计费', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);

INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('QUOTE_BILLING_METHOD', 'BY_WEIGHT', '按重量', 2, '按kg计费', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);

INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('QUOTE_BILLING_METHOD', 'BY_AREA', '按面积', 3, '按cm²计费', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);

-- 6) 字典：工艺类型
INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('PROCESS_TYPE', 'LATHE', '车床', 1, '车削加工', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);

INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('PROCESS_TYPE', 'CNC', 'CNC加工', 2, '数控加工中心', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);

INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('PROCESS_TYPE', 'EDM', '放电', 3, '电火花加工', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);

INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('PROCESS_TYPE', 'WIRE_CUT', '线割', 4, '线切割加工', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);

INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('PROCESS_TYPE', 'SURFACE', '表处', 5, '表面处理（氧化/喷涂等）', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);

INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('PROCESS_TYPE', 'HEAT', '热处理', 6, '热处理加工', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);

INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('PROCESS_TYPE', 'GRINDING', '磨削', 7, '磨削加工', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);
