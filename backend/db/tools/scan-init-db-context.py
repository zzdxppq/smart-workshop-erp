#!/usr/bin/env python3
"""Scan init.sql for cross-database table access without USE or schema prefix."""
import re
from pathlib import Path

init = Path(__file__).resolve().parent.parent / "init.sql"
lines = init.read_text(encoding="utf-8").splitlines()

PLATFORM = re.compile(
    r"\b(sys_menu|sys_role_permission|sys_role|sys_user|sys_dict|sys_dict_type|"
    r"sys_param|sys_workflow|sys_dept|sys_file|sys_printer|sys_print_log|sys_approval)\b",
    re.I,
)
BUSINESS = re.compile(
    r"\b(crm_|outsub_|sales_|hr_employee|hr_payroll|hr_performance|hr_recruitment|"
    r"purchase_request|purchase_order|rfq_|reconcile_|cost_accounting|profit_analysis|"
    r"payment_|receivable_|payable_|vendor_|crm_batch|crm_drawing|crm_material|"
    r"crm_material_barcode|warehouse_)\w*",
    re.I,
)
PRODUCTION = re.compile(r"\b(prod_|machine_status|machine_maintenance|work_order_process)\w*", re.I)

current_db = None
in_block = ""
issues: list[tuple] = []

for i, line in enumerate(lines, 1):
    m = re.match(r"USE\s+`(\w+)`", line.strip(), re.I)
    if m:
        current_db = m.group(1)
        continue
    if line.strip().startswith("-- include:"):
        in_block = line.strip()
        continue
    stripped = line.strip()
    if not stripped or stripped.startswith("--") or "information_schema" in stripped:
        continue

    def check(pattern, expected, kind):
        if pattern.search(stripped) and current_db not in (None, expected):
            if not re.search(rf"{expected}\.", stripped):
                issues.append((i, current_db, kind, in_block, stripped[:140]))

    check(PLATFORM, "cnc_platform", "platform")
    check(BUSINESS, "cnc_business", "business")
    check(PRODUCTION, "cnc_production", "production")

seen: set[tuple] = set()
for line_no, db, kind, block, sql in issues:
    key = (db, kind, block, sql[:80])
    if key in seen:
        continue
    seen.add(key)
    print(f"L{line_no} db={db} expect={kind} | {block}")
    print(f"  {sql}\n")

print(f"Total unique issues: {len(seen)}")
