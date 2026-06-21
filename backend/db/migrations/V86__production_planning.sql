-- ======================================================================
-- V86 · 报价与订单协同设计 V2.1 · Phase 4
-- 排产体系：待转产订单、转工单、工单生成、通知车间
-- 2026-06-20
-- ======================================================================

USE `cnc_business`;

-- 1) 排产计划表（转工单时生成）
CREATE TABLE IF NOT EXISTS `crm_production_planning` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_id` BIGINT NOT NULL COMMENT '销售订单ID',
    `workbench_id` BIGINT DEFAULT NULL COMMENT '工程转化工作台ID',
    `planning_no` VARCHAR(32) NOT NULL COMMENT '排产计划编号：SCH-YYYYMMDD-NNNN',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/ASSIGNED/SCHEDULED/CANCELLED',
    `planned_start` DATETIME DEFAULT NULL COMMENT '计划开始时间',
    `planned_end` DATETIME DEFAULT NULL COMMENT '计划结束时间',
    `actual_start` DATETIME DEFAULT NULL COMMENT '实际开始时间',
    `actual_end` DATETIME DEFAULT NULL COMMENT '实际结束时间',
    `planner_user_id` BIGINT DEFAULT NULL COMMENT '生管用户ID',
    `planner_name` VARCHAR(64) DEFAULT NULL COMMENT '生管姓名',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `cancel_reason` VARCHAR(255) DEFAULT NULL COMMENT '取消原因',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uniq_planning_no` (`planning_no`),
    INDEX `idx_planning_order` (`order_id`),
    INDEX `idx_planning_status` (`status`),
    INDEX `idx_planning_planner` (`planner_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排产计划表（V2.1）';

-- 2) 工序分配表（转工单时记录分配结果）
CREATE TABLE IF NOT EXISTS `crm_process_assignment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `planning_id` BIGINT NOT NULL COMMENT '排产计划ID',
    `sequence` INT NOT NULL DEFAULT 0 COMMENT '工序序号',
    `process_name` VARCHAR(64) NOT NULL COMMENT '工序名称',
    `machine_type` VARCHAR(64) DEFAULT NULL COMMENT '设备类型',
    `machine_id` BIGINT DEFAULT NULL COMMENT '设备ID',
    `machine_code` VARCHAR(32) DEFAULT NULL COMMENT '设备编号',
    `operator_user_id` BIGINT DEFAULT NULL COMMENT '操作工ID',
    `operator_name` VARCHAR(64) DEFAULT NULL COMMENT '操作工姓名',
    `planned_start` DATETIME DEFAULT NULL COMMENT '计划开始',
    `planned_end` DATETIME DEFAULT NULL COMMENT '计划结束',
    `actual_start` DATETIME DEFAULT NULL COMMENT '实际开始',
    `actual_end` DATETIME DEFAULT NULL COMMENT '实际结束',
    `is_outsource` TINYINT(1) DEFAULT 0 COMMENT '是否委外（0否1是）',
    `outsource_vendor_id` BIGINT DEFAULT NULL COMMENT '委外供应商ID',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    INDEX `idx_assignment_planning` (`planning_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工序分配表（V2.1）';

-- 3) 工单通知记录表
CREATE TABLE IF NOT EXISTS `crm_production_notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `planning_id` BIGINT NOT NULL COMMENT '排产计划ID',
    `workorder_id` BIGINT DEFAULT NULL COMMENT '关联工单ID',
    `workorder_no` VARCHAR(32) DEFAULT NULL COMMENT '工单编号',
    `recipient_type` VARCHAR(20) NOT NULL COMMENT '接收人类型：OPERATOR/ENGINEER/PLANNER/SUPERVISOR',
    `recipient_user_id` BIGINT DEFAULT NULL COMMENT '接收人用户ID',
    `recipient_name` VARCHAR(64) DEFAULT NULL COMMENT '接收人姓名',
    `channel` VARCHAR(20) NOT NULL COMMENT '通知渠道：APP/PUSH/SMS/EMAIL',
    `title` VARCHAR(128) NOT NULL COMMENT '通知标题',
    `content` TEXT DEFAULT NULL COMMENT '通知内容',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/SENT/READ/FAILED',
    `sent_at` DATETIME DEFAULT NULL COMMENT '发送时间',
    `read_at` DATETIME DEFAULT NULL COMMENT '阅读时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_notification_planning` (`planning_id`),
    INDEX `idx_notification_recipient` (`recipient_user_id`),
    INDEX `idx_notification_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单通知记录表（V2.1）';

-- 4) 工单表新增字段（关联工程转化数据）
SET @db := DATABASE();

SET @add_workorder_process_data := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_workorder ADD COLUMN process_data JSON DEFAULT NULL COMMENT ''工序详细数据JSON（V2.1）'' AFTER process_route_id',
        'SELECT ''skip crm_workorder.process_data'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_workorder' AND COLUMN_NAME = 'process_data'
);
PREPARE _stmt FROM @add_workorder_process_data;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

SET @add_workorder_bom_data := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_workorder ADD COLUMN bom_data JSON DEFAULT NULL COMMENT ''BOM数据JSON（V2.1）'' AFTER process_data',
        'SELECT ''skip crm_workorder.bom_data'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_workorder' AND COLUMN_NAME = 'bom_data'
);
PREPARE _stmt FROM @add_workorder_bom_data;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

SET @add_workorder_planning_id := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_workorder ADD COLUMN planning_id BIGINT DEFAULT NULL COMMENT ''关联排产计划ID（V2.1）'' AFTER bom_data',
        'SELECT ''skip crm_workorder.planning_id'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_workorder' AND COLUMN_NAME = 'planning_id'
);
PREPARE _stmt FROM @add_workorder_planning_id;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

-- 5) 字典：排产计划状态
USE `cnc_platform`;

INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('PLANNING_STATUS', 'PENDING', '待分配', 1, '待分配工序和资源', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);

INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('PLANNING_STATUS', 'ASSIGNED', '已分配', 2, '工序和资源已分配', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);

INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('PLANNING_STATUS', 'SCHEDULED', '已排产', 3, '已排入甘特图', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);

INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('PLANNING_STATUS', 'IN_PRODUCTION', '生产中', 4, '生产进行中', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);

INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('PLANNING_STATUS', 'COMPLETED', '已完成', 5, '生产完成', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);

-- 6) 字典：通知渠道
INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('NOTIFICATION_CHANNEL', 'APP', 'APP推送', 1, '通过APP推送通知', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);

INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('NOTIFICATION_CHANNEL', 'PUSH', '系统消息', 2, '系统内消息推送', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);

INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('NOTIFICATION_CHANNEL', 'SMS', '短信', 3, '短信通知', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);

INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('NOTIFICATION_CHANNEL', 'EMAIL', '邮件', 4, '邮件通知', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);
