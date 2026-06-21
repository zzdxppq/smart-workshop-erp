-- V1.3.7 · Story 1.43 · 绩效与招聘
USE `cnc_business`;

CREATE TABLE IF NOT EXISTS crm_hr_performance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    employee_no VARCHAR(64) NOT NULL,
    employee_name VARCHAR(64) NULL,
    period_year INT NOT NULL,
    period_month INT NOT NULL,
    score DECIMAL(5,2) NOT NULL,
    grade VARCHAR(8) NULL COMMENT 'A/B/C/D',
    kpi_items TEXT NULL,
    comment VARCHAR(500) NULL,
    created_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_perf_emp (employee_id),
    KEY idx_perf_period (period_year, period_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工绩效';

CREATE TABLE IF NOT EXISTS crm_hr_recruitment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recruitment_no VARCHAR(64) NOT NULL,
    candidate_name VARCHAR(64) NOT NULL,
    position VARCHAR(64) NULL,
    department VARCHAR(64) NULL,
    phone VARCHAR(32) NULL,
    email VARCHAR(128) NULL,
    hr_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    dept_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    hrd_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    final_status VARCHAR(16) NOT NULL DEFAULT 'RECRUITING',
    offer_date DATE NULL,
    onboard_date DATE NULL,
    created_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_recruitment_no (recruitment_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='招聘记录';

-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql
