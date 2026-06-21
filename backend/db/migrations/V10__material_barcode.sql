-- V1.3.7 · Story 1.11 · 物料条码生成 · Epic 4
-- 迁移：crm_material_barcode 物料条码 + crm_barcode_history 扫码历史
-- 继承 Story 1.4 (5 类码) + 1.7 (DocNoGenerator) + 1.9 (BOM 多级树)

-- 1. 物料分类表（5 段：原材料/外购件/自制件/委外件/成品）
CREATE TABLE IF NOT EXISTS crm_material_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_code VARCHAR(32) NOT NULL UNIQUE,         -- 分类编码：MAT-RAW / MAT-BUY / MAT-MFG / MAT-OUT / MAT-FIN
    category_name VARCHAR(64) NOT NULL,                 -- 分类名称
    prefix VARCHAR(8) NOT NULL,                        -- 条码 prefix：WL-原材料 / WJ-外购 / ZZ-自制 / WW-委外 / CP-成品
    seq_no INT DEFAULT 0,                               -- 排序
    is_active TINYINT(1) DEFAULT 1,
    remark VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category_code (category_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 物料主数据（继承 1.4 / 1.7 / 1.9）
CREATE TABLE IF NOT EXISTS crm_material (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    material_code VARCHAR(32) NOT NULL UNIQUE,          -- WL-XXXX / WJ-XXXX / ZZ-XXXX / WW-XXXX / CP-XXXX
    material_name VARCHAR(128) NOT NULL,
    spec VARCHAR(255),                                  -- 规格
    unit VARCHAR(16) DEFAULT '个',
    category_id BIGINT,                                 -- FK crm_material_category
    process_id BIGINT,                                  -- FK crm_process（工艺路线）
    cost_material DECIMAL(18,4) DEFAULT 0,              -- 5 段成本
    cost_labor DECIMAL(18,4) DEFAULT 0,
    cost_machine DECIMAL(18,4) DEFAULT 0,
    cost_overhead DECIMAL(18,4) DEFAULT 0,
    cost_outsource DECIMAL(18,4) DEFAULT 0,
    cost_total DECIMAL(18,4) DEFAULT 0,                 -- 5 段总成本
    is_active TINYINT(1) DEFAULT 1,
    owner_user_id BIGINT,
    dept_id BIGINT DEFAULT 10,
    remark VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_material_category (category_id),
    INDEX idx_material_process (process_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 物料条码主表（核心表）
CREATE TABLE IF NOT EXISTS crm_material_barcode (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    barcode_no VARCHAR(32) NOT NULL UNIQUE,             -- BC{yyyyMMdd}{seq:4}（DocNoGenerator）
    material_code VARCHAR(32) NOT NULL,                 -- WL-XXXX（FK crm_material）
    spec VARCHAR(255),
    payload TEXT,                                       -- AES-256-GCM 加密 payload（material_code + spec + process_id + cost + batch）
    process_id BIGINT,                                  -- 工艺 ID
    cost_breakdown VARCHAR(1024),                       -- JSON 5 段成本
    batch_no VARCHAR(64),                               -- 批次号（1.13 联动）
    qty INT DEFAULT 1,                                  -- 数量（默认 1）
    qr_code_url VARCHAR(512),                           -- 二维码 base64（P2 修补）
    status VARCHAR(16) DEFAULT 'ACTIVE',                -- ACTIVE / USED / DISCARDED
    generated_by BIGINT,
    generated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_barcode_material (material_code),
    INDEX idx_barcode_status (status),
    INDEX idx_barcode_batch (batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. 条码扫码历史
CREATE TABLE IF NOT EXISTS crm_barcode_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    barcode_no VARCHAR(32) NOT NULL,                    -- FK crm_material_barcode
    scan_user_id BIGINT NOT NULL,
    scan_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    scan_location VARCHAR(128),                         -- 扫码地点（如：A 仓 / B 仓 / 线边仓）
    scan_type VARCHAR(16) NOT NULL,                     -- GENERATE / PARSE / INBOUND / OUTBOUND / VERIFY
    scan_result VARCHAR(16) DEFAULT 'SUCCESS',          -- SUCCESS / FAILED
    error_msg VARCHAR(255),
    client_type VARCHAR(16) DEFAULT 'WEB',              -- WEB / ANDROID / IOS
    remark VARCHAR(255),
    INDEX idx_history_barcode (barcode_no),
    INDEX idx_history_user (scan_user_id),
    INDEX idx_history_at (scan_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
