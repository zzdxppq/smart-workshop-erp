-- V1.3.7 · Story 1.46 · 报表·销售排行
CREATE TABLE IF NOT EXISTS crm_sales_report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_no VARCHAR(64) NOT NULL,
    customer_name VARCHAR(128) NULL,
    sales_user VARCHAR(64) NULL,
    amount DECIMAL(18,2) NOT NULL DEFAULT 0,
    order_count INT NOT NULL DEFAULT 0,
    rank_no INT NULL,
    report_period VARCHAR(32) NULL,
    snapshot_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_report_no (report_no),
    KEY idx_period (report_period),
    KEY idx_rank (rank_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售报表';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
