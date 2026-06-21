-- V1.3.8 合规 · 采购申请（PR）+ PO/RFQ 来源关联
USE `cnc_business`;

-- ---------- 1. 采购申请主表（MRP 缺料 → 生管推送 → 采购转单） ----------
CREATE TABLE IF NOT EXISTS `crm_purchase_request` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `pr_no` VARCHAR(32) NOT NULL COMMENT 'PR-{yyyyMMdd}-{seq:4}',
  `mrp_run_id` BIGINT DEFAULT NULL COMMENT 'MRP 运行 ID',
  `mrp_shortage_id` BIGINT DEFAULT NULL COMMENT '缺料行 ID',
  `workorder_no` VARCHAR(64) DEFAULT NULL COMMENT '关联工单号（只读展示）',
  `sales_order_no` VARCHAR(64) DEFAULT NULL COMMENT '关联销售订单号',
  `material_id` BIGINT DEFAULT NULL,
  `material_code` VARCHAR(64) NOT NULL,
  `material_name` VARCHAR(128) DEFAULT NULL,
  `required_qty` INT NOT NULL DEFAULT 0 COMMENT '需求数量',
  `converted_qty` INT NOT NULL DEFAULT 0 COMMENT '已转采购数量',
  `required_date` DATE DEFAULT NULL COMMENT '期望交期',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PARTIAL/CONVERTED/CANCELLED',
  `source_type` VARCHAR(20) NOT NULL DEFAULT 'MRP' COMMENT 'MRP/NO_ORDER',
  `remark` VARCHAR(500) DEFAULT NULL,
  `created_by` BIGINT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_pr_no` (`pr_no`),
  KEY `idx_pr_status` (`status`),
  KEY `idx_pr_material` (`material_code`),
  KEY `idx_pr_mrp_run` (`mrp_run_id`),
  KEY `idx_pr_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购申请单（MRP 缺料触发）';

-- ---------- 2. PO 关联 PR / 工单 ----------
SET @col_pr_id = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'cnc_business' AND TABLE_NAME = 'crm_purchase_order' AND COLUMN_NAME = 'pr_id');
SET @sql_pr_id = IF(@col_pr_id = 0,
  'ALTER TABLE crm_purchase_order
     ADD COLUMN pr_id BIGINT NULL COMMENT ''来源采购申请 ID'' AFTER rfq_id,
     ADD COLUMN pr_no VARCHAR(32) NULL COMMENT ''来源单号 PR-XXX'' AFTER pr_id,
     ADD COLUMN workorder_no VARCHAR(64) NULL COMMENT ''关联工单号'' AFTER pr_no,
     ADD COLUMN mrp_run_id BIGINT NULL COMMENT ''MRP 运行 ID'' AFTER workorder_no,
     ADD KEY idx_po_pr (pr_id)',
  'SELECT 1');
PREPARE stmt FROM @sql_pr_id; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 3. RFQ 来源绑定 + 转单状态 ----------
SET @col_rfq_src = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'cnc_business' AND TABLE_NAME = 'crm_rfq' AND COLUMN_NAME = 'inquiry_source_type');
SET @sql_rfq_src = IF(@col_rfq_src = 0,
  'ALTER TABLE crm_rfq
     ADD COLUMN inquiry_source_type VARCHAR(20) NULL COMMENT ''MATERIAL/OUTSOURCE/NO_ORDER'' AFTER winner_mode,
     ADD COLUMN pr_id BIGINT NULL COMMENT ''绑定采购申请'' AFTER inquiry_source_type,
     ADD COLUMN pr_no VARCHAR(32) NULL AFTER pr_id,
     ADD COLUMN workorder_no VARCHAR(64) NULL COMMENT ''关联工单'' AFTER pr_no,
     ADD COLUMN process_step_no INT NULL COMMENT ''委外工序号'' AFTER workorder_no,
     ADD COLUMN allocation_id BIGINT NULL COMMENT ''待委外工序分配 ID'' AFTER process_step_no,
     ADD COLUMN convert_status VARCHAR(24) NOT NULL DEFAULT ''NOT_CONVERTED'' COMMENT ''NOT_CONVERTED/PO_CONVERTED/OUTSOURCE_CONVERTED'' AFTER allocation_id,
     ADD COLUMN converted_order_no VARCHAR(32) NULL COMMENT ''转单后 PO/WW 单号'' AFTER convert_status,
     ADD KEY idx_rfq_pr (pr_id),
     ADD KEY idx_rfq_convert (convert_status)',
  'SELECT 1');
PREPARE stmt2 FROM @sql_rfq_src; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;

-- ---------- 4. 菜单：采购转单（替换独立新建 PO 入口） ----------
USE `cnc_platform`;

INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(613, 6, 'src.pr-transfer', '采购转单', '/sourcing/purchase-transfer', 'MENU', 3);

UPDATE `sys_menu` SET `menu_name` = '询比价工作台', `menu_code` = 'src.rfq.workbench' WHERE `id` = 601;
UPDATE `sys_menu` SET `menu_name` = '委外转单', `menu_code` = 'src.outsub.transfer' WHERE `id` = 607;

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT r.id, 613, 'view' FROM `sys_role` r
WHERE r.role_code IN ('BUYER', 'PROCUREMENT_MANAGER', 'PROD_MGR', 'GM', 'SYS_ADMIN');
