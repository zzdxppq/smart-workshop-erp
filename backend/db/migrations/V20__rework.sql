-- V1.3.7 · Story 1.23 · 委外返修闭环 (FR-6-3)
-- 迁移：crm_rework 返修单 + crm_rework_history 返修历史 + crm_rework_alert 次数预警
-- 3 P1 修补：返修次数 ≤ 3 / 返修原因必填 / 返修成本计入

USE `cnc_business`;

-- 返修单（独立于委外单，关联 outsource_id）
CREATE TABLE IF NOT EXISTS `crm_rework` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `rework_no` VARCHAR(32) NOT NULL COMMENT 'RW{yyyyMMdd}{seq:4}',
  `outsource_id` BIGINT NOT NULL COMMENT '关联委外单主键 ID',
  `outsource_no` VARCHAR(32) NOT NULL COMMENT '冗余委外单号',
  `reason` VARCHAR(500) NOT NULL COMMENT '返修原因（必填 · P1 修补 2）',
  `cost` DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT '返修成本（P1 修补 3 · 计入月度对账）',
  `rework_count` INT NOT NULL DEFAULT 1 COMMENT '本次返修次序',
  `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/IN_PROGRESS/COMPLETED/CANCELLED',
  `expected_finish_date` DATE DEFAULT NULL,
  `finished_at` DATETIME DEFAULT NULL,
  `created_by` BIGINT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_rework_no` (`rework_no`),
  KEY `idx_rework_outsource_id` (`outsource_id`),
  KEY `idx_rework_outsource_no` (`outsource_no`),
  KEY `idx_rework_status` (`status`),
  KEY `idx_rework_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='委外返修单（V1.3.7 Story 1.23 FR-6-3）';

-- 返修历史
CREATE TABLE IF NOT EXISTS `crm_rework_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `rework_id` BIGINT NOT NULL,
  `operation` VARCHAR(20) NOT NULL COMMENT 'CREATE/FINISH/CANCEL',
  `before_json` TEXT DEFAULT NULL COMMENT '变更前快照',
  `after_json` TEXT DEFAULT NULL COMMENT '变更后快照',
  `changed_by` BIGINT NOT NULL,
  `changed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_rework_history_rework_id` (`rework_id`),
  CONSTRAINT `fk_rework_history_rework` FOREIGN KEY (`rework_id`) REFERENCES `crm_rework` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='返修变更历史';

-- 返修次数预警
CREATE TABLE IF NOT EXISTS `crm_rework_alert` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `outsource_id` BIGINT NOT NULL,
  `outsource_no` VARCHAR(32) NOT NULL,
  `rework_count` INT NOT NULL,
  `alert_level` VARCHAR(20) NOT NULL COMMENT 'INFO/WARN/CRITICAL/EXCEED',
  `alert_message` VARCHAR(500) DEFAULT NULL,
  `alerted_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_rework_alert_outsource_id` (`outsource_id`),
  KEY `idx_rework_alert_level` (`alert_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='返修次数预警（> 3 次告警）';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
