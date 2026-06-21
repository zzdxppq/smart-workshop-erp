-- V1.3.7 · Story 1.18 · 委外下单基础
-- 迁移：crm_outsource_order 委外单 + crm_outsource_item 委外明细 + crm_outsource_history 委外历史

CREATE TABLE IF NOT EXISTS crm_outsource_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    outsource_no VARCHAR(32) NOT NULL UNIQUE,            -- WW{yyyyMMdd}{seq:4}（复用 1.4 prefix）
    workorder_no VARCHAR(32),                            -- 关联工单
    step_no INT,                                        -- 工序号
    supplier_id BIGINT NOT NULL,
    supplier_name VARCHAR(128),
    process_name VARCHAR(64),
    material_code VARCHAR(32),                          -- 委外物料
    drawing_id BIGINT NULL COMMENT '加工图纸 ID（crm_drawing.id）',
    qty INT NOT NULL DEFAULT 0,
    unit_price DECIMAL(18,4) DEFAULT 0,                 -- 单价
    total_amount DECIMAL(18,4) DEFAULT 0,               -- 总金额 = unit_price × qty
    delivery_date DATE,                                  -- 交期
    status VARCHAR(16) DEFAULT 'DRAFT',                 -- DRAFT/SENT/ACCEPTED/IN_PRODUCTION/INSPECTED/COMPLETED/CLOSED/REWORK
    rework_count INT DEFAULT 0,                         -- 返修次数（≤ 3）
    creator_user_id BIGINT,
    submitted_at DATETIME,
    accepted_at DATETIME,
    completed_at DATETIME,
    closed_at DATETIME,
    is_urgent TINYINT(1) DEFAULT 0,                      -- 加急
    remark VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_outsource_status (status),
    INDEX idx_outsource_supplier (supplier_id),
    INDEX idx_outsource_workorder (workorder_no),
    INDEX idx_outsource_drawing (drawing_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS crm_outsource_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    outsource_no VARCHAR(32) NOT NULL,
    item_seq INT NOT NULL,
    material_code VARCHAR(32) NOT NULL,
    material_name VARCHAR(128),
    spec VARCHAR(255),
    qty INT NOT NULL,
    unit VARCHAR(16) DEFAULT '个',
    unit_price DECIMAL(18,4) DEFAULT 0,
    total_amount DECIMAL(18,4) DEFAULT 0,
    delivery_date DATE,
    remark VARCHAR(255),
    INDEX idx_item_outsource (outsource_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS crm_outsource_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    outsource_no VARCHAR(32) NOT NULL,
    operation VARCHAR(32) NOT NULL,                     -- CREATE/SUBMIT/ACCEPT/START/INSPECT/COMPLETE/CLOSE/REWORK
    operator_user_id BIGINT,
    operated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    from_status VARCHAR(16),
    to_status VARCHAR(16),
    note VARCHAR(255),
    INDEX idx_history_outsource (outsource_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
