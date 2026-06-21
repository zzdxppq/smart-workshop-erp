-- V1.3.7 · Story 1.15 · 工单与排产 · Epic 5
-- 迁移：crm_workorder 工单 + crm_workorder_step 工序 + crm_production_schedule 排产

CREATE TABLE IF NOT EXISTS crm_workorder (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    workorder_no VARCHAR(32) NOT NULL UNIQUE,            -- GD{yyyyMMdd}{seq:4}
    drawing_id BIGINT,
    bom_id BIGINT,
    process_route_id BIGINT,
    material_code VARCHAR(32) NOT NULL,                 -- 物料编码（成品 CP-XXXX）
    product_name VARCHAR(128),
    qty INT NOT NULL DEFAULT 1,
    unit VARCHAR(16) DEFAULT '台',
    priority INT DEFAULT 5,                              -- 1=紧急 ~ 10=低
    status VARCHAR(16) DEFAULT 'DRAFT',                 -- DRAFT/SCHEDULED/IN_PROGRESS/COMPLETED/CANCELLED
    scheduled_start DATETIME,
    scheduled_end DATETIME,
    actual_start DATETIME,
    actual_end DATETIME,
    equipment_id BIGINT,                                -- 机台 ID
    equipment_type VARCHAR(32),                         -- 机台类型
    estimated_hours DECIMAL(10,2) DEFAULT 0,             -- 预计工时
    actual_hours DECIMAL(10,2) DEFAULT 0,
    is_fa TINYINT(1) DEFAULT 0,
    created_by BIGINT,
    owner_user_id BIGINT,
    dept_id BIGINT DEFAULT 10,
    remark VARCHAR(255),
    sales_order_id BIGINT NULL COMMENT '销售订单ID',
    sales_order_no VARCHAR(32) NULL COMMENT '销售订单号 XS',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_workorder_status (status),
    INDEX idx_workorder_equipment (equipment_id),
    INDEX idx_workorder_priority (priority),
    INDEX idx_workorder_sales_order (sales_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS crm_workorder_step (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    workorder_id BIGINT NOT NULL,
    step_no INT NOT NULL,                                -- 工序号 1/2/3
    step_name VARCHAR(64) NOT NULL,
    equipment_type VARCHAR(32),
    estimated_minutes INT DEFAULT 0,
    actual_minutes INT DEFAULT 0,
    status VARCHAR(16) DEFAULT 'PENDING',                -- PENDING/IN_PROGRESS/COMPLETED
    started_at DATETIME,
    completed_at DATETIME,
    operator_user_id BIGINT,
    INDEX idx_step_workorder (workorder_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS crm_production_schedule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    schedule_no VARCHAR(32) NOT NULL UNIQUE,             -- SCH{yyyyMMdd}{seq:4}
    workorder_id BIGINT NOT NULL,
    equipment_id BIGINT NOT NULL,
    equipment_type VARCHAR(32),
    plan_start DATETIME NOT NULL,
    plan_end DATETIME NOT NULL,
    status VARCHAR(16) DEFAULT 'PLANNED',                -- PLANNED/IN_PROGRESS/COMPLETED/CONFLICT
    conflict_with BIGINT,                                -- 冲突工单 ID
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_schedule_equipment (equipment_id),
    INDEX idx_schedule_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
