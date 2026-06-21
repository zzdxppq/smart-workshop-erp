#!/usr/bin/env python3
"""Merge init.baseline.sql DDL + seed/migrations + V2.1 menu block into init.sql."""
import sys
from pathlib import Path

DB = Path(__file__).resolve().parent.parent
baseline_path = DB / "init.baseline.sql"
init_path = DB / "init.sql"
migrations_dir = DB / "migrations"

baseline = baseline_path.read_text(encoding="utf-8")

v21_path = migrations_dir / "V94__v21_menu_role_permission.sql"
if not v21_path.exists():
    raise SystemExit("V94__v21_menu_role_permission.sql not found")
v21_block = v21_path.read_text(encoding="utf-8").strip()

user_marker = "INSERT INTO `sys_user`"
ddl_end = baseline.index(user_marker)
ddl_end = baseline.rfind("--", 0, ddl_end)
try:
    footer_idx = baseline.index("SET FOREIGN_KEY_CHECKS = 1;", ddl_end)
except ValueError:
    footer_idx = len(baseline)
ddl = baseline[:ddl_end].strip()
idx = ddl.find("CREATE DATABASE IF NOT EXISTS `cnc_platform`")
if idx > 0:
    ddl = ddl[idx:].lstrip()
seed = baseline[ddl_end:footer_idx].strip()

header = """-- ============================================================
-- 昆山佰泰胜专属 ERP · 数据库一键初始化（空库专用 · 无 mock）
-- 库名：cnc_platform / cnc_business / cnc_production
-- 用法：mysql -h HOST -u USER -p --default-character-set=utf8mb4 < backend/db/init.sql
-- 维护：改 init.baseline.sql 或 migrations 后执行 tools/merge-init.py
-- 含全量 Mock（50 订单 + 50 员工 + 演示工单）：mysql ... < backend/db/init_data.sql
-- ============================================================

SET NAMES utf8mb4;
SET collation_connection = 'utf8mb4_unicode_ci';
SET FOREIGN_KEY_CHECKS = 0;
"""

skip = {
    "V58__drawing_link_partial_index.sql",
    "V60__cnc_production_schema.sql",
    "V60a__cnc_production_schema.sql",
    "V60b__cnc_production_data.sql",
    "V94__employee_position_dict.sql",
    "V94__v21_menu_role_permission.sql",
}

# 须在 V2.1 菜单块（V94）之后执行，避免 INSERT 覆盖 UPDATE
post_v21 = {
    "V99__sales_menu_hr_employee_no.sql",
    "V100__contract_no_align_order.sql",
    "V101__finance_order_receipt_menu.sql",
    "V102__ensure_crm_purchase_order_before_alter.sql",
    "V103__ensure_crm_hr_payroll.sql",
    "V104__quality_ipqc_source_merge_outsource.sql",
    "V105__production_menu_restructuring.sql",
    "V106__warehouse_menu_consolidation.sql",
    "V107__audit_log_menu.sql",
}

PLATFORM_TABLES = (
    "sys_menu",
    "sys_role_permission",
    "sys_role",
    "sys_user",
    "sys_user_role",
    "sys_dict",
    "sys_dict_type",
)


def ensure_platform_use(sql: str) -> str:
    """post_v21 块若操作 platform 表且未显式 USE，自动补上 cnc_platform。"""
    lower = sql.lower()
    if "use `" in lower:
        return sql
    if not any(t in lower for t in PLATFORM_TABLES):
        return sql
    body = sql.strip()
    if body.startswith("--"):
        first_nl = body.find("\n")
        header = body[: first_nl + 1] if first_nl >= 0 else ""
        rest = body[first_nl + 1 :] if first_nl >= 0 else body
        return f"{header}\nUSE `cnc_platform`;\n\n{rest.lstrip()}"
    return f"USE `cnc_platform`;\n\n{body}"


def migration_version(name: str) -> int:
    if name.startswith("V") and "__" in name:
        num = name[1 : name.index("__")]
        digits = "".join(c for c in num if c.isdigit())
        return int(digits) if digits else 99999
    return 99999


extra_parts: list[str] = []
post_v21_parts: list[str] = []
for p in sorted(migrations_dir.glob("V*.sql"), key=lambda x: migration_version(x.name)):
    if p.name in skip:
        continue
    if f"-- include: {p.name}" in baseline:
        continue
    body = p.read_text(encoding="utf-8").strip()
    block = f"\n-- include: {p.name}\n{body}"
    if p.name in post_v21:
        post_v21_parts.append(f"\n-- include: {p.name}\n{ensure_platform_use(body)}")
    else:
        extra_parts.append(block)

v62_name = "V62__prod_machine_and_workorder_process.sql"
if v62_name not in baseline and any(v62_name in part for part in extra_parts):
    v58 = migrations_dir / "V58__drawing_link_partial_index.sql"
    if v58.exists() and f"-- include: {v58.name}" not in baseline:
        extra_parts.append(
            f"\n-- include: {v58.name} (post-V62 indexes)\n{v58.read_text(encoding='utf-8').strip()}"
        )
    v60a = migrations_dir / "V60a__cnc_production_schema.sql"
    if v60a.exists():
        extra_parts.append(
            f"\n-- include: {v60a.name} (post-V62 schema)\n{v60a.read_text(encoding='utf-8').strip()}"
        )

v60a_footer = migrations_dir / "V60a__cnc_production_schema.sql"
footer_extra = ""
if v60a_footer.exists():
    footer_extra = (
        f"\n-- include: {v60a_footer.name} (footer ensure)\n"
        f"{v60a_footer.read_text(encoding='utf-8').strip()}"
    )

footer = """
SET FOREIGN_KEY_CHECKS = 1;

SELECT 'cnc_platform' AS db, COUNT(*) AS table_count FROM information_schema.tables WHERE table_schema = 'cnc_platform'
UNION ALL SELECT 'cnc_business', COUNT(*) FROM information_schema.tables WHERE table_schema = 'cnc_business'
UNION ALL SELECT 'cnc_production', COUNT(*) FROM information_schema.tables WHERE table_schema = 'cnc_production';
SELECT username, real_name FROM cnc_platform.sys_user WHERE id = 1;
-- Full mock (50 orders + bulk seed): mysql ... < backend/db/init_data.sql
"""

parts = [header.strip(), ddl, seed, *extra_parts, footer_extra.strip(), v21_block, *post_v21_parts, footer.strip()]
out = "\n\n".join(p for p in parts if p) + "\n"
init_path.write_text(out, encoding="utf-8")
print(f"Wrote {init_path} ({len(out.splitlines())} lines, {len(out.encode())/1024:.1f} KB)")

validate_script = DB / "tools" / "validate-init-sql.py"
if validate_script.exists():
    import subprocess
    result = subprocess.run([sys.executable, str(validate_script)], capture_output=True, text=True)
    print(result.stdout.strip() or result.stderr.strip())
    if result.returncode != 0:
        raise SystemExit(result.returncode)
