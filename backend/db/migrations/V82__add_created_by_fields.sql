-- V1.3.10 · 创建人字段 · 列表显示创建人
USE `cnc_business`;

-- 报价单增加创建人
ALTER TABLE `crm_quote`
  ADD COLUMN `created_by` BIGINT DEFAULT NULL COMMENT '创建人用户ID' AFTER `is_deleted`;

-- 订单增加创建人
ALTER TABLE `crm_order`
  ADD COLUMN `created_by` BIGINT DEFAULT NULL COMMENT '创建人用户ID' AFTER `is_deleted`;

-- 客户档案增加创建人
ALTER TABLE `crm_customer`
  ADD COLUMN `created_by` BIGINT DEFAULT NULL COMMENT '创建人用户ID' AFTER `status`;

-- 为新字段添加索引
CREATE INDEX `idx_quote_created_by` ON `crm_quote` (`created_by`);
CREATE INDEX `idx_order_created_by` ON `crm_order` (`created_by`);
CREATE INDEX `idx_customer_created_by` ON `crm_customer` (`created_by`);
