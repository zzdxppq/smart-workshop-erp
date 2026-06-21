-- V1.3.9 · 品质检验提交即判定 · 处置/审批/下游单据/自动推送
USE `cnc_business`;

ALTER TABLE `crm_quality_inspection`
  ADD COLUMN `disposition` VARCHAR(16) DEFAULT NULL COMMENT 'RETURN/REWORK/SCRAP' AFTER `result`,
  ADD COLUMN `approval_status` VARCHAR(16) DEFAULT NULL COMMENT 'PENDING/APPROVED/REJECTED' AFTER `disposition`,
  ADD COLUMN `defect_disposition_qty` INT DEFAULT NULL COMMENT '处置不良数量' AFTER `approval_status`,
  ADD COLUMN `drawing_no` VARCHAR(64) DEFAULT NULL COMMENT '关联图号' AFTER `defect_disposition_qty`,
  ADD COLUMN `source_ref` VARCHAR(128) DEFAULT NULL COMMENT '自动推送来源 SCAN:/REPORT:' AFTER `drawing_no`;

CREATE INDEX `idx_inspection_source_ref` ON `crm_quality_inspection` (`source_ref`);

CREATE TABLE IF NOT EXISTS `crm_quality_downstream` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `inspection_id` BIGINT NOT NULL,
  `downstream_type` VARCHAR(16) NOT NULL COMMENT 'RETURN/REWORK/SCRAP',
  `order_no` VARCHAR(32) NOT NULL,
  `qty` INT NOT NULL DEFAULT 0,
  `status` VARCHAR(16) NOT NULL DEFAULT 'CREATED',
  `remark` VARCHAR(500) DEFAULT NULL,
  `created_by` BIGINT DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_downstream_order_no` (`order_no`),
  KEY `idx_downstream_inspection` (`inspection_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='品质检验下游单据（退货/返工/报废）';

CREATE TABLE IF NOT EXISTS `crm_quality_concession_approval` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `inspection_id` BIGINT NOT NULL,
  `approver_role` VARCHAR(32) NOT NULL COMMENT 'QUALITY_MANAGER/PRODUCTION_MANAGER',
  `approval_status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
  `approver_user_id` BIGINT DEFAULT NULL,
  `approved_at` DATETIME DEFAULT NULL,
  `comment` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_concession_inspection` (`inspection_id`),
  KEY `idx_concession_role_status` (`approver_role`, `approval_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='让步接收双签审批';
