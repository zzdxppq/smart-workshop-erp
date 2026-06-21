-- ======================================================================
-- V9 · Story 1.10 工艺库与工序 (Epic 3 / Story 3.4)
-- PO 范蠡 + SM 萧何 · 2026-06-12 · 合同 XP-ZPF202606082405 · PRD V1.3.7
-- 3 表：crm_process 工艺 + crm_process_step 工序 + crm_process_route 工艺路线
-- 5 段成本：原材料/粗加工/精加工/表面处理/检验（V1.3.4 闭环）
-- 3 P1 修补：工序排序严格 / 机器类型匹配 / 工时非负
-- 3 P2 修补：5 段成本自动聚合 / 工艺复用 / 工艺变更历史
-- ======================================================================

-- 1) crm_process 工艺库主表
CREATE TABLE IF NOT EXISTS crm_process (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    process_code VARCHAR(64) NOT NULL COMMENT '工艺编码 PROC{yyyyMMdd}{seq:4}',
    process_name VARCHAR(256) NOT NULL COMMENT '工艺名称',
    process_type VARCHAR(32) NOT NULL DEFAULT 'STANDARD' COMMENT 'STANDARD/FA/PROTOTYPE',
    description TEXT COMMENT '工艺描述',
    total_steps INT NOT NULL DEFAULT 0 COMMENT '工序数',
    total_estimated_hours DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '总工时',
    total_cost DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '5 段成本聚合（V1.3.4 闭环）',
    cost_breakdown TEXT COMMENT '5 段成本明细 JSON',
    drawing_id BIGINT COMMENT '关联图纸（可空 · 工艺复用 · P2 修补）',
    drawing_no VARCHAR(64) COMMENT '图号（冗余）',
    is_reusable TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否可复用（P2 修补：工艺复用）',
    is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    owner_user_id BIGINT NOT NULL COMMENT '创建人',
    comment TEXT COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_process_code (process_code),
    KEY idx_type (process_type),
    KEY idx_drawing (drawing_id),
    KEY idx_active (is_active),
    KEY idx_owner (owner_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工艺库（Story 1.10 · 5 段成本聚合）';

-- 2) crm_process_step 工序库（5 段）
CREATE TABLE IF NOT EXISTS crm_process_step (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    process_id BIGINT NOT NULL COMMENT '所属工艺 ID',
    step_no INT NOT NULL COMMENT '工序序号（P1 修补 1：严格排序）',
    step_name VARCHAR(128) NOT NULL COMMENT '工序名称',
    segment VARCHAR(32) NOT NULL DEFAULT '原材料' COMMENT '5 段：原材料/粗加工/精加工/表面处理/检验',
    machine_type VARCHAR(64) COMMENT '机器类型（P1 修补 2：必须匹配）',
    machine_id BIGINT COMMENT '具体机器 ID',
    estimated_hours DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '工时（P1 修补 3：非负）',
    unit_cost DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '单价',
    description TEXT COMMENT '工序描述',
    is_quality_check TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否为质检工序',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_process_step (process_id, step_no) COMMENT '同工艺内 step_no 唯一（P1 修补 1）',
    KEY idx_process (process_id),
    KEY idx_segment (segment),
    KEY idx_machine (machine_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工序库（5 段 · 严格排序）';

-- 3) crm_process_route 工艺路线（图纸关联）
CREATE TABLE IF NOT EXISTS crm_process_route (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    drawing_id BIGINT NOT NULL COMMENT '图纸 ID',
    drawing_no VARCHAR(64) NOT NULL COMMENT '图号',
    process_id BIGINT NOT NULL COMMENT '工艺 ID',
    process_code VARCHAR(64) NOT NULL COMMENT '工艺编码（冗余）',
    version VARCHAR(16) NOT NULL DEFAULT 'v1' COMMENT '工艺路线版本',
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/RELEASED/ARCHIVED',
    released_by BIGINT COMMENT '发布人',
    released_at DATETIME COMMENT '发布时间',
    change_reason VARCHAR(512) COMMENT '变更原因（P2 修补 3：工艺变更历史）',
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_drawing_version (drawing_id, version),
    KEY idx_drawing (drawing_id),
    KEY idx_process (process_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工艺路线（图纸关联 · 变更历史）';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
