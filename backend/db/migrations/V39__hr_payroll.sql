-- V1.3.7 · Story 1.42 · 薪酬自动核算
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

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
