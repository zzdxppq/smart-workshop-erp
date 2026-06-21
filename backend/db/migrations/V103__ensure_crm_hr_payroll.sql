-- V103 · 确保 crm_hr_payroll 在 cnc_business（修复 V39 未 USE 导致表建错库）
USE `cnc_business`;

CREATE TABLE IF NOT EXISTS crm_hr_payroll (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payroll_no VARCHAR(64) NOT NULL,
    period_year INT NOT NULL,
    period_month INT NOT NULL,
    employee_id BIGINT NOT NULL,
    employee_no VARCHAR(64) NOT NULL,
    employee_name VARCHAR(64) NULL,
    base_salary DECIMAL(12,2) NOT NULL DEFAULT 0,
    overtime_hours DECIMAL(8,2) NOT NULL DEFAULT 0,
    overtime_pay DECIMAL(12,2) NOT NULL DEFAULT 0,
    bonus DECIMAL(12,2) NOT NULL DEFAULT 0,
    deduction DECIMAL(12,2) NOT NULL DEFAULT 0,
    tax DECIMAL(12,2) NOT NULL DEFAULT 0,
    net_salary DECIMAL(12,2) NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    approved_by BIGINT NULL,
    approved_at DATETIME NULL,
    created_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_payroll_no (payroll_no),
    UNIQUE KEY uk_emp_period (employee_id, period_year, period_month),
    KEY idx_payroll_period (period_year, period_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='薪酬单';

SET @col_pay_pos = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_hr_payroll' AND COLUMN_NAME = 'position_salary');
SET @sql_pay = IF(@col_pay_pos > 0,
  'SELECT ''skip crm_hr_payroll.scheme_cols'' AS note',
  'ALTER TABLE crm_hr_payroll
     ADD COLUMN position_salary DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT ''岗位工资'' AFTER base_salary,
     ADD COLUMN piece_pay DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT ''计件工资'' AFTER position_salary,
     ADD COLUMN performance_bonus DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT ''绩效奖金'' AFTER piece_pay,
     ADD COLUMN full_attendance_bonus DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT ''全勤奖'' AFTER bonus,
     ADD COLUMN social_insurance DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT ''社保扣款'' AFTER deduction');
PREPARE stmt FROM @sql_pay; EXECUTE stmt; DEALLOCATE PREPARE stmt;
