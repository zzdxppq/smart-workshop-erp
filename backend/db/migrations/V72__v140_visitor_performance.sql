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
