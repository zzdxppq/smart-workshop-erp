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
