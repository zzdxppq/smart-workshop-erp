-- V102 · 补建 crm_purchase_order（若 V51/V73 先于 V49 执行导致表缺失）
-- 幂等：已有表则跳过；随后补 V73 的 pr_id 列（若缺失）

USE `cnc_business`;

CREATE TABLE IF NOT EXISTS crm_purchase_order (
    id BIGINT NOT NULL AUTO_INCREMENT,
    po_no VARCHAR(32) NOT NULL COMMENT 'XS-{yyyyMMdd}-{seq:4}',
    rfq_id BIGINT DEFAULT NULL,
    supplier_id BIGINT NOT NULL,
    supplier_name VARCHAR(128) DEFAULT NULL,
    total_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_SHIP',
    source_type VARCHAR(20) NOT NULL DEFAULT 'FROM_ORDER',
    purchase_reason VARCHAR(30) DEFAULT NULL,
    approval_route VARCHAR(50) DEFAULT NULL,
    approval_status VARCHAR(20) DEFAULT 'PENDING',
    remark VARCHAR(500) DEFAULT NULL,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uniq_po_no (po_no),
    KEY idx_rfq (rfq_id),
    KEY idx_supplier (supplier_id),
    KEY idx_status (status),
    KEY idx_source_type (source_type),
    KEY idx_purchase_reason (purchase_reason),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V1.3.8 采购订单主表';

CREATE TABLE IF NOT EXISTS crm_purchase_order_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    po_id BIGINT NOT NULL,
    purchase_order_id BIGINT NOT NULL,
    material_id BIGINT DEFAULT NULL,
    material_code VARCHAR(64) NOT NULL,
    material_name VARCHAR(128) DEFAULT NULL,
    quantity INT NOT NULL DEFAULT 0,
    unit_price DECIMAL(15,4) NOT NULL DEFAULT 0.0000,
    amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    delivery_date DATE DEFAULT NULL,
    sort_no INT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_po_id (po_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购订单明细';

SET @col_pr_id = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'cnc_business' AND TABLE_NAME = 'crm_purchase_order' AND COLUMN_NAME = 'pr_id');
SET @sql_pr_id = IF(@col_pr_id > 0,
  'SELECT ''skip crm_purchase_order.pr_id'' AS note',
  'ALTER TABLE crm_purchase_order
     ADD COLUMN pr_id BIGINT NULL COMMENT ''来源采购申请 ID'' AFTER rfq_id,
     ADD COLUMN pr_no VARCHAR(32) NULL COMMENT ''来源单号 PR-XXX'' AFTER pr_id,
     ADD COLUMN workorder_no VARCHAR(64) NULL COMMENT ''关联工单号'' AFTER pr_no,
     ADD COLUMN mrp_run_id BIGINT NULL COMMENT ''MRP 运行 ID'' AFTER workorder_no,
     ADD KEY idx_po_pr (pr_id)');
PREPARE stmt FROM @sql_pr_id; EXECUTE stmt; DEALLOCATE PREPARE stmt;
