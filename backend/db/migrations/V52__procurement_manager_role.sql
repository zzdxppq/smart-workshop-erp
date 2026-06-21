-- ============================================================
-- V1.3.8 Story 4.2 · V52__procurement_manager_role.sql
-- 采购主管审批路由：PROCUREMENT_MANAGER 角色 + 权限 + sys_workflow_node 扩展
--
-- 关联：Story 4.2-采购主管审批路由.md + architect review 4.2-impl-review.md
--
-- 【precheck 校正 · 2026-06-13】原 Story 假设阈值路由存 sys_workflow_config 表，
-- V1.3.7 precheck 验证后纠正：实际生效的表是 sys_workflow_node（V2 迁移创建），
-- 由 WorkflowApprovalRouter 读 workflow_code + 节点链 amount.compareTo(threshold) 决策。
--
-- 【重要 · 2026-06-13 校正】sys_workflow_node 在两条路径下不一致：
--   - Spring Boot Flyway 路径：src/main/resources/db/migration/V2__workflow_split.sql
--     字段：create_time/update_time/create_by/update_by/version
--   - docker-compose 路径（生产部署）：db/migrations/V1-V6 挂载，
--     docker-compose 引用的是 ./db/migrations/V2__erp_platform.sql（该文件不存在）
--   因此本迁移必须自带 sys_workflow_node CREATE TABLE（兜底 docker-compose 路径）
--   字段命名与 V2 一致，避免 Story 3.1 字段名错配（is_deleted vs create_time）
-- ============================================================

USE `cnc_platform`;

-- 1. 兜底创建 sys_workflow_node（docker-compose 部署路径可能无 V2）
--    字段命名与 V2__workflow_split.sql 完全一致
CREATE TABLE IF NOT EXISTS sys_workflow_node (
    id BIGINT NOT NULL AUTO_INCREMENT,
    workflow_id BIGINT NOT NULL COMMENT 'sys_workflow.id（FK ON DELETE CASCADE）',
    node_index INT NOT NULL COMMENT '节点序号 1..N（严格递增）',
    node_type VARCHAR(20) NOT NULL COMMENT 'START/APPROVAL/CC/END',
    role_code VARCHAR(50) DEFAULT NULL COMMENT 'APPROVAL 节点必填（引用 sys_role.role_code）',
    threshold DECIMAL(15,2) DEFAULT NULL COMMENT '金额阈值（NULL=无限额）',
    or_sign_required TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'V1.3.7 P1 修补：OR 会签',
    extra_check_json TEXT COMMENT 'V1.3.7 条件扩展（如 extra_check=credit_limit）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT DEFAULT NULL,
    update_by BIGINT DEFAULT NULL,
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁',
    PRIMARY KEY (id),
    UNIQUE KEY uniq_workflow_node (workflow_id, node_index),
    KEY idx_role (role_code),
    KEY idx_node_type (node_type),
    KEY idx_workflow (workflow_id)
) ENGINE=InnoDB COMMENT='V1.3.8 工作流节点物理表（兜底 docker-compose 部署）';

-- 2. 新增 PROCUREMENT_MANAGER 角色（对齐 init.sql sys_role 字段）
INSERT IGNORE INTO sys_role (role_code, role_name, data_scope, status) VALUES
  ('PROCUREMENT_MANAGER', '采购主管', 'DEPT', 'ACTIVE');

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
INSERT IGNORE INTO sys_workflow_node
  (workflow_id, node_index, node_type, role_code, threshold, or_sign_required, create_time, update_time)
SELECT
    w.id, 5, 'APPROVAL', 'PROCUREMENT_MANAGER', 50000.00, 0, NOW(), NOW()
FROM sys_workflow w
WHERE w.workflow_code = 'PO_APPROVAL'
  AND NOT EXISTS (
    SELECT 1 FROM sys_workflow_node n
    WHERE n.workflow_id = w.id AND n.node_index = 5 AND n.role_code = 'PROCUREMENT_MANAGER'
  );

INSERT IGNORE INTO sys_workflow_node
  (workflow_id, node_index, node_type, role_code, threshold, or_sign_required, create_time, update_time)
SELECT
    w.id, 6, 'APPROVAL', 'PROCUREMENT_MANAGER', NULL, 0, NOW(), NOW()
FROM sys_workflow w
WHERE w.workflow_code = 'PO_APPROVAL'
  AND NOT EXISTS (
    SELECT 1 FROM sys_workflow_node n
    WHERE n.workflow_id = w.id AND n.node_index = 6 AND n.role_code = 'PROCUREMENT_MANAGER'
  );

-- 5. 兼容说明
--    V1.3.7 既有阈值节点（DEPT_MANAGER / GM 等）保留不动 → 兼容 legacy
--    路由算法：WorkflowApprovalRouter 按 node_index ASC 遍历，第一个 inScope 命中即返回
--    PROCUREMENT_MANAGER 节点排在原节点之后，金额 > 原 DEPT_MANAGER 阈值才会命中
--    应用层 route-preview 端点会基于此 node 链生成预览
