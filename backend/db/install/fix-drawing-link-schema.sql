-- 修复 V52/V54 未 USE cnc_business 导致 crm_drawing_link* 误建在 cnc_platform 的问题
-- 用法：mysql ... < backend/db/install/fix-drawing-link-schema.sql
-- 执行后再跑 sync-cnc-production.ps1

USE `cnc_platform`;

DROP TABLE IF EXISTS crm_drawing_link_backup;
DROP TABLE IF EXISTS crm_drawing_link;

USE `cnc_business`;

CREATE TABLE IF NOT EXISTS crm_drawing_link (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    drawing_id BIGINT NOT NULL COMMENT '图纸 ID',
    biz_type VARCHAR(20) NOT NULL COMMENT 'ORDER/PO/INCOMING/INSPECTION/WORKORDER_PROCESS',
    biz_id BIGINT NOT NULL COMMENT '业务单据 ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_biz_ref (biz_type, biz_id, drawing_id),
    KEY idx_drawing_link (drawing_id, biz_type, biz_id),
    KEY idx_biz_lookup (biz_type, biz_id),
    CONSTRAINT fk_draw_link_drawing FOREIGN KEY (drawing_id) REFERENCES crm_drawing(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图纸与业务单据关联';

CREATE TABLE IF NOT EXISTS crm_drawing_link_backup (
    id BIGINT NOT NULL,
    drawing_id BIGINT NOT NULL,
    biz_type VARCHAR(20) NOT NULL,
    biz_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    created_by BIGINT NOT NULL,
    backup_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_backup_biz_ref (biz_type, biz_id, drawing_id),
    KEY idx_backup_drawing (drawing_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图纸关联备份';

SELECT table_schema, table_name
FROM information_schema.tables
WHERE table_name IN ('crm_drawing_link', 'crm_drawing_link_backup')
ORDER BY table_schema, table_name;
