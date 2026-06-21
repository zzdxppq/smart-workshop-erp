#!/usr/bin/env python3
"""Sync embedded migration blocks in init.baseline.sql from migrations/*.sql."""
import re
from pathlib import Path

DB = Path(__file__).resolve().parent.parent
baseline_path = DB / "init.baseline.sql"
migrations_dir = DB / "migrations"

NAMES = [
    "V5__crm_order.sql",
    "V6__drawing.sql",
    "V7__drawing_conversion.sql",
    "V36__profit_analysis.sql",
]

baseline = baseline_path.read_text(encoding="utf-8")
include_re = re.compile(r"^-- include: (.+\.sql)", re.M)

# Process from bottom to top so string offsets stay valid.
pairs: list[tuple[str, int, int]] = []
markers = list(include_re.finditer(baseline))
positions = [m.start() for m in markers] + [len(baseline)]
for i, m in enumerate(markers):
    pairs.append((m.group(1).strip(), m.start(), positions[i + 1]))

updated = 0
for name, pos, end in reversed(pairs):
    if name not in NAMES:
        continue
    mig = migrations_dir / name
    if not mig.exists():
        raise SystemExit(f"migration missing: {name}")
    block_start = baseline.index("\n", pos) + 1
    new_body = mig.read_text(encoding="utf-8").strip() + "\n"
    baseline = baseline[:block_start] + new_body + baseline[end:]
    updated += 1

baseline_path.write_text(baseline, encoding="utf-8")
print(f"Synced {updated} include blocks in {baseline_path}")
