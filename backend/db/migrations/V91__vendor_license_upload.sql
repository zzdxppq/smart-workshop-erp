-- ======================================================================
-- V91 · 厂商资料增强 · 营业执照上传
-- 2026-06-20
-- ======================================================================

USE `cnc_business`;

SET @db := DATABASE();

-- 添加营业执照URL字段
SET @add_license_url := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE outsub_vendor ADD COLUMN business_license_url VARCHAR(500) DEFAULT NULL COMMENT ''营业执照URL'' AFTER default_recon_email',
        'SELECT ''skip'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'outsub_vendor' AND COLUMN_NAME = 'business_license_url'
);
PREPARE _stmt FROM @add_license_url; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;

-- 添加营业执照附件到期日期（提醒）
SET @add_license_expire := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE outsub_vendor ADD COLUMN business_license_expire_date DATE DEFAULT NULL COMMENT ''营业执照到期日期'' AFTER business_license_url',
        'SELECT ''skip'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'outsub_vendor' AND COLUMN_NAME = 'business_license_expire_date'
);
PREPARE _stmt FROM @add_license_expire; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;
