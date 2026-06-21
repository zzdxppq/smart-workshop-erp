-- V1.3.7 · Story 1.44 · 报表·生产工作台
CREATE TABLE IF NOT EXISTS crm_dashboard_production (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    snapshot_no VARCHAR(64) NOT NULL,
    workorder_no VARCHAR(64) NULL,
    product_name VARCHAR(128) NULL,
    workorder_status VARCHAR(16) NULL COMMENT 'PENDING/RUNNING/PAUSED/DONE',
    qty_planned INT NOT NULL DEFAULT 0,
    qty_completed INT NOT NULL DEFAULT 0,
    progress DECIMAL(5,2) NOT NULL DEFAULT 0,
    alert_type VARCHAR(16) NULL COMMENT 'INFO/WARN/CRITICAL',
    alert_message VARCHAR(500) NULL,
    snapshot_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_snapshot_no (snapshot_no),
    KEY idx_dash_status (workorder_status),
    KEY idx_dash_alert (alert_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产看板快照';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
