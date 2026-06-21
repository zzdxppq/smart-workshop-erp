-- ============================================================
-- 昆山佰泰胜专属 ERP · 数据库一键初始化（空库专用 · 无 mock）
-- 库名：cnc_platform / cnc_business / cnc_production
-- 用法：mysql -h HOST -u USER -p --default-character-set=utf8mb4 < backend/db/init.sql
-- 维护：改 init.baseline.sql 或 migrations 后执行 build-init.ps1
-- 含全量 Mock（50 订单 + 50 员工 + 演示工单）：mysql ... < backend/db/init_data.sql
-- ============================================================

SET NAMES utf8mb4;
SET collation_connection = 'utf8mb4_unicode_ci';
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 昆山佰泰胜专属 ERP 系统 V1.3.7
-- 数据库初始化脚本
-- 生成时间：2026-06-09
-- 生成人：Architect agent（orchestrix · 鲁班）
-- 配套文档：docs/architect-handoff.md V1.1
-- 字符集：utf8mb4 + utf8mb4_unicode_ci
-- 引擎：InnoDB
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 物理库 1: cnc_platform
-- 用户/角色/工作流/字典/文件/审计/邮件
-- ============================================================
CREATE DATABASE IF NOT EXISTS `cnc_platform` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `cnc_platform`;

-- ---------- 用户 / 角色 ----------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`, `role_id`),
  KEY `idx_role` (`role_id`)
) ENGINE=InnoDB COMMENT='用户 / 角色表';

DROP TABLE IF EXISTS `sys_role_permission`;
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_id` BIGINT DEFAULT NULL COMMENT '父菜单 ID',
  `menu_code` VARCHAR(64) NOT NULL COMMENT '菜单编码',
  `menu_name` VARCHAR(100) NOT NULL COMMENT '菜单名称',
  `path` VARCHAR(255) NOT NULL COMMENT '前端路由 path',
  `menu_type` VARCHAR(20) NOT NULL DEFAULT 'MENU' COMMENT 'MODULE/MENU/ROUTE',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `icon` VARCHAR(64) DEFAULT NULL COMMENT '图标',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_menu_code` (`menu_code`),
  UNIQUE KEY `uniq_menu_path` (`path`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB COMMENT='系统菜单';

CREATE TABLE `sys_role_permission` (
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `menu_id` BIGINT NOT NULL COMMENT 'menuID',
  `action` VARCHAR(50) NOT NULL COMMENT 'action',
  PRIMARY KEY (`role_id`, `menu_id`, `action`)
) ENGINE=InnoDB COMMENT='角色菜单权限';

DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` VARCHAR(50) NOT NULL COMMENT '登录名',
  `password_hash` VARCHAR(255) NOT NULL COMMENT 'BCrypt(cost=12)',
  `real_name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT 'AES-256-GCM 加密',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `dept_id` BIGINT DEFAULT NULL COMMENT '部门ID',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE / DISABLED',
  `availability_status` VARCHAR(16) NOT NULL DEFAULT 'ON_DUTY' COMMENT 'ON_DUTY/ON_LEAVE/ON_TRIP/RESIGNED',
  `leave_no` VARCHAR(64) DEFAULT NULL COMMENT '请假单号',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_username` (`username`),
  KEY `idx_dept` (`dept_id`)
) ENGINE=InnoDB COMMENT='用户表';

DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码',
  `role_name` VARCHAR(100) NOT NULL COMMENT '角色名称',
  `data_scope` VARCHAR(20) NOT NULL DEFAULT 'SELF' COMMENT 'SELF/DEPT/ALL/CUSTOM',
  `amount_threshold` DECIMAL(15,2) DEFAULT NULL COMMENT '金额阈值',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_role_code` (`role_code`)
) ENGINE=InnoDB COMMENT='角色表';

DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id` BIGINT DEFAULT NULL COMMENT '父级ID',
  `dept_name` VARCHAR(100) NOT NULL COMMENT '部门名称',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
  PRIMARY KEY (`id`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB COMMENT='部门表';

DROP TABLE IF EXISTS `sys_position`;
CREATE TABLE `sys_position` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dept_id` BIGINT NOT NULL COMMENT '部门ID',
  `position_name` VARCHAR(100) NOT NULL COMMENT '职位名称',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序号',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='职位表';

-- ---------- 工作流 ----------
DROP TABLE IF EXISTS `sys_workflow`;
CREATE TABLE `sys_workflow` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `workflow_code` VARCHAR(50) NOT NULL COMMENT '工作流编码',
  `nodes_json` TEXT NOT NULL COMMENT '审批节点JSON',
  `conditions_json` TEXT COMMENT '条件JSON',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_workflow_code` (`workflow_code`)
) ENGINE=InnoDB COMMENT='审批工作流配置';

-- ---------- 字典 ----------
DROP TABLE IF EXISTS `sys_dict`;
CREATE TABLE `sys_dict` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dict_type` VARCHAR(50) NOT NULL COMMENT '字典类型',
  `dict_code` VARCHAR(50) NOT NULL COMMENT '字典编码',
  `dict_label` VARCHAR(100) NOT NULL COMMENT '字典标签',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_dict_type_code` (`dict_type`, `dict_code`)
) ENGINE=InnoDB COMMENT='数据字典';

-- V94 · 兼容 V83/V84/V85/V86/V88/V89 后续字典 migration（remark + 时间戳）
USE `cnc_platform`;
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'cnc_platform' AND TABLE_NAME = 'sys_dict' AND COLUMN_NAME = 'remark');
SET @sql := IF(@col = 0,
  'ALTER TABLE `cnc_platform`.`sys_dict` ADD COLUMN `remark` VARCHAR(500) DEFAULT NULL COMMENT ''备注'' AFTER `status`, ADD COLUMN `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'' AFTER `remark`, ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'' AFTER `created_at`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 文件（V1.3.6 加密元数据） ----------
DROP TABLE IF EXISTS `sys_file`;
CREATE TABLE `sys_file` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `bucket` VARCHAR(50) NOT NULL COMMENT 'drawings/contracts/reports/signed_scan',
  `object_key` VARCHAR(255) NOT NULL COMMENT '对象键',
  `original_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
  `size` BIGINT NOT NULL COMMENT '文件大小(字节)',
  `mime` VARCHAR(100) NOT NULL COMMENT 'MIME类型',
  `uploader_id` BIGINT NOT NULL COMMENT '上传人ID',
  `encryption_meta` VARCHAR(255) DEFAULT NULL COMMENT 'V1.3.6 AES-256-GCM, iv=base64',
  `encryption_kek_id` VARCHAR(64) DEFAULT NULL COMMENT 'V1.3.6 密钥版本',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_bucket_object` (`bucket`, `object_key`),
  KEY `idx_uploader` (`uploader_id`)
) ENGINE=InnoDB COMMENT='文件表（V1.3.6 支持 AES-256-GCM 加密）';

-- ---------- 审计日志 ----------
DROP TABLE IF EXISTS `sys_audit_log`;
CREATE TABLE `sys_audit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `module` VARCHAR(50) NOT NULL COMMENT 'module',
  `action` VARCHAR(50) NOT NULL COMMENT 'action',
  `before_json` TEXT COMMENT 'beforejson',
  `after_json` TEXT COMMENT 'afterjson',
  `ip` VARCHAR(50) COMMENT 'ip',
  `ts` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'ts',
  PRIMARY KEY (`id`),
  KEY `idx_user_module_action_ts` (`user_id`, `module`, `action`, `ts`)
) ENGINE=InnoDB COMMENT='审计日志（V1.3.6 签字件下载 100% 留痕）';

-- ---------- V1.3.6 签字件下载审计 ----------
DROP TABLE IF EXISTS `sys_download_log`;
CREATE TABLE `sys_download_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `file_id` BIGINT NOT NULL COMMENT '文件ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `ip` VARCHAR(50) COMMENT 'ip',
  `ts` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'ts',
  `action` VARCHAR(50) NOT NULL DEFAULT 'DOWNLOAD' COMMENT 'action',
  PRIMARY KEY (`id`),
  KEY `idx_file_user_ts` (`file_id`, `user_id`, `ts`)
) ENGINE=InnoDB COMMENT='下载审计日志（V1.3.6 新增）';

-- ---------- V1.3.7 邮件配置（singleton） ----------
DROP TABLE IF EXISTS `email_config`;
CREATE TABLE `email_config` (
  `id` INT NOT NULL DEFAULT 1 COMMENT '主键ID',
  `smtp_host` VARCHAR(100) NOT NULL DEFAULT 'smtp.163.com' COMMENT 'smtphost',
  `smtp_port` INT NOT NULL DEFAULT 465 COMMENT 'smtpport',
  `use_ssl` BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'usessl',
  `from_address` VARCHAR(100) NOT NULL COMMENT 'fromaddress',
  `auth_code_kek` VARCHAR(255) NOT NULL COMMENT 'KEK 加密的 163 授权码（不存明文）',
  `retry_policy` VARCHAR(100) NOT NULL DEFAULT '1h,6h,24h' COMMENT 'retrypolicy',
  `daily_quota` INT NOT NULL DEFAULT 5000 COMMENT 'dailyquota',
  `warn_threshold` DECIMAL(3,2) NOT NULL DEFAULT 0.80 COMMENT 'warnthreshold',
  `log_retention_days` INT NOT NULL DEFAULT 90 COMMENT 'logretentiondays',
  `attachment_max_size_mb` INT NOT NULL DEFAULT 10 COMMENT 'attachmentmaxsizemb',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  CONSTRAINT `singleton` CHECK (`id` = 1)
) ENGINE=InnoDB COMMENT='邮件配置（V1.3.7 新增 · singleton）';

-- ---------- V1.3.7 邮件发送日志 ----------
DROP TABLE IF EXISTS `email_send_log`;
CREATE TABLE `email_send_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `to_address` VARCHAR(255) NOT NULL COMMENT 'toaddress',
  `subject` VARCHAR(500) NOT NULL COMMENT '主题',
  `attachment_hash` VARCHAR(64) DEFAULT NULL COMMENT 'SHA-256',
  `smtp_response` VARCHAR(500) COMMENT 'smtpresponse',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SENT/FAILED/RETRY_1H/RETRY_6H/RETRY_24H/DEAD',
  `retry_count` INT NOT NULL DEFAULT 0 COMMENT '重试次数',
  `sent_at` DATETIME DEFAULT NULL COMMENT '发送时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_status_sent_at` (`status`, `sent_at`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB COMMENT='邮件发送日志（V1.3.7 新增 · 90 天保留）';

-- ---------- V1.3.7 邮件额度监控 ----------
DROP TABLE IF EXISTS `email_quota_daily`;
CREATE TABLE `email_quota_daily` (
  `date` DATE NOT NULL COMMENT 'date',
  `sent_count` INT NOT NULL DEFAULT 0 COMMENT 'sentcount',
  `quota` INT NOT NULL DEFAULT 5000 COMMENT 'quota',
  `warn_threshold` DECIMAL(3,2) NOT NULL DEFAULT 0.80 COMMENT 'warnthreshold',
  `last_alert_at` DATETIME DEFAULT NULL COMMENT 'last_alert时间',
  PRIMARY KEY (`date`)
) ENGINE=InnoDB COMMENT='邮件日额度（V1.3.7 新增）';

-- ---------- V1.3.7 厂商通知日志（V1.3.6 升级） ----------
DROP TABLE IF EXISTS `outsub_vendor_notify_log`;
CREATE TABLE `outsub_vendor_notify_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `vendor_id` BIGINT NOT NULL COMMENT '厂商ID',
  `channel` VARCHAR(20) NOT NULL DEFAULT 'email_163' COMMENT 'V1.3.7 收窄',
  `subject` VARCHAR(500) NOT NULL COMMENT '主题',
  `body` TEXT COMMENT '正文',
  `attachment_path` VARCHAR(255) COMMENT '附件路径',
  `status` VARCHAR(20) NOT NULL COMMENT '状态',
  `sent_at` DATETIME DEFAULT NULL COMMENT '发送时间',
  `retry_count` INT NOT NULL DEFAULT 0 COMMENT '重试次数',
  PRIMARY KEY (`id`),
  KEY `idx_vendor_sent_at` (`vendor_id`, `sent_at`)
) ENGINE=InnoDB COMMENT='厂商通知日志（V1.3.7 收窄为单 163 邮箱）';


-- ============================================================
-- 物理库 2: cnc_business
-- 客户/销售/物料/仓储/品质/财务/人事/报表/委外/cost
-- ============================================================
CREATE DATABASE IF NOT EXISTS `cnc_business` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `cnc_business`;

-- ========== crm 客户域 ==========
DROP TABLE IF EXISTS `crm_customer`;
CREATE TABLE `crm_customer` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `customer_code` VARCHAR(50) NOT NULL COMMENT '客户编码',
  `name` VARCHAR(200) NOT NULL COMMENT '名称',
  `contact_name` VARCHAR(100) DEFAULT NULL COMMENT '联系人',
  `contact_phone` VARCHAR(32) DEFAULT NULL COMMENT '联系电话',
  `contact_email` VARCHAR(128) DEFAULT NULL COMMENT '联系邮箱（报价发送）',
  `industry` VARCHAR(50) COMMENT 'industry',
  `credit_limit` DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT 'creditlimit',
  `owner_id` BIGINT NOT NULL COMMENT 'ownerID',
  `protect_until` DATE DEFAULT NULL COMMENT '保护期 30 天可配',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_customer_code` (`customer_code`),
  KEY `idx_owner_protect` (`owner_id`, `protect_until`)
) ENGINE=InnoDB COMMENT='crm 客户域表';

DROP TABLE IF EXISTS `crm_contact`;
CREATE TABLE `crm_contact` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `customer_id` BIGINT NOT NULL COMMENT '客户ID',
  `name` VARCHAR(100) NOT NULL COMMENT '名称',
  `position` VARCHAR(50) COMMENT 'position',
  `phone` VARCHAR(20) COMMENT '手机号',
  `email` VARCHAR(100) COMMENT '邮箱',
  `is_primary` BOOLEAN DEFAULT FALSE COMMENT '是否primary(0否1是)',
  PRIMARY KEY (`id`),
  KEY `idx_customer` (`customer_id`)
) ENGINE=InnoDB COMMENT='crm 客户域表';

DROP TABLE IF EXISTS `crm_follow_record`;
CREATE TABLE `crm_follow_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `customer_id` BIGINT NOT NULL COMMENT '客户ID',
  `owner_id` BIGINT NOT NULL COMMENT 'ownerID',
  `follow_time` DATETIME NOT NULL COMMENT 'followtime',
  `content` TEXT COMMENT 'content',
  `stage` VARCHAR(20) COMMENT 'stage',
  `next_follow_time` DATETIME COMMENT 'nextfollowtime',
  PRIMARY KEY (`id`),
  KEY `idx_customer_owner` (`customer_id`, `owner_id`)
) ENGINE=InnoDB COMMENT='crm 客户域表';

DROP TABLE IF EXISTS `crm_customer_share`;
CREATE TABLE `crm_customer_share` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `customer_id` BIGINT NOT NULL COMMENT '客户ID',
  `shared_user_id` BIGINT NOT NULL COMMENT 'shared_userID',
  `permission` VARCHAR(20) NOT NULL COMMENT '权限',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='crm 客户域表';

-- ========== sales 销售域 ==========
DROP TABLE IF EXISTS `sales_quote`;
CREATE TABLE `sales_quote` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `quote_no` VARCHAR(50) NOT NULL COMMENT '报价编号',
  `customer_id` BIGINT NOT NULL COMMENT '客户ID',
  `currency` VARCHAR(10) NOT NULL DEFAULT 'CNY' COMMENT '币种',
  `delivery_date` DATE COMMENT '交期',
  `total_amount` DECIMAL(15,2) NOT NULL COMMENT '总金额',
  `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态',
  `owner_id` BIGINT NOT NULL COMMENT 'ownerID',
  `approver_id` BIGINT COMMENT 'approverID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_quote_no` (`quote_no`),
  KEY `idx_owner_status_date` (`owner_id`, `status`, `created_at` DESC)
) ENGINE=InnoDB COMMENT='sales 销售域表';

DROP TABLE IF EXISTS `sales_quote_item`;
CREATE TABLE `sales_quote_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `quote_id` BIGINT NOT NULL COMMENT '报价单ID',
  `drawing_no` VARCHAR(100) COMMENT '图号',
  `material` VARCHAR(100) COMMENT '物料',
  `spec` VARCHAR(200) COMMENT '规格',
  `qty` DECIMAL(15,2) NOT NULL COMMENT '数量',
  `unit_price` DECIMAL(15,2) NOT NULL COMMENT '单价',
  `amount` DECIMAL(15,2) NOT NULL COMMENT '金额',
  `is_fa` BOOLEAN DEFAULT FALSE COMMENT '是否首件(0否1是)',
  `is_new` BOOLEAN DEFAULT FALSE COMMENT '是否新品(0否1是)',
  PRIMARY KEY (`id`),
  KEY `idx_quote` (`quote_id`)
) ENGINE=InnoDB COMMENT='sales 销售域表';

DROP TABLE IF EXISTS `sales_order`;
CREATE TABLE `sales_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no` VARCHAR(50) NOT NULL COMMENT '订单号',
  `customer_id` BIGINT NOT NULL COMMENT '客户ID',
  `quote_id` BIGINT COMMENT '报价单ID',
  `order_type` VARCHAR(20) NOT NULL DEFAULT 'STANDARD' COMMENT '订单type',
  `is_fa` BOOLEAN DEFAULT FALSE COMMENT '是否首件(0否1是)',
  `total_amount` DECIMAL(15,2) NOT NULL COMMENT '总金额',
  `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态',
  `owner_id` BIGINT NOT NULL COMMENT 'ownerID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_order_no` (`order_no`),
  KEY `idx_customer_status` (`customer_id`, `status`)
) ENGINE=InnoDB COMMENT='sales 销售域表';

DROP TABLE IF EXISTS `sales_order_item`;
CREATE TABLE `sales_order_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `line_no` INT NOT NULL COMMENT 'line编号',
  `product_code` VARCHAR(50) NOT NULL COMMENT '产品编码',
  `qty` DECIMAL(15,2) NOT NULL COMMENT '数量',
  `unit_price` DECIMAL(15,2) NOT NULL COMMENT '单价',
  `amount` DECIMAL(15,2) NOT NULL COMMENT '金额',
  `delivery_date` DATE COMMENT '交期',
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB COMMENT='sales 销售域表';

DROP TABLE IF EXISTS `sales_order_change`;
CREATE TABLE `sales_order_change` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `change_type` VARCHAR(20) NOT NULL COMMENT 'changetype',
  `before_json` TEXT COMMENT 'beforejson',
  `after_json` TEXT COMMENT 'afterjson',
  `changed_by` BIGINT NOT NULL COMMENT 'changedby',
  `changed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'changed时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='sales 销售域表';

DROP TABLE IF EXISTS `sales_contract`;
CREATE TABLE `sales_contract` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `contract_no` VARCHAR(50) NOT NULL COMMENT '合同号（=销售订单号 order_no，PRD AC-2.4）',
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `file_id` BIGINT COMMENT '文件ID',
  `signed_at` DATE COMMENT '签约日期',
  `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_contract_no` (`contract_no`)
) ENGINE=InnoDB COMMENT='sales 销售域表';

DROP TABLE IF EXISTS `sales_payment_plan`;
CREATE TABLE `sales_payment_plan` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `contract_id` BIGINT NOT NULL COMMENT '合同ID',
  `period_no` INT NOT NULL COMMENT '期次',
  `plan_date` DATE NOT NULL COMMENT '计划日期',
  `plan_amount` DECIMAL(15,2) NOT NULL COMMENT '计划金额',
  `actual_amount` DECIMAL(15,2) DEFAULT 0 COMMENT '实际金额',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='sales 销售域表';

DROP TABLE IF EXISTS `sales_receipt`;
CREATE TABLE `sales_receipt` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `contract_id` BIGINT NOT NULL COMMENT '合同ID',
  `receipt_date` DATE NOT NULL COMMENT '收款日期',
  `amount` DECIMAL(15,2) NOT NULL COMMENT '金额',
  `payer` VARCHAR(200) COMMENT '付款方/方式',
  `remark` TEXT COMMENT '备注',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='sales 销售域表';

-- ========== mdm 物料域 ==========
DROP TABLE IF EXISTS `mdm_material`;
CREATE TABLE `mdm_material` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `material_code` VARCHAR(50) NOT NULL COMMENT '物料编码',
  `name` VARCHAR(200) NOT NULL COMMENT '名称',
  `category` VARCHAR(50) NOT NULL COMMENT '分类',
  `spec` VARCHAR(200) COMMENT '规格',
  `unit` VARCHAR(20) NOT NULL COMMENT '单位',
  `safety_stock_min` DECIMAL(15,2) DEFAULT 0 COMMENT 'safetystockmin',
  `safety_stock_max` DECIMAL(15,2) DEFAULT 0 COMMENT 'safetystockmax',
  `price_limit` DECIMAL(15,2) COMMENT 'pricelimit',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_material_code` (`material_code`)
) ENGINE=InnoDB COMMENT='mdm 物料域表';

DROP TABLE IF EXISTS `mdm_bom_header`;
CREATE TABLE `mdm_bom_header` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `product_code` VARCHAR(50) NOT NULL COMMENT '产品编码',
  `bom_version` VARCHAR(20) NOT NULL COMMENT 'BOMversion',
  `type` VARCHAR(20) NOT NULL DEFAULT 'DESIGN' COMMENT '类型',
  `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态',
  `published_at` DATETIME COMMENT 'published时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_product_version` (`product_code`, `bom_version`)
) ENGINE=InnoDB COMMENT='mdm 物料域表';

DROP TABLE IF EXISTS `mdm_bom_line`;
CREATE TABLE `mdm_bom_line` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `bom_header_id` BIGINT NOT NULL COMMENT 'bom_headerID',
  `parent_code` VARCHAR(50) NOT NULL COMMENT 'parent编码',
  `child_code` VARCHAR(50) NOT NULL COMMENT 'child编码',
  `qty` DECIMAL(15,4) NOT NULL COMMENT '数量',
  `loss_rate` DECIMAL(5,4) DEFAULT 0 COMMENT 'lossrate',
  `process_no` VARCHAR(20) COMMENT '工艺编号',
  PRIMARY KEY (`id`),
  KEY `idx_bom_header_line` (`bom_header_id`, `parent_code`)
) ENGINE=InnoDB COMMENT='mdm 物料域表';

DROP TABLE IF EXISTS `mdm_process`;
CREATE TABLE `mdm_process` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `process_code` VARCHAR(20) NOT NULL COMMENT '工序编码',
  `process_name` VARCHAR(50) NOT NULL COMMENT '工序名称',
  `std_time_min` DECIMAL(10,2) NOT NULL COMMENT 'stdtimemin',
  `machine_type` VARCHAR(20) COMMENT 'machinetype',
  `unit_price` DECIMAL(15,2) NOT NULL COMMENT '单价',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_process_code` (`process_code`)
) ENGINE=InnoDB COMMENT='mdm 物料域表';

DROP TABLE IF EXISTS `mdm_product_route`;
CREATE TABLE `mdm_product_route` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `product_code` VARCHAR(50) NOT NULL COMMENT '产品编码',
  `process_seq` INT NOT NULL COMMENT '工序序号',
  `process_code` VARCHAR(20) NOT NULL COMMENT '工序编码',
  `is_outsource` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否委外(0否1是)',
  PRIMARY KEY (`id`),
  KEY `idx_product` (`product_code`)
) ENGINE=InnoDB COMMENT='mdm 物料域表';

-- ========== warehouse 仓储域 ==========
DROP TABLE IF EXISTS `wms_warehouse`;
CREATE TABLE `wms_warehouse` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `warehouse_code` VARCHAR(20) NOT NULL COMMENT '仓库编码',
  `warehouse_name` VARCHAR(100) NOT NULL COMMENT '仓库名称',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_warehouse_code` (`warehouse_code`)
) ENGINE=InnoDB COMMENT='warehouse 仓储域表';

DROP TABLE IF EXISTS `wms_location`;
CREATE TABLE `wms_location` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `warehouse_id` BIGINT NOT NULL COMMENT '仓库ID',
  `location_code` VARCHAR(20) NOT NULL COMMENT '库位编码',
  `capacity` DECIMAL(15,2) COMMENT 'capacity',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_location_code` (`location_code`)
) ENGINE=InnoDB COMMENT='warehouse 仓储域表';

DROP TABLE IF EXISTS `wms_inbound`;
CREATE TABLE `wms_inbound` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `inbound_no` VARCHAR(50) NOT NULL COMMENT 'inbound编号',
  `type` VARCHAR(20) NOT NULL COMMENT '类型',
  `source_order_id` BIGINT COMMENT 'source_orderID',
  `warehouse_id` BIGINT NOT NULL COMMENT '仓库ID',
  `operator_id` BIGINT NOT NULL COMMENT '操作人ID',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_inbound_no` (`inbound_no`)
) ENGINE=InnoDB COMMENT='warehouse 仓储域表';

DROP TABLE IF EXISTS `wms_inbound_item`;
CREATE TABLE `wms_inbound_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `inbound_id` BIGINT NOT NULL COMMENT 'inboundID',
  `material_id` BIGINT NOT NULL COMMENT '物料ID',
  `batch_no` VARCHAR(50) NOT NULL COMMENT '批次号',
  `qty` DECIMAL(15,2) NOT NULL COMMENT '数量',
  `location_id` BIGINT COMMENT '库位ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='warehouse 仓储域表';

DROP TABLE IF EXISTS `wms_outbound`;
CREATE TABLE `wms_outbound` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `outbound_no` VARCHAR(50) NOT NULL COMMENT 'outbound编号',
  `type` VARCHAR(20) NOT NULL COMMENT '类型',
  `source_order_id` BIGINT COMMENT 'source_orderID',
  `warehouse_id` BIGINT NOT NULL COMMENT '仓库ID',
  `operator_id` BIGINT NOT NULL COMMENT '操作人ID',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_outbound_no` (`outbound_no`)
) ENGINE=InnoDB COMMENT='warehouse 仓储域表';

DROP TABLE IF EXISTS `wms_outbound_item`;
CREATE TABLE `wms_outbound_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `outbound_id` BIGINT NOT NULL COMMENT 'outboundID',
  `material_id` BIGINT NOT NULL COMMENT '物料ID',
  `batch_no` VARCHAR(50) NOT NULL COMMENT '批次号',
  `qty` DECIMAL(15,2) NOT NULL COMMENT '数量',
  `location_id` BIGINT COMMENT '库位ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='warehouse 仓储域表';

DROP TABLE IF EXISTS `wms_inventory`;
CREATE TABLE `wms_inventory` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `material_id` BIGINT NOT NULL COMMENT '物料ID',
  `warehouse_id` BIGINT NOT NULL COMMENT '仓库ID',
  `batch_no` VARCHAR(50) NOT NULL COMMENT '批次号',
  `qty_on_hand` DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT 'qtyonhand',
  `qty_frozen` DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT 'qtyfrozen',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_material_wh_batch` (`material_id`, `warehouse_id`, `batch_no`)
) ENGINE=InnoDB COMMENT='warehouse 仓储域表';

DROP TABLE IF EXISTS `wms_batch`;
CREATE TABLE `wms_batch` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `material_id` BIGINT NOT NULL COMMENT '物料ID',
  `batch_no` VARCHAR(50) NOT NULL COMMENT '批次号',
  `production_date` DATE COMMENT 'production日期',
  `expiry_date` DATE COMMENT 'expiry日期',
  `supplier_id` BIGINT COMMENT '供应商ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_material_batch` (`material_id`, `batch_no`)
) ENGINE=InnoDB COMMENT='warehouse 仓储域表';

-- ========== quality 品质域 ==========
DROP TABLE IF EXISTS `qc_inspection`;
CREATE TABLE `qc_inspection` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `inspection_no` VARCHAR(50) NOT NULL COMMENT '检验编号',
  `type` VARCHAR(20) NOT NULL COMMENT 'INCOMING/PROCESS/FINAL/FA/CMM',
  `workorder_id` BIGINT COMMENT '工单ID',
  `material_id` BIGINT COMMENT '物料ID',
  `inspector_id` BIGINT COMMENT 'inspectorID',
  `engineer_id` BIGINT COMMENT 'engineerID',
  `result` VARCHAR(20) COMMENT '结果',
  `report_file_id` BIGINT COMMENT 'report_fileID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_inspection_no` (`inspection_no`)
) ENGINE=InnoDB COMMENT='quality 品质域表';

DROP TABLE IF EXISTS `qc_defect`;
CREATE TABLE `qc_defect` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `inspection_id` BIGINT NOT NULL COMMENT '检验ID',
  `reason_category` VARCHAR(20) NOT NULL COMMENT 'MATERIAL/PROCESS/EQUIPMENT/HUMAN',
  `handle_type` VARCHAR(20) NOT NULL COMMENT 'REWORK/SCRAP/CONDITIONAL',
  `qty` DECIMAL(15,2) NOT NULL COMMENT '数量',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='quality 品质域表';

-- ========== finance 财务域 ==========
DROP TABLE IF EXISTS `fin_receivable`;
CREATE TABLE `fin_receivable` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `customer_id` BIGINT NOT NULL COMMENT '客户ID',
  `amount` DECIMAL(15,2) NOT NULL COMMENT '金额',
  `due_date` DATE NOT NULL COMMENT 'due日期',
  `paid_amount` DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT 'paid金额',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='finance 财务域表';

DROP TABLE IF EXISTS `fin_payable`;
CREATE TABLE `fin_payable` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `source_order_id` BIGINT NOT NULL COMMENT 'source_orderID',
  `source_type` VARCHAR(20) NOT NULL COMMENT '来源类型',
  `vendor_id` BIGINT NOT NULL COMMENT '厂商ID',
  `amount` DECIMAL(15,2) NOT NULL COMMENT '金额',
  `due_date` DATE NOT NULL COMMENT 'due日期',
  `paid_amount` DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT 'paid金额',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='finance 财务域表';

DROP TABLE IF EXISTS `fin_payment`;
CREATE TABLE `fin_payment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `payment_no` VARCHAR(50) NOT NULL COMMENT '回款编号',
  `vendor_id` BIGINT NOT NULL COMMENT '厂商ID',
  `amount` DECIMAL(15,2) NOT NULL COMMENT '金额',
  `approvers_json` TEXT COMMENT 'approversjson',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_payment_no` (`payment_no`)
) ENGINE=InnoDB COMMENT='finance 财务域表';

DROP TABLE IF EXISTS `fin_receipt`;
CREATE TABLE `fin_receipt` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `receipt_no` VARCHAR(50) NOT NULL COMMENT '收款编号',
  `customer_id` BIGINT NOT NULL COMMENT '客户ID',
  `amount` DECIMAL(15,2) NOT NULL COMMENT '金额',
  `contract_id` BIGINT COMMENT '合同ID',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_receipt_no` (`receipt_no`)
) ENGINE=InnoDB COMMENT='finance 财务域表';

DROP TABLE IF EXISTS `fin_cost`;
CREATE TABLE `fin_cost` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `workorder_id` BIGINT NOT NULL COMMENT '工单ID',
  `material_cost` DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT '物料成本',
  `labor_cost` DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT 'labor成本',
  `mfg_cost` DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT 'mfg成本',
  `outsource_cost` DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT '委外成本',
  `total_cost` DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT '总成本',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_workorder` (`workorder_id`)
) ENGINE=InnoDB COMMENT='finance 财务域表';

DROP TABLE IF EXISTS `fin_ar_aging`;
CREATE TABLE `fin_ar_aging` (
  `snapshot_date` DATE NOT NULL COMMENT '快照日期',
  `customer_id` BIGINT NOT NULL COMMENT '客户ID',
  `bucket_0_30` DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT 'bucket030',
  `bucket_30_60` DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT 'bucket3060',
  `bucket_60_90` DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT 'bucket6090',
  `bucket_90_plus` DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT 'bucket90plus',
  PRIMARY KEY (`snapshot_date`, `customer_id`)
) ENGINE=InnoDB COMMENT='finance 财务域表';

-- ========== hr 人事域 ==========
DROP TABLE IF EXISTS `hr_employee`;
CREATE TABLE `hr_employee` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `employee_no` VARCHAR(20) NOT NULL COMMENT '员工编号',
  `name` VARCHAR(50) NOT NULL COMMENT '名称',
  `id_card` VARCHAR(255) DEFAULT NULL COMMENT 'V1.3.6 AES-256-GCM 加密',
  `phone` VARCHAR(255) DEFAULT NULL COMMENT 'V1.3.6 AES-256-GCM 加密',
  `hire_date` DATE NOT NULL COMMENT 'hire日期',
  `dept_id` BIGINT COMMENT '部门ID',
  `position_id` BIGINT COMMENT 'positionID',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_employee_no` (`employee_no`)
) ENGINE=InnoDB COMMENT='hr 人事域表';

DROP TABLE IF EXISTS `hr_attendance`;
CREATE TABLE `hr_attendance` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `employee_id` BIGINT NOT NULL COMMENT '员工ID',
  `clock_in` DATETIME NOT NULL COMMENT 'clockin',
  `clock_out` DATETIME COMMENT 'clockout',
  `location_lat` DECIMAL(10,6) COMMENT '库位lat',
  `location_lng` DECIMAL(10,6) COMMENT '库位lng',
  `is_late` BOOLEAN DEFAULT FALSE COMMENT '是否late(0否1是)',
  `is_early` BOOLEAN DEFAULT FALSE COMMENT '是否early(0否1是)',
  PRIMARY KEY (`id`),
  KEY `idx_employee_date` (`employee_id`, `clock_in`)
) ENGINE=InnoDB COMMENT='hr 人事域表';

DROP TABLE IF EXISTS `hr_salary`;
CREATE TABLE `hr_salary` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `employee_id` BIGINT NOT NULL COMMENT '员工ID',
  `period` VARCHAR(10) NOT NULL COMMENT '2026-05',
  `base` DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT 'base',
  `post` DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT 'post',
  `performance` DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT 'performance',
  `overtime` DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT 'overtime',
  `social` DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT 'social',
  `tax` DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT 'tax',
  `net` DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT 'net',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_employee_period` (`employee_id`, `period`)
) ENGINE=InnoDB COMMENT='hr 人事域表';

DROP TABLE IF EXISTS `hr_recruitment_plan`;
CREATE TABLE `hr_recruitment_plan` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `position` VARCHAR(50) NOT NULL COMMENT 'position',
  `qty` INT NOT NULL COMMENT '数量',
  `deadline` DATE COMMENT 'deadline',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='hr 人事域表';

-- ========== V1.3.4 委外域（重大升级） ==========
DROP TABLE IF EXISTS `outsub_vendor`;
CREATE TABLE `outsub_vendor` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `vendor_code` VARCHAR(50) NOT NULL COMMENT '厂商编码',
  `vendor_name` VARCHAR(200) NOT NULL COMMENT '厂商名称',
  `capabilities_json` VARCHAR(500) COMMENT '能力标签JSON',
  `credit_level` VARCHAR(2) NOT NULL DEFAULT 'C' COMMENT '信用等级',
  `contact_email` VARCHAR(100) NOT NULL COMMENT 'V1.3.7 仍必填',
  `contact_phone` VARCHAR(20) DEFAULT NULL COMMENT 'V1.3.7 改选填',
  `contact_name` VARCHAR(50) COMMENT '联系人',
  `default_recon_email` VARCHAR(100) COMMENT 'V1.3.6 对账单邮箱',
  `notify_channel` VARCHAR(20) NOT NULL DEFAULT 'email_163' COMMENT 'V1.3.7 收窄',
  `avg_delivery_days` INT DEFAULT NULL COMMENT 'V1.3.4 近 3 次中位数',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_vendor_code` (`vendor_code`)
) ENGINE=InnoDB COMMENT='厂商资料（V1.3.7 邮箱必填 + 电话选填）';

-- V1.3.7 工序分配职责分离
DROP TABLE IF EXISTS `outsub_allocation`;
CREATE TABLE `outsub_allocation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `workorder_id` BIGINT NOT NULL COMMENT '工单ID',
  `process_seq` INT NOT NULL COMMENT '工序序号',
  `decision` VARCHAR(20) NOT NULL COMMENT 'INHOUSE/OUTSOURCE',
  `decided_by_user_id` BIGINT NOT NULL COMMENT '生管 ID（V1.3.7）',
  `decided_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'decided时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_workorder_process` (`workorder_id`, `process_seq`),
  KEY `idx_decided_by` (`decided_by_user_id`)
) ENGINE=InnoDB COMMENT='工序分配（V1.3.7 新增 · 生管）';

DROP TABLE IF EXISTS `outsub_allocation_vendor`;
CREATE TABLE `outsub_allocation_vendor` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `allocation_id` BIGINT NOT NULL COMMENT '工序分配ID',
  `vendor_id` BIGINT NOT NULL COMMENT '厂商ID',
  `unit_price` DECIMAL(15,4) NOT NULL COMMENT '单价',
  `delivery_date` DATE NOT NULL COMMENT '交期',
  `selected_by_user_id` BIGINT NOT NULL COMMENT '采购 ID（V1.3.7）',
  `selected_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'selected时间',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
  PRIMARY KEY (`id`),
  KEY `idx_allocation` (`allocation_id`),
  KEY `idx_vendor` (`vendor_id`)
) ENGINE=InnoDB COMMENT='工序-厂商选择（V1.3.7 新增 · 采购）';

-- V1.3.4 7 状态机
DROP TABLE IF EXISTS `outsub_order`;
CREATE TABLE `outsub_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `outsource_no` VARCHAR(50) NOT NULL COMMENT '委外单号',
  `workorder_id` BIGINT NOT NULL COMMENT '工单ID',
  `vendor_id` BIGINT NOT NULL COMMENT '厂商ID',
  `process_code` VARCHAR(20) NOT NULL COMMENT '工序编码',
  `qty` DECIMAL(15,2) NOT NULL COMMENT '数量',
  `unit_price` DECIMAL(15,4) NOT NULL COMMENT '单价',
  `total_amount` DECIMAL(15,2) NOT NULL COMMENT '总金额',
  `status` VARCHAR(30) NOT NULL DEFAULT 'PENDING_SHIP' COMMENT '7 状态机 + NOTIFIED_REPAIR',
  `rework_count` INT NOT NULL DEFAULT 0 COMMENT 'V1.3.4 返修次数',
  `original_outsub_order_id` BIGINT DEFAULT NULL COMMENT 'V1.3.4 返修关联原单',
  `is_rework_reinspection` BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'V1.3.4 复检标志',
  `delivery_date` DATE COMMENT '交期',
  `actual_delivery_date` DATE COMMENT 'actual_delivery日期',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_outsource_no` (`outsource_no`),
  KEY `idx_status_vendor` (`status`, `vendor_id`),
  KEY `idx_rework_count` (`rework_count`),
  KEY `idx_original` (`original_outsub_order_id`)
) ENGINE=InnoDB COMMENT='委外订单（V1.3.4 升级 7 状态机）';

DROP TABLE IF EXISTS `outsub_order_item`;
CREATE TABLE `outsub_order_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `outsource_id` BIGINT NOT NULL COMMENT '委外ID',
  `material_id` BIGINT NOT NULL COMMENT '物料ID',
  `qty` DECIMAL(15,2) NOT NULL COMMENT '数量',
  `unit_price` DECIMAL(15,4) NOT NULL COMMENT '单价',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='V1.3.4 7 状态机表';

DROP TABLE IF EXISTS `outsub_order_history`;
CREATE TABLE `outsub_order_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `outsource_id` BIGINT NOT NULL COMMENT '委外ID',
  `from_status` VARCHAR(30) COMMENT 'fromstatus',
  `to_status` VARCHAR(30) NOT NULL COMMENT 'tostatus',
  `operator_id` BIGINT NOT NULL COMMENT '操作人ID',
  `ts` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'ts',
  `remark` TEXT COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_outsource_ts` (`outsource_id`, `ts`)
) ENGINE=InnoDB COMMENT='委外订单状态机日志（V1.3.4 新增）';

-- V1.3.6 月度对账（不含"线下"）
DROP TABLE IF EXISTS `outsub_reconcile`;
CREATE TABLE `outsub_reconcile` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `vendor_id` BIGINT NOT NULL COMMENT '厂商ID',
  `period` VARCHAR(10) NOT NULL COMMENT '2026-05',
  `total_amount` DECIMAL(15,2) NOT NULL COMMENT '总金额',
  `freight` DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT 'freight',
  `payable` DECIMAL(15,2) NOT NULL COMMENT 'payable',
  `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/SENT/CONFIRMED/PAYING/PAID/REJECTED',
  `recon_pdf_file_id` BIGINT COMMENT '对账单 PDF',
  `signed_scan_file_id` BIGINT COMMENT '签字扫描件（V1.3.6 AES-256-GCM）',
  `recon_sent_at` DATETIME COMMENT 'recon_sent时间',
  `recon_confirmed_at` DATETIME COMMENT 'recon_confirmed时间',
  `payment_request_id` BIGINT COMMENT 'V1.3.6 付款触发',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_vendor_period` (`vendor_id`, `period`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB COMMENT='月度对账（V1.3.6 改版 · 不含"线下"）';

DROP TABLE IF EXISTS `outsub_reconcile_line`;
CREATE TABLE `outsub_reconcile_line` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `reconcile_id` BIGINT NOT NULL COMMENT '对账ID',
  `outsource_order_id` BIGINT NOT NULL COMMENT 'outsource_orderID',
  `qty` DECIMAL(15,2) NOT NULL COMMENT '数量',
  `unit_price` DECIMAL(15,4) NOT NULL COMMENT '单价',
  `amount` DECIMAL(15,2) NOT NULL COMMENT '金额',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='V1.3.6 月度对账（不含"线下"）表';

-- V1.3.4 委外历史交期预估
DROP TABLE IF EXISTS `outsub_delivery_history`;
CREATE TABLE `outsub_delivery_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `vendor_id` BIGINT NOT NULL COMMENT '厂商ID',
  `process_code` VARCHAR(20) NOT NULL COMMENT '工序编码',
  `delivery_days` INT NOT NULL COMMENT 'deliverydays',
  `completed_at` DATETIME NOT NULL COMMENT '完成时间',
  PRIMARY KEY (`id`),
  KEY `idx_vendor_process` (`vendor_id`, `process_code`)
) ENGINE=InnoDB COMMENT='委外历史交期（V1.3.4 新增）';

-- V1.3.4 料号成本聚合（5 段）
DROP TABLE IF EXISTS `cost_part_aggregate`;
CREATE TABLE `cost_part_aggregate` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `material_code` VARCHAR(50) NOT NULL COMMENT '物料编码',
  `period` VARCHAR(10) NOT NULL COMMENT '2026-05',
  `material_cost` DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT '物料成本',
  `labor_cost` DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT 'labor成本',
  `surface_cost` DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT 'surface成本',
  `outsource_cost` DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT '委外成本',
  `mfg_cost` DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT '管理费分摊',
  `total_cost` DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT '总成本',
  `version` INT NOT NULL DEFAULT 1 COMMENT '版本号',
  `computed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'computed时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_material_period` (`material_code`, `period`),
  KEY `idx_material_computed` (`material_code`, `computed_at`)
) ENGINE=InnoDB COMMENT='料号 5 段成本（V1.3.4 新增）';


-- ============================================================
-- 物理库 3: cnc_production
-- erp-production 独占 · 生产域 crm_* 表由末尾 V60 从 cnc_business 同步
-- ============================================================
CREATE DATABASE IF NOT EXISTS `cnc_production` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ============================================================
-- 初始化数据（最小集）
-- ============================================================
USE `cnc_platform`;

-- 默认管理员（密码 admin123，BCrypt）
INSERT INTO `sys_user` (`id`, `username`, `password_hash`, `real_name`, `phone`, `email`, `status`)
VALUES (1, 'admin', '$2a$12$zygNYMhNTvNv9ZH.lodFFe6Lu76wXRhpliIzDLfnGePBMjCOm8LWW', '系统管理员', NULL, 'admin@yourcompany.local', 'ACTIVE')
ON DUPLICATE KEY UPDATE `password_hash` = VALUES(`password_hash`), `updated_at` = CURRENT_TIMESTAMP;

-- 系统角色（含 APP 操作工 OPERATOR · role_code 非 username operator）
INSERT INTO `sys_role` (`id`, `role_code`, `role_name`, `data_scope`, `amount_threshold`) VALUES
(1, 'SYS_ADMIN',  '系统管理员',     'ALL',  NULL),
(2, 'SALES',      '业务员',         'SELF', 50000),
(3, 'SALES_MGR',  '销售经理',       'DEPT', 200000),
(4, 'GM',         '总经理',         'ALL',  NULL),
(5, 'PROD_MGR',   '生管',           'DEPT', NULL),
(6, 'ENGINEER',   '工程师',         'DEPT', NULL),
(7, 'WAREHOUSE',  '仓管',           'DEPT', NULL),
(8, 'QC',         '品检',           'DEPT', NULL),
(9, 'BUYER',      '采购',           'DEPT', 50000),
(10, 'FINANCE',   '财务',           'ALL', 100000),
(11, 'HR',         '人事',           'DEPT', NULL),
(12, 'OPERATOR',   '操作工',         'SELF', NULL),
(13, 'PROCUREMENT_MANAGER', '采购主管', 'DEPT', 50000),
(14, 'CUSTOMER_VISITOR', '客户现场演示', 'SELF', NULL)
ON DUPLICATE KEY UPDATE `role_name` = VALUES(`role_name`), `updated_at` = CURRENT_TIMESTAMP;

-- V94 · 用户角色绑定（admin → SYS_ADMIN；其他 13 个角色绑定到 id=1 的用户占位，
--      实际生产中各角色会有自己的 sys_user 账号，此处仅保证 admin 登录后能看到菜单）
INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(1, 1)   -- admin → SYS_ADMIN（全量权限）
ON DUPLICATE KEY UPDATE `user_id` = VALUES(`user_id`);

-- include: sys-menu-permission-seed.sql
-- 系统菜单 + 角色菜单权限种子（与 V67 保持一致）
-- 用于 init.sql / init.baseline.sql 初始安装；变更时请同步 V67__sys_menu_permission.sql

USE `cnc_platform`;

-- ---------- 顶级模块 ----------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`, `icon`) VALUES
(1, NULL, 'mod.dashboard',   '工作台', '/dashboard',   'MODULE', 10, 'HomeFilled'),
(2, NULL, 'mod.sales',       '销售',   '/sales',       'MODULE', 20, 'Money'),
(4, NULL, 'mod.engineering', '工程',   '/engineering', 'MODULE', 30, 'Goods'),
(3, NULL, 'mod.production',  '生产',   '/production',  'MODULE', 40, 'Tools'),
(6, NULL, 'mod.sourcing',    '采购',   '/sourcing',    'MODULE', 50, 'ShoppingCart'),
(10, NULL, 'mod.warehouse',  '仓储',   '/warehouse',   'MODULE', 55, 'Box'),
(5, NULL, 'mod.quality',     '品质',   '/quality',     'MODULE', 60, 'Medal'),
(7, NULL, 'mod.finance',     '财务',   '/finance',     'MODULE', 70, 'CreditCard'),
(8, NULL, 'mod.hr',          '人事',   '/hr',          'MODULE', 80, 'User'),
(9, NULL, 'mod.admin',       '管理',   '/admin',       'MODULE', 90, 'Setting');

-- ---------- 工作台 ----------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(101, 1, 'dash.index',        '总览驾驶舱',     '/dashboard/index',        'MENU', 1),
(102, 1, 'dash.production',   '生产驾驶舱',     '/dashboard/production',   'MENU', 2),
(103, 1, 'dash.sales',        '销售驾驶舱',     '/dashboard/sales',        'MENU', 3),
(104, 1, 'dash.finance',      '财务驾驶舱',     '/dashboard/finance',      'MENU', 4),
(105, 1, 'dash.quality',      '品质驾驶舱',     '/dashboard/quality',      'MENU', 5),
(106, 1, 'dash.outsource',    '委外驾驶舱',     '/dashboard/outsource',    'MENU', 6),
(107, 1, 'dash.procurement',  '采购驾驶舱',     '/dashboard/procurement',  'MENU', 7),
(108, 1, 'dash.engineer',     '工程师驾驶舱',   '/dashboard/engineer',     'MENU', 8),
(109, 1, 'dash.warehouse',    '仓管驾驶舱',     '/dashboard/warehouse',    'MENU', 9),
(110, 1, 'dash.alerts',       '总经理驾驶舱',   '/dashboard/alerts',       'MENU', 10),
(111, 1, 'dash.multi',        '多维度驾驶舱',   '/dashboard/multi',        'MENU', 11),
(112, 1, 'dash.performance',  '绩效驾驶舱',     '/dashboard/performance-board', 'MENU', 12);

-- ---------- 销售 ----------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(201, 2, 'sales.customers',    '客户档案', '/sales/customers',           'MENU', 1),
(202, 2, 'sales.protection',   '客户保护', '/sales/customer/protection', 'MENU', 2),
(203, 2, 'sales.quotes',       '报价单',   '/sales/quotes',              'MENU', 3),
(204, 2, 'sales.quote-approval','报价审批','/sales/quotes/approval',     'MENU', 4),
(205, 2, 'sales.orders',       '销售订单', '/sales/orders',              'MENU', 5),
(206, 2, 'sales.contracts',    '合同回款', '/sales/contracts',           'MENU', 6);

-- ---------- 生产 ----------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(301, 3, 'prod.workorders',    '工单',       '/production/workorders',          'MENU', 1),
(302, 3, 'prod.pending',       '待转产订单', '/production/pending-production',  'MENU', 2),
(303, 3, 'prod.schedule',      '排产看板',   '/production/schedule',            'MENU', 3),
(304, 3, 'prod.gantt',         '排产甘特',   '/production/schedule-gantt',        'MENU', 4),
(305, 3, 'prod.mrp',           'MRP 中心',   '/production/mrp',                 'MENU', 5),
(306, 3, 'prod.outsource',     '委外列表',   '/production/outsource',           'MENU', 6),
(307, 3, 'prod.allocation',    '工序分配',   '/production/allocation',          'MENU', 7),
(308, 3, 'prod.outsub-panel',  '委外面板',   '/production/outsub-panel',        'MENU', 8),
(309, 3, 'prod.machines',      '设备机台',   '/production/machines',            'MENU', 9);

-- ---------- 物料 ----------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(401, 4, 'mat.drawings',   '图纸',     '/material/drawings',          'MENU', 1),
(402, 4, 'mat.lookup',     '料号查询', '/material/lookup',            'MENU', 2),
(403, 4, 'mat.barcode',    '物料条码', '/material/barcode-list',      'MENU', 3),
(404, 4, 'mat.category',   '物料分类', '/material/material-category', 'MENU', 4),
(405, 4, 'mat.boms',       'BOM',      '/material/boms',                'MENU', 5),
(406, 4, 'mat.process',    '工艺库',   '/material/process',             'MENU', 6),
(407, 4, 'mat.cost',       '料号成本', '/material/cost-aggregator',   'MENU', 7);

-- ---------- 品质 ----------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(501, 5, 'qc.inspection',  '来料/过程/成品检', '/quality/inspection',          'MENU', 1),
(502, 5, 'qc.fa',          'FA 首件',          '/quality/fa',                  'MENU', 2),
(503, 5, 'qc.cmm',         '三次元',           '/quality/cmm',                 'MENU', 3),
(504, 5, 'qc.defect',      '不良品',           '/quality/defect',              'MENU', 4),
(505, 5, 'qc.pickup',      '提货检',           '/quality/pickup',              'MENU', 5),
(506, 5, 'qc.outsource',   '委外检',           '/quality/outsource-inspection','MENU', 6);

-- ---------- 采购 ----------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(601, 6, 'src.rfq.workbench', '询比价工作台', '/sourcing/rfq',              'MENU', 1),
(613, 6, 'src.pr-transfer',   '采购转单',     '/sourcing/purchase-transfer', 'MENU', 2),
(602, 6, 'src.po',            '采购订单 PO',  '/sourcing/po',               'MENU', 9),
(603, 6, 'src.incoming',   '到货回执',     '/sourcing/incoming',           'MENU', 3),
(604, 6, 'src.no-order',   '无单采购',     '/sourcing/no-order-purchase',  'MENU', 4),
(605, 6, 'src.approval',   '审批路由',     '/sourcing/approval-route',     'MENU', 5),
(606, 6, 'src.reconcile',  '月度对账',     '/sourcing/reconcile',          'MENU', 6),
(607, 6, 'src.outsub.transfer', '委外转单', '/sourcing/outsub-order',       'MENU', 7),
(608, 6, 'src.rework',     '返修协同',     '/sourcing/rework',             'MENU', 8),
(609, 6, 'src.vendors',    '厂商资料',     '/sourcing/vendors',            'MENU', 9);

-- ---------- 财务 ----------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(701, 7, 'fin.receivables', '应收',     '/finance/receivables', 'MENU', 1),
(702, 7, 'fin.payables',    '应付',     '/finance/payables',    'MENU', 2),
(703, 7, 'fin.aging',       '账龄',     '/finance/aging',       'MENU', 3),
(704, 7, 'fin.cost',        '成本',     '/finance/cost',        'MENU', 4),
(705, 7, 'fin.payments',    '付款',     '/finance/payments',    'MENU', 5),
(706, 7, 'fin.profit',      '利润分析', '/finance/profit',      'MENU', 6),
(707, 7, 'fin.scans',       '签字扫描', '/finance/signed-scans','MENU', 7),
(708, 7, 'fin.gm-summary',  '总经理汇总','/finance/gm-summary', 'MENU', 8);

-- ---------- 人事 ----------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(801, 8, 'hr.employees',   '员工档案', '/hr/employees',   'MENU', 1),
(802, 8, 'hr.accounts',    '账号管理', '/hr/accounts',    'MENU', 2),
(803, 8, 'hr.attendance',  '考勤',     '/hr/attendance',  'MENU', 3),
(804, 8, 'hr.payroll',     '薪资',     '/hr/payroll',     'MENU', 4);

-- ---------- 管理 ----------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(901, 9, 'adm.users',           '用户管理',   '/admin/users',                  'MENU', 1),
(902, 9, 'adm.workflows',       '审批流',     '/admin/workflows',              'MENU', 2),
(903, 9, 'adm.dict',            '数据字典',   '/admin/dict',                   'MENU', 3),
(904, 9, 'adm.keyboard',        '快捷键',     '/admin/keyboard',               'MENU', 4),
(905, 9, 'adm.printers',        '打印机',     '/admin/printers',               'MENU', 5),
(906, 9, 'adm.email-config',    '邮件配置',   '/admin/email-config',           'MENU', 6),
(907, 9, 'adm.email-templates', '邮件模板',   '/admin/email-templates',        'MENU', 7),
(908, 9, 'adm.field-encrypt',   '字段加密',   '/admin/field-encryption',       'MENU', 8),
(909, 9, 'adm.rpt-workflow',    '审批统计',   '/admin/reports/workflow-stats', 'MENU', 9),
(910, 9, 'adm.rpt-ranking',     '销售龙虎榜', '/admin/reports/sales-ranking',  'MENU', 10),
(911, 9, 'adm.rpt-trend',       '销售趋势',   '/admin/reports/sales-trend',    'MENU', 11),
(912, 9, 'adm.rpt-customer',    '客户分析',   '/admin/reports/customer-analysis','MENU', 12);

-- ---------- 仓储 ----------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(1001, 10, 'wh.index',    '多仓库总览', '/warehouse/index',           'MENU', 1),
(1002, 10, 'wh.locations','库位树',     '/warehouse/locations',       'MENU', 2),
(1003, 10, 'wh.batches',  '批次列表',   '/warehouse/batches',         'MENU', 3),
(1004, 10, 'wh.inventory','库存',       '/warehouse/inventory',       'MENU', 4),
(1005, 10, 'wh.alert',    '库存预警',   '/warehouse/inventory-alert', 'MENU', 5);

-- ---------- V74 · PRD V2.0 新增菜单（init.baseline 内置，避免 init_data.sql 才生效） ----------
-- 工程数据（原物料）：菜单名调整 + 工艺路线维护
UPDATE `sys_menu` SET `menu_name` = '工程数据' WHERE `menu_code` = 'mod.material';

INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(408, 4, 'mat.product-route', '工艺路线维护', '/material/process-routes', 'MENU', 8)
ON DUPLICATE KEY UPDATE `menu_name` = VALUES(`menu_name`), `path` = VALUES(`path`);

INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(1006, 10, 'wh.inbound',     '入库单',   '/warehouse/inbound',    'MENU', 6),
(1007, 10, 'wh.outbound',    '出库单',   '/warehouse/outbound',   'MENU', 7),
(1008, 10, 'wh.stock-query', '库存查询', '/warehouse/stock-query','MENU', 8),
(1009, 10, 'wh.stocktake',   '盘点单',   '/warehouse/stocktake',  'MENU', 9)
ON DUPLICATE KEY UPDATE `menu_name` = VALUES(`menu_name`), `path` = VALUES(`path`);

UPDATE `sys_menu` SET `menu_name` = '仓储总览' WHERE `menu_code` = 'wh.index';

-- ---------- 角色权限：SYS_ADMIN 全量 ----------
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 1, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE';

-- SALES 业务员（不含报价审批，需要更高角色）
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 2, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND (
  `path` IN ('/dashboard','/dashboard/index','/dashboard/sales','/sales')
  OR `path` LIKE '/sales/%'
);
-- 移除报价审批权限（报价审批需要 SALES_MGR/SALES_MANAGER/GM）
DELETE FROM `sys_role_permission` WHERE `role_id` = 2 AND `menu_id` = (SELECT `id` FROM `sys_menu` WHERE `path` = '/sales/quotes/approval');

-- SALES_MGR 销售经理
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 3, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND (
  `path` IN ('/dashboard','/dashboard/index','/dashboard/sales','/sales')
  OR `path` LIKE '/sales/%'
);

-- GM 总经理（除管理后台）
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 4, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND `path` NOT LIKE '/admin%';

-- PROD_MGR 生管（仅料号查询 + 图纸只读，不维护工程数据）
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 5, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND (
  `path` IN ('/dashboard','/dashboard/index','/dashboard/production','/dashboard/performance-board','/production')
  OR `path` LIKE '/dashboard/production%'
  OR `path` LIKE '/dashboard/performance-board%'
  OR `path` LIKE '/production/%'
  OR `path` IN ('/material/drawings','/material/lookup')
);

-- ENGINEER
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 6, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND (
  `path` IN ('/dashboard','/dashboard/index','/dashboard/engineer','/production','/material')
  OR `path` LIKE '/dashboard/engineer%'
  OR `path` LIKE '/production/schedule%'
  OR `path` LIKE '/production/mrp%'
  OR `path` LIKE '/production/workorders%'
  OR `path` LIKE '/material/%'
);

-- WAREHOUSE 仓管（仅料号查询 + 物料条码只读，不见「工程数据」维护页）
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 7, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND (
  `path` IN ('/dashboard','/dashboard/index','/dashboard/warehouse','/warehouse')
  OR `path` LIKE '/warehouse/%'
  OR `path` IN ('/material/lookup','/material/barcode-list')
);

-- QC 品检
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 8, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND (
  `path` IN ('/dashboard','/dashboard/index','/dashboard/quality','/quality')
  OR `path` LIKE '/quality/%'
);

-- BUYER 采购（不含审批路由，需要 PURCHASER_LEAD/PROCUREMENT_MANAGER）
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 9, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND (
  `path` IN ('/dashboard','/dashboard/index','/dashboard/procurement','/dashboard/outsource','/sourcing')
  OR `path` LIKE '/sourcing/%'
  OR `path` IN ('/production/outsub-panel')
);
-- 移除采购审批路由权限（审批路由需要 PURCHASER_LEAD/PROCUREMENT_MANAGER/GM）
DELETE FROM `sys_role_permission` WHERE `role_id` = 9 AND `menu_id` = (SELECT `id` FROM `sys_menu` WHERE `path` = '/sourcing/approval-route');

-- FINANCE 财务
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 10, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND (
  `path` IN ('/dashboard','/dashboard/index','/dashboard/finance','/finance')
  OR `path` LIKE '/finance/%'
  OR `path` IN ('/sales/contracts')
);

-- HR 人事
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 11, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND (
  `path` IN ('/dashboard','/dashboard/index','/hr')
  OR `path` LIKE '/hr/%'
);

-- OPERATOR 操作工
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT r.id, m.id, 'view'
FROM `sys_role` r
JOIN `sys_menu` m ON m.status = 'ACTIVE'
WHERE r.role_code = 'OPERATOR'
  AND (
    m.path IN ('/dashboard','/dashboard/index','/dashboard/production','/dashboard/performance-board','/production')
    OR m.path IN ('/production/workorders','/production/schedule','/dashboard/performance-board')
  );

-- PROCUREMENT_MANAGER 采购主管
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT r.id, m.id, 'view'
FROM `sys_role` r
JOIN `sys_menu` m ON m.status = 'ACTIVE'
WHERE r.role_code = 'PROCUREMENT_MANAGER'
  AND (
    m.path IN ('/dashboard','/dashboard/index','/dashboard/procurement','/sourcing','/finance')
    OR m.path LIKE '/sourcing/%'
    OR m.path IN ('/finance/gm-summary')
  );

-- ---------- V74/V76 · 角色权限补全（init.baseline 内置，避免 init_data.sql 才生效） ----------
-- 生管（PROD_MGR / role_id=5）工序分配页 confirm 权限
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 5, `id`, 'confirm' FROM `sys_menu` WHERE `menu_code` = 'prod.allocation';

-- 仓管（WAREHOUSE / role_id=7）仓储全量（含 V74 新增入库/出库/盘点/库存查询）
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 7, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND (
  `path` IN ('/warehouse', '/dashboard', '/dashboard/index', '/dashboard/warehouse')
  OR `path` LIKE '/warehouse/%'
);

-- 生管（PROD_MGR / role_id=5）仓储总览+库存查询+预警（只读）
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 5, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND `path` IN (
  '/warehouse/index', '/warehouse/stock-query', '/warehouse/inventory', '/warehouse/inventory-alert'
);

-- 采购（BUYER / role_id=9）仓储总览+入库单+预警
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 9, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND `path` IN (
  '/warehouse/index', '/warehouse/inbound', '/warehouse/inventory-alert'
);

-- 财务（FINANCE / role_id=10）仓储总览+库存查询
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 10, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND `path` IN (
  '/warehouse/index', '/warehouse/stock-query', '/warehouse/inventory'
);

-- 品检（QC / role_id=8）仓储总览+批次列表
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 8, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND `path` IN (
  '/warehouse/index', '/warehouse/batches'
);

-- 邮件配置（singleton）
INSERT INTO `email_config` (`id`, `from_address`, `auth_code_kek`) VALUES
(1, 'noreply@yourcompany.local', 'PLACEHOLDER_KMS_INJECT')
ON DUPLICATE KEY UPDATE `updated_at` = CURRENT_TIMESTAMP;

-- 工序/物料字典改由 V3__system_params（PROCESS_TYPE / MATERIAL_CATEGORY）统一初始化

USE `cnc_platform`;

-- 7 状态机字典（平台 sys_dict 表）
INSERT INTO `sys_dict` (`dict_type`, `dict_code`, `dict_label`, `sort`) VALUES
('outsub_status', 'PENDING_SHIP', '待发货', 1),
('outsub_status', 'SHIPPING', '送货中', 2),
('outsub_status', 'PENDING_INSPECTION', '待检', 3),
('outsub_status', 'INSPECTING', '质检中', 4),
('outsub_status', 'QUALIFIED_STORAGE', '待入库', 5),
('outsub_status', 'STORED', '已入库', 6),
('outsub_status', 'REPAIR_REQUESTED', '待返修', 7),
('outsub_status', 'NOTIFIED_REPAIR', '已通知返修', 8),
('decision', 'INHOUSE', '自制', 1),
('decision', 'OUTSOURCE', '委外', 2)
ON DUPLICATE KEY UPDATE `sort` = VALUES(`sort`);

USE `cnc_business`;

-- 工序库基础数据
INSERT INTO `mdm_process` (`process_code`, `process_name`, `std_time_min`, `machine_type`, `unit_price`) VALUES
('P00', '下料', 5.0, 'BANDSAW', 5.00),
('P01', '车床', 30.0, 'LATHE', 25.00),
('P02', 'CNC', 45.0, 'CNC', 40.00),
('P03', '铣床', 30.0, 'MILL', 30.00),
('P04', '钳工', 20.0, 'BENCH', 18.00),
('P05', '表处', 60.0, 'SURFACE', 15.00),
('P06', '热处理', 90.0, 'HEAT', 20.00),
('P07', '三次元', 30.0, 'CMM', 50.00),
('P08', '清洗', 10.0, 'CLEAN', 5.00),
('P09', '包装', 15.0, 'PACK', 8.00)
ON DUPLICATE KEY UPDATE `unit_price` = VALUES(`unit_price`);

-- 工作流模板（4 套）
USE `cnc_platform`;

INSERT INTO `sys_workflow` (`workflow_code`, `nodes_json`, `conditions_json`, `status`) VALUES
('QUOTE_FLOW',  '[{"node":1,"role":"OWNER","threshold":50000},{"node":2,"role":"SALES_MGR","threshold":200000},{"node":3,"role":"GM","threshold":null}]', '{"amount_field":"total_amount"}', 'ACTIVE'),
('ORDER_FLOW',  '[{"node":1,"role":"OWNER","threshold":50000},{"node":2,"role":"SALES_MGR","threshold":200000},{"node":3,"role":"GM","threshold":null}]', '{"amount_field":"total_amount","extra_check":"credit_limit"}', 'ACTIVE'),
('PURCHASE_FLOW','[{"node":1,"role":"BUYER","threshold":10000},{"node":2,"role":"SALES_MGR","threshold":50000},{"node":3,"role":"GM","threshold":null}]', '{"amount_field":"amount"}', 'ACTIVE'),
('PAYMENT_FLOW','[{"node":1,"role":"FINANCE","threshold":100000},{"node":2,"role":"GM","threshold":null}]', '{"amount_field":"amount","dual_sign":">100000"}', 'ACTIVE')
ON DUPLICATE KEY UPDATE `updated_at` = CURRENT_TIMESTAMP;

-- ============================================================

-- include: V2__workflow_split.sql
-- ============================================================
-- V1.3.7 Story 1.2 · V2__workflow_split.sql
-- P1 修补 ④：sys_workflow 拆分为 sys_workflow + sys_workflow_node 物理表
-- architect 评审通过 · 6 条 P2 反馈全部纳入
-- ============================================================

USE `cnc_platform`;

-- ---------- 新增 sys_workflow_node 物理表 ----------
DROP TABLE IF EXISTS `sys_workflow_node`;
CREATE TABLE `sys_workflow_node` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `workflow_id` BIGINT NOT NULL COMMENT 'sys_workflow.id（FK ON DELETE CASCADE）',
  `node_index` INT NOT NULL COMMENT '节点序号 1..N（严格递增）',
  `node_type` VARCHAR(20) NOT NULL COMMENT 'START/APPROVAL/CC/END',
  `role_code` VARCHAR(50) DEFAULT NULL COMMENT 'APPROVAL 节点必填（引用 sys_role.role_code）',
  `threshold` DECIMAL(15,2) DEFAULT NULL COMMENT '金额阈值（NULL=无限额）',
  `or_sign_required` BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'V1.3.7 P1 修补：OR 会签',
  `extra_check_json` TEXT COMMENT 'V1.3.7 条件扩展（如 extra_check=credit_limit）',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'createtime',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updatetime',
  `create_by` BIGINT DEFAULT NULL COMMENT 'createby',
  `update_by` BIGINT DEFAULT NULL COMMENT 'updateby',
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

-- include: V3__approval_record.sql
-- ============================================================
-- V1.3.7 Story 1.2 · V3__approval_record.sql
-- P1 修补 ④：创建 sys_approval_record 表
-- 关联：与 sys_workflow / sys_workflow_node 配合，构成完整审批中台
-- ============================================================

USE `cnc_platform`;

-- ---------- sys_approval_record 审批单表 ----------
DROP TABLE IF EXISTS `sys_approval_record`;
CREATE TABLE `sys_approval_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `biz_type` VARCHAR(20) NOT NULL COMMENT 'QUOTE/ORDER/PURCHASE/PAYMENT/OTHER',
  `biz_id` VARCHAR(50) NOT NULL COMMENT '业务单号（≤50 字符）',
  `workflow_code` VARCHAR(50) NOT NULL COMMENT 'sys_workflow.workflow_code',
  `current_node_index` INT NOT NULL DEFAULT 1 COMMENT '当前节点序号',
  `current_approver_user_id` BIGINT DEFAULT NULL COMMENT '当前审批人（首次分配 candidates[0]）',
  `candidates` TEXT COMMENT 'OR 会签候选人列表 JSON（[10010, 10011]）',
  `or_sign_required` BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'V1.3.7 P1 修补：OR 会签',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/SKIPPED/WAITING',
  `skip_reason` VARCHAR(20) DEFAULT NULL COMMENT 'ON_LEAVE/ON_TRIP/DISABLED/RESIGNED',
  `skipped_at` DATETIME DEFAULT NULL COMMENT 'skipped时间',
  `comment` VARCHAR(500) DEFAULT NULL COMMENT '审批意见',
  `reason` VARCHAR(500) DEFAULT NULL COMMENT '驳回原因（必填）',
  `timeout_at` DATETIME DEFAULT NULL COMMENT 'V1.3.7 24h 超时基准（= created_at + timeout_hours）',
  `is_overdue` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否已超时',
  `overdue_at` DATETIME DEFAULT NULL COMMENT 'overdue时间',
  `node_skipped` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '本节点是否被全员 SKIPPED 自动跳过',
  `approved_at` DATETIME DEFAULT NULL COMMENT 'approved时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'createtime',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updatetime',
  `create_by` BIGINT DEFAULT NULL COMMENT 'createby',
  `update_by` BIGINT DEFAULT NULL COMMENT 'updateby',
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

