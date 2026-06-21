-- V1.3.7 · 委外供应商切换（生管 + 采购双向确认）
CREATE TABLE IF NOT EXISTS `crm_outsource_vendor_switch` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `switch_no` VARCHAR(32) NOT NULL,
    `outsource_id` BIGINT NOT NULL,
    `outsource_no` VARCHAR(32) NOT NULL,
    `old_supplier_id` BIGINT NOT NULL,
    `old_supplier_name` VARCHAR(128) DEFAULT NULL,
    `new_supplier_id` BIGINT NOT NULL,
    `new_supplier_name` VARCHAR(128) DEFAULT NULL,
    `reason` VARCHAR(500) DEFAULT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    `prod_confirmed` TINYINT(1) NOT NULL DEFAULT 0,
    `purch_confirmed` TINYINT(1) NOT NULL DEFAULT 0,
    `prod_confirmed_by` BIGINT DEFAULT NULL,
    `purch_confirmed_by` BIGINT DEFAULT NULL,
    `prod_confirmed_at` DATETIME DEFAULT NULL,
    `purch_confirmed_at` DATETIME DEFAULT NULL,
    `created_by` BIGINT DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_switch_no` (`switch_no`),
    KEY `idx_switch_outsource` (`outsource_id`),
    KEY `idx_switch_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='委外供应商切换单';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
