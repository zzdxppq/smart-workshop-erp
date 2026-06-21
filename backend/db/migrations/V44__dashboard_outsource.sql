-- V1.3.7 · Story 1.47 · 报表·委外面板
CREATE TABLE IF NOT EXISTS crm_outsource_dashboard (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dashboard_no VARCHAR(64) NOT NULL,
    outsource_no VARCHAR(64) NULL,
    vendor_name VARCHAR(128) NULL,
    status VARCHAR(32) NULL,
    metric_type VARCHAR(32) NULL,
    metric_name VARCHAR(128) NULL,
    metric_value DECIMAL(18,2) NOT NULL DEFAULT 0,
    quality_pass_rate DECIMAL(5,2) NULL,
    alert_level VARCHAR(16) NULL,
    snapshot_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_dashboard_no (dashboard_no),
    KEY idx_status (status),
    KEY idx_metric_type (metric_type),
    KEY idx_alert (alert_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='委外看板';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
