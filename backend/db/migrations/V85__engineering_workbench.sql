-- ======================================================================
-- V85 · 报价与订单协同设计 V2.1 · Phase 3
-- 工程转化工作台 + 工艺明细化 + BOM编制
-- 2026-06-20
-- ======================================================================

USE `cnc_business`;

-- 1) 工程转化工作台表（订单维度的工程转化）
CREATE TABLE IF NOT EXISTS `crm_engineering_workbench` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `order_item_id` BIGINT DEFAULT NULL COMMENT '订单明细ID（可选，NULL表示整单）',
    `drawing_no` VARCHAR(64) NOT NULL COMMENT '图号',
    `material_no` VARCHAR(64) DEFAULT NULL COMMENT '料号',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/IN_PROGRESS/COMPLETED',
    `process_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '工艺状态：PENDING/IN_PROGRESS/COMPLETED',
    `bom_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'BOM状态：PENDING/IN_PROGRESS/COMPLETED',
    `engineer_user_id` BIGINT DEFAULT NULL COMMENT '负责工程师ID',
    `engineer_name` VARCHAR(64) DEFAULT NULL COMMENT '负责工程师姓名',
    `process_detail` JSON DEFAULT NULL COMMENT '详细工艺参数JSON',
    `bom_detail` JSON DEFAULT NULL COMMENT 'BOM明细JSON',
    `total_hours` DECIMAL(10,2) DEFAULT 0 COMMENT '总工时（小时）',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `started_at` DATETIME DEFAULT NULL COMMENT '开始时间',
    `completed_at` DATETIME DEFAULT NULL COMMENT '完成时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uniq_order_item` (`order_id`, `order_item_id`),
    INDEX `idx_workbench_order` (`order_id`),
    INDEX `idx_workbench_status` (`status`),
    INDEX `idx_workbench_engineer` (`engineer_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工程转化工作台表（V2.1）';

-- 2) 工艺明细表（详细工艺参数）
CREATE TABLE IF NOT EXISTS `crm_process_detail` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `workbench_id` BIGINT NOT NULL COMMENT '工作台ID',
    `sequence` INT NOT NULL DEFAULT 0 COMMENT '工序序号',
    `process_code` VARCHAR(32) DEFAULT NULL COMMENT '工序编码',
    `process_name` VARCHAR(64) NOT NULL COMMENT '工序名称',
    `machine_type` VARCHAR(64) DEFAULT NULL COMMENT '设备类型',
    `machine_id` BIGINT DEFAULT NULL COMMENT '设备ID',
    `spindle_speed` INT DEFAULT NULL COMMENT '转速（rpm）',
    `feed_rate` DECIMAL(10,4) DEFAULT NULL COMMENT '进给（mm/min）',
    `cutting_depth` DECIMAL(10,2) DEFAULT NULL COMMENT '切削深度（mm）',
    `tool_no` VARCHAR(32) DEFAULT NULL COMMENT '刀具号',
    `tool_spec` VARCHAR(64) DEFAULT NULL COMMENT '刀具规格',
    `fixture` VARCHAR(128) DEFAULT NULL COMMENT '工装夹具',
    `unit_time_minutes` INT DEFAULT 0 COMMENT '单位工时（分钟）',
    `outsource_flag` TINYINT(1) DEFAULT 0 COMMENT '是否委外（0否1是）',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    INDEX `idx_process_workbench` (`workbench_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工艺明细表（V2.1）';

-- 3) BOM子件明细表
CREATE TABLE IF NOT EXISTS `crm_bom_detail` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `workbench_id` BIGINT NOT NULL COMMENT '工作台ID',
    `item_type` VARCHAR(20) NOT NULL COMMENT '子件类型：MATERIAL/TOOL/PIN/SOCKET/CONSUMABLE/PACKAGE',
    `sequence` INT NOT NULL DEFAULT 0 COMMENT '序号',
    `material_code` VARCHAR(64) DEFAULT NULL COMMENT '物料编码',
    `material_name` VARCHAR(128) NOT NULL COMMENT '物料名称',
    `spec` VARCHAR(128) DEFAULT NULL COMMENT '规格',
    `quantity` DECIMAL(12,4) NOT NULL DEFAULT 1 COMMENT '用量',
    `unit` VARCHAR(16) DEFAULT '个' COMMENT '单位',
    `source` VARCHAR(20) DEFAULT 'STOCK' COMMENT '来源：STOCK/PURCHASE/OUTSOURCE',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    INDEX `idx_bom_detail_workbench` (`workbench_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BOM子件明细表（V2.1）';

-- 4) 字典：工程转化工作台状态
USE `cnc_platform`;

INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('ENGINEERING_STATUS', 'PENDING', '待处理', 1, '工程师待处理', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);

INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('ENGINEERING_STATUS', 'IN_PROGRESS', '进行中', 2, '工程师正在处理', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);

INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('ENGINEERING_STATUS', 'COMPLETED', '已完成', 3, '工程转化已完成', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);

-- 5) 字典：BOM子件类型
INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('BOM_ITEM_TYPE', 'MATERIAL', '原材料', 1, '原材料/毛坯', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);

INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('BOM_ITEM_TYPE', 'TOOL', '刀具', 2, '刀具/刀片', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);

INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('BOM_ITEM_TYPE', 'PIN', 'PIN针', 3, 'PIN针/连接件', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);

INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('BOM_ITEM_TYPE', 'SOCKET', '牙套', 4, '牙套/螺套', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);

INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('BOM_ITEM_TYPE', 'CONSUMABLE', '辅料', 5, '辅料（切削液/润滑油等）', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);

INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('BOM_ITEM_TYPE', 'PACKAGE', '包装', 6, '包装材料', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);
