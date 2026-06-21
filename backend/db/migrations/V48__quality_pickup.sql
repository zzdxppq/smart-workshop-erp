-- V1.3.7 · Story 1.51 · 委外协同·品质领料后质检
CREATE TABLE IF NOT EXISTS crm_quality_pickup (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pickup_no VARCHAR(64) NOT NULL,
    scan_no VARCHAR(64) NULL,
    inspector_id BIGINT NULL,
    inspector_name VARCHAR(64) NULL,
    vendor_name VARCHAR(128) NULL,
    pickup_type VARCHAR(32) NULL,
    inspect_status VARCHAR(32) NULL,
    total_count INT NOT NULL DEFAULT 0,
    pass_count INT NOT NULL DEFAULT 0,
    fail_count INT NOT NULL DEFAULT 0,
    email VARCHAR(128) NULL,
    pickup_time DATETIME NOT NULL,
    inspected_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_pickup_no (pickup_no),
    KEY idx_scan (scan_no),
    KEY idx_inspector (inspector_id),
    KEY idx_status (inspect_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='品质领料单';

CREATE TABLE IF NOT EXISTS crm_quality_pickup_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pickup_item_no VARCHAR(64) NOT NULL,
    pickup_no VARCHAR(64) NOT NULL,
    material_code VARCHAR(64) NULL,
    material_name VARCHAR(128) NULL,
    quantity INT NOT NULL DEFAULT 0,
    inspect_result VARCHAR(16) NULL,
    defect_desc VARCHAR(256) NULL,
    measure_value DECIMAL(18,4) NULL,
    inspect_time DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_pickup_item_no (pickup_item_no),
    KEY idx_pickup (pickup_no),
    KEY idx_result (inspect_result)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='品质领料明细';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
