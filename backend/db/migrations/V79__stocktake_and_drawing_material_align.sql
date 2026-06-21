-- V79 · 盘点单表 + 图纸料号与物料主数据对齐（WL-1001~1005）

USE `cnc_business`;

CREATE TABLE IF NOT EXISTS crm_warehouse_stocktake (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    stocktake_no    VARCHAR(32)  NOT NULL COMMENT '盘点单号 STK-YYYYMMDD-NNNN',
    warehouse_code  VARCHAR(32)  NOT NULL COMMENT '仓库编码',
    status          VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/COUNTING/CLOSED',
    created_by      BIGINT       NULL COMMENT '创建人',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_stocktake_no (stocktake_no),
    KEY idx_stocktake_wh (warehouse_code),
    KEY idx_stocktake_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓库盘点单';

-- V94 · mock 清理：图纸→物料对齐回填已移至 init_data.sql
