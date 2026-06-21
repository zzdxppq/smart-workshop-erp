-- ============================================================
-- V1.3.9 Sprint 12 Story 12.2 · V55__sys_printer.sql
-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- ============================================================

USE `cnc_platform`;

CREATE TABLE IF NOT EXISTS sys_printer (
    id                  BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    name                VARCHAR(50) NOT NULL COMMENT '打印机名称 · 1-50 字符 · UNIQUE',
    type                VARCHAR(20) NOT NULL COMMENT '类型：NORMAL / LABEL',
    ip                  VARCHAR(45) DEFAULT NULL COMMENT 'IPv4/IPv6 · LABEL 必填 · NORMAL 可空',
    port                INT NOT NULL DEFAULT 9100 COMMENT '端口 · 1-65535',
    protocol            VARCHAR(20) NOT NULL COMMENT '协议：ZPL / TSPL / PDF_BROWSER',
    model_suggestion    VARCHAR(30) NOT NULL DEFAULT 'OTHER' COMMENT '型号建议：DELI_DL888B / ZEBRA_ZD420 / TSC_TTP244PRO / OTHER',
    enabled             TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启停：1=启用 / 0=停用',
    status              VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN' COMMENT '心跳状态：ONLINE / OFFLINE / UNKNOWN',
    fail_count          INT NOT NULL DEFAULT 0 COMMENT '心跳连续失败计数 · 达 2 标 OFFLINE',
    last_heartbeat_at   DATETIME DEFAULT NULL COMMENT '最后心跳成功时间',
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    created_by          BIGINT NOT NULL COMMENT '创建人 · sys_user.id',
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    updated_by          BIGINT NOT NULL COMMENT '更新人 · sys_user.id',
    tenant_id           BIGINT NOT NULL COMMENT '租户 ID · sys_tenant.id',
    PRIMARY KEY (id),
    UNIQUE KEY uniq_printer_name (name),
    KEY idx_printer_type (type),
    KEY idx_printer_status (status),
    KEY idx_printer_enabled (enabled),
    KEY idx_printer_tenant (tenant_id),
    CONSTRAINT chk_printer_type   CHECK (type IN ('NORMAL','LABEL')),
    CONSTRAINT chk_printer_status CHECK (status IN ('ONLINE','OFFLINE','UNKNOWN')),
    CONSTRAINT chk_printer_proto  CHECK (protocol IN ('ZPL','TSPL','PDF_BROWSER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V1.3.9 打印机配置 · 含心跳状态 · 12.2 引入';

-- 字典：打印机型号预置（4 条）
-- 字段命名与 V3__system_params.sql 一致：dict_type / dict_code / dict_label / sort / status
INSERT IGNORE INTO sys_dict (dict_type, dict_code, dict_label, sort, status) VALUES
  ('PRINTER_MODEL', 'DELI_DL888B',    '得力 DL-888B',      1, 'ACTIVE'),
  ('PRINTER_MODEL', 'ZEBRA_ZD420',    '斑马 ZD420',        2, 'ACTIVE'),
  ('PRINTER_MODEL', 'TSC_TTP244PRO',  'TSC TTP-244 Pro',   3, 'ACTIVE'),
  ('PRINTER_MODEL', 'OTHER',          '其他',              9, 'ACTIVE');

-- 字典：打印机协议（3 条）
INSERT IGNORE INTO sys_dict (dict_type, dict_code, dict_label, sort, status) VALUES
  ('PRINTER_PROTOCOL', 'ZPL',         'ZPL (Zebra)',        1, 'ACTIVE'),
  ('PRINTER_PROTOCOL', 'TSPL',        'TSPL (TSC)',         2, 'ACTIVE'),
  ('PRINTER_PROTOCOL', 'PDF_BROWSER', 'PDF 浏览器打印',     3, 'ACTIVE');

-- 说明：
--   1. V55 不预置打印机实例 · admin 手工配置
--   2. ESC/POS 票据打印机明确不支持（文档层面说明 · 不在 DB 层校验）
--   3. 端口范围 1-65535 由应用层校验（DB 层 InnoDB CHECK 较复杂）
--   4. 心跳调度 60s 周期 · TCP Socket 探活 · fail_count >= 2 标 OFFLINE
--   5. NORMAL 类型保持 status=UNKNOWN（OS 打印队列无 IP · 不探活）
