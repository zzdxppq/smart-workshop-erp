-- V77 · 绩效申诉

USE `cnc_business`;

CREATE TABLE IF NOT EXISTS crm_hr_performance_appeal (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    performance_id BIGINT NOT NULL COMMENT '绩效记录ID',
    employee_id BIGINT NOT NULL COMMENT '员工ID',
    employee_name VARCHAR(64) NULL COMMENT '员工姓名',
    period_year INT NOT NULL COMMENT '年份',
    period_month INT NOT NULL COMMENT '月份',
    reason TEXT NOT NULL COMMENT '申诉理由',
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
    reviewer_user_id BIGINT NULL COMMENT '处理人用户ID',
    reply VARCHAR(500) NULL COMMENT '处理回复',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at DATETIME NULL,
    KEY idx_appeal_emp (employee_id),
    KEY idx_appeal_perf (performance_id),
    KEY idx_appeal_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='绩效申诉';
