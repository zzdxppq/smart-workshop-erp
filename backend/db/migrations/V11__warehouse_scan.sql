-- V1.3.7 · Story 1.12 · APP 扫码出入库
-- 迁移：crm_warehouse_scan 扫码记录 + crm_warehouse_location 库位（1.13 共享）
-- 复用 1.4 5 类码 prefix（WL-物料码 / WW-委外单码）

CREATE TABLE IF NOT EXISTS crm_warehouse_location (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    location_code VARCHAR(32) NOT NULL UNIQUE,          -- LOC-A01-01-01
    warehouse VARCHAR(32) NOT NULL,                    -- 仓库：WH-A 主仓 / WH-B 副仓 / WH-C 线边仓
    zone VARCHAR(32),                                  -- 库区：A01 / B02
    position VARCHAR(32),                              -- 库位：01 / 02
    capacity DECIMAL(18,4) DEFAULT 0,                  -- 库容
    is_active TINYINT(1) DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_location_warehouse (warehouse),
    INDEX idx_location_zone (zone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS crm_warehouse_scan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scan_no VARCHAR(32) NOT NULL UNIQUE,               -- SC{yyyyMMdd}{seq:4}
    scan_type VARCHAR(16) NOT NULL,                    -- INBOUND / OUTBOUND
    barcode_no VARCHAR(32) NOT NULL,                   -- 条码号
    material_code VARCHAR(32) NOT NULL,
    location_code VARCHAR(32),                         -- 库位
    qty INT NOT NULL DEFAULT 1,                        -- 数量
    workorder_no VARCHAR(32),                          -- 出库关联工单
    batch_no VARCHAR(64),                              -- 批次号
    client_id VARCHAR(64),                             -- APP 客户端 ID（离线同步用）
    sync_status VARCHAR(16) DEFAULT 'SYNCED',          -- SYNCED / PENDING / FAILED
    scanned_by BIGINT NOT NULL,
    scanned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    synced_at DATETIME,
    conflict_type VARCHAR(32),                         -- QTY_OVERFLOW / LOCATION_MISMATCH / DUPLICATE_SCAN
    conflict_resolution VARCHAR(32),                   -- LOCAL_OVERRIDE / SERVER_OVERRIDE / MANUAL
    remark VARCHAR(255),
    INDEX idx_scan_barcode (barcode_no),
    INDEX idx_scan_type (scan_type),
    INDEX idx_scan_status (sync_status),
    INDEX idx_scan_client (client_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
