-- ======================================================================
-- V96 · 客户档案联系人/电话/邮箱（报价发邮件依赖）
-- 2026-06-21
-- ======================================================================

USE `cnc_business`;

SET @db := DATABASE();

SET @add_contact_name := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_customer ADD COLUMN contact_name VARCHAR(100) DEFAULT NULL COMMENT ''联系人'' AFTER name',
        'SELECT ''skip crm_customer.contact_name'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_customer' AND COLUMN_NAME = 'contact_name'
);
PREPARE _stmt FROM @add_contact_name;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

SET @add_contact_phone := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_customer ADD COLUMN contact_phone VARCHAR(32) DEFAULT NULL COMMENT ''联系电话'' AFTER contact_name',
        'SELECT ''skip crm_customer.contact_phone'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_customer' AND COLUMN_NAME = 'contact_phone'
);
PREPARE _stmt FROM @add_contact_phone;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

SET @add_contact_email := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_customer ADD COLUMN contact_email VARCHAR(128) DEFAULT NULL COMMENT ''联系邮箱（报价发送）'' AFTER contact_phone',
        'SELECT ''skip crm_customer.contact_email'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_customer' AND COLUMN_NAME = 'contact_email'
);
PREPARE _stmt FROM @add_contact_email;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;
