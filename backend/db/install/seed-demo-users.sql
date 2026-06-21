-- 已有库补全 12 个演示账号（密码 123456 · 不删业务数据）
-- 用法：mysql ... cnc_platform < backend/db/install/seed-demo-users.sql
USE `cnc_platform`;

INSERT INTO `sys_role` (`id`, `role_code`, `role_name`, `data_scope`, `amount_threshold`, `status`) VALUES
(12, 'OPERATOR', '操作工', 'SELF', NULL, 'ACTIVE'),
(13, 'PROCUREMENT_MANAGER', '采购主管', 'DEPT', 50000, 'ACTIVE'),
(14, 'CUSTOMER_VISITOR', '客户现场演示', 'CUSTOM', NULL, 'ACTIVE')
ON DUPLICATE KEY UPDATE `role_name` = VALUES(`role_name`), `updated_at` = CURRENT_TIMESTAMP;

INSERT INTO `sys_user` (`id`, `username`, `password_hash`, `real_name`, `email`, `status`) VALUES
(1,  'admin',               '$2a$12$wulkj1kVO3eGc8v9ckGWEeE.Toh5NX/XL8CRtCWbdbgAlxCP1LtIe', '系统管理员', 'admin@cnc.local',               'ACTIVE'),
(2,  'sales',               '$2a$12$wulkj1kVO3eGc8v9ckGWEeE.Toh5NX/XL8CRtCWbdbgAlxCP1LtIe', '业务员',     'sales@cnc.local',               'ACTIVE'),
(3,  'sales_mgr',           '$2a$12$wulkj1kVO3eGc8v9ckGWEeE.Toh5NX/XL8CRtCWbdbgAlxCP1LtIe', '销售经理',   'sales_mgr@cnc.local',           'ACTIVE'),
(4,  'gm',                  '$2a$12$wulkj1kVO3eGc8v9ckGWEeE.Toh5NX/XL8CRtCWbdbgAlxCP1LtIe', '总经理',     'gm@cnc.local',                  'ACTIVE'),
(5,  'prod_mgr',            '$2a$12$wulkj1kVO3eGc8v9ckGWEeE.Toh5NX/XL8CRtCWbdbgAlxCP1LtIe', '生管',       'prod_mgr@cnc.local',            'ACTIVE'),
(6,  'engineer',            '$2a$12$wulkj1kVO3eGc8v9ckGWEeE.Toh5NX/XL8CRtCWbdbgAlxCP1LtIe', '工程师',     'engineer@cnc.local',            'ACTIVE'),
(7,  'warehouse',           '$2a$12$wulkj1kVO3eGc8v9ckGWEeE.Toh5NX/XL8CRtCWbdbgAlxCP1LtIe', '仓管',       'warehouse@cnc.local',           'ACTIVE'),
(8,  'qc',                  '$2a$12$wulkj1kVO3eGc8v9ckGWEeE.Toh5NX/XL8CRtCWbdbgAlxCP1LtIe', '品检',       'qc@cnc.local',                  'ACTIVE'),
(9,  'buyer',               '$2a$12$wulkj1kVO3eGc8v9ckGWEeE.Toh5NX/XL8CRtCWbdbgAlxCP1LtIe', '采购',       'buyer@cnc.local',               'ACTIVE'),
(10, 'finance',             '$2a$12$wulkj1kVO3eGc8v9ckGWEeE.Toh5NX/XL8CRtCWbdbgAlxCP1LtIe', '财务',       'finance@cnc.local',             'ACTIVE'),
(11, 'hr',                  '$2a$12$wulkj1kVO3eGc8v9ckGWEeE.Toh5NX/XL8CRtCWbdbgAlxCP1LtIe', '人事',       'hr@cnc.local',                  'ACTIVE'),
(12, 'procurement_manager', '$2a$12$wulkj1kVO3eGc8v9ckGWEeE.Toh5NX/XL8CRtCWbdbgAlxCP1LtIe', '操作工',     'procurement_manager@cnc.local', 'ACTIVE'),
(14, 'visitor_demo',        '$2a$12$wulkj1kVO3eGc8v9ckGWEeE.Toh5NX/XL8CRtCWbdbgAlxCP1LtIe', '客户演示账号', 'visitor_demo@cnc.local',        'ACTIVE')
ON DUPLICATE KEY UPDATE `password_hash` = VALUES(`password_hash`), `real_name` = VALUES(`real_name`), `updated_at` = CURRENT_TIMESTAMP;

DELETE FROM `sys_user_role` WHERE `user_id` BETWEEN 1 AND 14;

INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(1, 1), (2, 2), (3, 3), (4, 4), (5, 5), (6, 6), (7, 7), (8, 8), (9, 9), (10, 10), (11, 11), (12, 13), (14, 14);

SELECT u.username, u.real_name, r.role_code, r.role_name
FROM sys_user u
JOIN sys_user_role ur ON u.id = ur.user_id
JOIN sys_role r ON ur.role_id = r.id
WHERE u.id BETWEEN 1 AND 12
ORDER BY u.id;
