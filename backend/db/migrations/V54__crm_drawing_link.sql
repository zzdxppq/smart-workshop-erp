-- ============================================================
-- V1.3.9 Sprint 12 Story 12.1 · V54__crm_drawing_link.sql
-- 图纸权限矩阵 + 图纸-业务单据关联表 + 灰度 feature flag
--
-- 关联 Story：12.1-drawing-permission-matrix
-- 架构评审：docs/architecture/story-reviews/12.1-review.md（APPROVED · 9.1/10）
-- QA 测例：docs/qa/reviews/12.1-drawing-permission-matrix.md（24 测例）
-- 与 12.4 共 sys_print_log · parallel_group A
--
-- 设计要点：
--   1. 两套 ACL 完全独立命名空间（draw:* vs sys_global_threshold）
--   2. 7 角色 × 5 操作矩阵 · SpEL @drawingAuthz.canView 集中组件
--   3. crm_drawing_link 五元组（drawing_id + biz_type + biz_id）· ON DELETE RESTRICT
--   4. 灰度 feature flag 默认全 false · 灰度期间 admin 手动开
--   5. 错误码统一 40304（覆盖 FINANCE/SALES/OPERATOR 拒绝场景）
-- ============================================================

USE `cnc_business`;

-- ------------------------------------------------------------
-- 1. crm_drawing_link 关联表（V1.3.9 12.1 新增）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS crm_drawing_link (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    drawing_id BIGINT NOT NULL COMMENT '图纸 ID（crm_drawing.id）',
    biz_type VARCHAR(20) NOT NULL COMMENT '业务类型 ORDER/PO/INCOMING/INSPECTION/WORKORDER_PROCESS',
    biz_id BIGINT NOT NULL COMMENT '业务单据 ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人（admin/system）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_biz_ref (biz_type, biz_id, drawing_id) COMMENT '业务单据 + 图纸 唯一',
    KEY idx_drawing_link (drawing_id, biz_type, biz_id) COMMENT '图纸 → 业务单据 反向查询',
    KEY idx_biz_lookup (biz_type, biz_id) COMMENT '业务单据 → 图纸 正向查询',
    CONSTRAINT fk_draw_link_drawing FOREIGN KEY (drawing_id) REFERENCES crm_drawing(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V1.3.9 12.1 图纸与业务单据关联表';

-- ------------------------------------------------------------
-- 2. 备份表（data migration 前自动建立）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS crm_drawing_link_backup (
    id BIGINT NOT NULL,
    drawing_id BIGINT NOT NULL,
    biz_type VARCHAR(20) NOT NULL,
    biz_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    created_by BIGINT NOT NULL,
    backup_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '备份时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_backup_biz_ref (biz_type, biz_id, drawing_id),
    KEY idx_backup_drawing (drawing_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V1.3.9 12.1 图纸关联备份表（V54 末尾填充 · 用于回滚）';

-- ------------------------------------------------------------
-- 3. sys_dict DRAWING_SCOPE（7 角色 scope 配置）
-- ------------------------------------------------------------
USE `cnc_platform`;

INSERT IGNORE INTO sys_dict (dict_type, dict_code, dict_label, sort, status) VALUES
  ('DRAWING_SCOPE', 'ENGINEER',     'ALL',               1, 'ACTIVE'),
  ('DRAWING_SCOPE', 'PROD_PLANNER', 'ALL',               2, 'ACTIVE'),
  ('DRAWING_SCOPE', 'SALES',        'ORDER',             3, 'ACTIVE'),
  ('DRAWING_SCOPE', 'PURCHASER',    'PO',                4, 'ACTIVE'),
  ('DRAWING_SCOPE', 'WAREHOUSE',    'INCOMING',          5, 'ACTIVE'),
  ('DRAWING_SCOPE', 'QC',           'INSPECTION',        6, 'ACTIVE'),
  ('DRAWING_SCOPE', 'OPERATOR',     'WORKORDER_PROCESS', 7, 'ACTIVE'),
  ('DRAWING_SCOPE', 'FINANCE',      'NONE',              8, 'ACTIVE');

-- ------------------------------------------------------------
-- 4. sys_dict DRAWING_ACL_FEATURE_FLAG（7 角色灰度开关 · 默认全 false）
-- ------------------------------------------------------------
INSERT IGNORE INTO sys_dict (dict_type, dict_code, dict_label, sort, status) VALUES
  ('DRAWING_ACL_FEATURE_FLAG', 'draw.acl.gray.ENGINEER',     'false', 1, 'ACTIVE'),
  ('DRAWING_ACL_FEATURE_FLAG', 'draw.acl.gray.PROD_PLANNER', 'false', 2, 'ACTIVE'),
  ('DRAWING_ACL_FEATURE_FLAG', 'draw.acl.gray.SALES',        'false', 3, 'ACTIVE'),
  ('DRAWING_ACL_FEATURE_FLAG', 'draw.acl.gray.PURCHASER',    'false', 4, 'ACTIVE'),
  ('DRAWING_ACL_FEATURE_FLAG', 'draw.acl.gray.WAREHOUSE',    'false', 5, 'ACTIVE'),
  ('DRAWING_ACL_FEATURE_FLAG', 'draw.acl.gray.QC',           'false', 6, 'ACTIVE'),
  ('DRAWING_ACL_FEATURE_FLAG', 'draw.acl.gray.OPERATOR',     'false', 7, 'ACTIVE');

-- V94 · mock 清理：图纸关联回填/备份见 init_data.sql 或 data/V54__migrate_drawing_link.sql
