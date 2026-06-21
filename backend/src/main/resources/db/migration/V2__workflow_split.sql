-- ============================================================
-- V1.3.7 Story 1.2 · V2__workflow_split.sql
-- P1 修补 ④：sys_workflow 拆分为 sys_workflow + sys_workflow_node 物理表
-- architect 评审通过 · 6 条 P2 反馈全部纳入
-- ============================================================

USE `cnc_platform`;

-- ---------- 新增 sys_workflow_node 物理表 ----------
DROP TABLE IF EXISTS `sys_workflow_node`;
CREATE TABLE `sys_workflow_node` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `workflow_id` BIGINT NOT NULL COMMENT 'sys_workflow.id（FK ON DELETE CASCADE）',
  `node_index` INT NOT NULL COMMENT '节点序号 1..N（严格递增）',
  `node_type` VARCHAR(20) NOT NULL COMMENT 'START/APPROVAL/CC/END',
  `role_code` VARCHAR(50) DEFAULT NULL COMMENT 'APPROVAL 节点必填（引用 sys_role.role_code）',
  `threshold` DECIMAL(15,2) DEFAULT NULL COMMENT '金额阈值（NULL=无限额）',
  `or_sign_required` BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'V1.3.7 P1 修补：OR 会签',
  `extra_check_json` TEXT COMMENT 'V1.3.7 条件扩展（如 extra_check=credit_limit）',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` BIGINT DEFAULT NULL,
  `update_by` BIGINT DEFAULT NULL,
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_workflow_node` (`workflow_id`, `node_index`),
  KEY `idx_role` (`role_code`),
  KEY `idx_node_type` (`node_type`),
  CONSTRAINT `fk_node_workflow` FOREIGN KEY (`workflow_id`) REFERENCES `sys_workflow` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='工作流节点物理表（V1.3.7 P1 修补 ④）';

-- ---------- sys_workflow 新增字段（幂等 · 重复执行 init.sql 不报错） ----------
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_workflow' AND COLUMN_NAME = 'version');
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE `sys_workflow` ADD COLUMN `version` INT NOT NULL DEFAULT 1 COMMENT ''V1.3.7 灰度 hook'' AFTER `status`, ADD COLUMN `last_modified_by` BIGINT DEFAULT NULL COMMENT ''V1.3.7 审计'' AFTER `version`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 4 套内置模板数据迁移：nodes_json → sys_workflow_node ----------
-- JSON threshold 可能为 null · 不可直接 CAST(JSON_EXTRACT AS DECIMAL)

-- QUOTE_FLOW
INSERT INTO `sys_workflow_node` (`workflow_id`, `node_index`, `node_type`, `role_code`, `threshold`, `or_sign_required`)
SELECT id, 1, 'START', NULL, NULL, FALSE FROM `sys_workflow` WHERE `workflow_code` = 'QUOTE_FLOW'
UNION ALL
SELECT id, 2, 'APPROVAL',
  JSON_UNQUOTE(JSON_EXTRACT(`nodes_json`, '$[0].role')),
  CASE JSON_TYPE(JSON_EXTRACT(`nodes_json`, '$[0].threshold')) WHEN 'NULL' THEN NULL ELSE CAST(JSON_UNQUOTE(JSON_EXTRACT(`nodes_json`, '$[0].threshold')) AS DECIMAL(15,2)) END,
  FALSE
FROM `sys_workflow` WHERE `workflow_code` = 'QUOTE_FLOW'
UNION ALL
SELECT id, 3, 'APPROVAL',
  JSON_UNQUOTE(JSON_EXTRACT(`nodes_json`, '$[1].role')),
  CASE JSON_TYPE(JSON_EXTRACT(`nodes_json`, '$[1].threshold')) WHEN 'NULL' THEN NULL ELSE CAST(JSON_UNQUOTE(JSON_EXTRACT(`nodes_json`, '$[1].threshold')) AS DECIMAL(15,2)) END,
  FALSE
FROM `sys_workflow` WHERE `workflow_code` = 'QUOTE_FLOW'
UNION ALL
SELECT id, 4, 'APPROVAL',
  JSON_UNQUOTE(JSON_EXTRACT(`nodes_json`, '$[2].role')),
  CASE JSON_TYPE(JSON_EXTRACT(`nodes_json`, '$[2].threshold')) WHEN 'NULL' THEN NULL ELSE CAST(JSON_UNQUOTE(JSON_EXTRACT(`nodes_json`, '$[2].threshold')) AS DECIMAL(15,2)) END,
  FALSE
FROM `sys_workflow` WHERE `workflow_code` = 'QUOTE_FLOW'
UNION ALL
SELECT id, 5, 'END', NULL, NULL, FALSE FROM `sys_workflow` WHERE `workflow_code` = 'QUOTE_FLOW';

-- ORDER_FLOW
INSERT INTO `sys_workflow_node` (`workflow_id`, `node_index`, `node_type`, `role_code`, `threshold`, `or_sign_required`, `extra_check_json`)
SELECT id, 1, 'START', NULL, NULL, FALSE, NULL FROM `sys_workflow` WHERE `workflow_code` = 'ORDER_FLOW'
UNION ALL
SELECT id, 2, 'APPROVAL',
  JSON_UNQUOTE(JSON_EXTRACT(`nodes_json`, '$[0].role')),
  CASE JSON_TYPE(JSON_EXTRACT(`nodes_json`, '$[0].threshold')) WHEN 'NULL' THEN NULL ELSE CAST(JSON_UNQUOTE(JSON_EXTRACT(`nodes_json`, '$[0].threshold')) AS DECIMAL(15,2)) END,
  FALSE, '{"extra_check":"credit_limit"}'
