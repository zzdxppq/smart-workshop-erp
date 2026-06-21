-- V1.3.7 · Story 1.22 · 委外 7 状态机 (FR-6-2)
-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
USE `cnc_business`;

-- 委外状态机历史（按 outsource_id 主键维度，比 outsource_no 关联更紧密）
CREATE TABLE IF NOT EXISTS `crm_outsource_state_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `outsource_id` BIGINT NOT NULL COMMENT '委外单主键 ID',
  `outsource_no` VARCHAR(32) NOT NULL COMMENT '冗余委外单号便于查询',
  `from_state` VARCHAR(20) DEFAULT NULL COMMENT 'DRAFT/SENT/ACCEPTED/IN_PRODUCTION/INSPECTED/COMPLETED/CLOSED/REWORK/REJECTED',
  `to_state` VARCHAR(20) NOT NULL,
  `transition_type` VARCHAR(20) NOT NULL DEFAULT 'ADVANCE' COMMENT 'ADVANCE/ROLLBACK/REWORK',
  `operator_user_id` BIGINT NOT NULL,
  `operator_role` VARCHAR(40) DEFAULT NULL COMMENT '生管/采购/品检/财务（V1.3.7 AD-1）',
  `reason` VARCHAR(500) DEFAULT NULL,
  `occurred_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_state_history_outsource_id` (`outsource_id`),
  KEY `idx_state_history_outsource_no` (`outsource_no`),
  KEY `idx_state_history_to_state` (`to_state`),
  KEY `idx_state_history_occurred_at` (`occurred_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='委外状态机历史（V1.3.7 Story 1.22 FR-6-2）';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