-- include: V3__system_params.sql
-- ============================================================
-- V3__system_params.sql · V1.3.7 Story 1.3
-- ============================================================
-- 目的：Story 1.3 系统参数/数据字典/HR Feign 真实集成/性能与安全闭环
--     创建 5 张新表（sys_dict_type / sys_param / sys_change_log / sys_global_threshold / sys_audit_log_archive）
-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- ============================================================

USE `cnc_platform`;

-- ---------- 1. sys_dict_type（字典类型字典） ----------
DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `type_code` VARCHAR(50) NOT NULL COMMENT '字典类型编码（6 类：MATERIAL_CATEGORY/PROCESS_TYPE/SURFACE_TREATMENT/WORK_SHIFT/WAREHOUSE/CURRENCY）',
  `type_name` VARCHAR(100) NOT NULL COMMENT '字典类型名称',
  `description` VARCHAR(500) DEFAULT NULL,
  `is_builtin` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否内置（1=不可删 0=可删）',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_type_code` (`type_code`)
) ENGINE=InnoDB COMMENT='字典类型字典（V1.3.7 Story 1.3）';

-- ---------- 2. sys_param（系统参数） ----------
DROP TABLE IF EXISTS `sys_param`;
CREATE TABLE `sys_param` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `param_key` VARCHAR(100) NOT NULL COMMENT '参数键（dot.case）',
  `param_value` VARCHAR(1000) NOT NULL COMMENT '参数值',
  `param_group` VARCHAR(50) NOT NULL COMMENT 'BIZ_DOC_NO/PRINT_TEMPLATE/APP_CACHE_TTL',
  `description` VARCHAR(500) DEFAULT NULL,
  `updated_by` BIGINT DEFAULT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_param_key` (`param_key`),
  KEY `idx_param_group` (`param_group`)
) ENGINE=InnoDB COMMENT='系统参数（V1.3.7 Story 1.3 · FR-1-3-2）';

-- ---------- 3. sys_change_log（变更日志） ----------
DROP TABLE IF EXISTS `sys_change_log`;
CREATE TABLE `sys_change_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `entity` VARCHAR(50) NOT NULL COMMENT 'threshold/dict/param/...',
  `entity_id` BIGINT DEFAULT NULL COMMENT '业务实体 ID',
  `operation` VARCHAR(20) NOT NULL COMMENT 'CREATE/UPDATE/DELETE',
  `before_value` TEXT,
  `after_value` TEXT,
  `changed_by` BIGINT DEFAULT NULL,
  `changed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_entity_entity_id` (`entity`, `entity_id`),
  KEY `idx_changed_by` (`changed_by`),
  KEY `idx_changed_at` (`changed_at`)
) ENGINE=InnoDB COMMENT='变更日志（V1.3.7 Story 1.3 · FR-1-3-3 · V1.3.7 红线 5）';

-- ---------- 4. sys_global_threshold（金额阈值全局） ----------
DROP TABLE IF EXISTS `sys_global_threshold`;
CREATE TABLE `sys_global_threshold` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `biz_type` VARCHAR(50) NOT NULL COMMENT 'QUOTE/ORDER/PURCHASE/PAYMENT',
  `role_code` VARCHAR(50) NOT NULL COMMENT 'salesperson/dept_manager/gm',
  `threshold` DECIMAL(15,2) DEFAULT NULL COMMENT '金额阈值（NULL=无限额）',
  `currency` VARCHAR(10) NOT NULL DEFAULT 'CNY',
  `effective_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_biz_type_role` (`biz_type`, `role_code`)
) ENGINE=InnoDB COMMENT='金额阈值全局（V1.3.7 Story 1.3 · FR-1-3-3 · 双轨：Nacos 优先）';

