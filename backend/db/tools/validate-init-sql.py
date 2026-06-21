#!/usr/bin/env python3
"""Validate init.sql: platform tables must run under cnc_platform."""
from __future__ import annotations

import re
import sys
from pathlib import Path

DB = Path(__file__).resolve().parent.parent
INIT = DB / "init.sql"

PLATFORM_TABLES = {
    "sys_menu",
    "sys_role",
    "sys_role_permission",
    "sys_user",
    "sys_user_role",
    "sys_dict",
    "sys_dict_type",
    "sys_workflow",
    "sys_workflow_node",
    "sys_param",
}

USE_RE = re.compile(r"USE\s+`(?P<db>[^`]+)`", re.I)
INCLUDE_RE = re.compile(r"--\s*include:\s*(V\d+__[^\s]+)", re.I)
TABLE_RE = re.compile(
    r"\b(?:FROM|INTO|UPDATE|JOIN|TABLE)\s+`?(?P<table>sys_[a-z_]+)`?",
    re.I,
)


def scan_init(path: Path) -> list[str]:
    lines = path.read_text(encoding="utf-8").splitlines()
    current_db = "cnc_platform"  # init starts with platform DDL
    block = "header"
    issues: list[str] = []

    for i, line in enumerate(lines, 1):
        m_inc = INCLUDE_RE.search(line)
        if m_inc:
            block = m_inc.group(1)

        m_use = USE_RE.search(line)
        if m_use:
            current_db = m_use.group("db")

        stripped = line.strip()
        if stripped.startswith("--") or not stripped:
            continue

        for tm in TABLE_RE.finditer(line):
            table = tm.group("table").lower()
            if table in PLATFORM_TABLES and current_db != "cnc_platform":
                issues.append(
                    f"L{i} [{block}] `{table}` while USE `{current_db}` (expected cnc_platform)"
                )

    return issues


def main() -> int:
    if not INIT.exists():
        print(f"Missing {INIT}", file=sys.stderr)
        return 1
    issues = scan_init(INIT)
    if issues:
        print(f"init.sql schema context issues ({len(issues)}):")
        for item in issues:
            print(f"  - {item}")
        return 1
    print(f"OK: {INIT.name} platform table context check passed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
