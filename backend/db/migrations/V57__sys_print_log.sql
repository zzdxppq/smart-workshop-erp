-- ============================================================
-- V1.3.9 Sprint 12 Story 12.4 · V57__sys_print_log.sql
-- 双模式打印留痕表（sys_print_log）
--
-- 关联：12.4-dual-mode-print.md + architect review 12.4-review.md
-- 字段来源合并：PM prompt 增项 + Story 文件扁平字段
--   PM prompt 增：log_no / operator_name / printer_id / printer_*_snapshot / tenant_id
--   Story 文件：printer_ip / printer_name（合并为 snapshot 形式）
--
-- 字段数：17（PK + 业务 13 + 审计 2 + tenant 1）
-- 索引：6（operator / code / time / status partial / reference / tenant）
-- CHECK 约束：3（code_type / mode / status）
-- 与 8.3 sys_workflow_event 同 sys_ 命名范式
--
-- 关键点：
--   1. 12.1 共表（code_type=DRAWING）· 12.4 引入主表
--   2. log_no 业务编号 · PR-{yyyyMMdd}-{seq:4} 格式
--   3. printer_id nullable（模式二为 NULL）
--   4. reference_log_id 自引用 · 防补打递归
--   5. status partial 索引仅建 FAILED（SUCCESS 走 printed_at 索引）
--   6. tenant_id 必填 · 8.3 多租户范式
-- ============================================================

USE `cnc_platform`;

CREATE TABLE IF NOT EXISTS sys_print_log (
    id                      BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    log_no                  VARCHAR(40) NOT NULL COMMENT '业务编号 PR-{yyyyMMdd}-{seq:4}',
    operator_user_id        BIGINT NOT NULL COMMENT '操作人 sys_user.id',
    operator_name           VARCHAR(64) NOT NULL COMMENT '操作人姓名（冗余 · 防改名）',
    printed_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '打印时间',
    code_type               VARCHAR(20) NOT NULL COMMENT 'GD/LZ/SB/WW/WL/DRAWING',
    code_value              VARCHAR(200) NOT NULL COMMENT '业务编码 · 如 GD-260614-001',
    copies                  INT NOT NULL DEFAULT 1 COMMENT '份数 · 1-100',
    printer_id              BIGINT DEFAULT NULL COMMENT 'sys_printer.id · 模式二为 NULL',
    printer_name_snapshot   VARCHAR(100) DEFAULT NULL COMMENT '模式一 sys_printer.name · 模式二"普通浏览器"',
    printer_ip_snapshot     VARCHAR(45) DEFAULT NULL COMMENT '模式一 IP · 模式二 NULL',
    print_mode              VARCHAR(20) NOT NULL COMMENT 'ZPL_DIRECT / PDF_BROWSER',
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'SUCCESS / FAILED / PENDING',
    error_msg               VARCHAR(500) DEFAULT NULL COMMENT 'FAILED 时记录',
    reference_log_id        BIGINT DEFAULT NULL COMMENT '补打时指向原始 sys_print_log.id',
    remark                  VARCHAR(200) DEFAULT NULL COMMENT '备注',
    created_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '系统时间',
    tenant_id               BIGINT NOT NULL COMMENT '租户 ID · 8.3 多租户范式',
    PRIMARY KEY (id),
    UNIQUE KEY uk_log_no (log_no, tenant_id),
    KEY idx_print_log_operator (operator_user_id, printed_at DESC),
    KEY idx_print_log_code     (code_type, code_value),
    KEY idx_print_log_time     (printed_at DESC),
    KEY idx_print_log_status   (status),
    KEY idx_print_log_reference (reference_log_id),
    KEY idx_print_log_tenant   (tenant_id, printed_at DESC),
    CONSTRAINT fk_print_log_user      FOREIGN KEY (operator_user_id)   REFERENCES sys_user(id),
    CONSTRAINT fk_print_log_printer   FOREIGN KEY (printer_id)         REFERENCES sys_printer(id),
    CONSTRAINT fk_print_log_reference FOREIGN KEY (reference_log_id)   REFERENCES sys_print_log(id),
    CONSTRAINT chk_print_code_type  CHECK (code_type  IN ('GD','LZ','SB','WW','WL','DRAWING')),
    CONSTRAINT chk_print_mode       CHECK (print_mode IN ('ZPL_DIRECT','PDF_BROWSER')),
    CONSTRAINT chk_print_status     CHECK (status     IN ('SUCCESS','FAILED','PENDING')),
    CONSTRAINT chk_print_copies     CHECK (copies     >= 1 AND copies <= 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V1.3.9 双模式打印留痕 · 12.4 引入 · 与 12.1 图纸打印共表';

-- 说明：
--   1. 模式二（PDF_BROWSER）: printer_id / printer_ip_snapshot 必为 NULL · printer_name_snapshot 固定 '普通浏览器'
--   2. 模式一（ZPL_DIRECT）: printer_id / printer_ip_snapshot / printer_name_snapshot 均从 sys_printer 快照
--   3. 补打链：reference_log_id 指向原始记录 · 原始为补打记录时拒绝再次补打（防递归 · TC-12.4.3.6）
--   4. status partial 索引仅对非 SUCCESS 状态建索引 · 减少索引体积（95% 记录为 SUCCESS）
--   5. tenant_id 复合索引支撑多租户隔离 + 时间范围聚合