FROM `sys_workflow` WHERE `workflow_code` = 'ORDER_FLOW'
UNION ALL
SELECT id, 3, 'APPROVAL',
  JSON_UNQUOTE(JSON_EXTRACT(`nodes_json`, '$[1].role')),
  CASE JSON_TYPE(JSON_EXTRACT(`nodes_json`, '$[1].threshold')) WHEN 'NULL' THEN NULL ELSE CAST(JSON_UNQUOTE(JSON_EXTRACT(`nodes_json`, '$[1].threshold')) AS DECIMAL(15,2)) END,
  FALSE, '{"extra_check":"credit_limit"}'
FROM `sys_workflow` WHERE `workflow_code` = 'ORDER_FLOW'
UNION ALL
SELECT id, 4, 'APPROVAL',
  JSON_UNQUOTE(JSON_EXTRACT(`nodes_json`, '$[2].role')),
  CASE JSON_TYPE(JSON_EXTRACT(`nodes_json`, '$[2].threshold')) WHEN 'NULL' THEN NULL ELSE CAST(JSON_UNQUOTE(JSON_EXTRACT(`nodes_json`, '$[2].threshold')) AS DECIMAL(15,2)) END,
  FALSE, '{"extra_check":"credit_limit"}'
FROM `sys_workflow` WHERE `workflow_code` = 'ORDER_FLOW'
UNION ALL
SELECT id, 5, 'END', NULL, NULL, FALSE, NULL FROM `sys_workflow` WHERE `workflow_code` = 'ORDER_FLOW';

-- PURCHASE_FLOW
INSERT INTO `sys_workflow_node` (`workflow_id`, `node_index`, `node_type`, `role_code`, `threshold`, `or_sign_required`)
SELECT id, 1, 'START', NULL, NULL, FALSE FROM `sys_workflow` WHERE `workflow_code` = 'PURCHASE_FLOW'
UNION ALL
SELECT id, 2, 'APPROVAL',
  JSON_UNQUOTE(JSON_EXTRACT(`nodes_json`, '$[0].role')),
  CASE JSON_TYPE(JSON_EXTRACT(`nodes_json`, '$[0].threshold')) WHEN 'NULL' THEN NULL ELSE CAST(JSON_UNQUOTE(JSON_EXTRACT(`nodes_json`, '$[0].threshold')) AS DECIMAL(15,2)) END,
  FALSE
FROM `sys_workflow` WHERE `workflow_code` = 'PURCHASE_FLOW'
UNION ALL
SELECT id, 3, 'APPROVAL',
  JSON_UNQUOTE(JSON_EXTRACT(`nodes_json`, '$[1].role')),
  CASE JSON_TYPE(JSON_EXTRACT(`nodes_json`, '$[1].threshold')) WHEN 'NULL' THEN NULL ELSE CAST(JSON_UNQUOTE(JSON_EXTRACT(`nodes_json`, '$[1].threshold')) AS DECIMAL(15,2)) END,
  FALSE
FROM `sys_workflow` WHERE `workflow_code` = 'PURCHASE_FLOW'
UNION ALL
SELECT id, 4, 'APPROVAL',
  JSON_UNQUOTE(JSON_EXTRACT(`nodes_json`, '$[2].role')),
  CASE JSON_TYPE(JSON_EXTRACT(`nodes_json`, '$[2].threshold')) WHEN 'NULL' THEN NULL ELSE CAST(JSON_UNQUOTE(JSON_EXTRACT(`nodes_json`, '$[2].threshold')) AS DECIMAL(15,2)) END,
  FALSE
FROM `sys_workflow` WHERE `workflow_code` = 'PURCHASE_FLOW'
UNION ALL
SELECT id, 5, 'END', NULL, NULL, FALSE FROM `sys_workflow` WHERE `workflow_code` = 'PURCHASE_FLOW';

-- PAYMENT_FLOW
INSERT INTO `sys_workflow_node` (`workflow_id`, `node_index`, `node_type`, `role_code`, `threshold`, `or_sign_required`)
SELECT id, 1, 'START', NULL, NULL, FALSE FROM `sys_workflow` WHERE `workflow_code` = 'PAYMENT_FLOW'
UNION ALL
SELECT id, 2, 'APPROVAL',
  JSON_UNQUOTE(JSON_EXTRACT(`nodes_json`, '$[0].role')),
  CASE JSON_TYPE(JSON_EXTRACT(`nodes_json`, '$[0].threshold')) WHEN 'NULL' THEN NULL ELSE CAST(JSON_UNQUOTE(JSON_EXTRACT(`nodes_json`, '$[0].threshold')) AS DECIMAL(15,2)) END,
  FALSE
FROM `sys_workflow` WHERE `workflow_code` = 'PAYMENT_FLOW'
UNION ALL
SELECT id, 3, 'APPROVAL',
  JSON_UNQUOTE(JSON_EXTRACT(`nodes_json`, '$[1].role')),
  CASE JSON_TYPE(JSON_EXTRACT(`nodes_json`, '$[1].threshold')) WHEN 'NULL' THEN NULL ELSE CAST(JSON_UNQUOTE(JSON_EXTRACT(`nodes_json`, '$[1].threshold')) AS DECIMAL(15,2)) END,
  FALSE
FROM `sys_workflow` WHERE `workflow_code` = 'PAYMENT_FLOW'
UNION ALL
SELECT id, 4, 'END', NULL, NULL, FALSE FROM `sys_workflow` WHERE `workflow_code` = 'PAYMENT_FLOW';

-- ============================================================
-- 迁移完成
-- ============================================================
