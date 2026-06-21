-- 补全 163 SMTP（platform 库 · id=1 singleton）
-- 推荐：API 脚本（勿把授权码写入 Git）
--   API_BASE=https://bts.51xiaoping.com SMTP_AUTH_CODE=*** node backend/scripts/configure-email-smtp.mjs
-- 或手动 UPDATE（from_address 须为与授权码同一 163 账号）：
--   UPDATE email_config SET smtp_host='smtp.163.com', smtp_port=465, use_ssl=1,
--     from_address='your-account@163.com', auth_code_kek='***' WHERE id=1;
USE `cnc_platform`;

SELECT id, smtp_host, smtp_port, use_ssl, from_address, updated_at FROM email_config WHERE id = 1;
