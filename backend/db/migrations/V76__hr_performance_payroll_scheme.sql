-- V76 · 考核方案 + 工资账套 + 薪酬扩展字段

USE `cnc_business`;

CREATE TABLE IF NOT EXISTS crm_hr_performance_scheme (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scheme_name VARCHAR(64) NOT NULL COMMENT '方案名称',
    position VARCHAR(64) NULL COMMENT '适用岗位（空=通用）',
    output_weight DECIMAL(5,2) NOT NULL DEFAULT 40.00 COMMENT '产量权重%',
    quality_weight DECIMAL(5,2) NOT NULL DEFAULT 30.00 COMMENT '质量权重%',
    attendance_weight DECIMAL(5,2) NOT NULL DEFAULT 30.00 COMMENT '出勤权重%',
    is_default TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_scheme_position (position)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='绩效考核方案';

CREATE TABLE IF NOT EXISTS crm_hr_salary_package (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    package_name VARCHAR(64) NOT NULL COMMENT '账套名称',
    position VARCHAR(64) NULL COMMENT '适用岗位',
    base_salary DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '基本工资',
    position_salary DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '岗位工资',
    piece_unit_price DECIMAL(10,4) NOT NULL DEFAULT 0 COMMENT '计件单价（元/件）',
    performance_a_rate DECIMAL(5,4) NOT NULL DEFAULT 0.2000 COMMENT 'A级绩效系数',
    performance_b_rate DECIMAL(5,4) NOT NULL DEFAULT 0.1000 COMMENT 'B级绩效系数',
    performance_c_rate DECIMAL(5,4) NOT NULL DEFAULT 0.0500 COMMENT 'C级绩效系数',
    performance_d_rate DECIMAL(5,4) NOT NULL DEFAULT 0.0000 COMMENT 'D级绩效系数',
    overtime_rate DECIMAL(5,2) NOT NULL DEFAULT 1.50 COMMENT '加班倍率',
    full_attendance_bonus DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '全勤奖',
    social_insurance_rate DECIMAL(5,4) NOT NULL DEFAULT 0.1050 COMMENT '社保扣款比例',
    tax_threshold DECIMAL(12,2) NOT NULL DEFAULT 5000 COMMENT '个税起征点',
    tax_rate DECIMAL(5,4) NOT NULL DEFAULT 0.1000 COMMENT '个税税率',
    is_default TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_pkg_position (position)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工资账套';

SET @tbl_emp = (SELECT COUNT(*) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_hr_employee');
SET @col_emp_pkg = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_hr_employee' AND COLUMN_NAME = 'salary_package_id');
SET @sql_emp = IF(@tbl_emp = 0 OR @col_emp_pkg > 0,
  'SELECT ''skip crm_hr_employee.payroll_scheme_cols'' AS note',
  'ALTER TABLE crm_hr_employee
     ADD COLUMN salary_package_id BIGINT NULL COMMENT ''工资账套ID'' AFTER base_salary,
     ADD COLUMN performance_scheme_id BIGINT NULL COMMENT ''考核方案ID'' AFTER salary_package_id,
     ADD COLUMN reviewer_user_id BIGINT NULL COMMENT ''考核人用户ID'' AFTER performance_scheme_id');
PREPARE stmt FROM @sql_emp; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @tbl_pay = (SELECT COUNT(*) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_hr_payroll');
SET @col_pay_pos = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_hr_payroll' AND COLUMN_NAME = 'position_salary');
SET @sql_pay = IF(@tbl_pay = 0 OR @col_pay_pos > 0,
  'SELECT ''skip crm_hr_payroll.scheme_cols'' AS note',
  'ALTER TABLE crm_hr_payroll
     ADD COLUMN position_salary DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT ''岗位工资'' AFTER base_salary,
     ADD COLUMN piece_pay DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT ''计件工资'' AFTER position_salary,
     ADD COLUMN performance_bonus DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT ''绩效奖金'' AFTER piece_pay,
     ADD COLUMN full_attendance_bonus DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT ''全勤奖'' AFTER bonus,
     ADD COLUMN social_insurance DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT ''社保扣款'' AFTER deduction');
PREPARE stmt FROM @sql_pay; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- V94 · mock 清理：考核方案/工资账套演示 seed 已移至 init_data.sql
