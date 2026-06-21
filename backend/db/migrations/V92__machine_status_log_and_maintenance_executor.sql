-- V92: 设备状态变更日志表 + 维保记录执行人字段
-- 2026-06-20

USE `cnc_business`;

-- 状态变更日志
CREATE TABLE IF NOT EXISTS prod_machine_status_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    machine_id BIGINT NOT NULL COMMENT '设备ID',
    from_status VARCHAR(20) COMMENT '原状态',
    to_status VARCHAR(20) NOT NULL COMMENT '目标状态',
    reason VARCHAR(200) COMMENT '变更原因',
    estimated_recovery_date VARCHAR(20) COMMENT '预计恢复日期',
    changed_by VARCHAR(50) COMMENT '变更人',
    changed_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
    INDEX idx_machine_id (machine_id),
    INDEX idx_changed_at (changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备状态变更日志';

-- 维保记录表补充执行人字段
SET @db := DATABASE();
SET @add_executor := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE prod_machine_maintenance ADD COLUMN executor VARCHAR(50) DEFAULT NULL COMMENT ''执行人''',
        'SELECT ''skip prod_machine_maintenance.executor'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'prod_machine_maintenance' AND COLUMN_NAME = 'executor'
);
PREPARE _stmt FROM @add_executor;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

USE `cnc_production`;

-- cnc_production 库同步状态日志表
CREATE TABLE IF NOT EXISTS prod_machine_status_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    machine_id BIGINT NOT NULL COMMENT '设备ID',
    from_status VARCHAR(20) COMMENT '原状态',
    to_status VARCHAR(20) NOT NULL COMMENT '目标状态',
    reason VARCHAR(200) COMMENT '变更原因',
    estimated_recovery_date VARCHAR(20) COMMENT '预计恢复日期',
    changed_by VARCHAR(50) COMMENT '变更人',
    changed_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
    INDEX idx_machine_id (machine_id),
    INDEX idx_changed_at (changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备状态变更日志';

-- cnc_production 库同步维保记录执行人字段
SET @db := DATABASE();
SET @add_executor_prod := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE prod_machine_maintenance ADD COLUMN executor VARCHAR(50) DEFAULT NULL COMMENT ''执行人''',
        'SELECT ''skip prod_machine_maintenance.executor'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'prod_machine_maintenance' AND COLUMN_NAME = 'executor'
);
PREPARE _stmt FROM @add_executor_prod;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;
