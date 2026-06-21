#!/usr/bin/env python3
"""Strip business mock seed blocks from Flyway migration SQL files."""
from __future__ import annotations

import re
from pathlib import Path

DB = Path(__file__).resolve().parent.parent
MIGRATIONS = DB / "migrations"
MOCK_NOTE = "-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql"

SEED_START = re.compile(
    r"^-- .*?(?:"
    r"[Ss]eed\b|"
    r"种子|"
    r"演示 PO|"
    r"演示账号|"
    r"30\+ 客户种子|"
    r"示例订单|"
    r"标注历史：|"
    r"工程师工作量：|"
    r"\d+\)\s*[Ss]eed|"
    r"\d+\.\s*Seed|"
    r"^-- seed\b"
    r")",
    re.I,
)

RESUME = re.compile(
    r"^("
    r"-- include:|"
    r"-- =+|"
    r"-- -+|"
    r"-- V94 · mock|"
    r"CREATE |"
    r"ALTER |"
    r"DROP |"
    r"USE `|"
    r"SET @|"
    r"-- \d+ 条 UI|"
    r"SELECT 'V|"
    r"INSERT INTO `sys_|"
    r"INSERT INTO sys_|"
    r"INSERT IGNORE INTO `sys_|"
    r"INSERT IGNORE INTO sys_|"
    r"INSERT IGNORE INTO `sys_role`|"
    r"INSERT IGNORE INTO `sys_menu`|"
    r"DELETE FROM `sys_|"
    r"DELETE ur FROM `sys_|"
    r"UPDATE `sys_|"
    r"UPDATE sys_|"
    r"PREPARE |"
    r"-- ---------- \d"
    r")",
    re.I,
)

DEMO_FILE_STUB = {
    "V78__operator_hr_demo_seed.sql": """-- V78 · operator 演示 HR 种子（init.sql 无 mock · 见 init_data.sql）
USE `cnc_business`;
""",
    "V61__crm_purchase_order_item.sql": """-- V61 · 厂商/合同/PO 演示种子（init.sql 无 mock · 见 init_data.sql）
USE `cnc_business`;
""",
}

BUSINESS_INSERT = re.compile(
    r"^INSERT (?:IGNORE )?INTO `(?!sys_|cnc_platform\.sys_|cnc_platform`.`sys_)",
    re.I,
)


def strip_content(text: str, filename: str) -> str:
    if filename in DEMO_FILE_STUB:
        return DEMO_FILE_STUB[filename]

    out: list[str] = []
    skipping = False
    note_written = False

    for line in text.splitlines():
        stripped = line.rstrip()

        if stripped.startswith("-- V94 · mock 清理"):
            if skipping:
                skipping = False
                note_written = False
            out.append(stripped)
            continue

        if not skipping and SEED_START.match(stripped):
            skipping = True
            if not note_written:
                out.append(MOCK_NOTE)
                note_written = True
            continue

        if skipping:
            if RESUME.match(stripped):
                skipping = False
                note_written = False
                out.append(stripped)
            continue

        # strip 后残留的半截 INSERT（如 V64）
        if stripped.startswith("(`") or stripped.startswith("VALUES") or (
            stripped.startswith("('") and not stripped.startswith("('--")
        ):
            if not note_written:
                out.append(MOCK_NOTE)
                note_written = True
            continue

        # 无 Seed 标记的业务 INSERT（如 V78 残留、V4 客户字典若漏标）
        if BUSINESS_INSERT.match(stripped) and "sys_dict_type" not in stripped:
            # 允许 V3/V5 等保留 sys_dict 中非 CUSTOMER 演示数据需单独判断
            if "sys_dict" in stripped and "CUSTOMER_STATUS" not in stripped:
                out.append(stripped)
            elif "sys_dict" not in stripped:
                if not note_written:
                    out.append(MOCK_NOTE)
                    note_written = True
                continue
            else:
                if not note_written:
                    out.append(MOCK_NOTE)
                    note_written = True
                continue
        else:
            out.append(stripped)

    # 折叠连续 mock 注释
    body = "\n".join(out)
    body = re.sub(r"(?:-- V94 · mock 清理[^\n]*\n)+", MOCK_NOTE + "\n", body)
    return body.strip() + "\n"


def main() -> None:
    changed = 0
    for path in sorted(MIGRATIONS.glob("V*.sql")):
        original = path.read_text(encoding="utf-8")
        new = strip_content(original, path.name)
        if new != original:
            path.write_text(new, encoding="utf-8")
            changed += 1
            print(f"stripped: {path.name}")

    data_dir = MIGRATIONS / "data"
    if data_dir.exists():
        for path in sorted(data_dir.glob("V*.sql")):
            original = path.read_text(encoding="utf-8")
            new = strip_content(original, path.name)
            if new != original:
                path.write_text(new, encoding="utf-8")
                changed += 1
                print(f"stripped: data/{path.name}")

    print(f"Done. {changed} file(s) updated.")

if __name__ == "__main__":
    main()
