-- ============================================================
-- V62 · E5-S5 设备机台 prod_machine + E5-S6 工单工序 crm_workorder_process
-- erp-production · cnc_production（V60 同步）
-- ============================================================

USE `cnc_business`;

-- 1) 设备台账
CREATE TABLE IF NOT EXISTS prod_machine (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    machine_code VARCHAR(32) NOT NULL COMMENT '设备码 SB-{type}-{seq}',
    machine_name VARCHAR(128) NOT NULL COMMENT '设备名称',
    machine_type VARCHAR(32) NOT NULL COMMENT '设备类型 CNC/LATHE/MILLING 等',
    machine_no VARCHAR(32) COMMENT '机台号',
    status VARCHAR(16) NOT NULL DEFAULT 'IDLE' COMMENT 'IDLE/RUNNING/MAINTENANCE/FAULT',
    last_maintenance DATETIME COMMENT '上次维护时间',
    maintenance_cycle_days INT DEFAULT 90 COMMENT '维护周期（天）',
    remark VARCHAR(255) COMMENT '备注',
    is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_machine_code (machine_code),
    KEY idx_machine_type (machine_type),
    KEY idx_machine_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备机台台账（E5-S5 · prod_machine）';

-- 2) 机台日负荷
CREATE TABLE IF NOT EXISTS prod_machine_load (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    machine_id BIGINT NOT NULL COMMENT '机台ID',
    load_date DATE NOT NULL COMMENT '负荷日期',
    planned_hours DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '已排工时',
    available_hours DECIMAL(10,2) NOT NULL DEFAULT 12 COMMENT '可用工时',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_machine_date (machine_id, load_date),
    KEY idx_load_date (load_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机台日负荷（FR-5-4-2）';

-- 3) 维护记录
CREATE TABLE IF NOT EXISTS prod_machine_maintenance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    machine_id BIGINT NOT NULL COMMENT '机台ID',
    maintenance_type VARCHAR(32) NOT NULL COMMENT 'ROUTINE/PREVENTIVE/REPAIR',
    performed_at DATETIME NOT NULL COMMENT '执行时间',
    next_due DATETIME COMMENT '下次到期',
    remark VARCHAR(255) COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_machine_maint (machine_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备维护记录';

-- 4) 工单工序（操作工/图纸关联 · 补齐 crm_workorder_process）
CREATE TABLE IF NOT EXISTS crm_workorder_process (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    workorder_id BIGINT NOT NULL COMMENT '工单ID',
    workorder_no VARCHAR(32) NOT NULL COMMENT '工单号',
    process_seq INT NOT NULL COMMENT '工序序号',
    process_code VARCHAR(64) COMMENT '工序编码',
    process_name VARCHAR(64) NOT NULL COMMENT '工序名称',
    material_code VARCHAR(32) COMMENT '物料编码',
    machine_id BIGINT COMMENT '绑定机台ID',
    locked_machine_id BIGINT COMMENT '锁定下一机台ID',
    is_outsource TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否委外工序',
    status VARCHAR(16) DEFAULT 'PENDING' COMMENT 'PENDING/IN_PROGRESS/COMPLETED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_wo_process (workorder_id, process_seq),
    KEY idx_wo_no (workorder_no),
    KEY idx_material (material_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单工序（E5-S6 · 操作工扫码）';

-- 5) 工单工序表扩展机台字段（幂等）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_workorder_step' AND COLUMN_NAME = 'machine_id');
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE crm_workorder_step ADD COLUMN machine_id BIGINT COMMENT ''机台ID'' AFTER equipment_type, ADD COLUMN locked_machine_id BIGINT COMMENT ''锁定下一机台ID'' AFTER machine_id, ADD COLUMN is_outsource TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''是否委外'' AFTER locked_machine_id',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
