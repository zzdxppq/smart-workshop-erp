-- ============================================================
-- V1.3.7 Story 1.2 · V3__approval_record.sql
-- P1 修补 ④：创建 sys_approval_record 表
-- 关联：与 sys_workflow / sys_workflow_node 配合，构成完整审批中台
-- ============================================================

USE `cnc_platform`;

-- ---------- sys_approval_record 审批单表 ----------
DROP TABLE IF EXISTS `sys_approval_record`;
CREATE TABLE `sys_approval_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `biz_type` VARCHAR(20) NOT NULL COMMENT 'QUOTE/ORDER/PURCHASE/PAYMENT/OTHER',
  `biz_id` VARCHAR(50) NOT NULL COMMENT '业务单号（≤50 字符）',
  `workflow_code` VARCHAR(50) NOT NULL COMMENT 'sys_workflow.workflow_code',
  `current_node_index` INT NOT NULL DEFAULT 1 COMMENT '当前节点序号',
  `current_approver_user_id` BIGINT DEFAULT NULL COMMENT '当前审批人（首次分配 candidates[0]）',
  `candidates` TEXT COMMENT 'OR 会签候选人列表 JSON（[10010, 10011]）',
  `or_sign_required` BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'V1.3.7 P1 修补：OR 会签',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/SKIPPED/WAITING',
  `skip_reason` VARCHAR(20) DEFAULT NULL COMMENT 'ON_LEAVE/ON_TRIP/DISABLED/RESIGNED',
  `skipped_at` DATETIME DEFAULT NULL,
  `comment` VARCHAR(500) DEFAULT NULL COMMENT '审批意见',
  `reason` VARCHAR(500) DEFAULT NULL COMMENT '驳回原因（必填）',
  `timeout_at` DATETIME DEFAULT NULL COMMENT 'V1.3.7 24h 超时基准（= created_at + timeout_hours）',
  `is_overdue` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否已超时',
  `overdue_at` DATETIME DEFAULT NULL,
  `node_skipped` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '本节点是否被全员 SKIPPED 自动跳过',
  `approved_at` DATETIME DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` BIGINT DEFAULT NULL,
  `update_by` BIGINT DEFAULT NULL,
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_biz_node` (`biz_type`, `biz_id`, `current_node_index`),
  KEY `idx_approver_status` (`current_approver_user_id`, `status`),
  KEY `idx_overdue` (`status`, `timeout_at`, `is_overdue`),
  KEY `idx_biz` (`biz_type`, `biz_id`),
  KEY `idx_workflow` (`workflow_code`)
) ENGINE=InnoDB COMMENT='审批单表（V1.3.7 P1 修补 ④ · 同一 bizType+bizId 可多轮审批）';

-- ============================================================
-- 迁移完成
-- 索引说明：
--   1. uniq_biz_node：同一业务单同一节点唯一（防止重复创建）
--   2. idx_approver_status：待办列表查询加速（/approvals/pending）
--   3. idx_overdue：超时扫描加速（V1.3.7 P1-3 FOR UPDATE SKIP LOCKED）
-- ============================================================
