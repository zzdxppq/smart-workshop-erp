-- ======================================================================
-- V8 · Story 1.9 BOM 多级维护 (Epic 3 / Story 3.3)
-- PO 范蠡 + SM 萧何 · 2026-06-12 · 合同 XP-ZPF202606082405 · PRD V1.3.7
-- 3 表：BOM 主表 + BOM 多级树 + BOM 历史
-- 4 P1 修补：5 级递归上限 / 物料编码唯一 / 数量正整数 / 发布后只读
-- 4 P2 修补：物料 5 段成本聚合（V1.3.4 闭环）/ 物料替代 / 多 BOM 版本 / BOM 对比
-- ======================================================================

-- 1) crm_bom BOM 主表
CREATE TABLE IF NOT EXISTS crm_bom (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bom_no VARCHAR(64) NOT NULL COMMENT 'BOM 单号 BOM{yyyyMMdd}{seq:4}（V1.3.7 红线 4 · 100 并发不重复）',
    bom_version VARCHAR(16) NOT NULL DEFAULT 'v1' COMMENT 'BOM 版本（多版本 · P2 修补）',
    drawing_id BIGINT NOT NULL COMMENT '源图纸 ID（Story 1.8 工程转化）',
    drawing_no VARCHAR(64) NOT NULL COMMENT '图号（冗余）',
    bom_type VARCHAR(32) NOT NULL DEFAULT 'STANDARD' COMMENT 'STANDARD/FA/PROTOTYPE',
    target_qty INT NOT NULL DEFAULT 1 COMMENT '目标数量（正整数 · P1 修补）',
    material_code VARCHAR(64) NOT NULL COMMENT '主物料编码（唯一 · P1 修补）',
    total_cost DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '5 段成本聚合（V1.3.4 闭环 · P2 修补）',
    cost_breakdown TEXT COMMENT '5 段成本明细 JSON',
    process_route_id BIGINT COMMENT '关联工艺路线（Story 1.10）',
    parent_bom_id BIGINT COMMENT '父 BOM ID（多级树 · 自引用 · 5 级递归上限 P1 修补）',
    bom_level INT NOT NULL DEFAULT 0 COMMENT 'BOM 层级（0=根节点 · 1-4 子节点）',
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/RELEASED/ARCHIVED（P1 修补 4：发布后只读）',
    owner_user_id BIGINT NOT NULL COMMENT '创建人（工程师）',
    released_by BIGINT COMMENT '发布人',
    released_at DATETIME COMMENT '发布时间',
    is_substitutable TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否允许物料替代（P2 修补）',
    comment TEXT COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_bom_no_version (bom_no, bom_version) COMMENT 'BOM 单号 + 版本 唯一（P2 修补：多版本）',
    UNIQUE KEY uk_material_code_version (material_code, bom_version) COMMENT '物料编码 + 版本 唯一（P1 修补 2）',
    KEY idx_drawing (drawing_id),
    KEY idx_status (status),
    KEY idx_parent (parent_bom_id),
    KEY idx_level (bom_level),
    KEY idx_owner (owner_user_id),
    KEY idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BOM 主表（Story 1.9 · Epic 3 · 5 段成本聚合）';

-- 2) crm_bom_item BOM 多级树（自引用 + 5 级递归上限）
CREATE TABLE IF NOT EXISTS crm_bom_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bom_id BIGINT NOT NULL COMMENT '所属 BOM 主表 ID',
    parent_item_id BIGINT COMMENT '父物料项 ID（自引用 · 5 级递归）',
    item_level INT NOT NULL DEFAULT 0 COMMENT '物料层级 0-4',
    item_no INT NOT NULL DEFAULT 1 COMMENT '同级排序',
    material_code VARCHAR(64) NOT NULL COMMENT '物料编码',
    material_name VARCHAR(256) NOT NULL COMMENT '物料名称',
    spec VARCHAR(256) COMMENT '规格',
    qty DECIMAL(18,4) NOT NULL DEFAULT 1 COMMENT '数量（正数 · P1 修补 3）',
    unit VARCHAR(16) NOT NULL DEFAULT 'PCS' COMMENT '单位 PCS/KG/M',
    unit_cost DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '单价',
    total_cost DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '总成本（qty * unit_cost * target_qty）',
    segment VARCHAR(32) NOT NULL DEFAULT '原材料' COMMENT '5 段：原材料/粗加工/精加工/表面处理/检验',
    substitute_materials VARCHAR(512) COMMENT '替代物料编码（多个逗号分隔 · P2 修补）',
    is_substitute TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否为替代物料',
    process_step_id BIGINT COMMENT '关联工序（Story 1.10）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_bom (bom_id),
    KEY idx_parent_item (parent_item_id),
    KEY idx_level (item_level),
    KEY idx_material (material_code),
    KEY idx_segment (segment)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BOM 多级树（5 级递归 · P1 修补 1）';

-- 3) crm_bom_history BOM 历史（P1 修补：完整变更追踪）
CREATE TABLE IF NOT EXISTS crm_bom_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bom_id BIGINT NOT NULL,
    operation VARCHAR(32) NOT NULL COMMENT 'CREATE/UPDATE/RELEASE/ARCHIVE/CONVERT_TO_PRODUCTION',
    before_json TEXT COMMENT '变更前快照',
    after_json TEXT COMMENT '变更后快照',
    work_order_no VARCHAR(64) COMMENT '转生产工单号 GD{yyyyMMdd}{seq:4}（P1 修补 4 hook）',
    changed_by BIGINT NOT NULL,
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_bom_op (bom_id, operation),
    KEY idx_work_order (work_order_no),
    KEY idx_changed (changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BOM 变更历史（含转生产工单）';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
