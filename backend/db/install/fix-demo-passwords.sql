-- 修复演示账号 BCrypt 哈希（明文均为 12345678 · cost=12）
-- 适用：已导入旧版 init.sql、登录报「用户名或密码错误」的环境
USE `cnc_platform`;

UPDATE `sys_user`
SET `password_hash` = '$2b$12$x20AmoiudU3OcWpzsX4u2.DTOEkFFLt2Jj/LaSpScnfSAA6qavgSC',
    `updated_at` = CURRENT_TIMESTAMP
WHERE `username` IN (
  'admin', 'sales', 'sales_mgr', 'gm', 'prod_mgr', 'engineer',
  'warehouse', 'qc', 'buyer', 'finance', 'hr', 'procurement_manager', 'operator'
);
