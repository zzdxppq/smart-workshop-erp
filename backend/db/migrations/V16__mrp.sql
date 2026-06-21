-- V1.3.7 · Story 1.17 · MRP 物料需求分析
-- 迁移：crm_mrp_run MRP 运算记录 + crm_mrp_result MRP 结果 + crm_mrp_shortage 缺料清单

CREATE TABLE IF NOT EXISTS crm_mrp_run (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    run_no VARCHAR(32) NOT NULL UNIQUE,                  -- MR{yyyyMMdd}{seq:4}
    run_type VARCHAR(16) DEFAULT 'FULL',                 -- FULL / INCREMENTAL
    date_range_start DATE,
    date_range_end DATE,
    warehouse_ids VARCHAR(255),                          -- 仓库 ID 列表（逗号分隔）
    status VARCHAR(16) DEFAULT 'RUNNING',                -- RUNNING / COMPLETED / FAILED
    started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME,
    total_shortage INT DEFAULT 0,                        -- 缺料总数
    total_purchase_suggestion INT DEFAULT 0,             -- 建议采购总量
    triggered_by BIGINT,
    remark VARCHAR(255),
    INDEX idx_mrp_run_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS crm_mrp_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    run_id BIGINT NOT NULL,
    material_code VARCHAR(32) NOT NULL,
    material_name VARCHAR(128),
    required_qty INT NOT NULL,                           -- 需求数量
    current_stock INT DEFAULT 0,                         -- 当前库存
    on_order_qty INT DEFAULT 0,                          -- 在途数量
    shortage_qty INT DEFAULT 0,                          -- 缺料数量
    purchase_suggestion INT DEFAULT 0,                   -- 建议采购量
    supplier_id BIGINT,
    unit_cost DECIMAL(18,4) DEFAULT 0,
    total_cost DECIMAL(18,4) DEFAULT 0,
    INDEX idx_result_run (run_id),
    INDEX idx_result_material (material_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS crm_mrp_shortage (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    run_id BIGINT NOT NULL,
    material_code VARCHAR(32) NOT NULL,
    shortage_qty INT NOT NULL,
    required_date DATE,
    priority INT DEFAULT 5,
    source_workorders TEXT,                              -- 关联工单号（逗号分隔）
    INDEX idx_shortage_run (run_id),
    INDEX idx_shortage_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
