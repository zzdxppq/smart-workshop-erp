-- V1.3.7 · Story 1.50 · 委外协同·仓管到货扫码
CREATE TABLE IF NOT EXISTS crm_warehouse_incoming_scan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scan_no VARCHAR(64) NOT NULL,
    permission_no VARCHAR(64) NULL,
    user_id BIGINT NULL,
    vendor_name VARCHAR(128) NULL,
    outsource_no VARCHAR(64) NULL,
    scan_type VARCHAR(32) NULL,
    scan_status VARCHAR(32) NULL,
    total_count INT NOT NULL DEFAULT 0,
    email VARCHAR(128) NULL,
    scan_time DATETIME NOT NULL,
    confirmed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_scan_no (scan_no),
    KEY idx_permission (permission_no),
    KEY idx_user (user_id),
    KEY idx_status (scan_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓管扫码';

CREATE TABLE IF NOT EXISTS crm_warehouse_incoming_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_no VARCHAR(64) NOT NULL,
    scan_no VARCHAR(64) NOT NULL,
    barcode VARCHAR(64) NULL,
    barcode_type VARCHAR(16) NULL,
    material_code VARCHAR(64) NULL,
    material_name VARCHAR(128) NULL,
    quantity INT NOT NULL DEFAULT 0,
    batch_no VARCHAR(64) NULL,
    warehouse_location VARCHAR(64) NULL,
    remark VARCHAR(256) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_item_no (item_no),
    KEY idx_scan (scan_no),
    KEY idx_barcode (barcode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓管扫码明细';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
