-- V1.3.7 · Story 1.45 · 报表·多维度看板
CREATE TABLE IF NOT EXISTS crm_dashboard_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    snapshot_no VARCHAR(64) NOT NULL,
    dimension VARCHAR(32) NOT NULL COMMENT 'SALES/PRODUCTION/FINANCE/QUALITY',
    metric_name VARCHAR(64) NOT NULL,
    metric_value DECIMAL(18,4) NULL,
    metric_unit VARCHAR(16) NULL,
    dim_dept VARCHAR(64) NULL,
    dim_category VARCHAR(64) NULL,
    dim_period VARCHAR(32) NULL,
    snapshot_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_snapshot_no (snapshot_no),
    KEY idx_dim (dimension),
    KEY idx_dim_period (dim_period)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='多维看板快照';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
