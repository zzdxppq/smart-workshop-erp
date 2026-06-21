-- ======================================================================
-- V89 · 品质专项检验增强 · 不良品处置完善
-- 2026-06-20
--
-- 增强内容：
-- 1. 不良品原因分类（材料/工艺/设备/人为）
-- 2. 处置方式完善：返工自动转工单、报废扣减库存、让步接收审批
-- 3. 处置状态机完善
-- ======================================================================

USE `cnc_business`;

-- 1) 不良品状态机完善：新增"待处置"和"待审批"状态
SET @db := DATABASE();

SET @add_defect_disposition_status := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_quality_defect ADD COLUMN disposition_status VARCHAR(20) NOT NULL DEFAULT ''PENDING'' COMMENT ''处置状态：PENDING/APPROVED/REJECTED'' AFTER result',
        'SELECT ''skip crm_quality_defect.disposition_status'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_quality_defect' AND COLUMN_NAME = 'disposition_status'
);
PREPARE _stmt FROM @add_defect_disposition_status; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;

-- 2) 不良原因分类
SET @add_defect_cause_category := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_quality_defect ADD COLUMN cause_category VARCHAR(20) DEFAULT NULL COMMENT ''原因分类：MATERIAL/PROCESS/EQUIPMENT/HUMAN'' AFTER defect_type',
        'SELECT ''skip crm_quality_defect.cause_category'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_quality_defect' AND COLUMN_NAME = 'cause_category'
);
PREPARE _stmt FROM @add_defect_cause_category; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;

-- 3) 返工次数 + 返工工单关联
SET @add_rework_count := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_quality_defect ADD COLUMN rework_count INT NOT NULL DEFAULT 0 COMMENT ''返工次数'' AFTER cause_category',
        'SELECT ''skip crm_quality_defect.rework_count'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_quality_defect' AND COLUMN_NAME = 'rework_count'
);
PREPARE _stmt FROM @add_rework_count; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;

SET @add_rework_work_order_no := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_quality_defect ADD COLUMN rework_work_order_no VARCHAR(32) DEFAULT NULL COMMENT ''返工工单号'' AFTER rework_count',
        'SELECT ''skip crm_quality_defect.rework_work_order_no'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_quality_defect' AND COLUMN_NAME = 'rework_work_order_no'
);
PREPARE _stmt FROM @add_rework_work_order_no; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;

-- 4) 报废扣减库存关联
SET @add_scrap_inventory_deducted := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_quality_defect ADD COLUMN scrap_inventory_deducted TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''是否已扣减库存 0否1是'' AFTER rework_count',
        'SELECT ''skip crm_quality_defect.scrap_inventory_deducted'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_quality_defect' AND COLUMN_NAME = 'scrap_inventory_deducted'
);
PREPARE _stmt FROM @add_scrap_inventory_deducted; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;

-- 5) 让步接收审批字段
SET @add_concession_approver := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_quality_defect ADD COLUMN concession_approver_id BIGINT DEFAULT NULL COMMENT ''让步接收审批人ID'' AFTER scrap_inventory_deducted',
        'SELECT ''skip crm_quality_defect.concession_approver_id'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_quality_defect' AND COLUMN_NAME = 'concession_approver_id'
);
PREPARE _stmt FROM @add_concession_approver; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;

SET @add_concession_approved_at := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE crm_quality_defect ADD COLUMN concession_approved_at DATETIME DEFAULT NULL COMMENT ''让步接收审批时间'' AFTER concession_approver_id',
        'SELECT ''skip crm_quality_defect.concession_approved_at'' AS note')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_quality_defect' AND COLUMN_NAME = 'concession_approved_at'
);
PREPARE _stmt FROM @add_concession_approved_at; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;

-- 6) 索引
SET @idx_defect_disposition := (
    SELECT IF(COUNT(*) = 0,
        'CREATE INDEX idx_defect_disposition ON crm_quality_defect (disposition_status)',
        'SELECT ''skip idx_defect_disposition'' AS note')
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_quality_defect' AND INDEX_NAME = 'idx_defect_disposition'
);
PREPARE _stmt FROM @idx_defect_disposition; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;

SET @idx_defect_cause := (
    SELECT IF(COUNT(*) = 0,
        'CREATE INDEX idx_defect_cause ON crm_quality_defect (cause_category)',
        'SELECT ''skip idx_defect_cause'' AS note')
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'crm_quality_defect' AND INDEX_NAME = 'idx_defect_cause'
);
PREPARE _stmt FROM @idx_defect_cause; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;

-- 7) 字典：不良原因分类
USE `cnc_platform`;

INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('DEFECT_CAUSE_CATEGORY', 'MATERIAL', '材料', 1, '原材料/来料问题', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);

INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('DEFECT_CAUSE_CATEGORY', 'PROCESS', '工艺', 2, '工艺参数/操作SOP问题', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);

INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('DEFECT_CAUSE_CATEGORY', 'EQUIPMENT', '设备', 3, '设备/刀具/工装问题', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);

INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('DEFECT_CAUSE_CATEGORY', 'HUMAN', '人为', 4, '操作失误/人为损坏', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);

-- 8) 字典：处置状态
INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('DEFECT_DISPOSITION_STATUS', 'PENDING', '待处置', 1, '等待选择处置方式', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);

INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('DEFECT_DISPOSITION_STATUS', 'APPROVED', '已批准', 2, '让步接收已批准', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);

INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort, remark, created_at, updated_at)
VALUES ('DEFECT_DISPOSITION_STATUS', 'REJECTED', '已驳回', 3, '让步接收被驳回', NOW(), NOW())
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);
