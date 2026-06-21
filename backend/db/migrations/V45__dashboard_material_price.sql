-- V1.3.7 · Story 1.48 · 报表·料号价格面板
CREATE TABLE IF NOT EXISTS crm_material_price_dashboard (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dashboard_no VARCHAR(64) NOT NULL,
    material_code VARCHAR(64) NOT NULL,
    material_name VARCHAR(128) NULL,
    vendor_name VARCHAR(128) NULL,
    price DECIMAL(18,4) NOT NULL DEFAULT 0,
    price_period VARCHAR(32) NULL,
    price_type VARCHAR(32) NULL,
    cost_total DECIMAL(18,2) NULL,
    price_trend DECIMAL(8,2) NULL,
    snapshot_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_dashboard_no (dashboard_no),
    UNIQUE KEY uk_material_price_slice (material_code, price_type, price_period, vendor_name),
    KEY idx_vendor (vendor_name),
    KEY idx_period (price_period),
    KEY idx_material_type (material_code, price_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='料号价格看板';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
