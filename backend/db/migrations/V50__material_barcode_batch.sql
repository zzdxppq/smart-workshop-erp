-- ============================================================
-- V1.3.8 Story 3.2 · V50__material_barcode_batch.sql
-- 物料码批次生成：crm_material_barcode_batch 表
-- 关联：Story 3.2-物料码批次生成.md + architect review 3.2-impl-review.md
-- 复合物料码格式：WL-{material_no}-BATCH-{YYYYMMDD}-{seq:4}
-- ============================================================

CREATE TABLE IF NOT EXISTS crm_material_barcode_batch (
    id BIGINT NOT NULL AUTO_INCREMENT,
    material_id BIGINT NOT NULL,
    batch_id BIGINT NOT NULL,
    barcode_no VARCHAR(50) NOT NULL COMMENT 'WL-{material_no}-BATCH-{YYYYMMDD}-{seq}',
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uniq_barcode_no (barcode_no),
    KEY idx_material (material_id),
    KEY idx_batch (batch_id),
    KEY idx_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V1.3.8 物料码批次映射表';

-- 注：architect review §3.3 建议到货时同步将老 WL-XXXX 标记 is_active=0
--     此逻辑由应用层 MaterialBarcodeService 实现，不在迁移脚本中
