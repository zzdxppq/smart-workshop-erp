-- ============================================================
-- V5__crm_order.sql · V1.3.7 Story 1.6
-- ============================================================
-- 目的：Story 1.6 订单管理
--     创建 4 张新表（crm_order / crm_order_item / crm_order_history / crm_order_payment）
-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- ============================================================

USE `cnc_business`;

-- ---------- 1. crm_order 订单主表 ----------
DROP TABLE IF EXISTS `crm_order`;
CREATE TABLE `crm_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(30) NOT NULL COMMENT 'XS+YYYYMMDD+NNNN (继承1.5 DocNoGenerator 模板)',
  `quote_id` BIGINT DEFAULT NULL COMMENT '来源报价ID(手动创建为NULL, 1.5转单时非空)',
  `customer_id` BIGINT NOT NULL COMMENT '客户 ID (sys_dict?type=CUSTOMER_STATUS 查黑名单)',
  `customer_name` VARCHAR(200) NOT NULL,
  `owner_user_id` BIGINT NOT NULL COMMENT '业务员ID',
  `dept_id` BIGINT NOT NULL COMMENT '部门ID(经理按部门过滤)',
  `currency` VARCHAR(10) NOT NULL DEFAULT 'CNY',
  `total_amount` DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT '由 items 自动计算(只读)',
  `delivery_date` DATE NOT NULL,
  `is_fa` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否FA首件',
  `is_new` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否新件',
  `is_urgent` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否加急',
  `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/CONFIRMED/PRODUCING/PARTIAL_SHIPPED/SHIPPED/SETTLED/CLOSED/CANCELLED',
  `current_node` INT DEFAULT 1 COMMENT '当前审批节点 (1/2/3)',
  `comment` VARCHAR(1000) DEFAULT NULL,
  `production_order_no` VARCHAR(30) DEFAULT NULL COMMENT 'GD+YYYYMMDD+NNNN (转生产时生成, Epic 5)',
  `outsource_order_no` VARCHAR(30) DEFAULT NULL COMMENT 'WW+YYYYMMDD+NNNN (转委外时生成, Epic 6)',
  `credit_limit_check` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '信用额度是否校验通过(0=未通过/未检查,1=通过,-1=无限制)',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_order_no` (`order_no`),
  KEY `idx_customer_id` (`customer_id`),
  KEY `idx_owner_user_id` (`owner_user_id`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_quote_id` (`quote_id`)
) ENGINE=InnoDB COMMENT='订单主表 (V1.3.7 Story 1.6)';

-- ---------- 2. crm_order_item 订单明细 ----------
DROP TABLE IF EXISTS `crm_order_item`;
CREATE TABLE `crm_order_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `drawing_no` VARCHAR(50) NOT NULL COMMENT '图号',
  `material` VARCHAR(50) NOT NULL,
  `spec` VARCHAR(200) DEFAULT NULL,
  `quantity` INT NOT NULL COMMENT '订单数量',
  `unit_price` DECIMAL(15,2) NOT NULL,
  `amount` DECIMAL(15,2) NOT NULL COMMENT '= quantity * unit_price',
  `quantity_adjustment` INT NOT NULL DEFAULT 0 COMMENT '数量调整(来自1.5 quantityAdjustment hook)',
  `is_fa` TINYINT(1) NOT NULL DEFAULT 0,
  `is_new` TINYINT(1) NOT NULL DEFAULT 0,
  `sort` INT NOT NULL DEFAULT 0,
  `produced_qty` INT NOT NULL DEFAULT 0 COMMENT '已生产数量(转生产后累计)',
  `shipped_qty` INT NOT NULL DEFAULT 0 COMMENT '已发货数量(发货后累计)',
  PRIMARY KEY (`id`),
  KEY `idx_order_id_sort` (`order_id`, `sort`),
  CONSTRAINT `fk_order_item_order` FOREIGN KEY (`order_id`) REFERENCES `crm_order` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='订单明细 (V1.3.7 Story 1.6 · 含quantityAdjustment hook)';

-- ---------- 3. crm_order_history 订单变更历史 ----------
DROP TABLE IF EXISTS `crm_order_history`;
CREATE TABLE `crm_order_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `operation` VARCHAR(30) NOT NULL COMMENT 'CREATE/UPDATE/CONFIRM/APPROVE/REJECT/CONVERT_PROD/CONVERT_OUTSUB/SHIP/PARTIAL_SHIP/SETTLE/CLOSE/CANCEL/CREDIT_CHECK/PDF_DOWNLOAD/EXCEL_DOWNLOAD/PROFIT_ANALYSIS',
  `before_json` TEXT,
  `after_json` TEXT,
  `changed_by` BIGINT NOT NULL,
  `changed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_changed_by` (`changed_by`),
  KEY `idx_changed_at` (`changed_at`),
  KEY `idx_operation` (`operation`)
) ENGINE=InnoDB COMMENT='订单变更历史 (V1.3.7 Story 1.6 · 红线5 变更留痕)';

