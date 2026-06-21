-- ======================================================================
-- V88 · 品质专项检验增强 · FA 首件双签与判定完善
-- 2026-06-20
--
-- 增强内容：
-- 1. FA 状态机完善：新增"待双签"状态
-- 2. 双签字段：品检+工程师双签确认
-- 3. 驳回原因与重新检验流程
-- 4. 工单联动锁定
-- ======================================================================

USE `cnc_business`;

-- 1) FA 状态机完善：新增"待双签"状态
SET @db := DATABASE();

SET @add_fa_status_pending_sign := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_quality_fa ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT ''PENDING_INSPECT'' COMMENT ''状态：PENDING_INSPECT/INSPECTING/PENDING_SIGN/PASSED/FAILED'' AFTER result',
        'SELECT ''skip crm_quality_fa.status'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_quality_fa' AND COLUMN_NAME = 'status'
);
PREPARE _stmt FROM @add_fa_status_pending_sign; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;

-- 2) 双签字段：工程师签字
SET @add_engineer_sign := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_quality_fa ADD COLUMN engineer_user_id BIGINT DEFAULT NULL COMMENT ''工程师签字人ID'' AFTER inspector_user_id',
        'SELECT ''skip crm_quality_fa.engineer_user_id'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_quality_fa' AND COLUMN_NAME = 'engineer_user_id'
);
PREPARE _stmt FROM @add_engineer_sign; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;

SET @add_engineer_sign_at := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_quality_fa ADD COLUMN engineer_signed_at DATETIME DEFAULT NULL COMMENT ''工程师签字时间'' AFTER inspected_at',
        'SELECT ''skip crm_quality_fa.engineer_signed_at'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_quality_fa' AND COLUMN_NAME = 'engineer_signed_at'
);
PREPARE _stmt FROM @add_engineer_sign_at; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;

-- 3) 品检签字字段补充
SET @add_inspector_signed_at := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_quality_fa ADD COLUMN inspector_signed_at DATETIME DEFAULT NULL COMMENT ''品检签字时间'' AFTER inspector_user_id',
        'SELECT ''skip crm_quality_fa.inspector_signed_at'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_quality_fa' AND COLUMN_NAME = 'inspector_signed_at'
);
PREPARE _stmt FROM @add_inspector_signed_at; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;

-- 4) 驳回相关字段
SET @add_reject_reason := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_quality_fa ADD COLUMN reject_reason VARCHAR(500) DEFAULT NULL COMMENT ''驳回原因'' AFTER locked',
        'SELECT ''skip crm_quality_fa.reject_reason'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_quality_fa' AND COLUMN_NAME = 'reject_reason'
);
PREPARE _stmt FROM @add_reject_reason; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;

SET @add_rework_count := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_quality_fa ADD COLUMN rework_count INT NOT NULL DEFAULT 0 COMMENT ''返工次数'' AFTER reject_reason',
        'SELECT ''skip crm_quality_fa.rework_count'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_quality_fa' AND COLUMN_NAME = 'rework_count'
);
PREPARE _stmt FROM @add_rework_count; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;

-- 5) 工单锁定关联
SET @add_locked_work_order_no := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_quality_fa ADD COLUMN locked_work_order_no VARCHAR(32) DEFAULT NULL COMMENT ''被锁定的工单号'' AFTER locked',
        'SELECT ''skip crm_quality_fa.locked_work_order_no'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_quality_fa' AND COLUMN_NAME = 'locked_work_order_no'
);
PREPARE _stmt FROM @add_locked_work_order_no; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;

-- 6) 索引
SET @idx_fa_status := (
    SELECT IF(COUNT(*) = 0,
        'CREATE INDEX idx_fa_status ON crm_quality_fa (status)',
        'SELECT ''skip idx_fa_status'' AS note')
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_quality_fa' AND INDEX_NAME = 'idx_fa_status'
);
PREPARE _stmt FROM @idx_fa_status; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;

SET @idx_fa_engineer := (
    SELECT IF(COUNT(*) = 0,
        'CREATE INDEX idx_fa_engineer ON crm_quality_fa (engineer_user_id)',
        'SELECT ''skip idx_fa_engineer'' AS note')
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_quality_fa' AND INDEX_NAME = 'idx_fa_engineer'
);
PREPARE _stmt FROM @idx_fa_engineer; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;

-- 7) 字典：FA 状态
USE `cnc_platform`;

INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('FA_STATUS', 'PENDING_INSPECT', '待检验', 1, '待品检员检验', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);

INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('FA_STATUS', 'INSPECTING', '检验中', 2, '品检员正在检验', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);

INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('FA_STATUS', 'PENDING_SIGN', '待双签', 3, '品检通过，等待工程师签字', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);

INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('FA_STATUS', 'PASSED', '已通过', 4, '双签通过，可批量生产', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);

INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('FA_STATUS', 'FAILED', '已驳回', 5, '不合格，锁定工序', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);

INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('FA_STATUS', 'REWORK', '返工中', 6, '正在整改', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);
