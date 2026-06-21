#!/usr/bin/env python3
"""V94 · Python 版 build-init.ps1：跳过 mock 演示段，重建 init.sql"""
import os
import sys
import datetime

ROOT = r"E:/claude/smart-workshop-erp/backend/db"
BASELINE = os.path.join(ROOT, "init.baseline.sql")
OUT = os.path.join(ROOT, "init.sql")

with open(BASELINE, "r", encoding="utf-8") as f:
    baseline = f.read()

# DDL 段：从开头到 INSERT INTO ``sys_user``
ddl_end = baseline.index("INSERT INTO `sys_user` (`id`, `username`, `password_hash`")
ddl_end = baseline.rfind("--", 0, ddl_end)
footer_idx = baseline.index("SET FOREIGN_KEY_CHECKS = 1;", ddl_end)
ddl = baseline[:ddl_end].rstrip()
seed = baseline[ddl_end:footer_idx].rstrip()

ts = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
parts = [
    "-- " + "=" * 60,
    "-- 昆山佰泰胜专属 ERP · 数据库一键初始化（空库专用 · 无 mock）",
    "-- 库名：cnc_platform / cnc_business / cnc_production",
    f"-- 生成时间：{ts}",
    "-- 用法：mysql -h HOST -u USER -p --default-character-set=utf8mb4 < backend/db/init.sql",
    "-- 维护：改 init.baseline.sql 后运行此脚本",
    "-- 含全量 Mock（50 订单 + 50 员工 + 演示工单）：mysql ... < backend/db/init_data.sql",
    "-- " + "=" * 60,
    "",
    "SET NAMES utf8mb4;",
    "SET collation_connection = 'utf8mb4_unicode_ci';",
    "SET FOREIGN_KEY_CHECKS = 0;",
    "",
    ddl,
    "",
    seed,
    "",
    "SET FOREIGN_KEY_CHECKS = 1;",
    "",
    "SELECT 'cnc_platform' AS db, COUNT(*) AS table_count FROM information_schema.tables WHERE table_schema = 'cnc_platform'",
    "UNION ALL SELECT 'cnc_business', COUNT(*) FROM information_schema.tables WHERE table_schema = 'cnc_business'",
    "UNION ALL SELECT 'cnc_production', COUNT(*) FROM information_schema.tables WHERE table_schema = 'cnc_production';",
    "SELECT username, real_name FROM cnc_platform.sys_user WHERE id = 1;",
    "-- Full mock (50 orders + bulk seed): mysql ... < backend/db/init_data.sql",
    "",
]

with open(OUT, "w", encoding="utf-8") as f:
    f.write("\n".join(parts))

print(f"Wrote {OUT} ({os.path.getsize(OUT) // 1024} KB)")
print(f"init.baseline.sql: {os.path.getsize(BASELINE) // 1024} KB")