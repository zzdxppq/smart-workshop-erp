-- ======================================================================
-- V93 · 修复 crm_order 表缺少 source_quotation_id 字段
-- 修复报价转订单时查询报错 Unknown column 'source_quotation_id'
-- 2026-06-20
-- ======================================================================

USE `cnc_business`;

SET @db := DATABASE();

SET @add_source_quotation_id := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_order ADD COLUMN source_quotation_id BIGINT NULL COMMENT ''来源报价单ID'' AFTER quote_id',
        'SELECT ''skip crm_order.source_quotation_id'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_order' AND COLUMN_NAME = 'source_quotation_id'
);
PREPARE _stmt FROM @add_source_quotation_id;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;
