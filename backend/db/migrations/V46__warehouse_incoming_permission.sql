-- V1.3.7 · Story 1.49 · 委外协同·仓管到货扫码权限
CREATE TABLE IF NOT EXISTS crm_warehouse_incoming_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    permission_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    user_name VARCHAR(64) NULL,
    role VARCHAR(32) NULL,
    permission_type VARCHAR(32) NULL,
    valid_from DATETIME NOT NULL,
    valid_to DATETIME NOT NULL,
    granted_by VARCHAR(64) NULL,
    status VARCHAR(16) NULL,
    email VARCHAR(128) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_permission_no (permission_no),
    KEY idx_user (user_id),
    KEY idx_status (status),
    KEY idx_valid (valid_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓管扫码权限';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
