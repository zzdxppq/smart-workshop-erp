-- V1.3.7 · Story 1.14 · 安全库存与预警
-- 迁移：crm_inventory_safety 安全库存 + crm_inventory_alert 预警记录

CREATE TABLE IF NOT EXISTS crm_inventory_safety (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    material_code VARCHAR(32) NOT NULL UNIQUE,
    material_name VARCHAR(128),
    min_qty INT NOT NULL DEFAULT 0,                   -- 安全库存下限
    max_qty INT NOT NULL DEFAULT 0,                   -- 安全库存上限
    reorder_qty INT NOT NULL DEFAULT 0,               -- 补货量
    unit VARCHAR(16) DEFAULT '个',
    current_qty INT DEFAULT 0,                        -- 当前库存（来自 crm_batch 汇总）
    enabled TINYINT(1) DEFAULT 1,
    owner_user_id BIGINT,
    remark VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_safety_material (material_code),
    INDEX idx_safety_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS crm_inventory_alert (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    material_code VARCHAR(32) NOT NULL,
    alert_level VARCHAR(16) NOT NULL,                 -- INFO / WARN / ERROR / CRITICAL
    current_qty INT NOT NULL,
    min_qty INT NOT NULL,
    message VARCHAR(255),
    status VARCHAR(16) DEFAULT 'OPEN',                -- OPEN / RESOLVED / ARCHIVED
    triggered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at DATETIME,
    resolved_by BIGINT,
    resolution_note VARCHAR(255),
    notified TINYINT(1) DEFAULT 0,                    -- 是否已通知
    INDEX idx_alert_material (material_code),
    INDEX idx_alert_status (status),
    INDEX idx_alert_level (alert_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