-- ---------- 5. sys_audit_log_archive（审计日志归档表，5 年保留） ----------
DROP TABLE IF EXISTS `sys_audit_log_archive`;
CREATE TABLE `sys_audit_log_archive` (
  `id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `module` VARCHAR(50) NOT NULL,
  `action` VARCHAR(50) NOT NULL,
  `before_json` TEXT,
  `after_json` TEXT,
  `ip` VARCHAR(50),
  `ts` DATETIME NOT NULL,
  `archived_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_module_action_ts` (`user_id`, `module`, `action`, `ts`),
  KEY `idx_archived_at` (`archived_at`)
) ENGINE=InnoDB COMMENT='审计日志归档表（V1.3.7 Story 1.3 · 1 年保留 + 5 年归档 · 5 年后清理）';


-- ============================================================
-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- ============================================================

-- 1) 字典类型（6 类）
INSERT INTO `sys_dict_type` (`type_code`, `type_name`, `description`, `is_builtin`) VALUES
('MATERIAL_CATEGORY', '物料分类', '钢材/铝材/铜材/塑料/其他', 1),
('PROCESS_TYPE', '工序类型', 'CNC 加工/车削/铣削/磨削/表处', 1),
('SURFACE_TREATMENT', '表处类型', '阳极氧化/喷涂/电镀/抛光', 1),
('WORK_SHIFT', '班别', '早班/中班/晚班', 1),
('WAREHOUSE', '仓库', '原材料仓/半成品仓/成品仓/委外仓', 1),
('CURRENCY', '币种', 'CNY/USD/EUR/JPY', 1);

-- 清理 V1 遗留小写字典（utf8mb4_ci 下 process_type 与 PROCESS_TYPE 视为同一 type · CNC 会冲突）
DELETE FROM `sys_dict` WHERE BINARY `dict_type` IN ('process_type', 'material_category');

-- 2) 物料分类字典（5 项）
INSERT INTO `sys_dict` (`dict_type`, `dict_code`, `dict_label`, `sort`, `status`) VALUES
('MATERIAL_CATEGORY', 'STEEL', '钢材', 1, 'ACTIVE'),
('MATERIAL_CATEGORY', 'ALUMINUM', '铝材', 2, 'ACTIVE'),
('MATERIAL_CATEGORY', 'COPPER', '铜材', 3, 'ACTIVE'),
('MATERIAL_CATEGORY', 'PLASTIC', '塑料', 4, 'ACTIVE'),
('MATERIAL_CATEGORY', 'OTHER', '其他', 99, 'ACTIVE')
ON DUPLICATE KEY UPDATE
  `dict_type` = VALUES(`dict_type`),
  `dict_label` = VALUES(`dict_label`),
  `sort` = VALUES(`sort`),
  `status` = VALUES(`status`);

-- 3) 工序类型字典（5 项）
INSERT INTO `sys_dict` (`dict_type`, `dict_code`, `dict_label`, `sort`, `status`) VALUES
('PROCESS_TYPE', 'CNC', 'CNC 加工', 1, 'ACTIVE'),
('PROCESS_TYPE', 'TURNING', '车削', 2, 'ACTIVE'),
('PROCESS_TYPE', 'MILLING', '铣削', 3, 'ACTIVE'),
('PROCESS_TYPE', 'GRINDING', '磨削', 4, 'ACTIVE'),
('PROCESS_TYPE', 'SURFACE_TREATMENT', '表处', 99, 'ACTIVE')
ON DUPLICATE KEY UPDATE
  `dict_type` = VALUES(`dict_type`),
  `dict_label` = VALUES(`dict_label`),
  `sort` = VALUES(`sort`),
  `status` = VALUES(`status`);

-- 4) 表处类型字典（4 项）
INSERT INTO `sys_dict` (`dict_type`, `dict_code`, `dict_label`, `sort`, `status`) VALUES
('SURFACE_TREATMENT', 'ANODIZING', '阳极氧化', 1, 'ACTIVE'),
('SURFACE_TREATMENT', 'PAINTING', '喷涂', 2, 'ACTIVE'),
('SURFACE_TREATMENT', 'PLATING', '电镀', 3, 'ACTIVE'),
('SURFACE_TREATMENT', 'POLISHING', '抛光', 4, 'ACTIVE')
ON DUPLICATE KEY UPDATE
  `dict_type` = VALUES(`dict_type`),
  `dict_label` = VALUES(`dict_label`),
  `sort` = VALUES(`sort`),
  `status` = VALUES(`status`);

-- 5) 班别字典（3 项）
INSERT INTO `sys_dict` (`dict_type`, `dict_code`, `dict_label`, `sort`, `status`) VALUES
('WORK_SHIFT', 'MORNING', '早班 (08:00-16:00)', 1, 'ACTIVE'),
('WORK_SHIFT', 'NOON', '中班 (16:00-00:00)', 2, 'ACTIVE'),
('WORK_SHIFT', 'NIGHT', '晚班 (00:00-08:00)', 3, 'ACTIVE')
ON DUPLICATE KEY UPDATE
  `dict_type` = VALUES(`dict_type`),
  `dict_label` = VALUES(`dict_label`),
  `sort` = VALUES(`sort`),
  `status` = VALUES(`status`);

-- 6) 仓库字典（4 项）
INSERT INTO `sys_dict` (`dict_type`, `dict_code`, `dict_label`, `sort`, `status`) VALUES
('WAREHOUSE', 'RAW', '原材料仓', 1, 'ACTIVE'),
('WAREHOUSE', 'SEMI', '半成品仓', 2, 'ACTIVE'),
('WAREHOUSE', 'FINISHED', '成品仓', 3, 'ACTIVE'),
('WAREHOUSE', 'OUTSOURCE', '委外仓', 4, 'ACTIVE')
ON DUPLICATE KEY UPDATE
  `dict_type` = VALUES(`dict_type`),
  `dict_label` = VALUES(`dict_label`),
  `sort` = VALUES(`sort`),
  `status` = VALUES(`status`);

-- 7) 币种字典（4 项）
INSERT INTO `sys_dict` (`dict_type`, `dict_code`, `dict_label`, `sort`, `status`) VALUES
('CURRENCY', 'CNY', '人民币 (¥)', 1, 'ACTIVE'),
('CURRENCY', 'USD', '美元 ($)', 2, 'ACTIVE'),
('CURRENCY', 'EUR', '欧元 (€)', 3, 'ACTIVE'),
('CURRENCY', 'JPY', '日元 (¥)', 4, 'ACTIVE')
ON DUPLICATE KEY UPDATE
  `dict_type` = VALUES(`dict_type`),
  `dict_label` = VALUES(`dict_label`),
  `sort` = VALUES(`sort`),
  `status` = VALUES(`status`);


-- ============================================================
-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- ============================================================

-- 1) 单据编号规则（BIZ_DOC_NO · 4 类 × 4 段）
INSERT INTO `sys_param` (`param_key`, `param_value`, `param_group`, `description`) VALUES
('biz.doc-no.quote', 'QUOTE-{yyyyMMdd}-{seq:4}', 'BIZ_DOC_NO', '报价单编号：QUOTE-20260610-0001'),
('biz.doc-no.order', 'ORDER-{yyyyMMdd}-{seq:4}', 'BIZ_DOC_NO', '订单编号：ORDER-20260610-0001'),
('biz.doc-no.purchase', 'PO-{yyyyMMdd}-{seq:4}', 'BIZ_DOC_NO', '采购单编号：PO-20260610-0001'),
('biz.doc-no.payment', 'PAY-{yyyyMMdd}-{seq:4}', 'BIZ_DOC_NO', '付款单编号：PAY-20260610-0001'),
('biz.doc-no.seq-start', '1', 'BIZ_DOC_NO', '单据编号起始值（每日 0 点重置）'),
('biz.doc-no.seq-step', '1', 'BIZ_DOC_NO', '单据编号步长');

-- 2) 打印模板（PRINT_TEMPLATE · 2 项）
INSERT INTO `sys_param` (`param_key`, `param_value`, `param_group`, `description`) VALUES
('print.quote.default', 'QUOTE_TEMPLATE_V1', 'PRINT_TEMPLATE', '报价单默认打印模板'),
('print.order.default', 'ORDER_TEMPLATE_V1', 'PRINT_TEMPLATE', '订单默认打印模板');

-- 3) APP 端离线缓存时长（APP_CACHE_TTL · 1 项）
INSERT INTO `sys_param` (`param_key`, `param_value`, `param_group`, `description`) VALUES
('app.cache-ttl', 'PT24H', 'APP_CACHE_TTL', 'APP 端离线缓存时长（ISO 8601 Duration，默认 24h）');


-- ============================================================
-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- ============================================================

-- QUOTE 业务：业务员 5万 / 部门经理 20万 / 总经理 无限额
INSERT INTO `sys_global_threshold` (`biz_type`, `role_code`, `threshold`, `currency`) VALUES
('QUOTE', 'salesperson', 50000.00, 'CNY'),
('QUOTE', 'dept_manager', 200000.00, 'CNY'),
('QUOTE', 'gm', NULL, 'CNY');

-- ORDER 业务（同 QUOTE）
INSERT INTO `sys_global_threshold` (`biz_type`, `role_code`, `threshold`, `currency`) VALUES
('ORDER', 'salesperson', 50000.00, 'CNY'),
('ORDER', 'dept_manager', 200000.00, 'CNY'),
('ORDER', 'gm', NULL, 'CNY');

-- PURCHASE 业务：采购员 1万 / 部门经理 5万 / 总经理 无限额
INSERT INTO `sys_global_threshold` (`biz_type`, `role_code`, `threshold`, `currency`) VALUES
('PURCHASE', 'buyer', 10000.00, 'CNY'),
('PURCHASE', 'dept_manager', 50000.00, 'CNY'),
('PURCHASE', 'gm', NULL, 'CNY');

-- PAYMENT 业务：财务双签 10万（>10万 gm + 财务总监双签）
INSERT INTO `sys_global_threshold` (`biz_type`, `role_code`, `threshold`, `currency`) VALUES
('PAYMENT', 'finance', 100000.00, 'CNY'),
('PAYMENT', 'gm', NULL, 'CNY'),
('PAYMENT', 'finance_director', NULL, 'CNY');


-- ============================================================
-- 迁移完成
-- ============================================================
SELECT 'V3__system_params.sql 迁移完成' AS message,
       (SELECT COUNT(*) FROM `sys_dict_type`) AS dict_type_count,
       (SELECT COUNT(*) FROM `sys_dict`) AS dict_count,
       (SELECT COUNT(*) FROM `sys_param`) AS param_count,
       (SELECT COUNT(*) FROM `sys_global_threshold`) AS threshold_count;
-- include: V4__crm_quote.sql
-- ============================================================
-- V4__crm_quote.sql · V1.3.7 Story 1.5
-- ============================================================
-- 目的：Story 1.5 报价与多级审批
--     创建 3 张新表（crm_quote / crm_quote_item / crm_quote_history）
-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- ============================================================

USE `cnc_business`;

-- ---------- 1. crm_quote 报价单 ----------
DROP TABLE IF EXISTS `crm_quote`;
CREATE TABLE `crm_quote` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `quote_no` VARCHAR(30) NOT NULL COMMENT 'BJ+YYYYMMDD+NNNN (例 BJ20260611-0001)',
  `customer_id` BIGINT NOT NULL COMMENT '客户 ID (sys_dict?type=CUSTOMER_STATUS 查 BLACKLIST)',
  `customer_name` VARCHAR(200) NOT NULL,
  `owner_user_id` BIGINT NOT NULL,
  `dept_id` BIGINT NOT NULL,
  `currency` VARCHAR(10) NOT NULL DEFAULT 'CNY',
  `total_amount` DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT '由 items 自动计算(只读)',
  `delivery_date` DATE NOT NULL,
  `is_fa` TINYINT(1) NOT NULL DEFAULT 0,
  `is_new` TINYINT(1) NOT NULL DEFAULT 0,
  `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/SUBMITTED/APPROVED/REJECTED/CONVERTED',
  `current_node` INT DEFAULT 1 COMMENT '当前审批节点 (1/2/3)',
  `comment` VARCHAR(1000) DEFAULT NULL,
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_quote_no` (`quote_no`),
  KEY `idx_customer_id` (`customer_id`),
  KEY `idx_owner_user_id` (`owner_user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB COMMENT='报价单 (V1.3.7 Story 1.5)';

-- ---------- 2. crm_quote_item 报价明细 ----------
DROP TABLE IF EXISTS `crm_quote_item`;
CREATE TABLE `crm_quote_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `quote_id` BIGINT NOT NULL,
  `drawing_no` VARCHAR(50) NOT NULL COMMENT '图号',
  `material` VARCHAR(50) NOT NULL,
  `spec` VARCHAR(200) DEFAULT NULL,
  `quantity` INT NOT NULL,
  `unit_price` DECIMAL(15,2) NOT NULL,
  `amount` DECIMAL(15,2) NOT NULL COMMENT '= quantity * unit_price',
  `is_fa` TINYINT(1) NOT NULL DEFAULT 0,
  `is_new` TINYINT(1) NOT NULL DEFAULT 0,
  `sort` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_quote_id_sort` (`quote_id`, `sort`),
  CONSTRAINT `fk_quote_item_quote` FOREIGN KEY (`quote_id`) REFERENCES `crm_quote` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='报价明细 (V1.3.7 Story 1.5)';

-- ---------- 3. crm_quote_history 报价变更历史 ----------
DROP TABLE IF EXISTS `crm_quote_history`;
CREATE TABLE `crm_quote_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `quote_id` BIGINT NOT NULL,
  `operation` VARCHAR(20) NOT NULL COMMENT 'CREATE/UPDATE/SUBMIT/APPROVE/REJECT/CONVERT/PDF_DOWNLOAD',
  `before_json` TEXT,
  `after_json` TEXT,
  `changed_by` BIGINT NOT NULL,
  `changed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_quote_id` (`quote_id`),
  KEY `idx_changed_by` (`changed_by`),
  KEY `idx_changed_at` (`changed_at`)
) ENGINE=InnoDB COMMENT='报价变更历史 (V1.3.7 Story 1.5 · 红线 5)';

-- ---------- 4. CUSTOMER_STATUS 字典类型（黑名单） ----------
USE `cnc_platform`;

INSERT INTO `sys_dict_type` (`type_code`, `type_name`, `description`, `is_builtin`) VALUES
('CUSTOMER_STATUS', '客户状态', 'NORMAL / BLACKLIST（黑名单直接驳回 40902）', 1)
ON DUPLICATE KEY UPDATE `type_name` = VALUES(`type_name`);

-- V94 · mock 清理：CUSTOMER_STATUS 演示客户已移至 init_data.sql（保留字典类型）

-- 5 条 UI 红线检查
SELECT 'V4__crm_quote.sql 迁移完成' AS message,
       (SELECT COUNT(*) FROM sys_dict WHERE dict_type = 'CUSTOMER_STATUS') AS customer_status_count;
-- include: V5__crm_order.sql
-- ============================================================
-- V5__crm_order.sql · V1.3.7 Story 1.6
-- ============================================================
-- 目的：Story 1.6 订单管理
--     创建 4 张新表（crm_order / crm_order_item / crm_order_history / crm_order_payment）
-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- ============================================================

USE `cnc_business`;

-- ---------- 1. crm_order 订单主表 ----------
DROP TABLE IF EXISTS `crm_order`;
CREATE TABLE `crm_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(30) NOT NULL COMMENT 'XS+YYYYMMDD+NNNN (继承1.5 DocNoGenerator 模板)',
  `quote_id` BIGINT DEFAULT NULL COMMENT '来源报价ID(手动创建为NULL, 1.5转单时非空)',
  `customer_id` BIGINT NOT NULL COMMENT '客户 ID (sys_dict?type=CUSTOMER_STATUS 查黑名单)',
  `customer_name` VARCHAR(200) NOT NULL,
  `owner_user_id` BIGINT NOT NULL COMMENT '业务员ID',
  `dept_id` BIGINT NOT NULL COMMENT '部门ID(经理按部门过滤)',
  `currency` VARCHAR(10) NOT NULL DEFAULT 'CNY',
  `total_amount` DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT '由 items 自动计算(只读)',
  `delivery_date` DATE NOT NULL,
  `is_fa` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否FA首件',
  `is_new` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否新件',
  `is_urgent` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否加急',
  `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/CONFIRMED/PRODUCING/PARTIAL_SHIPPED/SHIPPED/SETTLED/CLOSED/CANCELLED',
  `current_node` INT DEFAULT 1 COMMENT '当前审批节点 (1/2/3)',
  `comment` VARCHAR(1000) DEFAULT NULL,
  `production_order_no` VARCHAR(30) DEFAULT NULL COMMENT 'GD+YYYYMMDD+NNNN (转生产时生成, Epic 5)',
  `outsource_order_no` VARCHAR(30) DEFAULT NULL COMMENT 'WW+YYYYMMDD+NNNN (转委外时生成, Epic 6)',
  `credit_limit_check` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '信用额度是否校验通过(0=未通过/未检查,1=通过,-1=无限制)',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_order_no` (`order_no`),
  KEY `idx_customer_id` (`customer_id`),
  KEY `idx_owner_user_id` (`owner_user_id`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_quote_id` (`quote_id`)
) ENGINE=InnoDB COMMENT='订单主表 (V1.3.7 Story 1.6)';

-- ---------- 2. crm_order_item 订单明细 ----------
DROP TABLE IF EXISTS `crm_order_item`;
CREATE TABLE `crm_order_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `drawing_no` VARCHAR(50) NOT NULL COMMENT '系统图号',
  `customer_drawing_no` VARCHAR(128) DEFAULT NULL COMMENT '客户图号',
  `drawing_id` BIGINT DEFAULT NULL COMMENT '关联图纸ID',
  `product_name` VARCHAR(128) DEFAULT NULL COMMENT '产品名称',
  `process_route` VARCHAR(500) DEFAULT NULL COMMENT '工艺路线预览',
  `material_no` VARCHAR(64) DEFAULT NULL COMMENT '料号，订单提交时生成',
  `source_quotation_detail_id` BIGINT DEFAULT NULL COMMENT '来源报价明细行ID',
  `material` VARCHAR(50) NOT NULL,
  `spec` VARCHAR(200) DEFAULT NULL,
  `unit_weight` DECIMAL(12,4) DEFAULT NULL COMMENT '单件重量(kg)',
  `quantity` INT NOT NULL COMMENT '订单数量',
  `unit_price` DECIMAL(15,2) NOT NULL,
  `amount` DECIMAL(15,2) NOT NULL COMMENT '= quantity * unit_price',
  `quantity_adjustment` INT NOT NULL DEFAULT 0 COMMENT '数量调整(来自1.5 quantityAdjustment hook)',
  `is_fa` TINYINT(1) NOT NULL DEFAULT 0,
  `is_new` TINYINT(1) NOT NULL DEFAULT 0,
  `sort` INT NOT NULL DEFAULT 0,
  `produced_qty` INT NOT NULL DEFAULT 0 COMMENT '已生产数量(转生产后累计)',
  `shipped_qty` INT NOT NULL DEFAULT 0 COMMENT '已发货数量(发货后累计)',
  PRIMARY KEY (`id`),
  KEY `idx_order_id_sort` (`order_id`, `sort`),
  KEY `idx_order_item_drawing_id` (`drawing_id`),
  CONSTRAINT `fk_order_item_order` FOREIGN KEY (`order_id`) REFERENCES `crm_order` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='订单明细 (V1.3.7 Story 1.6 · 含quantityAdjustment hook)';

-- ---------- 3. crm_order_history 订单变更历史 ----------
DROP TABLE IF EXISTS `crm_order_history`;
CREATE TABLE `crm_order_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `operation` VARCHAR(30) NOT NULL COMMENT 'CREATE/UPDATE/CONFIRM/APPROVE/REJECT/CONVERT_PROD/CONVERT_OUTSUB/SHIP/PARTIAL_SHIP/SETTLE/CLOSE/CANCEL/CREDIT_CHECK/PDF_DOWNLOAD/EXCEL_DOWNLOAD/PROFIT_ANALYSIS',
  `before_json` TEXT,
  `after_json` TEXT,
  `changed_by` BIGINT NOT NULL,
  `changed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_changed_by` (`changed_by`),
  KEY `idx_changed_at` (`changed_at`),
  KEY `idx_operation` (`operation`)
) ENGINE=InnoDB COMMENT='订单变更历史 (V1.3.7 Story 1.6 · 红线5 变更留痕)';

-- ---------- 4. crm_order_payment 订单回款 ----------
DROP TABLE IF EXISTS `crm_order_payment`;
CREATE TABLE `crm_order_payment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `payment_no` VARCHAR(30) NOT NULL COMMENT '回款单号(自动生成)',
  `amount` DECIMAL(15,2) NOT NULL,
  `payment_date` DATE NOT NULL,
  `payment_method` VARCHAR(20) NOT NULL DEFAULT 'BANK' COMMENT 'BANK/CASH/CHECK/OTHER',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/CONFIRMED/CANCELLED',
  `comment` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_payment_no` (`payment_no`),
  KEY `idx_order_id` (`order_id`),
  CONSTRAINT `fk_order_payment_order` FOREIGN KEY (`order_id`) REFERENCES `crm_order` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='订单回款 (V1.3.7 Story 1.6 · SETTLED 状态联动)';

-- ---------- 5. 7 状态机枚举字典 (V1.3.7 §附录-b) ----------
USE `cnc_platform`;

INSERT INTO `sys_dict_type` (`type_code`, `type_name`, `description`, `is_builtin`) VALUES
('ORDER_STATUS', '订单状态', 'DRAFT/CONFIRMED/PRODUCING/PARTIAL_SHIPPED/SHIPPED/SETTLED/CLOSED/CANCELLED', 1)
ON DUPLICATE KEY UPDATE `type_name` = VALUES(`type_name`);

INSERT IGNORE INTO `sys_dict` (`dict_type`, `dict_code`, `dict_label`, `sort`, `status`) VALUES
('ORDER_STATUS', 'DRAFT', '草稿', 1, 'ACTIVE'),
('ORDER_STATUS', 'CONFIRMED', '已确认', 2, 'ACTIVE'),
('ORDER_STATUS', 'PRODUCING', '生产中', 3, 'ACTIVE'),
('ORDER_STATUS', 'PARTIAL_SHIPPED', '部分发货', 4, 'ACTIVE'),
('ORDER_STATUS', 'SHIPPED', '已发货', 5, 'ACTIVE'),
('ORDER_STATUS', 'SETTLED', '已结算', 6, 'ACTIVE'),
('ORDER_STATUS', 'CLOSED', '已关闭', 7, 'ACTIVE'),
('ORDER_STATUS', 'CANCELLED', '已取消', 8, 'ACTIVE');

-- ---------- 6. 信用额度字典类型 (V1.3.7 P2 修补 3) ----------
INSERT INTO `sys_dict_type` (`type_code`, `type_name`, `description`, `is_builtin`) VALUES
('CREDIT_LIMIT', '客户信用额度', '客户ID -> 信用额度(CNY, -1=无限制), 超限抛40909', 1)
ON DUPLICATE KEY UPDATE `type_name` = VALUES(`type_name`);

-- V94 · mock 清理：CREDIT_LIMIT 演示客户额度已移至 init_data.sql

USE `cnc_business`;

-- 5 条 UI 红线检查
SELECT 'V5__crm_order.sql 迁移完成' AS message,
       (SELECT COUNT(*) FROM cnc_business.crm_order) AS order_count,
       (SELECT COUNT(*) FROM cnc_business.crm_order_item) AS order_item_count,
       (SELECT COUNT(*) FROM cnc_platform.sys_dict WHERE dict_type = 'ORDER_STATUS') AS order_status_count,
       (SELECT COUNT(*) FROM cnc_platform.sys_dict WHERE dict_type = 'CREDIT_LIMIT') AS credit_limit_count;
-- include: V6__drawing.sql
-- ======================================================================
-- V6 · Story 1.7 图纸与物料 (Epic 3 / Story 3.1)
-- PO 范蠡 + SM 萧何 · 2026-06-12 · 合同 XP-ZPF202606082405 · PRD V1.3.7
-- 4 状态机：DRAFT → RELEASED → ARCHIVED + OBSOLETE（被新版本替代）
-- 3 P1 修补：图号唯一索引 / 版本号严格递增 / AES-256-GCM 加密
-- ======================================================================

-- 1) crm_drawing 图纸主表
CREATE TABLE IF NOT EXISTS crm_drawing (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    drawing_no VARCHAR(64) NOT NULL COMMENT '图号 WL-XXXX (Story 1.4 5 类码 · V1.3.7 红线)',
    version VARCHAR(16) NOT NULL DEFAULT 'v1' COMMENT '当前版本（v1 < v2 < v3 严格递增，P1 修补）',
    title VARCHAR(256) NOT NULL COMMENT '图纸标题',
    material_code VARCHAR(64) NOT NULL COMMENT '物料编码 WL-XXXX（必填 + 唯一校验）',
    process_route TEXT NOT NULL COMMENT '工艺路线 JSON（5 段成本聚合 hook · V1.3.4 留 1.9 BOM Story）',
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/RELEASED/ARCHIVED/OBSOLETE',
    pdf_path VARCHAR(512) COMMENT 'PDF 路径（MinIO / 本地 filesystem）',
    signature_scan_path VARCHAR(512) COMMENT '签字扫描件路径（AES-256-GCM 加密存储 · V1.3.6 红线）',
    is_encrypted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '签字扫描件是否加密（P1 修补）',
    owner_user_id BIGINT NOT NULL COMMENT '创建人（工程师）',
    dept_id BIGINT COMMENT '部门',
    is_fa TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'FA 件（> 20万 二次密码）',
    is_new TINYINT(1) NOT NULL DEFAULT 0 COMMENT '新品',
    comment TEXT COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_drawing_no_version (drawing_no, version) COMMENT '图号 + 版本 唯一复合索引（P1 修补）',
    UNIQUE KEY uk_material_code (material_code) COMMENT '物料编码唯一索引（P1 修补）',
    KEY idx_status (status),
    KEY idx_owner (owner_user_id),
    KEY idx_dept (dept_id),
    KEY idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图纸主表（Story 1.7 · Epic 3）';

-- 2) crm_drawing_version 版本历史
CREATE TABLE IF NOT EXISTS crm_drawing_version (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    drawing_id BIGINT NOT NULL COMMENT '图纸主表 ID',
    version VARCHAR(16) NOT NULL COMMENT '版本号 v1/v2/v3（P1 修补：严格递增）',
    pdf_path VARCHAR(512) COMMENT '版本 PDF 路径',
    signature_scan_path VARCHAR(512) COMMENT '版本签字扫描件路径',
    is_encrypted TINYINT(1) NOT NULL DEFAULT 0,
    change_reason VARCHAR(512) COMMENT '变更原因',
    changed_by BIGINT NOT NULL COMMENT '变更人',
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_drawing_version (drawing_id, version),
    KEY idx_drawing (drawing_id),
    KEY idx_changed (changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图纸版本历史';

-- 3) crm_drawing_history 变更历史
CREATE TABLE IF NOT EXISTS crm_drawing_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    drawing_id BIGINT NOT NULL,
    operation VARCHAR(32) NOT NULL COMMENT 'CREATE/UPDATE/ADD_VERSION/RELEASE/ARCHIVE/OBSOLETE',
    before_json TEXT COMMENT '变更前快照',
    after_json TEXT COMMENT '变更后快照',
    changed_by BIGINT NOT NULL,
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_drawing_op (drawing_id, operation),
    KEY idx_changed (changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图纸变更历史（@AuditLog 写入）';

-- 4) crm_drawing_signature 签字扫描件
CREATE TABLE IF NOT EXISTS crm_drawing_signature (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    drawing_id BIGINT NOT NULL,
    version VARCHAR(16) NOT NULL COMMENT '签字版本',
    signer_user_id BIGINT NOT NULL COMMENT '签字人',
    signature_image_path VARCHAR(512) NOT NULL COMMENT '签字图片路径（加密后）',
    encrypted_aes_key VARCHAR(512) COMMENT 'AES 密钥（V1.3.6 红线 · 256-GCM 加密）',
    iv VARCHAR(64) COMMENT '初始化向量（IV 唯一）',
    signed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_drawing_version_signer (drawing_id, version, signer_user_id),
    KEY idx_drawing (drawing_id),
    KEY idx_signer (signer_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图纸签字扫描件（V1.3.6 红线 · AES-256-GCM）';

-- V94 · mock 清理：图纸 seed 已移至 init_data.sql
-- include: V7__drawing_conversion.sql
-- ======================================================================
-- V7 · Story 1.8 工程转化 (Epic 3 / Story 3.2)
-- PO 范蠡 + SM 萧何 · 2026-06-12 · 合同 XP-ZPF202606082405 · PRD V1.3.7
-- 工程转化表 + 标注表 + 5 状态机（DRAFT/RELEASED/ARCHIVED/OBSOLETE/CONVERTED）
-- 3 P1 修补：转化锁定原版本 / 标注不可修改（只追加）/ 转化结果 PDF 签字扫描件复用 1.7
-- 3 P2 修补：标注 SVG 嵌入（部署阶段）/ 转化历史 timeline / 工程师工作量统计 hook
-- ======================================================================

-- 1) crm_drawing_conversion 工程转化表（锁定原版本 + CONVERTED 状态）
CREATE TABLE IF NOT EXISTS crm_drawing_conversion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    drawing_id BIGINT NOT NULL COMMENT '图纸主表 ID',
    drawing_no VARCHAR(64) NOT NULL COMMENT '图号（冗余）',
    locked_version VARCHAR(16) NOT NULL COMMENT '锁定原版本（P1 修补：防回写）',
    bom_no VARCHAR(64) COMMENT '下游 BOM 单号 BOM{yyyyMMdd}{seq:4}',
    bom_type VARCHAR(32) NOT NULL DEFAULT 'STANDARD' COMMENT 'STANDARD/FA/PROTOTYPE',
    target_qty INT NOT NULL DEFAULT 1 COMMENT '目标数量（正整数 · P1 修补）',
    total_cost DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '5 段成本聚合总和',
    engineer_user_id BIGINT NOT NULL COMMENT '工程师 ID',
    engineer_name VARCHAR(64) COMMENT '工程师姓名（PDF 水印用）',
    status VARCHAR(16) NOT NULL DEFAULT 'CONVERTED' COMMENT 'CONVERTED/FAILED',
    error_message VARCHAR(1024) COMMENT '失败原因',
    process_route_snapshot TEXT COMMENT '工艺路线快照（5 段）',
    cost_breakdown TEXT COMMENT '5 段成本明细 JSON',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_drawing_version (drawing_id, locked_version) COMMENT '图+版本 唯一（防重复转化）',
    KEY idx_drawing (drawing_id),
    KEY idx_bom_no (bom_no),
    KEY idx_status (status),
    KEY idx_engineer (engineer_user_id),
    KEY idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工程转化记录（Story 1.8 · 锁定原版本 · 5 段成本聚合）';

-- 2) crm_drawing_annotation 标注表（挂载版本 + 不可修改只追加）
CREATE TABLE IF NOT EXISTS crm_drawing_annotation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    drawing_id BIGINT NOT NULL COMMENT '图纸主表 ID',
    drawing_no VARCHAR(64) NOT NULL COMMENT '图号（冗余）',
    version VARCHAR(16) NOT NULL COMMENT '挂载版本（P1 修补：防 v1→v2 标注丢失）',
    type VARCHAR(32) NOT NULL COMMENT 'DIMENSION/TOLERANCE/PROCESS_REQ/TECH_NOTE',
    content TEXT NOT NULL COMMENT '标注内容（必填 · 至少 1 字符）',
    color VARCHAR(16) NOT NULL DEFAULT 'RED' COMMENT 'RED/YELLOW/BLUE/GREEN',
    x DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT 'PDF 视口 X 坐标',
    y DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT 'PDF 视口 Y 坐标',
    width DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '标注框宽',
    height DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '标注框高',
    priority INT NOT NULL DEFAULT 5 COMMENT '1-10（10 最高）',
    is_archived TINYINT(1) NOT NULL DEFAULT 0 COMMENT '归档标记（新增 v2 时 v1 自动归档）',
    svg_data TEXT COMMENT 'SVG 嵌入数据（P2 修补 · 部署阶段）',
    created_by BIGINT NOT NULL COMMENT '创建人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_drawing_version_type_xy (drawing_id, version, type, x, y) COMMENT '位置唯一（防重叠）',
    KEY idx_drawing (drawing_id),
    KEY idx_version (version),
    KEY idx_type (type),
    KEY idx_priority (priority),
    KEY idx_creator (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图纸标注（4 类型 · 挂载版本 · 不可修改只追加）';

-- 3) crm_drawing_annotation_history 标注历史（P1 修补：只追加不留痕 + 工程师工作量统计 hook）
CREATE TABLE IF NOT EXISTS crm_drawing_annotation_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    annotation_id BIGINT NOT NULL COMMENT '标注 ID',
    drawing_id BIGINT NOT NULL,
    operation VARCHAR(16) NOT NULL COMMENT 'CREATE/ARCHIVE',
    actor_user_id BIGINT NOT NULL COMMENT '操作人',
    snapshot TEXT COMMENT '标注快照 JSON',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_annotation (annotation_id),
    KEY idx_drawing (drawing_id),
    KEY idx_actor (actor_user_id),
    KEY idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标注历史 + 工程师工作量统计 hook';

-- 4) crm_engineer_workload 工程师工作量统计 hook（P2 修补）
CREATE TABLE IF NOT EXISTS crm_engineer_workload (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '工程师 ID',
    user_name VARCHAR(64) NOT NULL,
    work_date DATE NOT NULL COMMENT '工作日',
    annotation_count INT NOT NULL DEFAULT 0 COMMENT '当日标注数',
    conversion_count INT NOT NULL DEFAULT 0 COMMENT '当日转化数',
    drawing_created_count INT NOT NULL DEFAULT 0 COMMENT '当日创建图纸数',
    UNIQUE KEY uk_user_date (user_id, work_date),
    KEY idx_user (user_id),
    KEY idx_date (work_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工程师工作量统计（P2 修补 · 部署阶段触发）';

-- V94 · mock 清理：工程转化/标注 seed 已移至 init_data.sql
-- include: V8__bom.sql
-- ======================================================================
-- V8 · Story 1.9 BOM 多级维护 (Epic 3 / Story 3.3)
-- PO 范蠡 + SM 萧何 · 2026-06-12 · 合同 XP-ZPF202606082405 · PRD V1.3.7
-- 3 表：BOM 主表 + BOM 多级树 + BOM 历史
-- 4 P1 修补：5 级递归上限 / 物料编码唯一 / 数量正整数 / 发布后只读
-- 4 P2 修补：物料 5 段成本聚合（V1.3.4 闭环）/ 物料替代 / 多 BOM 版本 / BOM 对比
-- ======================================================================

-- 1) crm_bom BOM 主表
CREATE TABLE IF NOT EXISTS crm_bom (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bom_no VARCHAR(64) NOT NULL COMMENT 'BOM 单号 BOM{yyyyMMdd}{seq:4}（V1.3.7 红线 4 · 100 并发不重复）',
    bom_version VARCHAR(16) NOT NULL DEFAULT 'v1' COMMENT 'BOM 版本（多版本 · P2 修补）',
    drawing_id BIGINT NOT NULL COMMENT '源图纸 ID（Story 1.8 工程转化）',
    drawing_no VARCHAR(64) NOT NULL COMMENT '图号（冗余）',
    bom_type VARCHAR(32) NOT NULL DEFAULT 'STANDARD' COMMENT 'STANDARD/FA/PROTOTYPE',
    target_qty INT NOT NULL DEFAULT 1 COMMENT '目标数量（正整数 · P1 修补）',
    material_code VARCHAR(64) NOT NULL COMMENT '主物料编码（唯一 · P1 修补）',
    total_cost DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '5 段成本聚合（V1.3.4 闭环 · P2 修补）',
    cost_breakdown TEXT COMMENT '5 段成本明细 JSON',
    process_route_id BIGINT COMMENT '关联工艺路线（Story 1.10）',
    parent_bom_id BIGINT COMMENT '父 BOM ID（多级树 · 自引用 · 5 级递归上限 P1 修补）',
    bom_level INT NOT NULL DEFAULT 0 COMMENT 'BOM 层级（0=根节点 · 1-4 子节点）',
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/RELEASED/ARCHIVED（P1 修补 4：发布后只读）',
    owner_user_id BIGINT NOT NULL COMMENT '创建人（工程师）',
    released_by BIGINT COMMENT '发布人',
    released_at DATETIME COMMENT '发布时间',
    is_substitutable TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否允许物料替代（P2 修补）',
    comment TEXT COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_bom_no_version (bom_no, bom_version) COMMENT 'BOM 单号 + 版本 唯一（P2 修补：多版本）',
    UNIQUE KEY uk_material_code_version (material_code, bom_version) COMMENT '物料编码 + 版本 唯一（P1 修补 2）',
    KEY idx_drawing (drawing_id),
    KEY idx_status (status),
    KEY idx_parent (parent_bom_id),
    KEY idx_level (bom_level),
    KEY idx_owner (owner_user_id),
    KEY idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BOM 主表（Story 1.9 · Epic 3 · 5 段成本聚合）';

-- 2) crm_bom_item BOM 多级树（自引用 + 5 级递归上限）
CREATE TABLE IF NOT EXISTS crm_bom_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bom_id BIGINT NOT NULL COMMENT '所属 BOM 主表 ID',
    parent_item_id BIGINT COMMENT '父物料项 ID（自引用 · 5 级递归）',
    item_level INT NOT NULL DEFAULT 0 COMMENT '物料层级 0-4',
    item_no INT NOT NULL DEFAULT 1 COMMENT '同级排序',
    material_code VARCHAR(64) NOT NULL COMMENT '物料编码',
    material_name VARCHAR(256) NOT NULL COMMENT '物料名称',
    spec VARCHAR(256) COMMENT '规格',
    qty DECIMAL(18,4) NOT NULL DEFAULT 1 COMMENT '数量（正数 · P1 修补 3）',
    unit VARCHAR(16) NOT NULL DEFAULT 'PCS' COMMENT '单位 PCS/KG/M',
    unit_cost DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '单价',
    total_cost DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '总成本（qty * unit_cost * target_qty）',
    segment VARCHAR(32) NOT NULL DEFAULT '原材料' COMMENT '5 段：原材料/粗加工/精加工/表面处理/检验',
    substitute_materials VARCHAR(512) COMMENT '替代物料编码（多个逗号分隔 · P2 修补）',
    is_substitute TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否为替代物料',
    process_step_id BIGINT COMMENT '关联工序（Story 1.10）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_bom (bom_id),
    KEY idx_parent_item (parent_item_id),
    KEY idx_level (item_level),
    KEY idx_material (material_code),
    KEY idx_segment (segment)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BOM 多级树（5 级递归 · P1 修补 1）';

-- 3) crm_bom_history BOM 历史（P1 修补：完整变更追踪）
CREATE TABLE IF NOT EXISTS crm_bom_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bom_id BIGINT NOT NULL,
    operation VARCHAR(32) NOT NULL COMMENT 'CREATE/UPDATE/RELEASE/ARCHIVE/CONVERT_TO_PRODUCTION',
    before_json TEXT COMMENT '变更前快照',
    after_json TEXT COMMENT '变更后快照',
    work_order_no VARCHAR(64) COMMENT '转生产工单号 GD{yyyyMMdd}{seq:4}（P1 修补 4 hook）',
    changed_by BIGINT NOT NULL,
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_bom_op (bom_id, operation),
    KEY idx_work_order (work_order_no),
    KEY idx_changed (changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BOM 变更历史（含转生产工单）';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- include: V9__process.sql
-- ======================================================================
-- V9 · Story 1.10 工艺库与工序 (Epic 3 / Story 3.4)
-- PO 范蠡 + SM 萧何 · 2026-06-12 · 合同 XP-ZPF202606082405 · PRD V1.3.7
-- 3 表：crm_process 工艺 + crm_process_step 工序 + crm_process_route 工艺路线
-- 5 段成本：原材料/粗加工/精加工/表面处理/检验（V1.3.4 闭环）
-- 3 P1 修补：工序排序严格 / 机器类型匹配 / 工时非负
-- 3 P2 修补：5 段成本自动聚合 / 工艺复用 / 工艺变更历史
-- ======================================================================

-- 1) crm_process 工艺库主表
CREATE TABLE IF NOT EXISTS crm_process (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    process_code VARCHAR(64) NOT NULL COMMENT '工艺编码 PROC{yyyyMMdd}{seq:4}',
    process_name VARCHAR(256) NOT NULL COMMENT '工艺名称',
    process_type VARCHAR(32) NOT NULL DEFAULT 'STANDARD' COMMENT 'STANDARD/FA/PROTOTYPE',
    description TEXT COMMENT '工艺描述',
    total_steps INT NOT NULL DEFAULT 0 COMMENT '工序数',
    total_estimated_hours DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '总工时',
    total_cost DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '5 段成本聚合（V1.3.4 闭环）',
    cost_breakdown TEXT COMMENT '5 段成本明细 JSON',
    drawing_id BIGINT COMMENT '关联图纸（可空 · 工艺复用 · P2 修补）',
    drawing_no VARCHAR(64) COMMENT '图号（冗余）',
    is_reusable TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否可复用（P2 修补：工艺复用）',
    is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    owner_user_id BIGINT NOT NULL COMMENT '创建人',
    comment TEXT COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_process_code (process_code),
    KEY idx_type (process_type),
    KEY idx_drawing (drawing_id),
    KEY idx_active (is_active),
    KEY idx_owner (owner_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工艺库（Story 1.10 · 5 段成本聚合）';

-- 2) crm_process_step 工序库（5 段）
CREATE TABLE IF NOT EXISTS crm_process_step (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    process_id BIGINT NOT NULL COMMENT '所属工艺 ID',
    step_no INT NOT NULL COMMENT '工序序号（P1 修补 1：严格排序）',
    step_name VARCHAR(128) NOT NULL COMMENT '工序名称',
    segment VARCHAR(32) NOT NULL DEFAULT '原材料' COMMENT '5 段：原材料/粗加工/精加工/表面处理/检验',
    machine_type VARCHAR(64) COMMENT '机器类型（P1 修补 2：必须匹配）',
    machine_id BIGINT COMMENT '具体机器 ID',
    estimated_hours DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '工时（P1 修补 3：非负）',
    unit_cost DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '单价',
    description TEXT COMMENT '工序描述',
    is_quality_check TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否为质检工序',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_process_step (process_id, step_no) COMMENT '同工艺内 step_no 唯一（P1 修补 1）',
    KEY idx_process (process_id),
    KEY idx_segment (segment),
    KEY idx_machine (machine_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工序库（5 段 · 严格排序）';

-- 3) crm_process_route 工艺路线（图纸关联）
CREATE TABLE IF NOT EXISTS crm_process_route (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    drawing_id BIGINT NOT NULL COMMENT '图纸 ID',
    drawing_no VARCHAR(64) NOT NULL COMMENT '图号',
    process_id BIGINT NOT NULL COMMENT '工艺 ID',
    process_code VARCHAR(64) NOT NULL COMMENT '工艺编码（冗余）',
    version VARCHAR(16) NOT NULL DEFAULT 'v1' COMMENT '工艺路线版本',
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/RELEASED/ARCHIVED',
    released_by BIGINT COMMENT '发布人',
    released_at DATETIME COMMENT '发布时间',
    change_reason VARCHAR(512) COMMENT '变更原因（P2 修补 3：工艺变更历史）',
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_drawing_version (drawing_id, version),
    KEY idx_drawing (drawing_id),
    KEY idx_process (process_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工艺路线（图纸关联 · 变更历史）';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- include: V10__material_barcode.sql
-- V1.3.7 · Story 1.11 · 物料条码生成 · Epic 4
-- 迁移：crm_material_barcode 物料条码 + crm_barcode_history 扫码历史
-- 继承 Story 1.4 (5 类码) + 1.7 (DocNoGenerator) + 1.9 (BOM 多级树)

-- 1. 物料分类表（5 段：原材料/外购件/自制件/委外件/成品）
CREATE TABLE IF NOT EXISTS crm_material_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_code VARCHAR(32) NOT NULL UNIQUE,         -- 分类编码：MAT-RAW / MAT-BUY / MAT-MFG / MAT-OUT / MAT-FIN
    category_name VARCHAR(64) NOT NULL,                 -- 分类名称
    prefix VARCHAR(8) NOT NULL,                        -- 条码 prefix：WL-原材料 / WJ-外购 / ZZ-自制 / WW-委外 / CP-成品
    seq_no INT DEFAULT 0,                               -- 排序
    is_active TINYINT(1) DEFAULT 1,
    remark VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category_code (category_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 物料主数据（继承 1.4 / 1.7 / 1.9）
CREATE TABLE IF NOT EXISTS crm_material (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    material_code VARCHAR(32) NOT NULL UNIQUE,          -- WL-XXXX / WJ-XXXX / ZZ-XXXX / WW-XXXX / CP-XXXX
    material_name VARCHAR(128) NOT NULL,
    spec VARCHAR(255),                                  -- 规格
    unit VARCHAR(16) DEFAULT '个',
    category_id BIGINT,                                 -- FK crm_material_category
    process_id BIGINT,                                  -- FK crm_process（工艺路线）
    cost_material DECIMAL(18,4) DEFAULT 0,              -- 5 段成本
    cost_labor DECIMAL(18,4) DEFAULT 0,
    cost_machine DECIMAL(18,4) DEFAULT 0,
    cost_overhead DECIMAL(18,4) DEFAULT 0,
    cost_outsource DECIMAL(18,4) DEFAULT 0,
    cost_total DECIMAL(18,4) DEFAULT 0,                 -- 5 段总成本
    is_active TINYINT(1) DEFAULT 1,
    owner_user_id BIGINT,
    dept_id BIGINT DEFAULT 10,
    remark VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_material_category (category_id),
    INDEX idx_material_process (process_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 物料条码主表（核心表）
CREATE TABLE IF NOT EXISTS crm_material_barcode (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    barcode_no VARCHAR(32) NOT NULL UNIQUE,             -- BC{yyyyMMdd}{seq:4}（DocNoGenerator）
    material_code VARCHAR(32) NOT NULL,                 -- WL-XXXX（FK crm_material）
    spec VARCHAR(255),
    payload TEXT,                                       -- AES-256-GCM 加密 payload（material_code + spec + process_id + cost + batch）
    process_id BIGINT,                                  -- 工艺 ID
    cost_breakdown VARCHAR(1024),                       -- JSON 5 段成本
    batch_no VARCHAR(64),                               -- 批次号（1.13 联动）
    qty INT DEFAULT 1,                                  -- 数量（默认 1）
    qr_code_url VARCHAR(512),                           -- 二维码 base64（P2 修补）
    status VARCHAR(16) DEFAULT 'ACTIVE',                -- ACTIVE / USED / DISCARDED
    generated_by BIGINT,
    generated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_barcode_material (material_code),
    INDEX idx_barcode_status (status),
    INDEX idx_barcode_batch (batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. 条码扫码历史
CREATE TABLE IF NOT EXISTS crm_barcode_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    barcode_no VARCHAR(32) NOT NULL,                    -- FK crm_material_barcode
    scan_user_id BIGINT NOT NULL,
    scan_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    scan_location VARCHAR(128),                         -- 扫码地点（如：A 仓 / B 仓 / 线边仓）
    scan_type VARCHAR(16) NOT NULL,                     -- GENERATE / PARSE / INBOUND / OUTBOUND / VERIFY
    scan_result VARCHAR(16) DEFAULT 'SUCCESS',          -- SUCCESS / FAILED
    error_msg VARCHAR(255),
    client_type VARCHAR(16) DEFAULT 'WEB',              -- WEB / ANDROID / IOS
    remark VARCHAR(255),
    INDEX idx_history_barcode (barcode_no),
    INDEX idx_history_user (scan_user_id),
    INDEX idx_history_at (scan_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- include: V11__warehouse_scan.sql
-- V1.3.7 · Story 1.12 · APP 扫码出入库
-- 迁移：crm_warehouse_scan 扫码记录 + crm_warehouse_location 库位（1.13 共享）
-- 复用 1.4 5 类码 prefix（WL-物料码 / WW-委外单码）

CREATE TABLE IF NOT EXISTS crm_warehouse_location (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    location_code VARCHAR(32) NOT NULL UNIQUE,          -- LOC-A01-01-01
    warehouse VARCHAR(32) NOT NULL,                    -- 仓库：WH-A 主仓 / WH-B 副仓 / WH-C 线边仓
    zone VARCHAR(32),                                  -- 库区：A01 / B02
    position VARCHAR(32),                              -- 库位：01 / 02
    capacity DECIMAL(18,4) DEFAULT 0,                  -- 库容
    is_active TINYINT(1) DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_location_warehouse (warehouse),
    INDEX idx_location_zone (zone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS crm_warehouse_scan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scan_no VARCHAR(32) NOT NULL UNIQUE,               -- SC{yyyyMMdd}{seq:4}
    scan_type VARCHAR(16) NOT NULL,                    -- INBOUND / OUTBOUND
    barcode_no VARCHAR(32) NOT NULL,                   -- 条码号
    material_code VARCHAR(32) NOT NULL,
    location_code VARCHAR(32),                         -- 库位
    qty INT NOT NULL DEFAULT 1,                        -- 数量
    workorder_no VARCHAR(32),                          -- 出库关联工单
    batch_no VARCHAR(64),                              -- 批次号
    client_id VARCHAR(64),                             -- APP 客户端 ID（离线同步用）
    sync_status VARCHAR(16) DEFAULT 'SYNCED',          -- SYNCED / PENDING / FAILED
    scanned_by BIGINT NOT NULL,
    scanned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    synced_at DATETIME,
    conflict_type VARCHAR(32),                         -- QTY_OVERFLOW / LOCATION_MISMATCH / DUPLICATE_SCAN
    conflict_resolution VARCHAR(32),                   -- LOCAL_OVERRIDE / SERVER_OVERRIDE / MANUAL
    remark VARCHAR(255),
    INDEX idx_scan_barcode (barcode_no),
    INDEX idx_scan_type (scan_type),
    INDEX idx_scan_status (sync_status),
    INDEX idx_scan_client (client_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- include: V12__warehouse_location.sql
-- V1.3.7 · Story 1.13 · 库位批次与多仓库
-- 迁移：crm_warehouse 仓库 + crm_warehouse_location 库位（升级版） + crm_batch 批次

CREATE TABLE IF NOT EXISTS crm_warehouse (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    warehouse_code VARCHAR(32) NOT NULL UNIQUE,
    warehouse_name VARCHAR(64) NOT NULL,
    warehouse_type VARCHAR(16) NOT NULL,               -- MAIN / SUB / LINE_SIDE
    address VARCHAR(255),
    manager_user_id BIGINT,
    is_active TINYINT(1) DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_warehouse_type (warehouse_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS crm_batch (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_no VARCHAR(64) NOT NULL UNIQUE,              -- B{yyyyMMdd}{seq:6}
    material_code VARCHAR(32) NOT NULL,
    supplier_id BIGINT,
    supplier_name VARCHAR(128),
    qty INT NOT NULL DEFAULT 0,
    received_at DATETIME,
    expired_at DATETIME,
    quality_status VARCHAR(16) DEFAULT 'PENDING',     -- PENDING / PASSED / FAILED
    location_code VARCHAR(32),
    fefo_order INT DEFAULT 0,                          -- FEFO 先入先出排序
    remark VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_batch_material (material_code),
    INDEX idx_batch_supplier (supplier_id),
    INDEX idx_batch_quality (quality_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- include: V13__inventory_safety.sql
-- V1.3.7 · Story 1.14 · 安全库存与预警
-- 迁移：crm_inventory_safety 安全库存 + crm_inventory_alert 预警记录

CREATE TABLE IF NOT EXISTS crm_inventory_safety (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    material_code VARCHAR(32) NOT NULL UNIQUE,
    material_name VARCHAR(128),
    min_qty INT NOT NULL DEFAULT 0,                   -- 安全库存下限
    max_qty INT NOT NULL DEFAULT 0,                   -- 安全库存上限
    reorder_qty INT NOT NULL DEFAULT 0,               -- 补货量
    unit VARCHAR(16) DEFAULT '个',
    current_qty INT DEFAULT 0,                        -- 当前库存（来自 crm_batch 汇总）
    enabled TINYINT(1) DEFAULT 1,
    owner_user_id BIGINT,
    remark VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_safety_material (material_code),
    INDEX idx_safety_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS crm_inventory_alert (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    material_code VARCHAR(32) NOT NULL,
    alert_level VARCHAR(16) NOT NULL,                 -- INFO / WARN / ERROR / CRITICAL
    current_qty INT NOT NULL,
    min_qty INT NOT NULL,
    message VARCHAR(255),
    status VARCHAR(16) DEFAULT 'OPEN',                -- OPEN / RESOLVED / ARCHIVED
    triggered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at DATETIME,
    resolved_by BIGINT,
    resolution_note VARCHAR(255),
    notified TINYINT(1) DEFAULT 0,                    -- 是否已通知
    INDEX idx_alert_material (material_code),
    INDEX idx_alert_status (status),
    INDEX idx_alert_level (alert_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- include: V14__workorder.sql
-- V1.3.7 · Story 1.15 · 工单与排产 · Epic 5
-- 迁移：crm_workorder 工单 + crm_workorder_step 工序 + crm_production_schedule 排产

CREATE TABLE IF NOT EXISTS crm_workorder (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    workorder_no VARCHAR(32) NOT NULL UNIQUE,            -- GD{yyyyMMdd}{seq:4}
    drawing_id BIGINT,
    bom_id BIGINT,
    process_route_id BIGINT,
    material_code VARCHAR(32) NOT NULL,                 -- 物料编码（成品 CP-XXXX）
    product_name VARCHAR(128),
    qty INT NOT NULL DEFAULT 1,
    unit VARCHAR(16) DEFAULT '台',
    priority INT DEFAULT 5,                              -- 1=紧急 ~ 10=低
    status VARCHAR(16) DEFAULT 'DRAFT',                 -- DRAFT/SCHEDULED/IN_PROGRESS/COMPLETED/CANCELLED
    scheduled_start DATETIME,
    scheduled_end DATETIME,
    actual_start DATETIME,
    actual_end DATETIME,
    equipment_id BIGINT,                                -- 机台 ID
    equipment_type VARCHAR(32),                         -- 机台类型
    estimated_hours DECIMAL(10,2) DEFAULT 0,             -- 预计工时
    actual_hours DECIMAL(10,2) DEFAULT 0,
    is_fa TINYINT(1) DEFAULT 0,
    created_by BIGINT,
    owner_user_id BIGINT,
    dept_id BIGINT DEFAULT 10,
    remark VARCHAR(255),
    sales_order_id BIGINT NULL COMMENT '销售订单ID',
    sales_order_no VARCHAR(32) NULL COMMENT '销售订单号 XS',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_workorder_status (status),
    INDEX idx_workorder_equipment (equipment_id),
    INDEX idx_workorder_priority (priority),
    INDEX idx_workorder_sales_order (sales_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS crm_workorder_step (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    workorder_id BIGINT NOT NULL,
    step_no INT NOT NULL,                                -- 工序号 1/2/3
    step_name VARCHAR(64) NOT NULL,
    equipment_type VARCHAR(32),
    estimated_minutes INT DEFAULT 0,
    actual_minutes INT DEFAULT 0,
    status VARCHAR(16) DEFAULT 'PENDING',                -- PENDING/IN_PROGRESS/COMPLETED
    started_at DATETIME,
    completed_at DATETIME,
    operator_user_id BIGINT,
    INDEX idx_step_workorder (workorder_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS crm_production_schedule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    schedule_no VARCHAR(32) NOT NULL UNIQUE,             -- SCH{yyyyMMdd}{seq:4}
    workorder_id BIGINT NOT NULL,
    equipment_id BIGINT NOT NULL,
    equipment_type VARCHAR(32),
    plan_start DATETIME NOT NULL,
    plan_end DATETIME NOT NULL,
    status VARCHAR(16) DEFAULT 'PLANNED',                -- PLANNED/IN_PROGRESS/COMPLETED/CONFLICT
    conflict_with BIGINT,                                -- 冲突工单 ID
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_schedule_equipment (equipment_id),
    INDEX idx_schedule_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- include: V15__production_scan.sql
-- V1.3.7 · Story 1.16 · APP 扫码开工/报工/过站
-- 迁移：crm_production_scan 生产扫码 + crm_production_report 报工 + crm_production_station 过站

CREATE TABLE IF NOT EXISTS crm_production_scan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scan_no VARCHAR(32) NOT NULL UNIQUE,                 -- PS{yyyyMMdd}{seq:4}
    workorder_no VARCHAR(32) NOT NULL,                   -- 工单号 GD-XXXX
    scan_type VARCHAR(16) NOT NULL,                      -- START / REPORT / STATION
    operator_user_id BIGINT NOT NULL,
    equipment_id BIGINT,
    qty INT DEFAULT 0,
    step_no INT,
    scanned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    client_id VARCHAR(64),                               -- APP 客户端 ID
    sync_status VARCHAR(16) DEFAULT 'SYNCED',
    remark VARCHAR(255),
    INDEX idx_ps_workorder (workorder_no),
    INDEX idx_ps_type (scan_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS crm_production_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    report_no VARCHAR(32) NOT NULL UNIQUE,               -- RP{yyyyMMdd}{seq:4}
    workorder_no VARCHAR(32) NOT NULL,
    step_no INT NOT NULL,                                -- 工序号
    reported_qty INT NOT NULL DEFAULT 0,                 -- 报工数量
    actual_minutes INT DEFAULT 0,                        -- 实际工时（分钟）
    is_abnormal TINYINT(1) DEFAULT 0,                    -- 异常标记
    abnormal_type VARCHAR(32),                          -- 异常类型：QUALITY/EQUIPMENT/MATERIAL
    abnormal_note VARCHAR(255),
    reported_by BIGINT NOT NULL,
    reported_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_report_workorder (workorder_no),
    INDEX idx_report_step (step_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS crm_production_station (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    transfer_no VARCHAR(32) NOT NULL UNIQUE,             -- TR{yyyyMMdd}{seq:4}
    workorder_no VARCHAR(32) NOT NULL,
    from_step_no INT NOT NULL,                          -- 源工序
    to_step_no INT NOT NULL,                            -- 目标工序
    from_equipment_id BIGINT,
    to_equipment_id BIGINT,
    transferred_by BIGINT NOT NULL,
    transferred_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(255),
    INDEX idx_station_workorder (workorder_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- include: V16__mrp.sql
-- V1.3.7 · Story 1.17 · MRP 物料需求分析
-- 迁移：crm_mrp_run MRP 运算记录 + crm_mrp_result MRP 结果 + crm_mrp_shortage 缺料清单

CREATE TABLE IF NOT EXISTS crm_mrp_run (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    run_no VARCHAR(32) NOT NULL UNIQUE,                  -- MR{yyyyMMdd}{seq:4}
    run_type VARCHAR(16) DEFAULT 'FULL',                 -- FULL / INCREMENTAL
    date_range_start DATE,
    date_range_end DATE,
    warehouse_ids VARCHAR(255),                          -- 仓库 ID 列表（逗号分隔）
    status VARCHAR(16) DEFAULT 'RUNNING',                -- RUNNING / COMPLETED / FAILED
    started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME,
    total_shortage INT DEFAULT 0,                        -- 缺料总数
    total_purchase_suggestion INT DEFAULT 0,             -- 建议采购总量
    triggered_by BIGINT,
    remark VARCHAR(255),
    INDEX idx_mrp_run_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS crm_mrp_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    run_id BIGINT NOT NULL,
    material_code VARCHAR(32) NOT NULL,
    material_name VARCHAR(128),
    required_qty INT NOT NULL,                           -- 需求数量
    current_stock INT DEFAULT 0,                         -- 当前库存
    on_order_qty INT DEFAULT 0,                          -- 在途数量
    shortage_qty INT DEFAULT 0,                          -- 缺料数量
    purchase_suggestion INT DEFAULT 0,                   -- 建议采购量
    supplier_id BIGINT,
    unit_cost DECIMAL(18,4) DEFAULT 0,
    total_cost DECIMAL(18,4) DEFAULT 0,
    INDEX idx_result_run (run_id),
    INDEX idx_result_material (material_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS crm_mrp_shortage (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    run_id BIGINT NOT NULL,
    material_code VARCHAR(32) NOT NULL,
    shortage_qty INT NOT NULL,
    required_date DATE,
    priority INT DEFAULT 5,
    source_workorders TEXT,                              -- 关联工单号（逗号分隔）
    INDEX idx_shortage_run (run_id),
    INDEX idx_shortage_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- include: V17__outsource.sql
-- V1.3.7 · Story 1.18 · 委外下单基础
-- 迁移：crm_outsource_order 委外单 + crm_outsource_item 委外明细 + crm_outsource_history 委外历史

CREATE TABLE IF NOT EXISTS crm_outsource_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    outsource_no VARCHAR(32) NOT NULL UNIQUE,            -- WW{yyyyMMdd}{seq:4}（复用 1.4 prefix）
    workorder_no VARCHAR(32),                            -- 关联工单
    step_no INT,                                        -- 工序号
    supplier_id BIGINT NOT NULL,
    supplier_name VARCHAR(128),
    process_name VARCHAR(64),
    material_code VARCHAR(32),                          -- 委外物料
    drawing_id BIGINT NULL COMMENT '加工图纸 ID（crm_drawing.id）',
    qty INT NOT NULL DEFAULT 0,
    unit_price DECIMAL(18,4) DEFAULT 0,                 -- 单价
    total_amount DECIMAL(18,4) DEFAULT 0,               -- 总金额 = unit_price × qty
    delivery_date DATE,                                  -- 交期
    status VARCHAR(16) DEFAULT 'DRAFT',                 -- DRAFT/SENT/ACCEPTED/IN_PRODUCTION/INSPECTED/COMPLETED/CLOSED/REWORK
    rework_count INT DEFAULT 0,                         -- 返修次数（≤ 3）
    creator_user_id BIGINT,
    submitted_at DATETIME,
    accepted_at DATETIME,
    completed_at DATETIME,
    closed_at DATETIME,
    is_urgent TINYINT(1) DEFAULT 0,                      -- 加急
    remark VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_outsource_status (status),
    INDEX idx_outsource_supplier (supplier_id),
    INDEX idx_outsource_workorder (workorder_no),
    INDEX idx_outsource_drawing (drawing_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS crm_outsource_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    outsource_no VARCHAR(32) NOT NULL,
    item_seq INT NOT NULL,
    material_code VARCHAR(32) NOT NULL,
    material_name VARCHAR(128),
    spec VARCHAR(255),
    qty INT NOT NULL,
    unit VARCHAR(16) DEFAULT '个',
    unit_price DECIMAL(18,4) DEFAULT 0,
    total_amount DECIMAL(18,4) DEFAULT 0,
    delivery_date DATE,
    remark VARCHAR(255),
    INDEX idx_item_outsource (outsource_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS crm_outsource_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    outsource_no VARCHAR(32) NOT NULL,
    operation VARCHAR(32) NOT NULL,                     -- CREATE/SUBMIT/ACCEPT/START/INSPECT/COMPLETE/CLOSE/REWORK
    operator_user_id BIGINT,
    operated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    from_status VARCHAR(16),
    to_status VARCHAR(16),
    note VARCHAR(255),
    INDEX idx_history_outsource (outsource_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- include: V18__reconcile.sql
-- V1.3.7 · Story 1.21 · 月度对账 (FR-6-1)
-- 迁移：crm_reconcile 月度对账单 + crm_reconcile_item 对账明细 + crm_reconcile_signature 厂商签字扫描件
-- V1.3.7 AD-2 红线：不含"线下"动作

USE `cnc_business`;

-- 月度对账单
CREATE TABLE IF NOT EXISTS `crm_reconcile` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `reconcile_no` VARCHAR(30) NOT NULL COMMENT 'RC{yyyyMM}{seq:4}',
  `vendor_id` BIGINT NOT NULL,
  `vendor_name` VARCHAR(200) NOT NULL,
  `period_year` INT NOT NULL,
  `period_month` INT NOT NULL,
  `total_amount` DECIMAL(15,2) NOT NULL DEFAULT 0.00,
  `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/VENDOR_CONFIRMED/BOTH_CONFIRMED/FINANCE_CONFIRMED/CLOSED',
  `current_step` INT DEFAULT 1 COMMENT '1-4 步',
  `is_locked` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '对账期锁定',
  `created_by` BIGINT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_reconcile_no` (`reconcile_no`),
  KEY `idx_vendor_id` (`vendor_id`),
  KEY `idx_period` (`period_year`, `period_month`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='月度对账单 (V1.3.7 AD-2 不含"线下")';

-- 对账明细
CREATE TABLE IF NOT EXISTS `crm_reconcile_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `reconcile_id` BIGINT NOT NULL,
  `outsource_order_id` BIGINT NOT NULL,
  `outsource_order_no` VARCHAR(30) NOT NULL,
  `item_name` VARCHAR(200) NOT NULL,
  `quantity` INT NOT NULL,
  `unit_price` DECIMAL(15,2) NOT NULL,
  `amount` DECIMAL(15,2) NOT NULL,
  `vendor_amount` DECIMAL(15,2) DEFAULT NULL COMMENT '厂商确认金额',
  `final_amount` DECIMAL(15,2) DEFAULT NULL COMMENT '最终对账金额',
  `is_consistent` TINYINT(1) DEFAULT NULL COMMENT '金额是否一致',
  `sort` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_reconcile_id` (`reconcile_id`),
  CONSTRAINT `fk_reconcile_item_reconcile` FOREIGN KEY (`reconcile_id`) REFERENCES `crm_reconcile` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对账明细';

-- 厂商签字扫描件 (AES-256-GCM 加密)
CREATE TABLE IF NOT EXISTS `crm_reconcile_signature` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `reconcile_id` BIGINT NOT NULL,
  `signer_user_id` BIGINT NOT NULL,
  `signer_name` VARCHAR(100) NOT NULL,
  `signature_image_path` VARCHAR(500) NOT NULL,
  `encrypted_data` TEXT NOT NULL COMMENT 'AES-256-GCM 加密后的扫描件',
  `iv` VARCHAR(64) NOT NULL COMMENT '12 字节 IV 唯一',
  `auth_tag` VARCHAR(64) NOT NULL COMMENT '128-bit GCM tag',
  `signed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_reconcile_id_sig` (`reconcile_id`),
  CONSTRAINT `fk_reconcile_sig_reconcile` FOREIGN KEY (`reconcile_id`) REFERENCES `crm_reconcile` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='厂商签字扫描件 (V1.3.6 加密)';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- include: V19__outsource_state_machine.sql
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
-- include: V20__rework.sql
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
-- include: V21__outsource_eta.sql
-- V1.3.7 · Story 1.24 · 委外历史交期预估 (FR-6-4)
-- 迁移：crm_outsource_eta 预估交期 + crm_outsource_actual 实际交期
-- 3 P1 修补：偏差超 20% 自动告警 / 预估准确率 ≥ 80% / 预估必填
-- 模板：OE{yyyyMMdd}{seq:4}

USE `cnc_business`;

-- 预估交期（基于历史数据预测）
CREATE TABLE IF NOT EXISTS `crm_outsource_eta` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `eta_no` VARCHAR(32) NOT NULL COMMENT 'OE{yyyyMMdd}{seq:4}',
  `outsource_id` BIGINT NOT NULL COMMENT '关联委外单主键 ID',
  `outsource_no` VARCHAR(32) NOT NULL COMMENT '冗余委外单号',
  `supplier_id` BIGINT NOT NULL COMMENT '供应商 ID',
  `supplier_name` VARCHAR(128) DEFAULT NULL,
  `process_name` VARCHAR(64) DEFAULT NULL,
  `qty` INT NOT NULL DEFAULT 1 COMMENT '数量',
  `predicted_days` INT NOT NULL COMMENT '预估交期（天）',
  `predicted_delivery_date` DATE NOT NULL COMMENT '预估交付日期',
  `confidence` DECIMAL(5,2) NOT NULL DEFAULT 0.80 COMMENT '预估置信度 0~1（≥ 0.80 通过）',
  `base_samples` INT NOT NULL DEFAULT 0 COMMENT '基于历史样本数',
  `actual_delivery_date` DATE DEFAULT NULL COMMENT '实际交付日期（actual 表关联）',
  `deviation_pct` DECIMAL(5,2) DEFAULT NULL COMMENT '偏差率 %（actual - predicted）/ predicted × 100',
  `accuracy_passed` TINYINT(1) DEFAULT NULL COMMENT '是否通过 ≥ 80%',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PREDICTED' COMMENT 'PREDICTED/IN_PROGRESS/COMPLETED/OVERDUE/ALERTED',
  `created_by` BIGINT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_eta_no` (`eta_no`),
  KEY `idx_eta_outsource_id` (`outsource_id`),
  KEY `idx_eta_outsource_no` (`outsource_no`),
  KEY `idx_eta_supplier_id` (`supplier_id`),
  KEY `idx_eta_status` (`status`),
  KEY `idx_eta_predicted_date` (`predicted_delivery_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='委外预估交期（V1.3.7 Story 1.24 FR-6-4）';

-- 实际交期（仅记录历史完成实绩）
CREATE TABLE IF NOT EXISTS `crm_outsource_actual` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `eta_id` BIGINT DEFAULT NULL COMMENT '关联预估 ID',
  `outsource_id` BIGINT NOT NULL,
  `outsource_no` VARCHAR(32) NOT NULL,
  `supplier_id` BIGINT NOT NULL,
  `supplier_name` VARCHAR(128) DEFAULT NULL,
  `process_name` VARCHAR(64) DEFAULT NULL,
  `qty` INT NOT NULL DEFAULT 1,
  `promised_date` DATE NOT NULL COMMENT '承诺交期',
  `actual_date` DATE NOT NULL COMMENT '实际完成日',
  `actual_days` INT NOT NULL COMMENT '实际天数',
  `predicted_days` INT DEFAULT NULL COMMENT '预估天数（冗余）',
  `deviation_pct` DECIMAL(5,2) DEFAULT NULL COMMENT '偏差率',
  `on_time` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否准时 0/1',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_actual_outsource_id` (`outsource_id`),
  KEY `idx_actual_supplier_id` (`supplier_id`),
  KEY `idx_actual_actual_date` (`actual_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='委外实际交期历史（V1.3.7 Story 1.24 FR-6-4）';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- include: V22__outsource_incoming_inspection.sql
-- V1.3.7 · Story 1.25 · 委外来料质检 (FR-6-5)
-- 迁移：crm_outsource_incoming_inspection 来料质检单 + crm_outsource_incoming_item 检验项目 + crm_outsource_incoming_defect 不良项
-- 3 P1 修补：单一 163 邮箱（V1.3.7 AD-3）/ 检验项目必填 / 严重度分级
-- 模板：OI{yyyyMMdd}{seq:4}

USE `cnc_business`;

-- 来料质检单
CREATE TABLE IF NOT EXISTS `crm_outsource_incoming_inspection` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `inspection_no` VARCHAR(32) NOT NULL COMMENT 'OI{yyyyMMdd}{seq:4}',
  `outsource_id` BIGINT NOT NULL COMMENT '关联委外单主键 ID',
  `outsource_no` VARCHAR(32) NOT NULL,
  `supplier_id` BIGINT NOT NULL,
  `supplier_name` VARCHAR(128) DEFAULT NULL,
  `material_code` VARCHAR(32) DEFAULT NULL,
  `inspect_qty` INT NOT NULL DEFAULT 0 COMMENT '送检数量',
  `passed_qty` INT NOT NULL DEFAULT 0 COMMENT '合格数',
  `failed_qty` INT NOT NULL DEFAULT 0 COMMENT '不合格数',
  `defect_rate` DECIMAL(5,2) DEFAULT NULL COMMENT '不良率 %',
  `result` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PASSED/FAILED/CONDITIONAL',
  `inspector_user_id` BIGINT DEFAULT NULL,
  `inspected_at` DATETIME DEFAULT NULL,
  `notify_email` VARCHAR(128) DEFAULT NULL COMMENT '通知邮箱（V1.3.7 AD-3 · 单一 163 邮箱）',
  `remark` VARCHAR(500) DEFAULT NULL,
  `created_by` BIGINT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_inspection_no` (`inspection_no`),
  KEY `idx_incoming_outsource_id` (`outsource_id`),
  KEY `idx_incoming_outsource_no` (`outsource_no`),
  KEY `idx_incoming_result` (`result`),
  KEY `idx_incoming_supplier` (`supplier_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='委外来料质检单（V1.3.7 Story 1.25 FR-6-5）';

-- 检验项目（必填 · P1 修补 2）
CREATE TABLE IF NOT EXISTS `crm_outsource_incoming_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `inspection_id` BIGINT NOT NULL,
  `item_name` VARCHAR(128) NOT NULL COMMENT '检验项目名称（必填）',
  `standard` VARCHAR(500) DEFAULT NULL COMMENT '检验标准',
  `measured_value` VARCHAR(128) DEFAULT NULL COMMENT '实测值',
  `passed` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '0/1',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_incoming_item_inspection` (`inspection_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='来料检验项目（必填）';

-- 不良项（严重度分级 · P1 修补 3）
CREATE TABLE IF NOT EXISTS `crm_outsource_incoming_defect` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `inspection_id` BIGINT NOT NULL,
  `item_id` BIGINT DEFAULT NULL,
  `defect_type` VARCHAR(64) NOT NULL COMMENT '不良类型',
  `severity` VARCHAR(16) NOT NULL DEFAULT 'MINOR' COMMENT 'MINOR/MAJOR/CRITICAL（严重度分级）',
  `qty` INT NOT NULL DEFAULT 1 COMMENT '不良数量',
  `description` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_incoming_defect_inspection` (`inspection_id`),
  KEY `idx_incoming_defect_severity` (`severity`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='来料不良项（严重度分级）';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- include: V23__outsource_cost_aggregation.sql
-- V1.3.7 · Story 1.26 · 工序/整单委外成本归集 (FR-6-6)
-- 迁移：crm_outsource_cost_aggregation 委外成本归集（5 段成本自动聚合）
-- 3 P1 修补：5 段成本自动聚合 / 成本非负 / 偏差率统计
-- 5 段：MATERIAL/LABOR/MACHINE/OVERHEAD/OUTSOURCE

USE `cnc_business`;

-- 委外成本归集（按委外单 × 物料 × 工序）
CREATE TABLE IF NOT EXISTS `crm_outsource_cost_aggregation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `outsource_id` BIGINT NOT NULL COMMENT '关联委外单主键 ID',
  `outsource_no` VARCHAR(32) NOT NULL,
  `material_code` VARCHAR(32) NOT NULL,
  `process_name` VARCHAR(64) DEFAULT NULL,
  `cost_material` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '材料成本（5 段 1）',
  `cost_labor` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '人工成本（5 段 2）',
  `cost_machine` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '设备成本（5 段 3）',
  `cost_overhead` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '管理成本（5 段 4）',
  `cost_outsource` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '委外成本（5 段 5）',
  `cost_total` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '总成本 = 5 段累加',
  `budget_cost` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '预算成本（用于偏差率）',
  `deviation_pct` DECIMAL(5,2) DEFAULT NULL COMMENT '偏差率 %',
  `deviation_level` VARCHAR(16) DEFAULT NULL COMMENT 'WITHIN/WARN/OVER',
  `aggregation_scope` VARCHAR(16) NOT NULL DEFAULT 'STEP' COMMENT 'STEP/PROCESS/WHOLE',
  `remark` VARCHAR(500) DEFAULT NULL,
  `created_by` BIGINT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_outsource_material_process` (`outsource_id`, `material_code`, `process_name`, `aggregation_scope`),
  KEY `idx_cost_agg_outsource_id` (`outsource_id`),
  KEY `idx_cost_agg_material` (`material_code`),
  KEY `idx_cost_agg_scope` (`aggregation_scope`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='委外成本归集（V1.3.7 Story 1.26 FR-6-6）';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- include: V24__outsource_quality.sql
-- V1.3.7 · Story 1.27 · 委外质检 (FR-6-7)
-- 迁移：crm_outsource_quality 委外工序质检单 + crm_outsource_quality_item FA/CMM 检验项目 + crm_outsource_quality_defect 不良项
-- 3 P1 修补：检验项目必填 / 严重度分级 / 不良率 > 10% 告警
-- 模板：OQ{yyyyMMdd}{seq:4}

USE `cnc_business`;

-- 委外工序质检单（区别于 7 品质的来料/过程/成品检）
CREATE TABLE IF NOT EXISTS `crm_outsource_quality` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `quality_no` VARCHAR(32) NOT NULL COMMENT 'OQ{yyyyMMdd}{seq:4}',
  `outsource_id` BIGINT NOT NULL,
  `outsource_no` VARCHAR(32) NOT NULL,
  `process_name` VARCHAR(64) NOT NULL COMMENT '工序名称（区别于 1.25 来料）',
  `supplier_id` BIGINT NOT NULL,
  `supplier_name` VARCHAR(128) DEFAULT NULL,
  `inspect_type` VARCHAR(16) NOT NULL DEFAULT 'FA' COMMENT 'FA（首件）/CMM（三次元）',
  `inspect_qty` INT NOT NULL DEFAULT 1,
  `passed_qty` INT NOT NULL DEFAULT 0,
  `failed_qty` INT NOT NULL DEFAULT 0,
  `defect_rate` DECIMAL(5,2) DEFAULT NULL,
  `alerted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '不良率 > 10% 告警',
  `result` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PASSED/FAILED/CONDITIONAL',
  `inspector_user_id` BIGINT DEFAULT NULL,
  `inspected_at` DATETIME DEFAULT NULL,
  `remark` VARCHAR(500) DEFAULT NULL,
  `created_by` BIGINT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_quality_no` (`quality_no`),
  KEY `idx_quality_outsource_id` (`outsource_id`),
  KEY `idx_quality_process` (`process_name`),
  KEY `idx_quality_inspect_type` (`inspect_type`),
  KEY `idx_quality_result` (`result`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='委外工序质检单（V1.3.7 Story 1.27 FR-6-7）';

-- 检验项目（FA/CMM · 必填）
CREATE TABLE IF NOT EXISTS `crm_outsource_quality_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `quality_id` BIGINT NOT NULL,
  `item_type` VARCHAR(16) NOT NULL DEFAULT 'FA' COMMENT 'FA/CMM',
  `item_name` VARCHAR(128) NOT NULL,
  `standard` VARCHAR(500) DEFAULT NULL,
  `measured_value` VARCHAR(128) DEFAULT NULL,
  `tolerance` VARCHAR(64) DEFAULT NULL COMMENT 'CMM 专用：±0.05 mm',
  `passed` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_quality_item_quality_id` (`quality_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='委外检验项目（FA/CMM）';

-- 不良项（严重度分级）
CREATE TABLE IF NOT EXISTS `crm_outsource_quality_defect` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `quality_id` BIGINT NOT NULL,
  `item_id` BIGINT DEFAULT NULL,
  `defect_type` VARCHAR(64) NOT NULL,
  `severity` VARCHAR(16) NOT NULL DEFAULT 'MINOR',
  `qty` INT NOT NULL DEFAULT 1,
  `description` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_quality_defect_quality_id` (`quality_id`),
  KEY `idx_quality_defect_severity` (`severity`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='委外不良项（严重度分级）';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- include: V25__quality_inspection.sql
-- V1.3.7 · Story 1.28 · 品质·来料/过程/成品检 (FR-7-1)
-- 迁移：crm_quality_inspection 3 检单 + crm_quality_inspection_item 检验项目 + crm_quality_sample AQL 抽样
-- 3 P1 修补：抽样规则 AQL / 检验项目必填 / 严重度 4 级（INFO/WARN/ERROR/CRITICAL）
-- 模板：QI{yyyyMMdd}{seq:4}

USE `cnc_business`;

-- 来料/过程/成品检单（IQC/IPQC/OQC）
CREATE TABLE IF NOT EXISTS `crm_quality_inspection` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `inspection_no` VARCHAR(32) NOT NULL COMMENT 'QI{yyyyMMdd}{seq:4}',
  `inspect_type` VARCHAR(16) NOT NULL COMMENT 'IQC（来料）/IPQC（过程）/OQC（成品）',
  `material_id` BIGINT DEFAULT NULL COMMENT '物料 ID（IQC 必填）',
  `material_code` VARCHAR(64) DEFAULT NULL,
  `material_name` VARCHAR(128) DEFAULT NULL,
  `work_order_id` BIGINT DEFAULT NULL COMMENT '工单 ID（IPQC 必填）',
  `work_order_no` VARCHAR(32) DEFAULT NULL,
  `process_name` VARCHAR(64) DEFAULT NULL COMMENT '工序名称（IPQC 必填）',
  `batch_no` VARCHAR(32) DEFAULT NULL,
  `lot_size` INT NOT NULL DEFAULT 0 COMMENT '批量',
  `sample_size` INT NOT NULL DEFAULT 0 COMMENT '抽样量',
  `sample_rule` VARCHAR(64) DEFAULT 'AQL-1.0' COMMENT '抽样规则 AQL',
  `aql_level` VARCHAR(16) DEFAULT '1.0' COMMENT 'AQL 等级 0.65/1.0/1.5/2.5/4.0',
  `inspect_qty` INT NOT NULL DEFAULT 0,
  `passed_qty` INT NOT NULL DEFAULT 0,
  `failed_qty` INT NOT NULL DEFAULT 0,
  `defect_rate` DECIMAL(5,2) DEFAULT NULL,
  `result` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PASSED/FAILED/CONDITIONAL',
  `inspector_user_id` BIGINT DEFAULT NULL,
  `inspected_at` DATETIME DEFAULT NULL,
  `max_severity` VARCHAR(16) DEFAULT NULL COMMENT '最高严重度 INFO/WARN/ERROR/CRITICAL',
  `trigger_rework` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '触发返修（IQC 不通过）',
  `trigger_stockin` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '触发入库（OQC 通过）',
  `remark` VARCHAR(500) DEFAULT NULL,
  `created_by` BIGINT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_inspection_no` (`inspection_no`),
  KEY `idx_inspection_type` (`inspect_type`),
  KEY `idx_inspection_material` (`material_id`),
  KEY `idx_inspection_workorder` (`work_order_id`),
  KEY `idx_inspection_result` (`result`),
  KEY `idx_inspection_severity` (`max_severity`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='来料/过程/成品检单（V1.3.7 Story 1.28 FR-7-1）';

-- 检验项目（必填）
CREATE TABLE IF NOT EXISTS `crm_quality_inspection_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `inspection_id` BIGINT NOT NULL,
  `item_name` VARCHAR(128) NOT NULL COMMENT '检验项目名称（必填）',
  `standard` VARCHAR(500) DEFAULT NULL COMMENT '判定标准',
  `measured_value` VARCHAR(128) DEFAULT NULL,
  `severity` VARCHAR(16) NOT NULL DEFAULT 'INFO' COMMENT 'INFO/WARN/ERROR/CRITICAL',
  `passed` TINYINT(1) NOT NULL DEFAULT 0,
  `defect_desc` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_inspection_item_inspection_id` (`inspection_id`),
  KEY `idx_inspection_item_severity` (`severity`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='来料/过程/成品检项目（V1.3.7 P1 修补 2 · 必填）';

-- AQL 抽样记录
CREATE TABLE IF NOT EXISTS `crm_quality_sample` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `inspection_id` BIGINT NOT NULL,
  `sample_no` VARCHAR(32) NOT NULL COMMENT '样本编号 S{seq:3}',
  `item_id` BIGINT DEFAULT NULL,
  `sample_qty` INT NOT NULL DEFAULT 1,
  `defect_qty` INT NOT NULL DEFAULT 0,
  `aql_passed` TINYINT(1) NOT NULL DEFAULT 0,
  `remark` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_sample_no` (`sample_no`),
  KEY `idx_sample_inspection_id` (`inspection_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AQL 抽样记录（V1.3.7 P1 修补 1）';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- include: V26__quality_fa.sql
-- V1.3.7 · Story 1.29 · 品质·FA 首件 (FR-7-2)
-- 迁移：crm_quality_fa FA 首件单 + crm_quality_fa_item FA 8 维度检验项目
-- 3 P1 修补：FA 必检（开工前）/ 检验项目 8 维度 / 不合格阻断生产
-- 模板：QF{yyyyMMdd}{seq:4}

USE `cnc_business`;

-- FA 首件单（开工前必检）
CREATE TABLE IF NOT EXISTS `crm_quality_fa` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `fa_no` VARCHAR(32) NOT NULL COMMENT 'QF{yyyyMMdd}{seq:4}',
  `work_order_id` BIGINT NOT NULL,
  `work_order_no` VARCHAR(32) NOT NULL,
  `process_id` BIGINT NOT NULL,
  `process_name` VARCHAR(64) NOT NULL,
  `operator_user_id` BIGINT DEFAULT NULL,
  `inspect_qty` INT NOT NULL DEFAULT 1 COMMENT '首件数量 默认 1',
  `result` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PASSED/FAILED',
  `locked` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '不合格锁定工序',
  `pdf_url` VARCHAR(255) DEFAULT NULL COMMENT '首件 PDF 报告',
  `inspector_user_id` BIGINT DEFAULT NULL,
  `inspected_at` DATETIME DEFAULT NULL,
  `remark` VARCHAR(500) DEFAULT NULL,
  `created_by` BIGINT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_fa_no` (`fa_no`),
  KEY `idx_fa_work_order_id` (`work_order_id`),
  KEY `idx_fa_process_id` (`process_id`),
  KEY `idx_fa_result` (`result`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='FA 首件单（V1.3.7 Story 1.29 FR-7-2 · 开工前必检）';

-- FA 8 维度检验项目
CREATE TABLE IF NOT EXISTS `crm_quality_fa_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `fa_id` BIGINT NOT NULL,
  `dimension` VARCHAR(32) NOT NULL COMMENT '尺寸/形位/粗糙度/硬度/材质/外观/装配/性能 8 维度',
  `item_name` VARCHAR(128) NOT NULL,
  `standard` VARCHAR(500) DEFAULT NULL,
  `measured_value` VARCHAR(128) DEFAULT NULL,
  `tolerance` VARCHAR(64) DEFAULT NULL,
  `passed` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_fa_item_fa_id` (`fa_id`),
  KEY `idx_fa_item_dimension` (`dimension`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='FA 8 维度检验项目（V1.3.7 P1 修补 2）';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- include: V27__quality_cmm.sql
-- V1.3.7 · Story 1.30 · 品质·CMM 三次元 (FR-7-3)
-- 迁移：crm_quality_cmm CMM 测量单 + crm_quality_cmm_point 测点数据
-- 3 P1 修补：CMM 测点 ≥ 3 / 偏差超差告警 / 报告 PDF 必存
-- 模板：QC{yyyyMMdd}{seq:4}

USE `cnc_business`;

-- CMM 三次元测量单
CREATE TABLE IF NOT EXISTS `crm_quality_cmm` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `cmm_no` VARCHAR(32) NOT NULL COMMENT 'QC{yyyyMMdd}{seq:4}',
  `work_order_id` BIGINT DEFAULT NULL,
  `work_order_no` VARCHAR(32) DEFAULT NULL,
  `drawing_no` VARCHAR(32) DEFAULT NULL,
  `part_name` VARCHAR(128) DEFAULT NULL,
  `point_count` INT NOT NULL DEFAULT 0 COMMENT '测点数量 · P1 修补 1：≥ 3',
  `cpk` DECIMAL(8,4) DEFAULT NULL COMMENT 'Cpk 过程能力指数',
  `pp` DECIMAL(8,4) DEFAULT NULL,
  `ppk` DECIMAL(8,4) DEFAULT NULL,
  `cp` DECIMAL(8,4) DEFAULT NULL,
  `max_deviation` DECIMAL(8,4) DEFAULT NULL COMMENT '最大偏差 mm',
  `deviation_alert` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '超差告警 · P1 修补 2',
  `result` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PASSED/FAILED',
  `pdf_url` VARCHAR(255) DEFAULT NULL COMMENT 'PDF 报告 · P1 修补 3：必存',
  `inspector_user_id` BIGINT DEFAULT NULL,
  `inspected_at` DATETIME DEFAULT NULL,
  `remark` VARCHAR(500) DEFAULT NULL,
  `created_by` BIGINT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_cmm_no` (`cmm_no`),
  KEY `idx_cmm_work_order_id` (`work_order_id`),
  KEY `idx_cmm_result` (`result`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CMM 三次元测量单（V1.3.7 Story 1.30 FR-7-3）';

-- CMM 测点
CREATE TABLE IF NOT EXISTS `crm_quality_cmm_point` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `cmm_id` BIGINT NOT NULL,
  `point_no` VARCHAR(16) NOT NULL COMMENT 'P1/P2/P3 ...',
  `axis` VARCHAR(8) NOT NULL DEFAULT 'X' COMMENT 'X/Y/Z',
  `nominal_value` DECIMAL(10,4) NOT NULL COMMENT '标称值',
  `measured_value` DECIMAL(10,4) NOT NULL COMMENT '实测值',
  `tolerance_upper` DECIMAL(10,4) DEFAULT NULL COMMENT '上偏差',
  `tolerance_lower` DECIMAL(10,4) DEFAULT NULL COMMENT '下偏差',
  `deviation` DECIMAL(10,4) DEFAULT NULL COMMENT '偏差 = 实测 - 标称',
  `passed` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_cmm_point_cmm_id` (`cmm_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CMM 测点（P1 修补 1 ≥ 3）';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- include: V28__quality_defect.sql
-- V1.3.7 · Story 1.31 · 品质·不良品处理 (FR-7-4)
-- 迁移：crm_quality_defect 不良品单 + crm_quality_defect_history 处理历史 + crm_quality_defect_action 处理动作
-- 3 P1 修补：3 动作（返工/报废/让步接收）/ 责任部门必填 / 成本非负
-- 模板：QD{yyyyMMdd}{seq:4}

USE `cnc_business`;

-- 不良品单（8D 报告）
CREATE TABLE IF NOT EXISTS `crm_quality_defect` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `defect_no` VARCHAR(32) NOT NULL COMMENT 'QD{yyyyMMdd}{seq:4}',
  `source_type` VARCHAR(16) NOT NULL COMMENT 'INTERNAL（厂内）/OUTSOURCE（委外）',
  `source_id` BIGINT DEFAULT NULL COMMENT '来源单 ID（IQC/IPQC/OQC/FA/CMM 单 ID）',
  `source_no` VARCHAR(32) DEFAULT NULL,
  `defect_type` VARCHAR(64) NOT NULL,
  `severity` VARCHAR(16) NOT NULL DEFAULT 'MAJOR' COMMENT 'MINOR/MAJOR/CRITICAL',
  `qty` INT NOT NULL DEFAULT 1,
  `material_id` BIGINT DEFAULT NULL,
  `material_code` VARCHAR(64) DEFAULT NULL,
  `work_order_id` BIGINT DEFAULT NULL,
  `work_order_no` VARCHAR(32) DEFAULT NULL,
  `d1_team` VARCHAR(255) DEFAULT NULL COMMENT '8D D1 团队组建',
  `d4_root_cause` TEXT DEFAULT NULL COMMENT '8D D4 根本原因',
  `d5_action` TEXT DEFAULT NULL COMMENT '8D D5 永久对策',
  `d8_closure` TEXT DEFAULT NULL COMMENT '8D D8 关闭',
  `defect_rate_ppm` DECIMAL(10,2) DEFAULT NULL COMMENT 'PPM 不良率',
  `total_qty` INT DEFAULT NULL COMMENT '总生产数量',
  `status` VARCHAR(20) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/IN_PROGRESS/RESOLVED/CLOSED',
  `result` VARCHAR(20) DEFAULT NULL COMMENT 'REWORK/SCRAP/CONCESSION/RETURN',
  `responsible_dept` VARCHAR(64) DEFAULT NULL COMMENT '责任部门 · P1 修补 2 必填',
  `cost_amount` DECIMAL(12,2) DEFAULT NULL COMMENT '成本 · P1 修补 3 非负',
  `created_by` BIGINT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_defect_no` (`defect_no`),
  KEY `idx_defect_source` (`source_type`, `source_id`),
  KEY `idx_defect_status` (`status`),
  KEY `idx_defect_result` (`result`),
  KEY `idx_defect_severity` (`severity`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='不良品单（V1.3.7 Story 1.31 FR-7-4 · 8D 报告）';

-- 不良处理历史（时序）
CREATE TABLE IF NOT EXISTS `crm_quality_defect_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `defect_id` BIGINT NOT NULL,
  `from_status` VARCHAR(20) DEFAULT NULL,
  `to_status` VARCHAR(20) NOT NULL,
  `operator_user_id` BIGINT DEFAULT NULL,
  `operator_name` VARCHAR(64) DEFAULT NULL,
  `comment` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_defect_history_defect_id` (`defect_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='不良处理历史';

-- 不良处理动作（3 选 1）
CREATE TABLE IF NOT EXISTS `crm_quality_defect_action` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `defect_id` BIGINT NOT NULL,
  `action_type` VARCHAR(16) NOT NULL COMMENT 'REWORK（返工）/SCRAP（报废）/CONCESSION（让步接收） · P1 修补 1',
  `qty` INT NOT NULL DEFAULT 1,
  `responsible_dept` VARCHAR(64) DEFAULT NULL COMMENT '责任部门 · P1 修补 2 必填',
  `cost_amount` DECIMAL(12,2) DEFAULT NULL COMMENT '成本 · P1 修补 3 非负',
  `executed_at` DATETIME DEFAULT NULL,
  `executor_user_id` BIGINT DEFAULT NULL,
  `remark` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_defect_action_defect_id` (`defect_id`),
  KEY `idx_defect_action_type` (`action_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='不良处理动作（3 选 1）';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- include: V29__rfq.sql
-- V1.3.7 · Story 1.32 · 采购·询比价 (FR-8-1)
-- 迁移：crm_rfq 询价单 + crm_rfq_vendor 询价厂商 + crm_rfq_quote 厂商报价
-- 3 P1 修补：询价单唯一 / 厂商报价必填 / 选最低不超预算 / 中标自动触发 PO
-- 模板：RF{yyyyMMdd}{seq:4}

USE `cnc_business`;

-- 询价单（RFQ）
CREATE TABLE IF NOT EXISTS `crm_rfq` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `rfq_no` VARCHAR(32) NOT NULL COMMENT 'RF{yyyyMMdd}{seq:4}',
  `title` VARCHAR(255) NOT NULL COMMENT '询价标题',
  `material_id` BIGINT NOT NULL,
  `material_code` VARCHAR(64) DEFAULT NULL,
  `material_name` VARCHAR(255) DEFAULT NULL,
  `qty` DECIMAL(12,2) NOT NULL COMMENT '需求数量',
  `unit` VARCHAR(16) DEFAULT NULL COMMENT '单位',
  `budget_amount` DECIMAL(14,2) NOT NULL COMMENT '预算金额 · P1 修补 3 校验',
  `required_date` DATE DEFAULT NULL COMMENT '需求到货日',
  `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/QUOTING/COMPARED/AWARDED/CLOSED',
  `awarded_vendor_id` BIGINT DEFAULT NULL,
  `awarded_vendor_name` VARCHAR(128) DEFAULT NULL,
  `awarded_quote_id` BIGINT DEFAULT NULL,
  `awarded_amount` DECIMAL(14,2) DEFAULT NULL,
  `purchase_order_id` BIGINT DEFAULT NULL COMMENT 'P1 修补 4 中标自动触发 PO',
  `purchase_order_no` VARCHAR(32) DEFAULT NULL,
  `winner_mode` VARCHAR(16) DEFAULT 'LOWEST' COMMENT 'LOWEST（最低价）/WEIGHTED（加权评分）',
  `created_by` BIGINT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_rfq_no` (`rfq_no`),
  KEY `idx_rfq_material` (`material_id`),
  KEY `idx_rfq_status` (`status`),
  KEY `idx_rfq_awarded_vendor` (`awarded_vendor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='询价单（V1.3.7 Story 1.32 FR-8-1）';

-- 询价-厂商（询价单关联的 3+ 候选厂商）
CREATE TABLE IF NOT EXISTS `crm_rfq_vendor` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `rfq_id` BIGINT NOT NULL,
  `vendor_id` BIGINT NOT NULL COMMENT '厂商 ID（来自系统厂商库）',
  `vendor_name` VARCHAR(128) NOT NULL,
  `vendor_code` VARCHAR(64) DEFAULT NULL,
  `contact_name` VARCHAR(64) DEFAULT NULL,
  `contact_phone` VARCHAR(32) DEFAULT NULL,
  `invited_at` DATETIME DEFAULT NULL,
  `quote_status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/QUOTED/NO_QUOTE/REJECTED',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_rfq_vendor` (`rfq_id`, `vendor_id`),
  KEY `idx_rfq_vendor_vendor` (`vendor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='询价-厂商关联（每 RFQ ≥ 3 厂商）';

-- 厂商报价
CREATE TABLE IF NOT EXISTS `crm_rfq_quote` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `rfq_id` BIGINT NOT NULL,
  `rfq_vendor_id` BIGINT NOT NULL,
  `vendor_id` BIGINT NOT NULL,
  `unit_price` DECIMAL(14,4) NOT NULL COMMENT '单价 · P1 修补 2 必填',
  `total_amount` DECIMAL(14,2) NOT NULL COMMENT '总报价',
  `lead_time_days` INT DEFAULT NULL COMMENT '交货周期（天）',
  `valid_until` DATE DEFAULT NULL COMMENT '报价有效期',
  `payment_terms` VARCHAR(255) DEFAULT NULL COMMENT '付款条件',
  `quality_score` DECIMAL(3,2) DEFAULT NULL COMMENT '质量评分 0-5（P1 修补 4 加权）',
  `delivery_score` DECIMAL(3,2) DEFAULT NULL COMMENT '交付评分 0-5',
  `service_score` DECIMAL(3,2) DEFAULT NULL COMMENT '服务评分 0-5',
  `weighted_score` DECIMAL(5,2) DEFAULT NULL COMMENT '加权总分（price 50% + quality 20% + delivery 20% + service 10%）',
  `is_awarded` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否中标',
  `remark` VARCHAR(500) DEFAULT NULL,
  `submitted_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `submitted_by` BIGINT DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_rfq_quote_vendor` (`rfq_id`, `vendor_id`),
  KEY `idx_rfq_quote_rfq` (`rfq_id`),
  KEY `idx_rfq_quote_awarded` (`is_awarded`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='厂商报价（每厂商 1 报价）';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- include: V30__price_control.sql
-- V1.3.7 · Story 1.33 · 采购·价格控制 (FR-8-2)
-- 迁移：crm_price_control 物料限价 + crm_price_history 历史价
-- 3 P1 修补：价格上限非负 / 偏差率 ≥ 20% ALERTED / 唯一索引 (material_id, vendor_id)
-- 模板：PL{yyyyMMdd}{seq:4}

USE `cnc_business`;

-- 物料采购限价（按物料 + 厂商维度）
CREATE TABLE IF NOT EXISTS `crm_price_control` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `control_no` VARCHAR(32) NOT NULL COMMENT 'PL{yyyyMMdd}{seq:4}',
  `material_id` BIGINT NOT NULL,
  `material_code` VARCHAR(64) DEFAULT NULL,
  `material_name` VARCHAR(255) DEFAULT NULL,
  `vendor_id` BIGINT DEFAULT NULL COMMENT 'NULL 表示通用限价；指定厂商表示该厂商专项限价',
  `vendor_name` VARCHAR(128) DEFAULT NULL,
  `price_limit` DECIMAL(14,4) NOT NULL COMMENT '采购价上限 · P1 修补 1 非负',
  `currency` VARCHAR(8) NOT NULL DEFAULT 'CNY',
  `effective_date` DATE NOT NULL COMMENT '生效日',
  `expiry_date` DATE DEFAULT NULL COMMENT '失效日',
  `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/EXPIRED/INACTIVE',
  `set_by` BIGINT NOT NULL,
  `set_by_name` VARCHAR(64) DEFAULT NULL,
  `remark` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_control_material_vendor` (`material_id`, `vendor_id`, `effective_date`),
  KEY `idx_control_material` (`material_id`),
  KEY `idx_control_status` (`status`),
  KEY `idx_control_effective` (`effective_date`, `expiry_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物料采购限价（V1.3.7 Story 1.33 FR-8-2）';

-- 历史价（过去 3 个月内的实际采购价）
CREATE TABLE IF NOT EXISTS `crm_price_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `material_id` BIGINT NOT NULL,
  `material_code` VARCHAR(64) DEFAULT NULL,
  `vendor_id` BIGINT NOT NULL,
  `vendor_name` VARCHAR(128) DEFAULT NULL,
  `unit_price` DECIMAL(14,4) NOT NULL,
  `qty` DECIMAL(12,2) DEFAULT NULL,
  `total_amount` DECIMAL(14,2) DEFAULT NULL,
  `source_type` VARCHAR(16) NOT NULL DEFAULT 'PO' COMMENT 'PO/RFQ_QUOTE/MANUAL',
  `source_no` VARCHAR(32) DEFAULT NULL,
  `purchased_at` DATE NOT NULL,
  `created_by` BIGINT DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_price_history_material` (`material_id`),
  KEY `idx_price_history_vendor` (`vendor_id`),
  KEY `idx_price_history_purchased_at` (`purchased_at`),
  KEY `idx_price_history_material_vendor` (`material_id`, `vendor_id`, `purchased_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购历史价（V1.3.7 Story 1.33 FR-8-2）';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- include: V31__incoming_alert.sql
-- V1.3.7 · Story 1.34 · 采购·到货提醒 (FR-8-3)
-- 迁移：crm_incoming_alert 到货提醒 + crm_incoming 实际到货
-- 3 P1 修补：预估到货日必填 / 提前 3 天 ALERT / 逾期 ALERT_CRITICAL / 唯一索引 (po_id, material_id)
-- 模板：IA{yyyyMMdd}{seq:4}

USE `cnc_business`;

-- 到货提醒（PO 创建时生成）
CREATE TABLE IF NOT EXISTS `crm_incoming_alert` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `alert_no` VARCHAR(32) NOT NULL COMMENT 'IA{yyyyMMdd}{seq:4}',
  `po_id` BIGINT NOT NULL,
  `po_no` VARCHAR(32) NOT NULL,
  `vendor_id` BIGINT DEFAULT NULL,
  `vendor_name` VARCHAR(128) DEFAULT NULL,
  `material_id` BIGINT NOT NULL,
  `material_code` VARCHAR(64) DEFAULT NULL,
  `material_name` VARCHAR(255) DEFAULT NULL,
  `qty` DECIMAL(12,2) NOT NULL,
  `unit` VARCHAR(16) DEFAULT NULL,
  `expected_date` DATE NOT NULL COMMENT '预估到货日 · P1 修补 1 必填',
  `alert_level` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/ALERT（提前 3 天）/ALERT_CRITICAL（逾期）/ARRIVED',
  `alert_message` VARCHAR(500) DEFAULT NULL,
  `arrived_qty` DECIMAL(12,2) DEFAULT NULL,
  `arrived_at` DATETIME DEFAULT NULL,
  `arrived_by` BIGINT DEFAULT NULL,
  `reminded_at` DATETIME DEFAULT NULL,
  `reminded_count` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_alert_po_material` (`po_id`, `material_id`),
  KEY `idx_alert_po` (`po_id`),
  KEY `idx_alert_level` (`alert_level`),
  KEY `idx_alert_expected_date` (`expected_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='到货提醒（V1.3.7 Story 1.34 FR-8-3）';

-- 实际到货（扫码入库记录）
CREATE TABLE IF NOT EXISTS `crm_incoming` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `incoming_no` VARCHAR(32) NOT NULL COMMENT 'IN{yyyyMMdd}{seq:4}',
  `alert_id` BIGINT NOT NULL,
  `po_id` BIGINT NOT NULL,
  `po_no` VARCHAR(32) NOT NULL,
  `vendor_id` BIGINT DEFAULT NULL,
  `vendor_name` VARCHAR(128) DEFAULT NULL,
  `material_id` BIGINT NOT NULL,
  `material_code` VARCHAR(64) DEFAULT NULL,
  `material_name` VARCHAR(255) DEFAULT NULL,
  `arrived_qty` DECIMAL(12,2) NOT NULL,
  `expected_qty` DECIMAL(12,2) NOT NULL,
  `unit` VARCHAR(16) DEFAULT NULL,
  `arrived_at` DATETIME NOT NULL,
  `arrived_by` BIGINT NOT NULL,
  `quality_status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PASS/FAIL',
  `scan_batch_no` VARCHAR(32) DEFAULT NULL COMMENT '关联 1.12 扫码批次号',
  `remark` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_incoming_alert` (`alert_id`),
  KEY `idx_incoming_po` (`po_id`),
  KEY `idx_incoming_material` (`material_id`),
  KEY `idx_incoming_arrived_at` (`arrived_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实际到货（V1.3.7 Story 1.34 FR-8-3）';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- include: V32__purchase_incoming_inspection.sql
-- V1.3.7 · Story 1.35 · 采购·来料质检 (FR-8-4)
-- 迁移：crm_purchase_incoming_inspection 来料质检单 + crm_purchase_incoming_item 检验项
-- 3 P1 修补：单一 163 邮箱（AD-3）/ 抽样 AQL / 不良率 > 10% 阻断入库 / 跨 1.32 PO 关联
-- 模板：PI{yyyyMMdd}{seq:4}

USE `cnc_business`;

-- 采购来料质检单
CREATE TABLE IF NOT EXISTS `crm_purchase_incoming_inspection` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `inspection_no` VARCHAR(32) NOT NULL COMMENT 'PI{yyyyMMdd}{seq:4}',
  `incoming_id` BIGINT DEFAULT NULL COMMENT '关联 1.34 实际到货',
  `po_id` BIGINT NOT NULL,
  `po_no` VARCHAR(32) NOT NULL,
  `vendor_id` BIGINT DEFAULT NULL,
  `vendor_name` VARCHAR(128) DEFAULT NULL,
  `material_id` BIGINT NOT NULL,
  `material_code` VARCHAR(64) DEFAULT NULL,
  `material_name` VARCHAR(255) DEFAULT NULL,
  `batch_no` VARCHAR(32) DEFAULT NULL COMMENT '关联 1.12 扫码批次',
  `inspector_id` BIGINT NOT NULL COMMENT '质检员 user_id',
  `inspector_name` VARCHAR(64) DEFAULT NULL,
  `sample_size` INT NOT NULL DEFAULT 0 COMMENT '抽样数',
  `sample_pass` INT NOT NULL DEFAULT 0 COMMENT '抽样合格数',
  `sample_fail` INT NOT NULL DEFAULT 0 COMMENT '抽样不合格数',
  `defect_rate` DECIMAL(5,2) DEFAULT NULL COMMENT '不良率 % · P1 修补 3 > 10% 阻断',
  `aql_level` VARCHAR(16) NOT NULL DEFAULT 'II' COMMENT 'AQL 等级 · P1 修补 2 I/II/III',
  `result` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PASS/REJECT',
  `notify_email` VARCHAR(128) NOT NULL DEFAULT 'inspect@btsheng-163.com' COMMENT 'P1 修补 1 单一 163 邮箱 AD-3',
  `inspected_at` DATETIME DEFAULT NULL,
  `remark` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_inspection_po_material` (`po_id`, `material_id`),
  KEY `idx_inspection_po` (`po_id`),
  KEY `idx_inspection_result` (`result`),
  KEY `idx_inspection_material` (`material_id`),
  KEY `idx_inspection_inspected_at` (`inspected_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购来料质检单（V1.3.7 Story 1.35 FR-8-4）';

-- 采购来料质检检验项
CREATE TABLE IF NOT EXISTS `crm_purchase_incoming_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `inspection_id` BIGINT NOT NULL,
  `seq_no` INT NOT NULL DEFAULT 1,
  `check_item` VARCHAR(128) NOT NULL COMMENT '检验项目（外观/尺寸/材质/性能等）',
  `standard` VARCHAR(500) DEFAULT NULL COMMENT '判定标准',
  `sample_qty` INT NOT NULL DEFAULT 1,
  `pass_qty` INT NOT NULL DEFAULT 0,
  `fail_qty` INT NOT NULL DEFAULT 0,
  `is_critical` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '关键项 1 票否决',
  `result` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PASS/FAIL/PENDING',
  `remark` VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_item_inspection` (`inspection_id`),
  KEY `idx_item_result` (`result`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购来料质检检验项（V1.3.7 Story 1.35 FR-8-4）';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- include: V33__receivable_payable.sql
-- V1.3.7 · Story 1.36 · 财务·应收应付 (FR-9-1)
-- 迁移：crm_receivable 应收 + crm_payable 应付 + crm_payment 收付款记录
-- 4 P1 修补：应收/应付金额非负 / 收付款金额 ≤ 未收/未付金额 / 账龄 4 段（30/60/90/90+）/ 跨订单/PO 关联
-- 模板：RV{yyyyMMdd}{seq:4}（应收）/ PV{yyyyMMdd}{seq:4}（应付）/ PM{yyyyMMdd}{seq:4}（收付款记录）

USE `cnc_business`;

-- 应收账款（客户欠款）
CREATE TABLE IF NOT EXISTS `crm_receivable` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `receivable_no` VARCHAR(32) NOT NULL COMMENT 'RV{yyyyMMdd}{seq:4}',
  `customer_id` BIGINT NOT NULL,
  `customer_name` VARCHAR(128) DEFAULT NULL,
  `order_id` BIGINT NOT NULL COMMENT '关联 1.6 销售订单',
  `order_no` VARCHAR(32) NOT NULL,
  `total_amount` DECIMAL(14,2) NOT NULL COMMENT '订单金额 · P1 修补 1 非负',
  `paid_amount` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '已收金额',
  `unpaid_amount` DECIMAL(14,2) NOT NULL COMMENT '未收金额',
  `due_date` DATE NOT NULL COMMENT '到期日',
  `aging_days` INT NOT NULL DEFAULT 0 COMMENT '账龄天数',
  `aging_bucket` VARCHAR(16) NOT NULL DEFAULT 'CURRENT' COMMENT '账龄段 CURRENT(0-30)/D30(30-60)/D60(60-90)/D90(90+) · P1 修补 3',
  `status` VARCHAR(16) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/PARTIAL/CLOSED/OVERDUE',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_receivable_order` (`order_id`),
  KEY `idx_receivable_customer` (`customer_id`),
  KEY `idx_receivable_status` (`status`),
  KEY `idx_receivable_due_date` (`due_date`),
  KEY `idx_receivable_aging_bucket` (`aging_bucket`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应收账款（V1.3.7 Story 1.36 FR-9-1）';

-- 应付账款（欠供应商）
CREATE TABLE IF NOT EXISTS `crm_payable` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `payable_no` VARCHAR(32) NOT NULL COMMENT 'PV{yyyyMMdd}{seq:4}',
  `vendor_id` BIGINT NOT NULL,
  `vendor_name` VARCHAR(128) DEFAULT NULL,
  `po_id` BIGINT NOT NULL COMMENT '关联 1.32 采购订单',
  `po_no` VARCHAR(32) NOT NULL,
  `total_amount` DECIMAL(14,2) NOT NULL COMMENT 'PO 金额 · P1 修补 1 非负',
  `paid_amount` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '已付金额',
  `unpaid_amount` DECIMAL(14,2) NOT NULL COMMENT '未付金额',
  `due_date` DATE NOT NULL COMMENT '到期日',
  `aging_days` INT NOT NULL DEFAULT 0,
  `aging_bucket` VARCHAR(16) NOT NULL DEFAULT 'CURRENT' COMMENT '账龄段',
  `status` VARCHAR(16) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/PARTIAL/CLOSED/OVERDUE',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_payable_po` (`po_id`),
  KEY `idx_payable_vendor` (`vendor_id`),
  KEY `idx_payable_status` (`status`),
  KEY `idx_payable_due_date` (`due_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应付账款（V1.3.7 Story 1.36 FR-9-1）';

-- 收付款记录
CREATE TABLE IF NOT EXISTS `crm_payment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `payment_no` VARCHAR(32) NOT NULL COMMENT 'PM{yyyyMMdd}{seq:4}',
  `type` VARCHAR(16) NOT NULL COMMENT 'RECEIPT(收客户)/PAYMENT(付供应商)',
  `ref_id` BIGINT NOT NULL COMMENT 'crm_receivable.id 或 crm_payable.id',
  `ref_no` VARCHAR(32) NOT NULL COMMENT 'RV/PV 单号',
  `amount` DECIMAL(14,2) NOT NULL COMMENT '本次收/付金额 · P1 修补 2 ≤ 未收/未付',
  `method` VARCHAR(16) NOT NULL DEFAULT 'BANK' COMMENT 'BANK/CASH/CHECK/WECHAT/ALIPAY',
  `paid_by` BIGINT NOT NULL,
  `paid_at` DATETIME NOT NULL,
  `remark` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_payment_no` (`payment_no`),
  KEY `idx_payment_type` (`type`),
  KEY `idx_payment_ref` (`ref_id`),
  KEY `idx_payment_paid_at` (`paid_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收付款记录（V1.3.7 Story 1.36 FR-9-1）';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- include: V34__cost_accounting.sql
-- V1.3.7 · Story 1.37 · 财务·成本核算 (FR-9-2)
-- 迁移：crm_cost_accounting 成本核算 + crm_cost_segment 5 段成本
-- 3 P1 修补：5 段自动归集 / 成本非负 / 偏差率统计
-- 模板：CA{yyyyMMdd}{seq:4}

USE `cnc_business`;

-- 成本核算单（按订单/工单/委外单聚合）
CREATE TABLE IF NOT EXISTS `crm_cost_accounting` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `cost_no` VARCHAR(32) NOT NULL COMMENT 'CA{yyyyMMdd}{seq:4}',
  `ref_type` VARCHAR(16) NOT NULL COMMENT 'ORDER(1.6) / WORKORDER(1.15) / OUTSOURCE(1.18)',
  `ref_id` BIGINT NOT NULL,
  `ref_no` VARCHAR(32) NOT NULL,
  `material_id` BIGINT DEFAULT NULL,
  `material_code` VARCHAR(64) DEFAULT NULL,
  `material_name` VARCHAR(255) DEFAULT NULL,
  `qty` DECIMAL(12,2) NOT NULL DEFAULT 1,
  `unit_cost` DECIMAL(14,4) NOT NULL DEFAULT 0 COMMENT '单位成本',
  `total_cost` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '总成本 = sum(5 段)',
  `standard_cost` DECIMAL(14,2) DEFAULT NULL COMMENT '标准成本（参考）',
  `variance` DECIMAL(14,2) DEFAULT NULL COMMENT '偏差 = total - standard',
  `variance_rate` DECIMAL(8,4) DEFAULT NULL COMMENT '偏差率 %',
  `status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/CONFIRMED',
  `cost_date` DATE NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_cost_ref` (`ref_type`, `ref_id`),
  KEY `idx_cost_date` (`cost_date`),
  KEY `idx_cost_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成本核算单（V1.3.7 Story 1.37 FR-9-2）';

-- 5 段成本（材料/加工/委外/管理/折旧）
CREATE TABLE IF NOT EXISTS `crm_cost_segment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `cost_id` BIGINT NOT NULL,
  `segment_code` VARCHAR(16) NOT NULL COMMENT 'MATERIAL/PROCESS/OUTSOURCE/MANAGE/DEPRECIATION',
  `segment_name` VARCHAR(64) NOT NULL COMMENT '材料/加工/委外/管理/折旧',
  `amount` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '本段成本 · P1 修补 2 非负',
  `source` VARCHAR(32) DEFAULT NULL COMMENT '来源 1.9/1.10/1.17/1.26',
  `remark` VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_cost_segment` (`cost_id`, `segment_code`),
  KEY `idx_segment_code` (`segment_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='5 段成本明细（V1.3.7 Story 1.37 FR-9-2）';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- include: V35__payment_collection.sql
-- V1.3.7 · Story 1.38 · 财务·回款控制 (FR-9-3)
-- 迁移：crm_payment_plan 回款计划 + crm_payment_alert 逾期告警
-- 3 P1 修补：回款金额 ≤ 订单金额 / 提前 3 天 ALERT / 逾期 ALERT_CRITICAL / 跨 1.36 应收
-- 模板：PP{yyyyMMdd}{seq:4}

USE `cnc_business`;

-- 回款计划（订单 SETTLED 触发）
CREATE TABLE IF NOT EXISTS `crm_payment_plan` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `plan_no` VARCHAR(32) NOT NULL COMMENT 'PP{yyyyMMdd}{seq:4}',
  `customer_id` BIGINT NOT NULL,
  `customer_name` VARCHAR(128) DEFAULT NULL,
  `order_id` BIGINT NOT NULL COMMENT '关联 1.6 订单',
  `order_no` VARCHAR(32) NOT NULL,
  `receivable_id` BIGINT DEFAULT NULL COMMENT '关联 1.36 应收',
  `receivable_no` VARCHAR(32) DEFAULT NULL,
  `total_amount` DECIMAL(14,2) NOT NULL COMMENT '订单金额',
  `planned_amount` DECIMAL(14,2) NOT NULL COMMENT '计划回款金额 · P1 修补 1 ≤ 订单金额',
  `paid_amount` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '已回款',
  `planned_date` DATE NOT NULL COMMENT '计划回款日',
  `alert_level` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/ALERT（提前 3 天）/ALERT_CRITICAL（逾期）/PAID',
  `paid_at` DATETIME DEFAULT NULL,
  `paid_by` BIGINT DEFAULT NULL,
  `remark` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_plan_order` (`order_id`),
  KEY `idx_plan_customer` (`customer_id`),
  KEY `idx_plan_level` (`alert_level`),
  KEY `idx_plan_planned_date` (`planned_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回款计划（V1.3.7 Story 1.38 FR-9-3）';

-- 逾期告警
CREATE TABLE IF NOT EXISTS `crm_payment_alert` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `plan_id` BIGINT NOT NULL,
  `alert_level` VARCHAR(20) NOT NULL COMMENT 'ALERT（提前 3 天）/ALERT_CRITICAL（逾期）',
  `alert_message` VARCHAR(500) DEFAULT NULL,
  `days_to_due` INT DEFAULT NULL COMMENT '距离到期天数（负数=逾期）',
  `notified_at` DATETIME DEFAULT NULL,
  `notified_channel` VARCHAR(16) DEFAULT NULL COMMENT 'EMAIL/SMS/INAPP',
  `acknowledged` TINYINT(1) NOT NULL DEFAULT 0,
  `acknowledged_by` BIGINT DEFAULT NULL,
  `acknowledged_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_alert_plan` (`plan_id`),
  KEY `idx_alert_level` (`alert_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回款告警（V1.3.7 Story 1.38 FR-9-3）';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- include: V36__profit_analysis.sql
-- V1.3.7 · Story 1.39 · 财务·利润分析 (FR-9-4)
-- 迁移：crm_profit_analysis 利润分析单
-- 4 P1 修补：利润 = 收入 - 5 段成本 / 利润率 -100% ~ +∞ / 跨订单+成本 跨模块 / PDF 1h 缓存
-- 模板：PA{yyyyMMdd}{seq:4}

USE `cnc_business`;

-- 利润分析单（按 SETTLED 订单聚合）
CREATE TABLE IF NOT EXISTS `crm_profit_analysis` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `profit_no` VARCHAR(32) NOT NULL COMMENT 'PA{yyyyMMdd}{seq:4}',
  `order_id` BIGINT NOT NULL COMMENT '关联 1.6 SETTLED 订单',
  `order_no` VARCHAR(32) NOT NULL,
  `customer_id` BIGINT NOT NULL,
  `customer_name` VARCHAR(128) NOT NULL,
  `product_id` BIGINT DEFAULT NULL,
  `product_code` VARCHAR(64) DEFAULT NULL,
  `product_name` VARCHAR(255) DEFAULT NULL,
  `revenue` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '订单收入（不含税）',
  `cost_id` BIGINT DEFAULT NULL COMMENT '关联 1.37 5 段成本核算单',
  `cost_no` VARCHAR(32) DEFAULT NULL,
  `total_cost` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '5 段总成本',
  `profit` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT 'P1 修补 1：利润 = 收入 - 5 段成本',
  `profit_rate` DECIMAL(8,4) NOT NULL DEFAULT 0 COMMENT 'P1 修补 2：利润率 -100% ~ +∞',
  `alert_level` VARCHAR(16) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/WARNING/CRITICAL',
  `settled_date` DATE NOT NULL COMMENT '订单 SETTLED 日期',
  `analysis_month` VARCHAR(7) NOT NULL COMMENT 'yyyy-MM 月份',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_profit_order` (`order_id`),
  KEY `idx_profit_customer` (`customer_id`),
  KEY `idx_profit_month` (`analysis_month`),
  KEY `idx_profit_alert` (`alert_level`),
  KEY `idx_profit_settled` (`settled_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='利润分析单（V1.3.7 Story 1.39 FR-9-4）';

-- V94 · mock 清理：利润分析 seed 已移至 init_data.sql（E11-S3 客户利润汇总）
-- include: V37__material_cost_aggregation.sql
-- V1.3.7 · Story 1.40 · 财务·料号成本聚合视图 (FR-9-5 V1.3.4 新增 · P0)
-- 迁移：crm_material_cost_aggregation 物料 × 5 段成本自动聚合视图
-- 4 P1 修补：5 段严格 V1.3.4 标准 / 物料编码唯一 / 趋势 12 月 / 厂商对比
-- 模板：MC{yyyyMMdd}{seq:4}

USE `cnc_business`;

-- 物料成本聚合视图（按物料编码 × 月份维度聚合 5 段成本）
CREATE TABLE IF NOT EXISTS `crm_material_cost_aggregation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `agg_no` VARCHAR(32) NOT NULL COMMENT 'MC{yyyyMMdd}{seq:4}',
  `material_id` BIGINT NOT NULL,
  `material_code` VARCHAR(64) NOT NULL COMMENT 'P1 修补 2：物料编码唯一',
  `material_name` VARCHAR(255) NOT NULL,
  `agg_month` VARCHAR(7) NOT NULL COMMENT 'yyyy-MM',
  `vendor_id` BIGINT DEFAULT NULL,
  `vendor_name` VARCHAR(128) DEFAULT NULL,
  `qty` DECIMAL(14,2) NOT NULL DEFAULT 0,
  `material_amount` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT 'P1 修补 1：5 段严格 V1.3.4 标准 · 原材料',
  `process_amount` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '加工',
  `outsource_amount` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '委外',
  `manage_amount` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '管理',
  `depreciation_amount` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '折旧',
  `total_cost` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '5 段总成本',
  `unit_cost` DECIMAL(14,4) NOT NULL DEFAULT 0,
  `cost_sources` VARCHAR(255) DEFAULT NULL COMMENT '来源 BOM 1.9 + 工艺 1.10 + 工单 1.15 + 委外 1.18/1.26 + 库存 1.14',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_material_month_vendor` (`material_code`, `agg_month`, `vendor_id`),
  KEY `idx_material_code` (`material_code`),
  KEY `idx_agg_month` (`agg_month`),
  KEY `idx_vendor` (`vendor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='料号成本聚合视图（V1.3.7 Story 1.40 FR-9-5 V1.3.4 强化）';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- include: V38__hr_employee.sql
-- V1.3.7 · Story 1.41 · 员工档案与考勤
-- 2 表：crm_hr_employee + crm_hr_attendance

CREATE TABLE IF NOT EXISTS crm_hr_employee (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_no VARCHAR(64) NOT NULL,
    user_id BIGINT NULL,
    name VARCHAR(64) NOT NULL,
    department VARCHAR(64) NULL,
    position VARCHAR(64) NULL,
    phone VARCHAR(32) NULL,
    email VARCHAR(128) NULL,
    hire_date DATE NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    on_leave TINYINT NOT NULL DEFAULT 0,
    base_salary DECIMAL(12,2) NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_employee_no (employee_no),
    KEY idx_employee_dept (department),
    KEY idx_employee_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工档案';

CREATE TABLE IF NOT EXISTS crm_hr_attendance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    employee_no VARCHAR(64) NOT NULL,
    clock_type VARCHAR(16) NOT NULL COMMENT 'IN/OUT/LUNCH_IN/LUNCH_OUT',
    clock_at DATETIME NOT NULL,
    is_on_leave TINYINT NOT NULL DEFAULT 0,
    effective TINYINT NOT NULL DEFAULT 1 COMMENT '1=有效 / 0=请假中无效',
    remark VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_att_emp (employee_id),
    KEY idx_att_time (clock_at),
    KEY idx_att_emp_type_time (employee_id, clock_type, clock_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤记录';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- include: V50__material_barcode_batch.sql
-- ============================================================
-- V1.3.8 Story 3.2 · V50__material_barcode_batch.sql
-- 物料码批次生成：crm_material_barcode_batch 表
-- 关联：Story 3.2-物料码批次生成.md + architect review 3.2-impl-review.md
-- 复合物料码格式：WL-{material_no}-BATCH-{YYYYMMDD}-{seq:4}
-- ============================================================

CREATE TABLE IF NOT EXISTS crm_material_barcode_batch (
    id BIGINT NOT NULL AUTO_INCREMENT,
    material_id BIGINT NOT NULL,
    batch_id BIGINT NOT NULL,
    barcode_no VARCHAR(50) NOT NULL COMMENT 'WL-{material_no}-BATCH-{YYYYMMDD}-{seq}',
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uniq_barcode_no (barcode_no),
    KEY idx_material (material_id),
    KEY idx_batch (batch_id),
    KEY idx_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V1.3.8 物料码批次映射表';

-- 注：architect review §3.3 建议到货时同步将老 WL-XXXX 标记 is_active=0
--     此逻辑由应用层 MaterialBarcodeService 实现，不在迁移脚本中
-- include: V51__purchase_reason.sql
-- ============================================================
-- V1.3.8 Story 4.1 · V51__purchase_reason.sql
-- 无订单采购模式：sys_dict PURCHASE_REASON（PO 字段已由 V49 建表包含）
-- ============================================================

USE `cnc_platform`;

INSERT IGNORE INTO sys_dict (dict_type, dict_code, dict_label, sort, status) VALUES
  ('PURCHASE_REASON', 'URGENT_REPLENISH', '紧急补料', 1, 'ACTIVE'),
  ('PURCHASE_REASON', 'CUSTOMER_ADD',     '客户加单', 2, 'ACTIVE'),
  ('PURCHASE_REASON', 'STOCK_SWAP',       '库存置换', 3, 'ACTIVE'),
  ('PURCHASE_REASON', 'OTHER',            '其他',     4, 'ACTIVE');

USE `cnc_business`;

-- V49 已含 source_type / purchase_reason · 此处仅幂等补列（旧库升级路径）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_purchase_order' AND COLUMN_NAME = 'source_type');
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE crm_purchase_order ADD COLUMN source_type VARCHAR(20) NOT NULL DEFAULT ''FROM_ORDER'' COMMENT ''FROM_ORDER/FROM_MRP/NO_ORDER'', ADD COLUMN purchase_reason VARCHAR(30) NULL COMMENT ''采购理由'', ADD KEY idx_source_type (source_type), ADD KEY idx_purchase_reason (purchase_reason)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- include: V52__procurement_manager_role.sql
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
-- include: V53__workflow_event.sql
-- ============================================================
-- V1.3.8 Sprint 8 Story 8.3 · V53__workflow_event.sql
-- sys_workflow_event 表实装（V1.3.7 规划但未实装的预留表名）
--
-- 关联 Story：8.3-sys_workflow_event
-- 8.3.1 写事件：approval complete / approve / reject 时 INSERT
-- 8.3.2 GmSummaryService 改用真实 JOIN 统计 PROCUREMENT_MANAGER 工作量
-- ============================================================

USE `cnc_business`;

CREATE TABLE IF NOT EXISTS sys_workflow_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_no VARCHAR(40) NOT NULL COMMENT 'EV-{yyyyMMddHHmmss}-{seq}',
    workflow_code VARCHAR(50) NOT NULL COMMENT 'PO_APPROVAL / QUOTE_APPROVAL',
    biz_id BIGINT NOT NULL COMMENT '业务实体 ID（crm_purchase_order.id 等）',
    biz_no VARCHAR(40) DEFAULT NULL COMMENT '业务单号',
    event_type VARCHAR(20) NOT NULL COMMENT 'CREATED / APPROVED / REJECTED / DELEGATED',
    approver_role VARCHAR(50) NOT NULL COMMENT 'PROCUREMENT_MANAGER / DEPT_MANAGER / GM',
    approver_user_id BIGINT DEFAULT NULL,
    approver_user_name VARCHAR(64) DEFAULT NULL,
    comment VARCHAR(500) DEFAULT NULL,
    matched_node_index INT DEFAULT NULL COMMENT '命中 sys_workflow_node.node_index',
    matched_threshold VARCHAR(50) DEFAULT NULL COMMENT 'AMOUNT_10K_50K / CATEGORY_TOOL 等',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uniq_event_no (event_no),
    KEY idx_workflow_code (workflow_code),
    KEY idx_biz (biz_id),
    KEY idx_event_type (event_type),
    KEY idx_approver_role (approver_role),
    KEY idx_created_at (created_at),
    KEY idx_workflow_approver (workflow_code, approver_role, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V1.3.8 审批事件表';
-- include: V54__crm_drawing_link.sql
-- ============================================================
-- V1.3.9 Sprint 12 Story 12.1 · V54__crm_drawing_link.sql
-- 图纸权限矩阵 + 图纸-业务单据关联表 + 灰度 feature flag
--
-- 关联 Story：12.1-drawing-permission-matrix
-- 架构评审：docs/architecture/story-reviews/12.1-review.md（APPROVED · 9.1/10）
-- QA 测例：docs/qa/reviews/12.1-drawing-permission-matrix.md（24 测例）
-- 与 12.4 共 sys_print_log · parallel_group A
--
-- 设计要点：
--   1. 两套 ACL 完全独立命名空间（draw:* vs sys_global_threshold）
--   2. 7 角色 × 5 操作矩阵 · SpEL @drawingAuthz.canView 集中组件
--   3. crm_drawing_link 五元组（drawing_id + biz_type + biz_id）· ON DELETE RESTRICT
--   4. 灰度 feature flag 默认全 false · 灰度期间 admin 手动开
--   5. 错误码统一 40304（覆盖 FINANCE/SALES/OPERATOR 拒绝场景）
-- ============================================================

USE `cnc_business`;

-- ------------------------------------------------------------
-- 1. crm_drawing_link 关联表（V1.3.9 12.1 新增）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS crm_drawing_link (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    drawing_id BIGINT NOT NULL COMMENT '图纸 ID（crm_drawing.id）',
    biz_type VARCHAR(20) NOT NULL COMMENT '业务类型 ORDER/PO/INCOMING/INSPECTION/WORKORDER_PROCESS',
    biz_id BIGINT NOT NULL COMMENT '业务单据 ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人（admin/system）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_biz_ref (biz_type, biz_id, drawing_id) COMMENT '业务单据 + 图纸 唯一',
    KEY idx_drawing_link (drawing_id, biz_type, biz_id) COMMENT '图纸 → 业务单据 反向查询',
    KEY idx_biz_lookup (biz_type, biz_id) COMMENT '业务单据 → 图纸 正向查询',
    CONSTRAINT fk_draw_link_drawing FOREIGN KEY (drawing_id) REFERENCES crm_drawing(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V1.3.9 12.1 图纸与业务单据关联表';

-- ------------------------------------------------------------
-- 2. 备份表（data migration 前自动建立）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS crm_drawing_link_backup (
    id BIGINT NOT NULL,
    drawing_id BIGINT NOT NULL,
    biz_type VARCHAR(20) NOT NULL,
    biz_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    created_by BIGINT NOT NULL,
    backup_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '备份时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_backup_biz_ref (biz_type, biz_id, drawing_id),
    KEY idx_backup_drawing (drawing_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V1.3.9 12.1 图纸关联备份表（V54 末尾填充 · 用于回滚）';

-- ------------------------------------------------------------
-- 3. sys_dict DRAWING_SCOPE（7 角色 scope 配置）
-- ------------------------------------------------------------
USE `cnc_platform`;

INSERT IGNORE INTO sys_dict (dict_type, dict_code, dict_label, sort, status) VALUES
  ('DRAWING_SCOPE', 'ENGINEER',     'ALL',               1, 'ACTIVE'),
  ('DRAWING_SCOPE', 'PROD_PLANNER', 'ALL',               2, 'ACTIVE'),
  ('DRAWING_SCOPE', 'SALES',        'ORDER',             3, 'ACTIVE'),
  ('DRAWING_SCOPE', 'PURCHASER',    'PO',                4, 'ACTIVE'),
  ('DRAWING_SCOPE', 'WAREHOUSE',    'INCOMING',          5, 'ACTIVE'),
  ('DRAWING_SCOPE', 'QC',           'INSPECTION',        6, 'ACTIVE'),
  ('DRAWING_SCOPE', 'OPERATOR',     'WORKORDER_PROCESS', 7, 'ACTIVE'),
  ('DRAWING_SCOPE', 'FINANCE',      'NONE',              8, 'ACTIVE');

-- ------------------------------------------------------------
-- 4. sys_dict DRAWING_ACL_FEATURE_FLAG（7 角色灰度开关 · 默认全 false）
-- ------------------------------------------------------------
INSERT IGNORE INTO sys_dict (dict_type, dict_code, dict_label, sort, status) VALUES
  ('DRAWING_ACL_FEATURE_FLAG', 'draw.acl.gray.ENGINEER',     'false', 1, 'ACTIVE'),
  ('DRAWING_ACL_FEATURE_FLAG', 'draw.acl.gray.PROD_PLANNER', 'false', 2, 'ACTIVE'),
  ('DRAWING_ACL_FEATURE_FLAG', 'draw.acl.gray.SALES',        'false', 3, 'ACTIVE'),
  ('DRAWING_ACL_FEATURE_FLAG', 'draw.acl.gray.PURCHASER',    'false', 4, 'ACTIVE'),
  ('DRAWING_ACL_FEATURE_FLAG', 'draw.acl.gray.WAREHOUSE',    'false', 5, 'ACTIVE'),
  ('DRAWING_ACL_FEATURE_FLAG', 'draw.acl.gray.QC',           'false', 6, 'ACTIVE'),
  ('DRAWING_ACL_FEATURE_FLAG', 'draw.acl.gray.OPERATOR',     'false', 7, 'ACTIVE');

-- V94 · mock 清理：图纸关联回填/备份见 init_data.sql 或 data/V54__migrate_drawing_link.sql
-- include: V55__sys_printer.sql
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
-- include: V56__label_template.sql
-- ============================================================
-- V1.3.9 Sprint 12 Story 12.3 · V56__label_template.sql
-- 标签模板 4 种 (GD-/LZ-/WW-/WL-) · SB- 由代码层 fallback 到 GD 模板 + 改色条
-- 关联：12.3-label-template.md + architect review 12.3-review.md
--
-- 字段数：10（PK + 业务 7 + tenant 1 + 审计 2）
-- 索引：3（PRIMARY KEY + UNIQUE KEY uk_label_type + KEY idx_tenant）
-- CHECK 约束：2（chk_label_type / chk_label_dpi）
-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- ============================================================

CREATE TABLE IF NOT EXISTS label_template (
    id            BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `type`        VARCHAR(10) NOT NULL COMMENT '模板类型：GD / LZ / WW / WL（SB 由代码层 fallback 到 GD）',
    color_strip   VARCHAR(7) NOT NULL COMMENT '色条 HEX · #1E40AF / #16A34A / #EA580C / #000000',
    factory_name  VARCHAR(20) NOT NULL DEFAULT '昆山佰泰胜精密加工' COMMENT '厂名 · 默认昆山佰泰胜精密加工 · 来自 sys_dict dict_type=COMPANY_NAME',
    layout_json   VARCHAR(500) NOT NULL COMMENT '三区坐标 + 字体 + DPI · JSON 字符串 · {"topBarH":5,"qrAreaH":18,"textAreaH":7,"fontSize":8,"qrSizePx":300}',
    dpi           SMALLINT NOT NULL DEFAULT 300 COMMENT 'DPI · 203 / 300',
    enabled       TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启停：1=启用 / 0=停用',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户 ID · sys_tenant.id',
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_label_type (`type`, tenant_id),
    KEY idx_tenant (tenant_id),
    CONSTRAINT chk_label_type CHECK (`type` IN ('GD','LZ','WW','WL')),
    CONSTRAINT chk_label_dpi  CHECK (dpi IN (203, 300))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V1.3.9 标签模板 · 4 行 seed · SB 由代码层 fallback 到 GD';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- include: V57__sys_print_log.sql
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
-- include: V62__prod_machine_and_workorder_process.sql
-- ============================================================
-- V62 · E5-S5 设备机台 prod_machine + E5-S6 工单工序 crm_workorder_process
-- erp-production · cnc_production（V60 同步）
-- ============================================================

USE `cnc_business`;

-- 1) 设备台账
CREATE TABLE IF NOT EXISTS prod_machine (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    machine_code VARCHAR(32) NOT NULL COMMENT '设备码 SB-{type}-{seq}',
    machine_name VARCHAR(128) NOT NULL COMMENT '设备名称',
    machine_type VARCHAR(32) NOT NULL COMMENT '设备类型 CNC/LATHE/MILLING 等',
    machine_no VARCHAR(32) COMMENT '机台号',
    status VARCHAR(16) NOT NULL DEFAULT 'IDLE' COMMENT 'IDLE/RUNNING/MAINTENANCE/FAULT',
    last_maintenance DATETIME COMMENT '上次维护时间',
    maintenance_cycle_days INT DEFAULT 90 COMMENT '维护周期（天）',
    remark VARCHAR(255) COMMENT '备注',
    is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_machine_code (machine_code),
    KEY idx_machine_type (machine_type),
    KEY idx_machine_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备机台台账（E5-S5 · prod_machine）';

-- 2) 机台日负荷
CREATE TABLE IF NOT EXISTS prod_machine_load (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    machine_id BIGINT NOT NULL COMMENT '机台ID',
    load_date DATE NOT NULL COMMENT '负荷日期',
    planned_hours DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '已排工时',
    available_hours DECIMAL(10,2) NOT NULL DEFAULT 12 COMMENT '可用工时',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_machine_date (machine_id, load_date),
    KEY idx_load_date (load_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机台日负荷（FR-5-4-2）';

-- 3) 维护记录
CREATE TABLE IF NOT EXISTS prod_machine_maintenance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    machine_id BIGINT NOT NULL COMMENT '机台ID',
    maintenance_type VARCHAR(32) NOT NULL COMMENT 'ROUTINE/PREVENTIVE/REPAIR',
    performed_at DATETIME NOT NULL COMMENT '执行时间',
    next_due DATETIME COMMENT '下次到期',
    remark VARCHAR(255) COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_machine_maint (machine_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备维护记录';

-- 4) 工单工序（操作工/图纸关联 · 补齐 crm_workorder_process）
CREATE TABLE IF NOT EXISTS crm_workorder_process (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    workorder_id BIGINT NOT NULL COMMENT '工单ID',
    workorder_no VARCHAR(32) NOT NULL COMMENT '工单号',
    process_seq INT NOT NULL COMMENT '工序序号',
    process_code VARCHAR(64) COMMENT '工序编码',
    process_name VARCHAR(64) NOT NULL COMMENT '工序名称',
    material_code VARCHAR(32) COMMENT '物料编码',
    machine_id BIGINT COMMENT '绑定机台ID',
    locked_machine_id BIGINT COMMENT '锁定下一机台ID',
    is_outsource TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否委外工序',
    status VARCHAR(16) DEFAULT 'PENDING' COMMENT 'PENDING/IN_PROGRESS/COMPLETED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_wo_process (workorder_id, process_seq),
    KEY idx_wo_no (workorder_no),
    KEY idx_material (material_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单工序（E5-S6 · 操作工扫码）';

-- 5) 工单工序表扩展机台字段（幂等）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_workorder_step' AND COLUMN_NAME = 'machine_id');
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE crm_workorder_step ADD COLUMN machine_id BIGINT COMMENT ''机台ID'' AFTER equipment_type, ADD COLUMN locked_machine_id BIGINT COMMENT ''锁定下一机台ID'' AFTER machine_id, ADD COLUMN is_outsource TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''是否委外'' AFTER locked_machine_id',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- include: V58__drawing_link_partial_index.sql (post-V62 indexes)
-- ============================================================
-- V1.3.9 Sprint 13 Story 13.3 · V58__drawing_link_partial_index.sql
-- crm_drawing_link 5 类 link 部分索引 + 业务表 material_code JOIN 索引
-- init.sql：在 V62 之后执行（crm_workorder_process 依赖 V62 建表）
-- ============================================================

USE `cnc_business`;

-- 1. crm_drawing_link 5 类 link 部分索引（MySQL 8 函数索引）
CREATE INDEX idx_drawing_link_order
    ON crm_drawing_link(
        (CASE WHEN biz_type = 'ORDER' THEN drawing_id END),
        (CASE WHEN biz_type = 'ORDER' THEN biz_id END)
    );

CREATE INDEX idx_drawing_link_po
    ON crm_drawing_link(
        (CASE WHEN biz_type = 'PO' THEN drawing_id END),
        (CASE WHEN biz_type = 'PO' THEN biz_id END)
    );

CREATE INDEX idx_drawing_link_incoming
    ON crm_drawing_link(
        (CASE WHEN biz_type = 'INCOMING' THEN drawing_id END),
        (CASE WHEN biz_type = 'INCOMING' THEN biz_id END)
    );

CREATE INDEX idx_drawing_link_inspection
    ON crm_drawing_link(
        (CASE WHEN biz_type = 'INSPECTION' THEN drawing_id END),
        (CASE WHEN biz_type = 'INSPECTION' THEN biz_id END)
    );

CREATE INDEX idx_drawing_link_process
    ON crm_drawing_link(
        (CASE WHEN biz_type = 'WORKORDER_PROCESS' THEN drawing_id END),
        (CASE WHEN biz_type = 'WORKORDER_PROCESS' THEN biz_id END)
    );

-- 2. 业务表 material_code JOIN 索引（真实表名 · 非 crm_incoming_order_item / crm_inspection_item）
CREATE INDEX idx_order_item_material_order
    ON crm_order_item(material, order_id);

CREATE INDEX idx_po_item_material_po
    ON crm_purchase_order_item(material_code, purchase_order_id);

CREATE INDEX idx_incoming_material_id
    ON crm_incoming(material_code, id);

CREATE INDEX idx_quality_inspection_material_id
    ON crm_quality_inspection(material_code, id);

CREATE INDEX idx_workorder_process_material
    ON crm_workorder_process(material_code);

ALTER TABLE crm_drawing_link
    COMMENT = 'V1.3.9 Sprint 12.1 创建 · Sprint 13.3 真实查询对接 · V58 加 5 部分索引 + material_code JOIN 索引';

SELECT 'V58__drawing_link_partial_index.sql 迁移完成' AS message,
       (SELECT COUNT(*) FROM information_schema.statistics
        WHERE table_schema = 'cnc_business'
          AND table_name = 'crm_drawing_link'
          AND index_name LIKE 'idx_drawing_link_%') AS partial_index_count,
       (SELECT COUNT(*) FROM information_schema.statistics
        WHERE table_schema = 'cnc_business'
          AND ((table_name = 'crm_order_item' AND index_name = 'idx_order_item_material_order')
            OR (table_name = 'crm_purchase_order_item' AND index_name = 'idx_po_item_material_po')
            OR (table_name = 'crm_incoming' AND index_name = 'idx_incoming_material_id')
            OR (table_name = 'crm_quality_inspection' AND index_name = 'idx_quality_inspection_material_id')
            OR (table_name = 'crm_workorder_process' AND index_name = 'idx_workorder_process_material'))) AS item_index_count;
-- include: V60a__cnc_production_schema.sql (post-V62 schema)
-- ============================================================
-- V60a · cnc_production 物理库 · 生产域表结构（LIKE cnc_business）
-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS `cnc_production` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `cnc_production`;

CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_workorder` LIKE `cnc_business`.`crm_workorder`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_workorder_step` LIKE `cnc_business`.`crm_workorder_step`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_production_schedule` LIKE `cnc_business`.`crm_production_schedule`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_production_scan` LIKE `cnc_business`.`crm_production_scan`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_production_report` LIKE `cnc_business`.`crm_production_report`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_production_station` LIKE `cnc_business`.`crm_production_station`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_rework` LIKE `cnc_business`.`crm_rework`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_rework_history` LIKE `cnc_business`.`crm_rework_history`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_rework_alert` LIKE `cnc_business`.`crm_rework_alert`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_outsource_order` LIKE `cnc_business`.`crm_outsource_order`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_outsource_item` LIKE `cnc_business`.`crm_outsource_item`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_outsource_history` LIKE `cnc_business`.`crm_outsource_history`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_outsource_state_history` LIKE `cnc_business`.`crm_outsource_state_history`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_outsource_quality` LIKE `cnc_business`.`crm_outsource_quality`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_outsource_quality_item` LIKE `cnc_business`.`crm_outsource_quality_item`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_outsource_quality_defect` LIKE `cnc_business`.`crm_outsource_quality_defect`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_outsource_incoming_inspection` LIKE `cnc_business`.`crm_outsource_incoming_inspection`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_outsource_incoming_item` LIKE `cnc_business`.`crm_outsource_incoming_item`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_outsource_incoming_defect` LIKE `cnc_business`.`crm_outsource_incoming_defect`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_outsource_eta` LIKE `cnc_business`.`crm_outsource_eta`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_outsource_actual` LIKE `cnc_business`.`crm_outsource_actual`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_mrp_run` LIKE `cnc_business`.`crm_mrp_run`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_mrp_result` LIKE `cnc_business`.`crm_mrp_result`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_mrp_shortage` LIKE `cnc_business`.`crm_mrp_shortage`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`outsub_allocation` LIKE `cnc_business`.`outsub_allocation`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`outsub_allocation_vendor` LIKE `cnc_business`.`outsub_allocation_vendor`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`prod_machine` LIKE `cnc_business`.`prod_machine`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`prod_machine_load` LIKE `cnc_business`.`prod_machine_load`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`prod_machine_maintenance` LIKE `cnc_business`.`prod_machine_maintenance`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_workorder_process` LIKE `cnc_business`.`crm_workorder_process`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_process` LIKE `cnc_business`.`crm_process`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_process_step` LIKE `cnc_business`.`crm_process_step`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_process_route` LIKE `cnc_business`.`crm_process_route`;

USE `cnc_business`;
-- include: V63__sys_user_availability.sql
-- V63 · 审批跳过请假：platform 本地维护用户可用性（由 erp-business HR 模块 Feign 同步）
USE `cnc_platform`;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'cnc_platform' AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'availability_status');
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE sys_user ADD COLUMN availability_status VARCHAR(16) NOT NULL DEFAULT ''ON_DUTY'' COMMENT ''ON_DUTY/ON_LEAVE/ON_TRIP/RESIGNED'' AFTER status, ADD COLUMN leave_no VARCHAR(64) NULL COMMENT ''请假单号'' AFTER availability_status',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- include: V64__outsource_vendor_switch.sql
-- V1.3.7 · 委外供应商切换（生管 + 采购双向确认）
CREATE TABLE IF NOT EXISTS `crm_outsource_vendor_switch` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `switch_no` VARCHAR(32) NOT NULL,
    `outsource_id` BIGINT NOT NULL,
    `outsource_no` VARCHAR(32) NOT NULL,
    `old_supplier_id` BIGINT NOT NULL,
    `old_supplier_name` VARCHAR(128) DEFAULT NULL,
    `new_supplier_id` BIGINT NOT NULL,
    `new_supplier_name` VARCHAR(128) DEFAULT NULL,
    `reason` VARCHAR(500) DEFAULT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    `prod_confirmed` TINYINT(1) NOT NULL DEFAULT 0,
    `purch_confirmed` TINYINT(1) NOT NULL DEFAULT 0,
    `prod_confirmed_by` BIGINT DEFAULT NULL,
    `purch_confirmed_by` BIGINT DEFAULT NULL,
    `prod_confirmed_at` DATETIME DEFAULT NULL,
    `purch_confirmed_at` DATETIME DEFAULT NULL,
    `created_by` BIGINT DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_switch_no` (`switch_no`),
    KEY `idx_switch_outsource` (`outsource_id`),
    KEY `idx_switch_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='委外供应商切换单';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
-- include: V65__workorder_sales_order_link.sql
-- V1.3.9 · 工单关联销售订单（AC-5.1.1 订单转工单）
USE `cnc_business`;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'cnc_business' AND TABLE_NAME = 'crm_workorder' AND COLUMN_NAME = 'sales_order_id');
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE crm_workorder ADD COLUMN sales_order_id BIGINT NULL COMMENT ''销售订单ID'' AFTER remark, ADD COLUMN sales_order_no VARCHAR(32) NULL COMMENT ''销售订单号 XS'' AFTER sales_order_id, ADD INDEX idx_workorder_sales_order (sales_order_id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 同步至 cnc_production（V60 模式）
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_workorder` LIKE `cnc_business`.`crm_workorder`;

SET @col_prod = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'cnc_production' AND TABLE_NAME = 'crm_workorder' AND COLUMN_NAME = 'sales_order_id');
SET @sql_prod = IF(@col_prod = 0,
  'ALTER TABLE cnc_production.crm_workorder ADD COLUMN sales_order_id BIGINT NULL COMMENT ''销售订单ID'' AFTER remark, ADD COLUMN sales_order_no VARCHAR(32) NULL COMMENT ''销售订单号 XS'' AFTER sales_order_id, ADD INDEX idx_workorder_sales_order (sales_order_id)',
  'SELECT 1');
PREPARE stmt2 FROM @sql_prod; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;
-- include: V66__outsource_drawing_id.sql
-- V1.3.8 · 委外单关联加工图纸（Epic 3 · 采购下单前确认图纸）
USE `cnc_business`;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'cnc_business' AND TABLE_NAME = 'crm_outsource_order' AND COLUMN_NAME = 'drawing_id');
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE crm_outsource_order ADD COLUMN drawing_id BIGINT NULL COMMENT ''加工图纸 ID（crm_drawing.id）'' AFTER material_code, ADD INDEX idx_outsource_drawing (drawing_id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_prod = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'cnc_production' AND TABLE_NAME = 'crm_outsource_order' AND COLUMN_NAME = 'drawing_id');
SET @sql_prod = IF(@col_prod = 0,
  'ALTER TABLE cnc_production.crm_outsource_order ADD COLUMN drawing_id BIGINT NULL COMMENT ''加工图纸 ID（crm_drawing.id）'' AFTER material_code, ADD INDEX idx_outsource_drawing (drawing_id)',
  'SELECT 1');
PREPARE stmt2 FROM @sql_prod; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;
-- include: V54__migrate_drawing_link.sql (data)
-- V54 · 图纸关联回填（init.sql 空库跳过 · 有业务数据时见 init_data.sql 或运维脚本）
USE `cnc_business`;

-- V94 · mock 清理：5 表 JOIN 回填 crm_drawing_link 已移至 init_data.sql
-- include: V72__v140_visitor_performance.sql
-- V1.4.0 · 客户第八次反馈：演示角色 + 绩效日聚合表 + 菜单
USE `cnc_platform`;

-- ---------- 1. CUSTOMER_VISITOR 角色 ----------
INSERT IGNORE INTO `sys_role` (`role_code`, `role_name`, `data_scope`, `status`) VALUES
('CUSTOMER_VISITOR', '客户现场演示', 'CUSTOM', 'ACTIVE');

-- V94 · mock 清理：visitor_demo 演示账号已移至 init_data.sql

-- ---------- 3. 菜单：绩效看板 ----------
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(112, 1, 'dash.performance', '绩效看板', '/dashboard/performance-board', 'MENU', 12);

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT r.id, 112, 'view' FROM `sys_role` r
WHERE r.role_code IN ('PROD_MGR', 'PRODUCTION_MANAGER', 'OPERATOR', 'GM', 'ADMIN', 'SYS_ADMIN');

USE `cnc_business`;

-- ---------- 4. 绩效日聚合表（XXL-JOB performanceDailyAgg 每晚刷新） ----------
CREATE TABLE IF NOT EXISTS `crm_employee_performance_daily` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `stat_date` DATE NOT NULL COMMENT '统计日期',
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作工用户 ID',
  `operator_name` VARCHAR(64) DEFAULT NULL,
  `machine_id` BIGINT DEFAULT NULL COMMENT '设备 ID',
  `machine_code` VARCHAR(32) DEFAULT NULL,
  `finished_qty` INT NOT NULL DEFAULT 0 COMMENT '完工总数',
  `qualified_qty` INT NOT NULL DEFAULT 0 COMMENT '合格数',
  `scrap_qty` INT NOT NULL DEFAULT 0 COMMENT '报废数',
  `actual_minutes` INT NOT NULL DEFAULT 0 COMMENT '实际工时',
  `std_minutes` INT NOT NULL DEFAULT 0 COMMENT '标准工时',
  `utilization_rate` DECIMAL(8,4) DEFAULT NULL COMMENT '工时利用率',
  `pass_rate` DECIMAL(8,4) DEFAULT NULL COMMENT '合格率',
  `score` DECIMAL(8,2) DEFAULT NULL COMMENT '考核分',
  `grade` VARCHAR(16) DEFAULT NULL COMMENT '等级',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_perf_daily` (`stat_date`, `operator_id`, `machine_id`),
  KEY `idx_perf_date` (`stat_date`),
  KEY `idx_perf_operator` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V1.4.0 · E11-S6 · 员工绩效日聚合';

CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_employee_performance_daily` LIKE `cnc_business`.`crm_employee_performance_daily`;
-- include: V73__purchase_request_workflow.sql
-- V1.3.8 合规 · 采购申请（PR）+ PO/RFQ 来源关联
USE `cnc_business`;

-- ---------- 1. 采购申请主表（MRP 缺料 → 生管推送 → 采购转单） ----------
CREATE TABLE IF NOT EXISTS `crm_purchase_request` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `pr_no` VARCHAR(32) NOT NULL COMMENT 'PR-{yyyyMMdd}-{seq:4}',
  `mrp_run_id` BIGINT DEFAULT NULL COMMENT 'MRP 运行 ID',
  `mrp_shortage_id` BIGINT DEFAULT NULL COMMENT '缺料行 ID',
  `workorder_no` VARCHAR(64) DEFAULT NULL COMMENT '关联工单号（只读展示）',
  `sales_order_no` VARCHAR(64) DEFAULT NULL COMMENT '关联销售订单号',
  `material_id` BIGINT DEFAULT NULL,
  `material_code` VARCHAR(64) NOT NULL,
  `material_name` VARCHAR(128) DEFAULT NULL,
  `required_qty` INT NOT NULL DEFAULT 0 COMMENT '需求数量',
  `converted_qty` INT NOT NULL DEFAULT 0 COMMENT '已转采购数量',
  `required_date` DATE DEFAULT NULL COMMENT '期望交期',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PARTIAL/CONVERTED/CANCELLED',
  `source_type` VARCHAR(20) NOT NULL DEFAULT 'MRP' COMMENT 'MRP/NO_ORDER',
  `remark` VARCHAR(500) DEFAULT NULL,
  `created_by` BIGINT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_pr_no` (`pr_no`),
  KEY `idx_pr_status` (`status`),
  KEY `idx_pr_material` (`material_code`),
  KEY `idx_pr_mrp_run` (`mrp_run_id`),
  KEY `idx_pr_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购申请单（MRP 缺料触发）';

-- ---------- 2. PO 关联 PR / 工单 ----------
SET @col_pr_id = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'cnc_business' AND TABLE_NAME = 'crm_purchase_order' AND COLUMN_NAME = 'pr_id');
SET @sql_pr_id = IF(@col_pr_id = 0,
  'ALTER TABLE crm_purchase_order
     ADD COLUMN pr_id BIGINT NULL COMMENT ''来源采购申请 ID'' AFTER rfq_id,
     ADD COLUMN pr_no VARCHAR(32) NULL COMMENT ''来源单号 PR-XXX'' AFTER pr_id,
     ADD COLUMN workorder_no VARCHAR(64) NULL COMMENT ''关联工单号'' AFTER pr_no,
     ADD COLUMN mrp_run_id BIGINT NULL COMMENT ''MRP 运行 ID'' AFTER workorder_no,
     ADD KEY idx_po_pr (pr_id)',
  'SELECT 1');
PREPARE stmt FROM @sql_pr_id; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 3. RFQ 来源绑定 + 转单状态 ----------
SET @col_rfq_src = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'cnc_business' AND TABLE_NAME = 'crm_rfq' AND COLUMN_NAME = 'inquiry_source_type');
SET @sql_rfq_src = IF(@col_rfq_src = 0,
  'ALTER TABLE crm_rfq
     ADD COLUMN inquiry_source_type VARCHAR(20) NULL COMMENT ''MATERIAL/OUTSOURCE/NO_ORDER'' AFTER winner_mode,
     ADD COLUMN pr_id BIGINT NULL COMMENT ''绑定采购申请'' AFTER inquiry_source_type,
     ADD COLUMN pr_no VARCHAR(32) NULL AFTER pr_id,
     ADD COLUMN workorder_no VARCHAR(64) NULL COMMENT ''关联工单'' AFTER pr_no,
     ADD COLUMN process_step_no INT NULL COMMENT ''委外工序号'' AFTER workorder_no,
     ADD COLUMN allocation_id BIGINT NULL COMMENT ''待委外工序分配 ID'' AFTER process_step_no,
     ADD COLUMN convert_status VARCHAR(24) NOT NULL DEFAULT ''NOT_CONVERTED'' COMMENT ''NOT_CONVERTED/PO_CONVERTED/OUTSOURCE_CONVERTED'' AFTER allocation_id,
     ADD COLUMN converted_order_no VARCHAR(32) NULL COMMENT ''转单后 PO/WW 单号'' AFTER convert_status,
     ADD KEY idx_rfq_pr (pr_id),
     ADD KEY idx_rfq_convert (convert_status)',
  'SELECT 1');
PREPARE stmt2 FROM @sql_rfq_src; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;

-- ---------- 4. 菜单：采购转单（替换独立新建 PO 入口） ----------
USE `cnc_platform`;

INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(613, 6, 'src.pr-transfer', '采购转单', '/sourcing/purchase-transfer', 'MENU', 3);

UPDATE `sys_menu` SET `menu_name` = '询比价工作台', `menu_code` = 'src.rfq.workbench' WHERE `id` = 601;
UPDATE `sys_menu` SET `menu_name` = '委外转单', `menu_code` = 'src.outsub.transfer' WHERE `id` = 607;

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT r.id, 613, 'view' FROM `sys_role` r
WHERE r.role_code IN ('BUYER', 'PROCUREMENT_MANAGER', 'PROD_MGR', 'GM', 'SYS_ADMIN');
-- include: V67__sys_menu_permission.sql（与 build-init-data.ps1 行为对齐）
-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
USE `cnc_platform`;

CREATE TABLE IF NOT EXISTS `sys_menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_id` BIGINT DEFAULT NULL COMMENT '父菜单 ID',
  `menu_code` VARCHAR(64) NOT NULL COMMENT '菜单编码',
  `menu_name` VARCHAR(100) NOT NULL COMMENT '菜单名称',
  `path` VARCHAR(255) NOT NULL COMMENT '前端路由 path',
  `menu_type` VARCHAR(20) NOT NULL DEFAULT 'MENU' COMMENT 'MODULE/MENU/ROUTE',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `icon` VARCHAR(64) DEFAULT NULL COMMENT '图标',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_menu_code` (`menu_code`),
  UNIQUE KEY `uniq_menu_path` (`path`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统菜单';

DELETE FROM `sys_role_permission`;
DELETE FROM `sys_menu`;

-- ---------- 顶级模块 ----------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`, `icon`) VALUES
(1, NULL, 'mod.dashboard',   '工作台', '/dashboard',   'MODULE', 10, 'HomeFilled'),
(2, NULL, 'mod.sales',       '销售',   '/sales',       'MODULE', 20, 'Money'),
(3, NULL, 'mod.production',  '生产',   '/production',  'MODULE', 30, 'Tools'),
(4, NULL, 'mod.material',    '物料',   '/material',    'MODULE', 40, 'Goods'),
(5, NULL, 'mod.quality',     '品质',   '/quality',     'MODULE', 50, 'Medal'),
(6, NULL, 'mod.sourcing',    '采购',   '/sourcing',    'MODULE', 60, 'ShoppingCart'),
(7, NULL, 'mod.finance',     '财务',   '/finance',     'MODULE', 70, 'CreditCard'),
(8, NULL, 'mod.hr',          '人事',   '/hr',          'MODULE', 80, 'User'),
(9, NULL, 'mod.admin',       '管理',   '/admin',       'MODULE', 90, 'Setting'),
(10, NULL, 'mod.warehouse',  '仓储',   '/warehouse',   'MODULE', 45, 'Box');

-- ---------- 工作台 ----------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(101, 1, 'dash.index',        '总览',       '/dashboard/index',        'MENU', 1),
(102, 1, 'dash.production',   '生产工作台', '/dashboard/production',   'MENU', 2),
(103, 1, 'dash.sales',        '销售驾驶舱', '/dashboard/sales',        'MENU', 3),
(104, 1, 'dash.finance',      '财务驾驶舱', '/dashboard/finance',      'MENU', 4),
(105, 1, 'dash.quality',      '品质驾驶舱', '/dashboard/quality',      'MENU', 5),
(106, 1, 'dash.outsource',    '委外驾驶舱', '/dashboard/outsource',    'MENU', 6),
(107, 1, 'dash.procurement',  '采购驾驶舱', '/dashboard/procurement',  'MENU', 7),
(108, 1, 'dash.engineer',     '工程师工作台','/dashboard/engineer',    'MENU', 8),
(109, 1, 'dash.warehouse',    '仓管工作台', '/dashboard/warehouse',    'MENU', 9),
(110, 1, 'dash.alerts',       '总经理告警', '/dashboard/alerts',       'MENU', 10),
(111, 1, 'dash.multi',        '多维度看板', '/dashboard/multi',        'MENU', 11);

-- ---------- 销售 ----------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(201, 2, 'sales.customers',    '客户档案', '/sales/customers',           'MENU', 1),
(202, 2, 'sales.protection',   '客户保护', '/sales/customer/protection', 'MENU', 2),
(203, 2, 'sales.quotes',       '报价单',   '/sales/quotes',              'MENU', 3),
(204, 2, 'sales.quote-approval','报价审批','/sales/quotes/approval',     'MENU', 4),
(205, 2, 'sales.orders',       '销售订单', '/sales/orders',              'MENU', 5),
(206, 2, 'sales.contracts',    '合同回款', '/sales/contracts',           'MENU', 6);

-- ---------- 生产 ----------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(301, 3, 'prod.workorders',    '工单',       '/production/workorders',          'MENU', 1),
(302, 3, 'prod.pending',       '待转产订单', '/production/pending-production',  'MENU', 2),
(303, 3, 'prod.schedule',      '排产看板',   '/production/schedule',            'MENU', 3),
(304, 3, 'prod.gantt',         '排产甘特',   '/production/schedule-gantt',        'MENU', 4),
(305, 3, 'prod.mrp',           'MRP 中心',   '/production/mrp',                 'MENU', 5),
(306, 3, 'prod.outsource',     '委外列表',   '/production/outsource',           'MENU', 6),
(307, 3, 'prod.allocation',    '工序分配',   '/production/allocation',          'MENU', 7),
(308, 3, 'prod.outsub-panel',  '委外面板',   '/production/outsub-panel',        'MENU', 8),
(309, 3, 'prod.machines',      '设备机台',   '/production/machines',            'MENU', 9);

-- ---------- 物料 ----------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(401, 4, 'mat.drawings',   '图纸',     '/material/drawings',          'MENU', 1),
(402, 4, 'mat.lookup',     '料号查询', '/material/lookup',            'MENU', 2),
(403, 4, 'mat.barcode',    '物料条码', '/material/barcode-list',      'MENU', 3),
(404, 4, 'mat.category',   '物料分类', '/material/material-category', 'MENU', 4),
(405, 4, 'mat.boms',       'BOM',      '/material/boms',                'MENU', 5),
(406, 4, 'mat.process',    '工艺库',   '/material/process',             'MENU', 6),
(407, 4, 'mat.cost',       '料号成本', '/material/cost-aggregator',   'MENU', 7);

-- ---------- 品质 ----------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(501, 5, 'qc.inspection',  '来料/过程/成品检', '/quality/inspection',          'MENU', 1),
(502, 5, 'qc.fa',          'FA 首件',          '/quality/fa',                  'MENU', 2),
(503, 5, 'qc.cmm',         '三次元',           '/quality/cmm',                 'MENU', 3),
(504, 5, 'qc.defect',      '不良品',           '/quality/defect',              'MENU', 4),
(505, 5, 'qc.pickup',      '提货检',           '/quality/pickup',              'MENU', 5),
(506, 5, 'qc.outsource',   '委外检',           '/quality/outsource-inspection','MENU', 6);

-- ---------- 采购 ----------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(601, 6, 'src.rfq',        '询价比价 RFQ', '/sourcing/rfq',              'MENU', 1),
(602, 6, 'src.po',         '采购订单 PO',  '/sourcing/po',               'MENU', 2),
(603, 6, 'src.incoming',   '到货回执',     '/sourcing/incoming',           'MENU', 3),
(604, 6, 'src.no-order',   '无单采购',     '/sourcing/no-order-purchase',  'MENU', 4),
(605, 6, 'src.approval',   '审批路由',     '/sourcing/approval-route',     'MENU', 5),
(606, 6, 'src.reconcile',  '月度对账',     '/sourcing/reconcile',          'MENU', 6),
(607, 6, 'src.outsub',     '委外下单',     '/sourcing/outsub-order',       'MENU', 7),
(608, 6, 'src.rework',     '返修协同',     '/sourcing/rework',             'MENU', 8),
(609, 6, 'src.vendors',    '厂商资料',     '/sourcing/vendors',            'MENU', 9);

-- ---------- 财务 ----------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(701, 7, 'fin.receivables', '应收',     '/finance/receivables', 'MENU', 1),
(702, 7, 'fin.payables',    '应付',     '/finance/payables',    'MENU', 2),
(703, 7, 'fin.aging',       '账龄',     '/finance/aging',       'MENU', 3),
(704, 7, 'fin.cost',        '成本',     '/finance/cost',        'MENU', 4),
(705, 7, 'fin.payments',    '付款',     '/finance/payments',    'MENU', 5),
(706, 7, 'fin.profit',      '利润分析', '/finance/profit',      'MENU', 6),
(707, 7, 'fin.scans',       '签字扫描', '/finance/signed-scans','MENU', 7),
(708, 7, 'fin.gm-summary',  '总经理汇总','/finance/gm-summary', 'MENU', 8);

-- ---------- 人事 ----------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(801, 8, 'hr.employees',   '员工档案', '/hr/employees',   'MENU', 1),
(802, 8, 'hr.accounts',    '账号管理', '/hr/accounts',    'MENU', 2),
(803, 8, 'hr.attendance',  '考勤',     '/hr/attendance',  'MENU', 3),
(804, 8, 'hr.payroll',     '薪资',     '/hr/payroll',     'MENU', 4);

-- ---------- 管理 ----------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(901, 9, 'adm.users',           '用户管理',   '/admin/users',                  'MENU', 1),
(902, 9, 'adm.workflows',       '审批流',     '/admin/workflows',              'MENU', 2),
(903, 9, 'adm.dict',            '数据字典',   '/admin/dict',                   'MENU', 3),
(904, 9, 'adm.keyboard',        '快捷键',     '/admin/keyboard',               'MENU', 4),
(905, 9, 'adm.printers',        '打印机',     '/admin/printers',               'MENU', 5),
(906, 9, 'adm.email-config',    '邮件配置',   '/admin/email-config',           'MENU', 6),
(907, 9, 'adm.email-templates', '邮件模板',   '/admin/email-templates',        'MENU', 7),
(908, 9, 'adm.field-encrypt',   '字段加密',   '/admin/field-encryption',       'MENU', 8),
(909, 9, 'adm.rpt-workflow',    '审批统计',   '/admin/reports/workflow-stats', 'MENU', 9),
(910, 9, 'adm.rpt-ranking',     '销售龙虎榜', '/admin/reports/sales-ranking',  'MENU', 10),
(911, 9, 'adm.rpt-trend',       '销售趋势',   '/admin/reports/sales-trend',    'MENU', 11),
(912, 9, 'adm.rpt-customer',    '客户分析',   '/admin/reports/customer-analysis','MENU', 12);

-- ---------- 仓储 ----------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `path`, `menu_type`, `sort`) VALUES
(1001, 10, 'wh.index',    '多仓库总览', '/warehouse/index',           'MENU', 1),
(1002, 10, 'wh.locations','库位树',     '/warehouse/locations',       'MENU', 2),
(1003, 10, 'wh.batches',  '批次列表',   '/warehouse/batches',         'MENU', 3),
(1004, 10, 'wh.inventory','库存',       '/warehouse/inventory',       'MENU', 4),
(1005, 10, 'wh.alert',    '库存预警',   '/warehouse/inventory-alert', 'MENU', 5);

-- ---------- 角色权限：SYS_ADMIN 全量 ----------
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 1, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE';

-- SALES 业务员
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 2, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND (
  `path` IN ('/dashboard','/dashboard/index','/dashboard/sales','/sales')
  OR `path` LIKE '/sales/%'
);

-- SALES_MGR 销售经理（同 SALES + 报价审批已有）
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 3, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND (
  `path` IN ('/dashboard','/dashboard/index','/dashboard/sales','/sales')
  OR `path` LIKE '/sales/%'
);

-- GM 总经理（除管理后台）
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 4, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND `path` NOT LIKE '/admin%';

-- PROD_MGR 生管
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 5, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND (
  `path` IN ('/dashboard','/dashboard/index','/dashboard/production','/production','/material')
  OR `path` LIKE '/dashboard/production%'
  OR `path` LIKE '/production/%'
  OR `path` IN ('/material/drawings','/material/lookup','/material/boms','/material/process','/material/material-category')
);

-- ENGINEER
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 6, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND (
  `path` IN ('/dashboard','/dashboard/index','/dashboard/engineer','/production','/material')
  OR `path` LIKE '/dashboard/engineer%'
  OR `path` LIKE '/production/schedule%'
  OR `path` LIKE '/production/mrp%'
  OR `path` LIKE '/production/workorders%'
  OR `path` LIKE '/material/%'
);

-- WAREHOUSE 仓管
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 7, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND (
  `path` IN ('/dashboard','/dashboard/index','/dashboard/warehouse','/warehouse','/material')
  OR `path` LIKE '/warehouse/%'
  OR `path` IN ('/material/lookup','/material/barcode-list')
);

-- QC 品检
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 8, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND (
  `path` IN ('/dashboard','/dashboard/index','/dashboard/quality','/quality')
  OR `path` LIKE '/quality/%'
);

-- BUYER 采购
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 9, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND (
  `path` IN ('/dashboard','/dashboard/index','/dashboard/procurement','/dashboard/outsource','/sourcing')
  OR `path` LIKE '/sourcing/%'
  OR `path` IN ('/production/outsub-panel')
);

-- FINANCE 财务
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 10, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND (
  `path` IN ('/dashboard','/dashboard/index','/dashboard/finance','/finance')
  OR `path` LIKE '/finance/%'
  OR `path` IN ('/sales/contracts')
);

-- HR 人事
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT 11, `id`, 'view' FROM `sys_menu` WHERE `status` = 'ACTIVE' AND (
  `path` IN ('/dashboard','/dashboard/index','/hr')
  OR `path` LIKE '/hr/%'
);

-- OPERATOR 操作工
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT r.id, m.id, 'view'
FROM `sys_role` r
JOIN `sys_menu` m ON m.status = 'ACTIVE'
WHERE r.role_code = 'OPERATOR'
  AND (
    m.path IN ('/dashboard','/dashboard/index','/dashboard/production','/production')
    OR m.path IN ('/production/workorders','/production/schedule')
  );

-- PROCUREMENT_MANAGER 采购主管
INSERT INTO `sys_role_permission` (`role_id`, `menu_id`, `action`)
SELECT r.id, m.id, 'view'
FROM `sys_role` r
JOIN `sys_menu` m ON m.status = 'ACTIVE'
WHERE r.role_code = 'PROCUREMENT_MANAGER'
  AND (
    m.path IN ('/dashboard','/dashboard/index','/dashboard/procurement','/sourcing','/finance')
    OR m.path LIKE '/sourcing/%'
    OR m.path IN ('/finance/gm-summary')
  );
