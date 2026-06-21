#!/usr/bin/env python3
"""Generate v140-performance-mock-seed.sql"""
from datetime import date, timedelta
from pathlib import Path

base = date(2026, 6, 19)
out = Path(__file__).with_name("v140-performance-mock-seed.sql")
lines = [
    "-- V1.4.0 · 近 30 日报工 + 扫码 + 绩效日聚合 mock",
    "USE `cnc_business`;",
    "",
    "DELETE FROM crm_production_report WHERE report_no LIKE 'RP-MOCK-%';",
    "DELETE FROM crm_production_scan WHERE scan_no LIKE 'PS-MOCK-%';",
    "DELETE FROM crm_employee_performance_daily WHERE stat_date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY);",
    "",
]
operators = [(1, "操作工#1"), (2, "操作工#2"), (5, "操作工#5")]
workorders = ["GD-20260615-0001", "GD-20260615-0002", "GD20260612-0001", "GD20260612-0002"]
machines = [1, 2, 3, 6, 7, 12]
machine_codes = {1: "SB-CNC-001", 2: "SB-CNC-002", 3: "SB-CNC-003", 6: "SB-LATHE-001", 7: "SB-LATHE-002", 12: "SB-MILL-002"}
report_vals, scan_vals, daily_vals = [], [], []
seq_r = seq_s = 1
for d in range(30):
    stat = base - timedelta(days=29 - d)
    stat_s = stat.isoformat()
    for oi, (op_id, op_name) in enumerate(operators):
        wo = workorders[(d + oi) % len(workorders)]
        qty = 12 + (d % 7) * 3 + oi * 2
        abnormal = 1 if (d + oi) % 6 == 0 else 0
        qual = 0 if abnormal else qty
        scrap = qty if abnormal else 0
        mins = 90 + (d % 5) * 15 + oi * 10
        std = 100 + oi * 5
        report_vals.append(
            f"('RP-MOCK-{stat_s.replace('-', '')}-{seq_r:04d}', '{wo}', 1, {qty}, {mins}, {abnormal}, {op_id}, '{stat_s} 10:{(oi*15)%60:02d}:00')"
        )
        seq_r += 1
        mid = machines[(d + oi) % len(machines)]
        scan_vals.append(
            f"('PS-MOCK-{stat_s.replace('-', '')}-{seq_s:04d}', '{wo}', 'REPORT', {op_id}, {mid}, {qty}, 1, '{stat_s} 10:{(oi*15)%60:02d}:00')"
        )
        seq_s += 1
        pass_rate = round(qual / qty, 4) if qty else 0
        util = round(mins / std, 4) if std else 0
        score = round(40 * min(qty / 40, 1) * 100 / 100 + 30 * pass_rate * 100 + 30 * util * 100, 1)
        grade = "A" if score >= 90 else "B" if score >= 80 else "C" if score >= 70 else "D"
        daily_vals.append(
            f"('{stat_s}', {op_id}, '{op_name}', NULL, NULL, {qty}, {qual}, {scrap}, {mins}, {std}, {util}, {pass_rate}, {score}, '{grade}')"
        )
    for mi, mid in enumerate(machines[:4]):
        mqty = 8 + (d % 5) * 2 + mi
        mcode = machine_codes.get(mid, f"SB-MOCK-{mid:02d}")
        pass_rate = 0.95
        util = round(0.75 + mi * 0.03, 4)
        score = round(40 * min(mqty / 30, 1) * 100 + 30 * pass_rate * 100 + 30 * util * 100, 1)
        grade = "A" if score >= 90 else "B" if score >= 80 else "C"
        daily_vals.append(
            f"('{stat_s}', NULL, NULL, {mid}, '{mcode}', {mqty}, {mqty}, 0, {mqty*12}, {mqty*14}, {util}, {pass_rate}, {score}, '{grade}')"
        )

lines.append("INSERT INTO crm_production_report (report_no, workorder_no, step_no, reported_qty, actual_minutes, is_abnormal, reported_by, reported_at) VALUES")
lines.append(",\n".join(report_vals) + ";")
lines.append("")
lines.append("INSERT INTO crm_production_scan (scan_no, workorder_no, scan_type, operator_user_id, equipment_id, qty, step_no, scanned_at) VALUES")
lines.append(",\n".join(scan_vals) + ";")
lines.append("")
lines.append("INSERT INTO crm_employee_performance_daily (stat_date, operator_id, operator_name, machine_id, machine_code, finished_qty, qualified_qty, scrap_qty, actual_minutes, std_minutes, utilization_rate, pass_rate, score, grade) VALUES")
lines.append(",\n".join(daily_vals) + ";")
lines.append("")
lines.append("INSERT IGNORE INTO `cnc_production`.`crm_production_report` SELECT * FROM `cnc_business`.`crm_production_report` WHERE report_no LIKE 'RP-MOCK-%';")
lines.append("INSERT IGNORE INTO `cnc_production`.`crm_production_scan` SELECT * FROM `cnc_business`.`crm_production_scan` WHERE scan_no LIKE 'PS-MOCK-%';")
lines.append("INSERT IGNORE INTO `cnc_production`.`crm_employee_performance_daily` SELECT * FROM `cnc_business`.`crm_employee_performance_daily` WHERE stat_date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY);")
out.write_text("\n".join(lines) + "\n", encoding="utf-8")
print(f"written {out} reports={len(report_vals)} daily={len(daily_vals)}")
