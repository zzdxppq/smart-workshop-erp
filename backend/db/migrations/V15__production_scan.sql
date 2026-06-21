-- V1.3.7 · Story 1.16 · APP 扫码开工/报工/过站
-- 迁移：crm_production_scan 生产扫码 + crm_production_report 报工 + crm_production_station 过站

CREATE TABLE IF NOT EXISTS crm_production_scan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scan_no VARCHAR(32) NOT NULL UNIQUE,                 -- PS{yyyyMMdd}{seq:4}
    workorder_no VARCHAR(32) NOT NULL,                   -- 工单号 GD-XXXX
    scan_type VARCHAR(16) NOT NULL,                      -- START / REPORT / STATION
    operator_user_id BIGINT NOT NULL,
    equipment_id BIGINT,
    qty INT DEFAULT 0,
    step_no INT,
    scanned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    client_id VARCHAR(64),                               -- APP 客户端 ID
    sync_status VARCHAR(16) DEFAULT 'SYNCED',
    remark VARCHAR(255),
    INDEX idx_ps_workorder (workorder_no),
    INDEX idx_ps_type (scan_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS crm_production_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    report_no VARCHAR(32) NOT NULL UNIQUE,               -- RP{yyyyMMdd}{seq:4}
    workorder_no VARCHAR(32) NOT NULL,
    step_no INT NOT NULL,                                -- 工序号
    reported_qty INT NOT NULL DEFAULT 0,                 -- 报工数量
    actual_minutes INT DEFAULT 0,                        -- 实际工时（分钟）
    is_abnormal TINYINT(1) DEFAULT 0,                    -- 异常标记
    abnormal_type VARCHAR(32),                          -- 异常类型：QUALITY/EQUIPMENT/MATERIAL
    abnormal_note VARCHAR(255),
    reported_by BIGINT NOT NULL,
    reported_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_report_workorder (workorder_no),
    INDEX idx_report_step (step_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS crm_production_station (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    transfer_no VARCHAR(32) NOT NULL UNIQUE,             -- TR{yyyyMMdd}{seq:4}
    workorder_no VARCHAR(32) NOT NULL,
    from_step_no INT NOT NULL,                          -- 源工序
    to_step_no INT NOT NULL,                            -- 目标工序
    from_equipment_id BIGINT,
    to_equipment_id BIGINT,
    transferred_by BIGINT NOT NULL,
    transferred_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(255),
    INDEX idx_station_workorder (workorder_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
