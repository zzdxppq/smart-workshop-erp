-- ======================================================================
-- V6 · Story 1.7 图纸与物料 (Epic 3 / Story 3.1)
-- PO 范蠡 + SM 萧何 · 2026-06-12 · 合同 XP-ZPF202606082405 · PRD V1.3.7
-- 4 状态机：DRAFT → RELEASED → ARCHIVED + OBSOLETE（被新版本替代）
-- 3 P1 修补：图号唯一索引 / 版本号严格递增 / AES-256-GCM 加密
-- ======================================================================

-- 1) crm_drawing 图纸主表
CREATE TABLE IF NOT EXISTS crm_drawing (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    drawing_no VARCHAR(64) NOT NULL COMMENT '图号 WL-XXXX (Story 1.4 5 类码 · V1.3.7 红线)',
    version VARCHAR(16) NOT NULL DEFAULT 'v1' COMMENT '当前版本（v1 < v2 < v3 严格递增，P1 修补）',
    title VARCHAR(256) NOT NULL COMMENT '图纸标题',
    material_code VARCHAR(64) NOT NULL COMMENT '物料编码 WL-XXXX（必填 + 唯一校验）',
    process_route TEXT NOT NULL COMMENT '工艺路线 JSON（5 段成本聚合 hook · V1.3.4 留 1.9 BOM Story）',
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/RELEASED/ARCHIVED/OBSOLETE',
    pdf_path VARCHAR(512) COMMENT 'PDF 路径（MinIO / 本地 filesystem）',
    signature_scan_path VARCHAR(512) COMMENT '签字扫描件路径（AES-256-GCM 加密存储 · V1.3.6 红线）',
    is_encrypted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '签字扫描件是否加密（P1 修补）',
    owner_user_id BIGINT NOT NULL COMMENT '创建人（工程师）',
    dept_id BIGINT COMMENT '部门',
    is_fa TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'FA 件（> 20万 二次密码）',
    is_new TINYINT(1) NOT NULL DEFAULT 0 COMMENT '新品',
    comment TEXT COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_drawing_no_version (drawing_no, version) COMMENT '图号 + 版本 唯一复合索引（P1 修补）',
    UNIQUE KEY uk_material_code (material_code) COMMENT '物料编码唯一索引（P1 修补）',
    KEY idx_status (status),
    KEY idx_owner (owner_user_id),
    KEY idx_dept (dept_id),
    KEY idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图纸主表（Story 1.7 · Epic 3）';

-- 2) crm_drawing_version 版本历史
CREATE TABLE IF NOT EXISTS crm_drawing_version (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    drawing_id BIGINT NOT NULL COMMENT '图纸主表 ID',
    version VARCHAR(16) NOT NULL COMMENT '版本号 v1/v2/v3（P1 修补：严格递增）',
    pdf_path VARCHAR(512) COMMENT '版本 PDF 路径',
    signature_scan_path VARCHAR(512) COMMENT '版本签字扫描件路径',
    is_encrypted TINYINT(1) NOT NULL DEFAULT 0,
    change_reason VARCHAR(512) COMMENT '变更原因',
    changed_by BIGINT NOT NULL COMMENT '变更人',
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_drawing_version (drawing_id, version),
    KEY idx_drawing (drawing_id),
    KEY idx_changed (changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图纸版本历史';

-- 3) crm_drawing_history 变更历史
CREATE TABLE IF NOT EXISTS crm_drawing_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    drawing_id BIGINT NOT NULL,
    operation VARCHAR(32) NOT NULL COMMENT 'CREATE/UPDATE/ADD_VERSION/RELEASE/ARCHIVE/OBSOLETE',
    before_json TEXT COMMENT '变更前快照',
    after_json TEXT COMMENT '变更后快照',
    changed_by BIGINT NOT NULL,
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_drawing_op (drawing_id, operation),
    KEY idx_changed (changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图纸变更历史（@AuditLog 写入）';

-- 4) crm_drawing_signature 签字扫描件
CREATE TABLE IF NOT EXISTS crm_drawing_signature (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    drawing_id BIGINT NOT NULL,
    version VARCHAR(16) NOT NULL COMMENT '签字版本',
    signer_user_id BIGINT NOT NULL COMMENT '签字人',
    signature_image_path VARCHAR(512) NOT NULL COMMENT '签字图片路径（加密后）',
    encrypted_aes_key VARCHAR(512) COMMENT 'AES 密钥（V1.3.6 红线 · 256-GCM 加密）',
    iv VARCHAR(64) COMMENT '初始化向量（IV 唯一）',
    signed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_drawing_version_signer (drawing_id, version, signer_user_id),
    KEY idx_drawing (drawing_id),
    KEY idx_signer (signer_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图纸签字扫描件（V1.3.6 红线 · AES-256-GCM）';

-- V94 · mock 清理：图纸 seed 已移至 init_data.sql
