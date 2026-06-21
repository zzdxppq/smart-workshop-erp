-- ============================================================
-- V1.3.9 Sprint 12 Story 12.3 · V56__label_template.sql
-- 标签模板 4 种 (GD-/LZ-/WW-/WL-) · SB- 由代码层 fallback 到 GD 模板 + 改色条
-- 关联：12.3-label-template.md + architect review 12.3-review.md
--
-- 字段数：10（PK + 业务 7 + tenant 1 + 审计 2）
-- 索引：3（PRIMARY KEY + UNIQUE KEY uk_label_type + KEY idx_tenant）
-- CHECK 约束：2（chk_label_type / chk_label_dpi）
-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- ============================================================

CREATE TABLE IF NOT EXISTS label_template (
    id            BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `type`        VARCHAR(10) NOT NULL COMMENT '模板类型：GD / LZ / WW / WL（SB 由代码层 fallback 到 GD）',
    color_strip   VARCHAR(7) NOT NULL COMMENT '色条 HEX · #1E40AF / #16A34A / #EA580C / #000000',
    factory_name  VARCHAR(20) NOT NULL DEFAULT '昆山佰泰胜精密加工' COMMENT '厂名 · 默认昆山佰泰胜精密加工 · 来自 sys_dict dict_type=COMPANY_NAME',
    layout_json   VARCHAR(500) NOT NULL COMMENT '三区坐标 + 字体 + DPI · JSON 字符串 · {"topBarH":5,"qrAreaH":18,"textAreaH":7,"fontSize":8,"qrSizePx":300}',
    dpi           SMALLINT NOT NULL DEFAULT 300 COMMENT 'DPI · 203 / 300',
    enabled       TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启停：1=启用 / 0=停用',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户 ID · sys_tenant.id',
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_label_type (`type`, tenant_id),
    KEY idx_tenant (tenant_id),
    CONSTRAINT chk_label_type CHECK (`type` IN ('GD','LZ','WW','WL')),
    CONSTRAINT chk_label_dpi  CHECK (dpi IN (203, 300))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V1.3.9 标签模板 · 4 行 seed · SB 由代码层 fallback 到 GD';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
