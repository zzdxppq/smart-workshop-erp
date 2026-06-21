-- ======================================================================
-- V7 · Story 1.8 工程转化 (Epic 3 / Story 3.2)
-- PO 范蠡 + SM 萧何 · 2026-06-12 · 合同 XP-ZPF202606082405 · PRD V1.3.7
-- 工程转化表 + 标注表 + 5 状态机（DRAFT/RELEASED/ARCHIVED/OBSOLETE/CONVERTED）
-- 3 P1 修补：转化锁定原版本 / 标注不可修改（只追加）/ 转化结果 PDF 签字扫描件复用 1.7
-- 3 P2 修补：标注 SVG 嵌入（部署阶段）/ 转化历史 timeline / 工程师工作量统计 hook
-- ======================================================================

-- 1) crm_drawing_conversion 工程转化表（锁定原版本 + CONVERTED 状态）
CREATE TABLE IF NOT EXISTS crm_drawing_conversion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    drawing_id BIGINT NOT NULL COMMENT '图纸主表 ID',
    drawing_no VARCHAR(64) NOT NULL COMMENT '图号（冗余）',
    locked_version VARCHAR(16) NOT NULL COMMENT '锁定原版本（P1 修补：防回写）',
    bom_no VARCHAR(64) COMMENT '下游 BOM 单号 BOM{yyyyMMdd}{seq:4}',
    bom_type VARCHAR(32) NOT NULL DEFAULT 'STANDARD' COMMENT 'STANDARD/FA/PROTOTYPE',
    target_qty INT NOT NULL DEFAULT 1 COMMENT '目标数量（正整数 · P1 修补）',
    total_cost DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '5 段成本聚合总和',
    engineer_user_id BIGINT NOT NULL COMMENT '工程师 ID',
    engineer_name VARCHAR(64) COMMENT '工程师姓名（PDF 水印用）',
    status VARCHAR(16) NOT NULL DEFAULT 'CONVERTED' COMMENT 'CONVERTED/FAILED',
    error_message VARCHAR(1024) COMMENT '失败原因',
    process_route_snapshot TEXT COMMENT '工艺路线快照（5 段）',
    cost_breakdown TEXT COMMENT '5 段成本明细 JSON',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_drawing_version (drawing_id, locked_version) COMMENT '图+版本 唯一（防重复转化）',
    KEY idx_drawing (drawing_id),
    KEY idx_bom_no (bom_no),
    KEY idx_status (status),
    KEY idx_engineer (engineer_user_id),
    KEY idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工程转化记录（Story 1.8 · 锁定原版本 · 5 段成本聚合）';

-- 2) crm_drawing_annotation 标注表（挂载版本 + 不可修改只追加）
CREATE TABLE IF NOT EXISTS crm_drawing_annotation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    drawing_id BIGINT NOT NULL COMMENT '图纸主表 ID',
    drawing_no VARCHAR(64) NOT NULL COMMENT '图号（冗余）',
    version VARCHAR(16) NOT NULL COMMENT '挂载版本（P1 修补：防 v1→v2 标注丢失）',
    type VARCHAR(32) NOT NULL COMMENT 'DIMENSION/TOLERANCE/PROCESS_REQ/TECH_NOTE',
    content TEXT NOT NULL COMMENT '标注内容（必填 · 至少 1 字符）',
    color VARCHAR(16) NOT NULL DEFAULT 'RED' COMMENT 'RED/YELLOW/BLUE/GREEN',
    x DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT 'PDF 视口 X 坐标',
    y DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT 'PDF 视口 Y 坐标',
    width DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '标注框宽',
    height DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '标注框高',
    priority INT NOT NULL DEFAULT 5 COMMENT '1-10（10 最高）',
    is_archived TINYINT(1) NOT NULL DEFAULT 0 COMMENT '归档标记（新增 v2 时 v1 自动归档）',
    svg_data TEXT COMMENT 'SVG 嵌入数据（P2 修补 · 部署阶段）',
    created_by BIGINT NOT NULL COMMENT '创建人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_drawing_version_type_xy (drawing_id, version, type, x, y) COMMENT '位置唯一（防重叠）',
    KEY idx_drawing (drawing_id),
    KEY idx_version (version),
    KEY idx_type (type),
    KEY idx_priority (priority),
    KEY idx_creator (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图纸标注（4 类型 · 挂载版本 · 不可修改只追加）';

-- 3) crm_drawing_annotation_history 标注历史（P1 修补：只追加不留痕 + 工程师工作量统计 hook）
CREATE TABLE IF NOT EXISTS crm_drawing_annotation_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    annotation_id BIGINT NOT NULL COMMENT '标注 ID',
    drawing_id BIGINT NOT NULL,
    operation VARCHAR(16) NOT NULL COMMENT 'CREATE/ARCHIVE',
    actor_user_id BIGINT NOT NULL COMMENT '操作人',
    snapshot TEXT COMMENT '标注快照 JSON',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_annotation (annotation_id),
    KEY idx_drawing (drawing_id),
    KEY idx_actor (actor_user_id),
    KEY idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标注历史 + 工程师工作量统计 hook';

-- 4) crm_engineer_workload 工程师工作量统计 hook（P2 修补）
CREATE TABLE IF NOT EXISTS crm_engineer_workload (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '工程师 ID',
    user_name VARCHAR(64) NOT NULL,
    work_date DATE NOT NULL COMMENT '工作日',
    annotation_count INT NOT NULL DEFAULT 0 COMMENT '当日标注数',
    conversion_count INT NOT NULL DEFAULT 0 COMMENT '当日转化数',
    drawing_created_count INT NOT NULL DEFAULT 0 COMMENT '当日创建图纸数',
    UNIQUE KEY uk_user_date (user_id, work_date),
    KEY idx_user (user_id),
    KEY idx_date (work_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工程师工作量统计（P2 修补 · 部署阶段触发）';

-- V94 · mock 清理：工程转化/标注 seed 已移至 init_data.sql