-- ---------- 4. crm_order_payment 订单回款 ----------
DROP TABLE IF EXISTS `crm_order_payment`;
CREATE TABLE `crm_order_payment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `payment_no` VARCHAR(30) NOT NULL COMMENT '回款单号(自动生成)',
  `amount` DECIMAL(15,2) NOT NULL,
  `payment_date` DATE NOT NULL,
  `payment_method` VARCHAR(20) NOT NULL DEFAULT 'BANK' COMMENT 'BANK/CASH/CHECK/OTHER',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/CONFIRMED/CANCELLED',
  `comment` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_payment_no` (`payment_no`),
  KEY `idx_order_id` (`order_id`),
  CONSTRAINT `fk_order_payment_order` FOREIGN KEY (`order_id`) REFERENCES `crm_order` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='订单回款 (V1.3.7 Story 1.6 · SETTLED 状态联动)';

-- ---------- 5. 7 状态机枚举字典 (V1.3.7 §附录-b) ----------
USE `cnc_platform`;

INSERT INTO `sys_dict_type` (`type_code`, `type_name`, `description`, `is_builtin`) VALUES
('ORDER_STATUS', '订单状态', 'DRAFT/CONFIRMED/PRODUCING/PARTIAL_SHIPPED/SHIPPED/SETTLED/CLOSED/CANCELLED', 1)
ON DUPLICATE KEY UPDATE `type_name` = VALUES(`type_name`);

INSERT IGNORE INTO `sys_dict` (`dict_type`, `dict_code`, `dict_label`, `sort`, `status`) VALUES
('ORDER_STATUS', 'DRAFT', '草稿', 1, 'ACTIVE'),
('ORDER_STATUS', 'CONFIRMED', '已确认', 2, 'ACTIVE'),
('ORDER_STATUS', 'PRODUCING', '生产中', 3, 'ACTIVE'),
('ORDER_STATUS', 'PARTIAL_SHIPPED', '部分发货', 4, 'ACTIVE'),
('ORDER_STATUS', 'SHIPPED', '已发货', 5, 'ACTIVE'),
('ORDER_STATUS', 'SETTLED', '已结算', 6, 'ACTIVE'),
('ORDER_STATUS', 'CLOSED', '已关闭', 7, 'ACTIVE'),
('ORDER_STATUS', 'CANCELLED', '已取消', 8, 'ACTIVE');

-- ---------- 6. 信用额度字典类型 (V1.3.7 P2 修补 3) ----------
INSERT INTO `sys_dict_type` (`type_code`, `type_name`, `description`, `is_builtin`) VALUES
('CREDIT_LIMIT', '客户信用额度', '客户ID -> 信用额度(CNY, -1=无限制), 超限抛40909', 1)
ON DUPLICATE KEY UPDATE `type_name` = VALUES(`type_name`);

-- V94 · mock 清理：CREDIT_LIMIT 演示客户额度已移至 init_data.sql

USE `cnc_business`;

-- 5 条 UI 红线检查
SELECT 'V5__crm_order.sql 迁移完成' AS message,
       (SELECT COUNT(*) FROM cnc_business.crm_order) AS order_count,
       (SELECT COUNT(*) FROM cnc_business.crm_order_item) AS order_item_count,
       (SELECT COUNT(*) FROM cnc_platform.sys_dict WHERE dict_type = 'ORDER_STATUS') AS order_status_count,
       (SELECT COUNT(*) FROM cnc_platform.sys_dict WHERE dict_type = 'CREDIT_LIMIT') AS credit_limit_count;
