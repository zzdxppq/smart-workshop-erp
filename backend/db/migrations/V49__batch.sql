-- ============================================================
-- V1.3.8 Sprint 7 · V49__batch.sql
-- 分批到货处理机制 + crm_purchase_order 新表
--
-- 关联 Story：
--   3.1 分批到货处理机制（crm_batch + crm_batch_shadow + PO 状态机）
--   4.1 无订单采购模式（crm_purchase_order 物理表）
--   4.2 采购主管审批路由（sys_workflow_node 扩展，与本迁移无冲突）
--
-- 重要约束：
--   1. V1.3.7 没有独立 crm_purchase_order 表（RFQ 流程只在 crm_rfq.purchase_order_no
--      维护字符串字段），V1.3.8 由本迁移首次创建
--   2. 老 PO 状态数据迁移（architect review §2.1 硬性约束）：无老数据可迁，
--      但保留脚本以便二次部署兼容
--   3. sys_workflow_node 表由 src/main/resources/db/migration/V2__workflow_split.sql
--      创建（Spring Boot Flyway 路径），不在本迁移处理
--
-- author: dev agent Opus 4.8 · 2026-06-13
-- ============================================================

-- 1. V1.3.8 新增 crm_purchase_order 物理表（V1.3.7 RFQ 流程只有字符串编号）
--    字段覆盖：3.1 PO 状态机 + 4.1 source_type + purchase_reason + 4.2 审批路由触发
CREATE TABLE IF NOT EXISTS crm_purchase_order (
    id BIGINT NOT NULL AUTO_INCREMENT,
    po_no VARCHAR(32) NOT NULL COMMENT 'XS-{yyyyMMdd}-{seq:4}（DocNoGenerator.nextOrderNo）',
    rfq_id BIGINT DEFAULT NULL COMMENT 'V1.3.7 RFQ 关联（FROM_ORDER 来源）',
    supplier_id BIGINT NOT NULL,
    supplier_name VARCHAR(128) DEFAULT NULL,
    total_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_SHIP' COMMENT 'PENDING_SHIP/PARTIAL_ARRIVED/ALL_ARRIVED/CANCELLED',
    source_type VARCHAR(20) NOT NULL DEFAULT 'FROM_ORDER' COMMENT 'FROM_ORDER/FROM_MRP/NO_ORDER',
    purchase_reason VARCHAR(30) DEFAULT NULL COMMENT 'URGENT_REPLENISH/CUSTOMER_ADD/STOCK_SWAP/OTHER',
    approval_route VARCHAR(50) DEFAULT NULL COMMENT 'PROCUREMENT_MANAGER/GM/GM+PROCUREMENT_MANAGER/SELF',
    approval_status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
    remark VARCHAR(500) DEFAULT NULL,
    created_by BIGINT NOT NULL,
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

-- 1b. V1.3.8 采购订单明细（Story 3.1/1.24 · V58 索引 / DrawingLink JOIN）
CREATE TABLE IF NOT EXISTS crm_purchase_order_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    po_id BIGINT NOT NULL COMMENT 'crm_purchase_order.id',
    purchase_order_id BIGINT NOT NULL COMMENT '与 po_id 同值 · DrawingLink JOIN',
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

-- 2. V1.3.8 重建 crm_batch（取代 V12 仓储批次结构 · Story 3.1 物料+PO 粒度）
DROP TABLE IF EXISTS crm_batch_shadow;
DROP TABLE IF EXISTS crm_batch;

CREATE TABLE crm_batch (
    id BIGINT NOT NULL AUTO_INCREMENT,
    batch_no VARCHAR(30) NOT NULL COMMENT 'BATCH-YYYYMMDD-流水',
    material_id BIGINT NOT NULL,
    po_id BIGINT NOT NULL,
    po_item_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    arrived_at DATETIME NOT NULL,
    quality_status ENUM('PENDING','PASSED','REJECTED') NOT NULL DEFAULT 'PENDING',
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uniq_batch_no (batch_no),
    KEY idx_material (material_id),
    KEY idx_po (po_id),
    KEY idx_arrived (arrived_at),
    KEY idx_quality (quality_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V1.3.8 物料批次表';

-- 3. 影子表（与 crm_batch 结构一致，供双写对比 cron 使用 · LIKE 已含 idx_material/idx_po）
CREATE TABLE crm_batch_shadow LIKE crm_batch;

-- 4. PO 状态机扩展（architect review §2.1，硬性约束）
--    V1.3.8 首次部署时 crm_purchase_order 为空表，UPDATE 影响 0 行（无害）
--    二次部署时若有老数据按 PENDING/ALERT/ARRIVED 写入，按以下映射升级
UPDATE crm_purchase_order SET status = 'PENDING_SHIP'     WHERE status = 'PENDING';
UPDATE crm_purchase_order SET status = 'PARTIAL_ARRIVED'  WHERE status = 'ALERT';
UPDATE crm_purchase_order SET status = 'ALL_ARRIVED'      WHERE status = 'ARRIVED';

-- 5. PO status 枚举扩展（DDL 默认值兼容）
ALTER TABLE crm_purchase_order
    MODIFY COLUMN status ENUM(
        'PENDING_SHIP',
        'PARTIAL_ARRIVED',
        'ALL_ARRIVED',
        'CANCELLED'
    ) NOT NULL DEFAULT 'PENDING_SHIP';
