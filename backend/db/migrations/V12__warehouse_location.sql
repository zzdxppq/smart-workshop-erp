-- V1.3.7 · Story 1.13 · 库位批次与多仓库
-- 迁移：crm_warehouse 仓库 + crm_warehouse_location 库位（升级版） + crm_batch 批次

CREATE TABLE IF NOT EXISTS crm_warehouse (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    warehouse_code VARCHAR(32) NOT NULL UNIQUE,
    warehouse_name VARCHAR(64) NOT NULL,
    warehouse_type VARCHAR(16) NOT NULL,               -- MAIN / SUB / LINE_SIDE
    address VARCHAR(255),
    manager_user_id BIGINT,
    is_active TINYINT(1) DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_warehouse_type (warehouse_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS crm_batch (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_no VARCHAR(64) NOT NULL UNIQUE,              -- B{yyyyMMdd}{seq:6}
    material_code VARCHAR(32) NOT NULL,
    supplier_id BIGINT,
    supplier_name VARCHAR(128),
    qty INT NOT NULL DEFAULT 0,
    received_at DATETIME,
    expired_at DATETIME,
    quality_status VARCHAR(16) DEFAULT 'PENDING',     -- PENDING / PASSED / FAILED
    location_code VARCHAR(32),
    fefo_order INT DEFAULT 0,                          -- FEFO 先入先出排序
    remark VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_batch_material (material_code),
    INDEX idx_batch_supplier (supplier_id),
    INDEX idx_batch_quality (quality_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
