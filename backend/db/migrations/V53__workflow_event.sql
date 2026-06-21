-- ============================================================
-- V1.3.8 Sprint 8 Story 8.3 · V53__workflow_event.sql
-- sys_workflow_event 表实装（V1.3.7 规划但未实装的预留表名）
--
-- 关联 Story：8.3-sys_workflow_event
-- 8.3.1 写事件：approval complete / approve / reject 时 INSERT
-- 8.3.2 GmSummaryService 改用真实 JOIN 统计 PROCUREMENT_MANAGER 工作量
-- ============================================================

USE `cnc_business`;

CREATE TABLE IF NOT EXISTS sys_workflow_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_no VARCHAR(40) NOT NULL COMMENT 'EV-{yyyyMMddHHmmss}-{seq}',
    workflow_code VARCHAR(50) NOT NULL COMMENT 'PO_APPROVAL / QUOTE_APPROVAL',
    biz_id BIGINT NOT NULL COMMENT '业务实体 ID（crm_purchase_order.id 等）',
    biz_no VARCHAR(40) DEFAULT NULL COMMENT '业务单号',
    event_type VARCHAR(20) NOT NULL COMMENT 'CREATED / APPROVED / REJECTED / DELEGATED',
    approver_role VARCHAR(50) NOT NULL COMMENT 'PROCUREMENT_MANAGER / DEPT_MANAGER / GM',
    approver_user_id BIGINT DEFAULT NULL,
    approver_user_name VARCHAR(64) DEFAULT NULL,
    comment VARCHAR(500) DEFAULT NULL,
    matched_node_index INT DEFAULT NULL COMMENT '命中 sys_workflow_node.node_index',
    matched_threshold VARCHAR(50) DEFAULT NULL COMMENT 'AMOUNT_10K_50K / CATEGORY_TOOL 等',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uniq_event_no (event_no),
    KEY idx_workflow_code (workflow_code),
    KEY idx_biz (biz_id),
    KEY idx_event_type (event_type),
    KEY idx_approver_role (approver_role),
    KEY idx_created_at (created_at),
    KEY idx_workflow_approver (workflow_code, approver_role, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V1.3.8 审批事件表';
