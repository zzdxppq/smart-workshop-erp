#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
生成 mock-bulk-seed.sql · 50 在途订单 + 50 员工 + 全链路 Mock
输出：backend/db/install/mock-bulk-seed.sql
"""
from __future__ import annotations

import os
from datetime import date, timedelta

OUT = os.path.join(os.path.dirname(__file__), "mock-bulk-seed.sql")

ORDER_COUNT = 50
EMPLOYEE_COUNT = 50
MACHINE_COUNT = 20
CUSTOMER_COUNT = 20
BASE_DATE = date(2026, 6, 1)

DEPARTMENTS = ["生产部", "品质部", "工程部", "采购部", "销售部", "财务部", "仓储部", "人事部"]
POSITIONS = {
    "生产部": ["CNC 操作员", "车工", "铣工", "班组长"],
    "品质部": ["质检员", "IQC 专员"],
    "工程部": ["工艺工程师", "编程员"],
    "采购部": ["采购员", "采购主管"],
    "销售部": ["业务员", "销售经理"],
    "财务部": ["会计", "出纳"],
    "仓储部": ["仓管员", "叉车工"],
    "人事部": ["HR 专员", "薪酬专员"],
}
SURNAMES = "赵钱孙李周吴郑王冯陈褚卫蒋沈韩杨朱秦尤许何吕施张孔曹严华金魏陶姜"
GIVEN = "伟芳娜强静磊军敏婷洋勇艳杰娟涛明超秀霞平刚桂英华玉萍红丽"

# 产品命名对齐整改 5.1 中 PRD 的演示产品族：
#   法兰盘 / 连接器外壳 / 液压阀体 / 齿轮箱 / 轴承座 / 轴套 / 支架 / 端盖 / 联轴器 / 导轨
# 热门搜索关键词可命中其中前 3 个
MATERIALS = [
    ("WL-A001", "精密法兰盘 A 型",   "DN80 / φ120",      1280.0),
    ("WL-A002", "液压阀体 B 型",     "DN50 / 16MPa",      980.0),
    ("WL-A003", "航空连接器外壳 C",   "φ60×45 航空铝",    1560.0),
    ("WL-A004", "齿轮箱壳体 D 型",   "HT250 / 250×180",    680.0),
    ("WL-A005", "精密轴套 E",        "40Cr / φ45×80",      420.0),
    ("WL-A006", "设备支架 F",         "Q235 / 200×150",     890.0),
    ("WL-A007", "齿轮端盖 G",         "45# 钢",             720.0),
    ("WL-A008", "电机底座 H",         "HT200 / 320×260",   1100.0),
    ("WL-A009", "不锈钢滑块 I",      "304 / 80×60",        1350.0),
    ("WL-A010", "直线导轨 J",         "20mm / HGR20",      1680.0),
]
EQUIP_TYPES = ["CNC", "LATHE", "MILLING"]
# 工单状态分布（DRAFT/SCHEDULED/IN_PROGRESS/COMPLETED/CANCELLED）：
#   60% IN_PROGRESS（活跃进行中 - visitor 默认视图主力）
#   20% SCHEDULED（已排产待开工）
#   10% COMPLETED（已完成）
#   10% DRAFT（草稿）
# 让 visitor/progress 默认视图有 23+ 条活跃订单
WO_STATUS_POOL = (
    ["IN_PROGRESS"] * 30   # 50 单中 30 单 = 60% 在产
    + ["SCHEDULED"] * 10   # 10 单 = 20% 已排产
    + ["COMPLETED"] * 5    # 5 单 = 10% 已完成
    + ["DRAFT"] * 5        # 5 单 = 10% 草稿
)
OUTSOURCE_STATUSES = ["DRAFT", "SENT", "ACCEPTED", "IN_PRODUCTION", "INSPECTED", "COMPLETED"]


def esc(s: str) -> str:
    return s.replace("'", "''")


def name_for(i: int) -> str:
    return SURNAMES[i % len(SURNAMES)] + GIVEN[(i * 3) % len(GIVEN)]


def lines() -> list[str]:
    out: list[str] = []
    out.append("-- ============================================================")
    out.append("-- Mock 批量演示数据 · 50 在途订单 + 50 员工 + 全链路")
    out.append("-- 由 tools/generate-mock-bulk-data.py 生成，勿手改")
    out.append("-- ============================================================")
    out.append("")
    out.append("SET NAMES utf8mb4;")
    out.append("SET FOREIGN_KEY_CHECKS = 0;")
    out.append("USE `cnc_business`;")
    out.append("")

    # ---------- 物料 ----------
    out.append("-- ---------- 物料主数据（10 个成品循环） ----------")
    mat_vals = []
    for code, name, spec, cost in MATERIALS:
        mat_vals.append(
            f"('{code}', '{esc(name)}', '{esc(spec)}', 'PCS', 1, "
            f"{cost * 0.6:.2f}, {cost * 0.15:.2f}, {cost * 0.1:.2f}, {cost * 0.05:.2f}, 0.00, {cost * 0.85:.2f})"
        )
    out.append(
        "INSERT IGNORE INTO `crm_material` (`material_code`, `material_name`, `spec`, `unit`, `category_id`, "
        "`cost_material`, `cost_labor`, `cost_machine`, `cost_overhead`, `cost_outsource`, `cost_total`) VALUES\n"
        + ",\n".join(mat_vals)
        + ";"
    )
    out.append("")

    # ---------- 客户 ----------
    out.append("-- ---------- 20 个演示客户 ----------")
    out.append("DELETE FROM `crm_customer` WHERE id BETWEEN 1101 AND 1120;")
    cust_vals = []
    for i in range(1, CUSTOMER_COUNT + 1):
        cid = 1100 + i
        cust_vals.append(
            f"({cid}, 'C-MOCK-{i:04d}', '{esc(f'演示客户{i:02d}有限公司')}', '机械制造', "
            f"{500000 + i * 10000:.2f}, 2, NULL, 'ACTIVE')"
        )
    out.append(
        "INSERT INTO `crm_customer` (`id`, `customer_code`, `name`, `industry`, `credit_limit`, `owner_id`, `protect_until`, `status`) VALUES\n"
        + ",\n".join(cust_vals)
        + ";"
    )
    out.append("")

    # ---------- 清理旧 Mock 业务数据 ----------
    out.append("-- ---------- 清理旧 Mock 区间 ----------")
    out.append("DELETE FROM `crm_quality_inspection_item` WHERE inspection_id BETWEEN 12001 AND 12099;")
    out.append("DELETE FROM `crm_quality_inspection` WHERE id BETWEEN 12001 AND 12099;")
    out.append("DELETE FROM `crm_purchase_order_item` WHERE po_id BETWEEN 1102 AND 1160;")
    out.append("DELETE FROM `crm_purchase_order` WHERE id BETWEEN 1102 AND 1160;")
    out.append("DELETE FROM `outsub_allocation_vendor` WHERE allocation_id BETWEEN 5001 AND 5099;")
    out.append("DELETE FROM `outsub_allocation` WHERE id BETWEEN 5001 AND 5099;")
    out.append("DELETE FROM `crm_outsource_item` WHERE outsource_no LIKE 'WW-20260601-%';")
    out.append("DELETE FROM `crm_outsource_history` WHERE outsource_no LIKE 'WW-20260601-%';")
    out.append("DELETE FROM `crm_outsource_order` WHERE outsource_no LIKE 'WW-20260601-%';")
    out.append("DELETE FROM `crm_workorder_step` WHERE workorder_id BETWEEN 9103 AND 9160;")
    out.append("DELETE FROM `crm_production_schedule` WHERE workorder_id BETWEEN 9103 AND 9160;")
    out.append("DELETE FROM `crm_workorder` WHERE id BETWEEN 9103 AND 9160;")
    out.append("DELETE FROM `crm_order_item` WHERE order_id BETWEEN 7102 AND 7160;")
    out.append("DELETE FROM `crm_order_history` WHERE order_id BETWEEN 7102 AND 7160;")
    out.append("DELETE FROM `crm_order` WHERE id BETWEEN 7102 AND 7160;")
    out.append("DELETE FROM `crm_quote_item` WHERE quote_id BETWEEN 8102 AND 8160;")
    out.append("DELETE FROM `crm_quote` WHERE id BETWEEN 8102 AND 8160;")
    out.append("DELETE FROM `crm_hr_attendance` WHERE employee_no LIKE 'EM-MOCK-%';")
    out.append("DELETE FROM `crm_hr_payroll` WHERE employee_no LIKE 'EM-MOCK-%';")
    out.append("DELETE FROM `crm_hr_performance` WHERE employee_no LIKE 'EM-MOCK-%';")
    out.append("DELETE FROM `crm_hr_employee` WHERE employee_no LIKE 'EM-MOCK-%';")
    out.append("")

    # ---------- 50 订单全链路 ----------
    out.append(f"-- ---------- {ORDER_COUNT} 条在途订单（报价→订单→工单→委外→采购→质检） ----------")

    quote_rows, quote_item_rows = [], []
    order_rows, order_item_rows = [], []
    history_rows = []
    wo_rows, step_rows, sched_rows = [], [], []
    alloc_rows, oo_rows, oo_item_rows = [], [], []
    po_rows, po_item_rows = [], []
    insp_rows, insp_item_rows = [], []

    for i in range(1, ORDER_COUNT + 1):
        qid = 8101 + i
        oid = 7101 + i
        wid = 9102 + i
        cust_id = 1100 + ((i - 1) % CUSTOMER_COUNT) + 1
        mat = MATERIALS[(i - 1) % len(MATERIALS)]
        qty = 20 + (i * 7) % 180
        unit_price = mat[3]
        amount = round(qty * unit_price, 2)
        d = BASE_DATE + timedelta(days=i % 28)
        # 交付期分布：30% 近（7~14 天），40% 中（15~30 天），30% 远（45~60 天）
        if i % 10 < 3:
            delivery = d + timedelta(days=7 + i % 8)        # 近
        elif i % 10 < 7:
            delivery = d + timedelta(days=15 + i % 16)       # 中
        else:
            delivery = d + timedelta(days=45 + i % 16)       # 远
        # 计划完成日（schedule_end）：开工后 7~14 天完工
        from datetime import datetime as _dt
        sched_start_dt = _dt(2026, 6, 15) + timedelta(days=i % 10)
        sched_end_dt = sched_start_dt + timedelta(days=7 + i % 7)

        # 状态分布（与 WO_STATUS_POOL 对齐，确保 visitor-active 默认视图有 23+ 单）
        if i <= 5:
            ostatus, pno = "CONFIRMED", "NULL"
            wstatus = None
        elif i <= 40:
            ostatus = "PRODUCING"
            pno = f"'GD-20260601-{i:04d}'"
            wstatus = WO_STATUS_POOL[(i - 6) % len(WO_STATUS_POOL)]
        elif i <= 45:
            ostatus, pno = "PARTIAL_SHIPPED", f"'GD-20260601-{i:04d}'"
            wstatus = "IN_PROGRESS"
        else:
            ostatus, pno = "SHIPPED", f"'GD-20260601-{i:04d}'"
            wstatus = "COMPLETED"

        quote_rows.append(
            f"({qid}, 'BJ-20260615-{i:04d}', {cust_id}, '{esc(f'演示客户{(cust_id-1100):02d}有限公司')}', "
            f"2, 11, {amount:.2f}, '{delivery}', 'APPROVED', 99)"
        )
        quote_item_rows.append(
            f"({qid}, 'DWG-20260615-{i:04d}', '45#钢', '{esc(mat[2])}', {qty}, {unit_price:.2f}, {amount:.2f}, 1)"
        )
        order_rows.append(
            f"({oid}, 'XS-20260601-{i:04d}', {qid}, {cust_id}, '{esc(f'演示客户{(cust_id-1100):02d}有限公司')}', "
            f"2, 11, {amount:.2f}, '{delivery}', '{ostatus}', {pno})"
        )
        order_item_rows.append(
            f"({oid}, 'DWG-20260615-{i:04d}', '45#钢', {qty}, {unit_price:.2f}, {amount:.2f}, 1)"
        )
        history_rows.append(
            f"({oid}, 'CREATE', NULL, '{{\"status\":\"DRAFT\"}}', 2, '{d} 09:00:00')"
        )
        history_rows.append(
            f"({oid}, 'CONFIRM', NULL, '{{\"status\":\"CONFIRMED\"}}', 2, '{d} 10:00:00')"
        )
        if ostatus != "CONFIRMED":
            history_rows.append(
                f"({oid}, 'CONVERT_PROD', NULL, '{{\"productionOrderNo\":\"GD-20260601-{i:04d}\"}}', 5, '{d} 11:00:00')"
            )

        if wstatus:
            eq = EQUIP_TYPES[i % len(EQUIP_TYPES)]
            wo_rows.append(
                f"({wid}, 'GD-20260601-{i:04d}', '{mat[0]}', '{esc(mat[1])}', {min(qty, 100)}, "
                f"{1 + (i % 5)}, '{wstatus}', '{sched_start_dt.strftime('%Y-%m-%d %H:%M:%S')}', "
                f"'{sched_end_dt.strftime('%Y-%m-%d %H:%M:%S')}', {(i % MACHINE_COUNT) + 1}, '{eq}', "
                f"{6 + (i % 8)}.0, 5, 10, {oid}, 'XS-20260601-{i:04d}')"
            )

            # 5 工序：下料→CNC→精车→表面处理→检验
            # 根据工单状态分配各工序状态，让详情时间线更真实
            if wstatus == 'COMPLETED':
                step_statuses = ['COMPLETED', 'COMPLETED', 'COMPLETED', 'COMPLETED', 'COMPLETED']
            elif wstatus == 'IN_PROGRESS':
                # 进行中：部分已完成 + 当前在跑 + 后续待开始
                base = i % 5
                step_statuses = [
                    'COMPLETED' if base >= 1 else 'IN_PROGRESS',
                    'COMPLETED' if base >= 2 else 'IN_PROGRESS',
                    'COMPLETED' if base >= 3 else 'IN_PROGRESS',
                    'COMPLETED' if base >= 4 else 'IN_PROGRESS',
                    'COMPLETED' if base >= 5 else 'PENDING',
                ]
            elif wstatus == 'SCHEDULED':
                step_statuses = ['PENDING', 'PENDING', 'PENDING', 'PENDING', 'PENDING']
            else:  # DRAFT / CANCELLED
                step_statuses = ['PENDING', 'PENDING', 'PENDING', 'PENDING', 'PENDING']

            steps_def = [
                (1, '下料',       'BANDSAW', 60),
                (2, 'CNC 粗加工', 'CNC',    180),
                (3, '精车',       'LATHE',  150),
                (4, '表面处理',   'GENERAL', 90),
                (5, '成品检验',   'QC',     45),
            ]
            # 已完成工序注入完成时间；进行中注入 started_at
            step_lines = []
            cum_minutes = 0
            for (sno, sname, stype, smin), ss in zip(steps_def, step_statuses):
                started_at = sched_start_dt + timedelta(minutes=cum_minutes)
                completed_at = sched_start_dt + timedelta(minutes=cum_minutes + smin) if ss == 'COMPLETED' else None
                completed_sql = 'NULL' if completed_at is None else "'" + completed_at.strftime('%Y-%m-%d %H:%M:%S') + "'"
                started_sql = "'" + started_at.strftime('%Y-%m-%d %H:%M:%S') + "'"
                step_lines.append(
                    f"({wid}, {sno}, '{sname}', '{stype}', {smin}, '{ss}', {started_sql}, {completed_sql}, 5)"
                )
                cum_minutes += smin
            step_rows.append(",\n".join(step_lines))
            if wstatus in ("SCHEDULED", "IN_PROGRESS"):
                sched_rows.append(
                    f"('SCH-20260615-{i:04d}', {wid}, {(i % MACHINE_COUNT) + 1}, '{eq}', "
                    f"'{sched_start_dt.strftime('%Y-%m-%d %H:%M:%S')}', '{sched_end_dt.strftime('%Y-%m-%d %H:%M:%S')}', "
                    f"'{'IN_PROGRESS' if wstatus == 'IN_PROGRESS' else 'PLANNED'}')"
                )

            # 工序分配（约 40% 委外）
            if i % 5 <= 1 and ostatus == "PRODUCING":
                alloc_rows.append(
                    f"(5000 + {i}, {wid}, 1, 'OUTSOURCE', 5, DATE_SUB(NOW(), INTERVAL {i % 10} DAY))"
                )
                ww = f"WW-20260601-{i:04d}"
                oo_st = OUTSOURCE_STATUSES[i % len(OUTSOURCE_STATUSES)]
                oo_rows.append(
                    f"('{ww}', 'GD-20260601-{i:04d}', 1, {100 + (i % 5)}, '{esc('苏州外协厂' + str(i % 5))}', "
                    f"'表面处理', '{mat[0]}', {min(qty, 50)}, 25.00, {min(qty, 50) * 25:.2f}, "
                    f"'{delivery}', '{oo_st}', 9)"
                )
                oo_item_rows.append(
                    f"('{ww}', 1, '{mat[0]}', '{esc(mat[1])}', '{esc(mat[2])}', "
                    f"{min(qty, 50)}, 'PCS', 25.00, {min(qty, 50) * 25:.2f})"
                )

            # 采购单（约 30%）
            if i % 3 == 0 and ostatus in ("PRODUCING", "PARTIAL_SHIPPED"):
                po_id = 1101 + i
                po_amt = round(amount * 0.3, 2)
                po_st = "PENDING_SHIP" if i % 2 else "PARTIAL_ARRIVED"
                po_rows.append(
                    f"({po_id}, 'PO-20260401-{i:04d}', {100 + (i % 5)}, '{esc('东莞某CNC外协')}', "
                    f"{po_amt:.2f}, '{po_st}', 'FROM_ORDER', 'APPROVED', 'Mock 采购', 9)"
                )
                po_item_rows.append(
                    f"({po_id}, {po_id}, '{mat[0]}', '{esc(mat[1])}', {qty // 2}, 25.0000, {po_amt:.2f}, '{delivery}', 1)"
                )

            # 质检（IQC / IPQC）
            if i % 4 == 0:
                insp_id = 12000 + i
                itype = "IQC" if i % 8 == 0 else "IPQC"
                wo_id_sql = str(wid) if itype == "IPQC" else "NULL"
                wo_no_sql = f"'GD-20260601-{i:04d}'" if itype == "IPQC" else "NULL"
                proc_sql = "'CNC 粗加工'" if itype == "IPQC" else "NULL"
                insp_rows.append(
                    f"({insp_id}, 'QI-20260601-{i:04d}', '{itype}', NULL, '{mat[0]}', '{esc(mat[1])}', "
                    f"{wo_id_sql}, {wo_no_sql}, {proc_sql}, "
                    f"'WL-A001-BATCH-20260615-{i:04d}', {min(qty, 100)}, 5, 'AQL-1.0', '1.0', "
                    f"5, 4, 1, 20.00, 'PASSED', 8, NOW(), 'INFO', 0, 0, 'Mock 检验', 8)"
                )
                insp_item_rows.append(
                    f"({insp_id}, '外观检验', '无划伤', '合格', 'INFO', 1, NULL)"
                )

    out.append(
        "INSERT INTO `crm_quote` (`id`, `quote_no`, `customer_id`, `customer_name`, `owner_user_id`, "
        "`dept_id`, `total_amount`, `delivery_date`, `status`, `current_node`) VALUES\n"
        + ",\n".join(quote_rows) + ";"
    )
    out.append(
        "\nINSERT INTO `crm_quote_item` (`quote_id`, `drawing_no`, `material`, `spec`, `quantity`, `unit_price`, `amount`, `sort`) VALUES\n"
        + ",\n".join(quote_item_rows) + ";"
    )
    out.append(
        "\nINSERT INTO `crm_order` (`id`, `order_no`, `quote_id`, `customer_id`, `customer_name`, "
        "`owner_user_id`, `dept_id`, `total_amount`, `delivery_date`, `status`, `production_order_no`) VALUES\n"
        + ",\n".join(order_rows) + ";"
    )
    out.append(
        "\nINSERT INTO `crm_order_item` (`order_id`, `drawing_no`, `material`, `quantity`, `unit_price`, `amount`, `sort`) VALUES\n"
        + ",\n".join(order_item_rows) + ";"
    )
    out.append(
        "\nINSERT INTO `crm_order_history` (`order_id`, `operation`, `before_json`, `after_json`, `changed_by`, `changed_at`) VALUES\n"
        + ",\n".join(history_rows) + ";"
    )

    if wo_rows:
        out.append(
            "\nINSERT INTO `crm_workorder` (`id`, `workorder_no`, `material_code`, `product_name`, `qty`, "
            "`priority`, `status`, `scheduled_start`, `scheduled_end`, `equipment_id`, `equipment_type`, "
            "`estimated_hours`, `owner_user_id`, `dept_id`, `sales_order_id`, `sales_order_no`) VALUES\n"
            + ",\n".join(wo_rows) + ";"
        )
        out.append(
            "\nINSERT INTO `crm_workorder_step` (`workorder_id`, `step_no`, `step_name`, `equipment_type`, "
            "`estimated_minutes`, `status`, `started_at`, `completed_at`, `operator_user_id`) VALUES\n"
            + ",\n".join(step_rows) + ";"
        )
    if sched_rows:
        out.append(
            "\nINSERT INTO `crm_production_schedule` (`schedule_no`, `workorder_id`, `equipment_id`, "
            "`equipment_type`, `plan_start`, `plan_end`, `status`) VALUES\n"
            + ",\n".join(sched_rows) + ";"
        )
    if alloc_rows:
        out.append(
            "\nINSERT INTO `outsub_allocation` (`id`, `workorder_id`, `process_seq`, `decision`, "
            "`decided_by_user_id`, `decided_at`) VALUES\n" + ",\n".join(alloc_rows) + ";"
        )
    if oo_rows:
        out.append(
            "\nINSERT INTO `crm_outsource_order` (`outsource_no`, `workorder_no`, `step_no`, `supplier_id`, "
            "`supplier_name`, `process_name`, `material_code`, `qty`, `unit_price`, `total_amount`, "
            "`delivery_date`, `status`, `creator_user_id`) VALUES\n" + ",\n".join(oo_rows) + ";"
        )
        out.append(
            "\nINSERT INTO `crm_outsource_item` (`outsource_no`, `item_seq`, `material_code`, `material_name`, "
            "`spec`, `qty`, `unit`, `unit_price`, `total_amount`) VALUES\n" + ",\n".join(oo_item_rows) + ";"
        )
    if po_rows:
        out.append(
            "\nINSERT INTO `crm_purchase_order` (`id`, `po_no`, `supplier_id`, `supplier_name`, `total_amount`, "
            "`status`, `source_type`, `approval_status`, `remark`, `created_by`) VALUES\n"
            + ",\n".join(po_rows) + ";"
        )
        out.append(
            "\nINSERT INTO `crm_purchase_order_item` (`po_id`, `purchase_order_id`, `material_code`, "
            "`material_name`, `quantity`, `unit_price`, `amount`, `delivery_date`, `sort_no`) VALUES\n"
            + ",\n".join(po_item_rows) + ";"
        )
    if insp_rows:
        out.append(
            "\nINSERT INTO `crm_quality_inspection` (`id`, `inspection_no`, `inspect_type`, `material_id`, "
            "`material_code`, `material_name`, `work_order_id`, `work_order_no`, `process_name`, `batch_no`, "
            "`lot_size`, `sample_size`, `sample_rule`, `aql_level`, `inspect_qty`, `passed_qty`, `failed_qty`, "
            "`defect_rate`, `result`, `inspector_user_id`, `inspected_at`, `max_severity`, `trigger_rework`, "
            "`trigger_stockin`, `remark`, `created_by`) VALUES\n" + ",\n".join(insp_rows) + ";"
        )
        out.append(
            "\nINSERT INTO `crm_quality_inspection_item` (`inspection_id`, `item_name`, `standard`, "
            "`measured_value`, `severity`, `passed`, `defect_desc`) VALUES\n"
            + ",\n".join(insp_item_rows) + ";"
        )

    out.append("")

    # ---------- 50 员工 + 考勤 + 薪酬 ----------
    out.append(f"-- ---------- {EMPLOYEE_COUNT} 员工 · 考勤 · 薪酬 ----------")
    emp_rows, att_parts, pay_rows, perf_rows = [], [], [], []
    for i in range(1, EMPLOYEE_COUNT + 1):
        dept = DEPARTMENTS[i % len(DEPARTMENTS)]
        pos = POSITIONS[dept][i % len(POSITIONS[dept])]
        base = 5500 + (i % 10) * 500
        hire = BASE_DATE - timedelta(days=30 * (i % 24))
        emp_rows.append(
            f"('EM-MOCK-{i:04d}', '{esc(name_for(i))}', '{dept}', '{pos}', "
            f"'138{10000000 + i:08d}', 'emp{i}@mock.local', '{hire}', 'ACTIVE', {base:.2f})"
        )
        for day in range(5):
            dstr = (BASE_DATE + timedelta(days=day)).strftime("%Y-%m-%d")
            att_parts.append(
                f"((SELECT id FROM crm_hr_employee WHERE employee_no='EM-MOCK-{i:04d}'), "
                f"'EM-MOCK-{i:04d}', 'IN', '{dstr} 08:{30 + i % 29:02d}:00')"
            )
            att_parts.append(
                f"((SELECT id FROM crm_hr_employee WHERE employee_no='EM-MOCK-{i:04d}'), "
                f"'EM-MOCK-{i:04d}', 'OUT', '{dstr} 17:{30 + i % 29:02d}:00')"
            )
        ot = round((i % 12) * 0.5, 1)
        ot_pay = round(base / 21.75 / 8 * 1.5 * ot, 2)
        bonus = 100 + (i % 5) * 50
        ded = 50 + (i % 3) * 20
        tax = round(max(0, (base + ot_pay + bonus - ded - 5000) * 0.03), 2)
        net = round(base + ot_pay + bonus - ded - tax, 2)
        st = "APPROVED" if i % 3 else "DRAFT"
        pay_rows.append(
            f"('PY-MOCK-{i:04d}', 2026, 6, (SELECT id FROM crm_hr_employee WHERE employee_no='EM-MOCK-{i:04d}'), "
            f"'EM-MOCK-{i:04d}', '{esc(name_for(i))}', {base:.2f}, {ot:.1f}, {ot_pay:.2f}, "
            f"{bonus:.2f}, {ded:.2f}, {tax:.2f}, {net:.2f}, '{st}')"
        )
        grade = ["A", "B", "B", "C", "A"][i % 5]
        score = 85 + (i % 15)
        perf_rows.append(
            f"((SELECT id FROM crm_hr_employee WHERE employee_no='EM-MOCK-{i:04d}'), "
            f"'EM-MOCK-{i:04d}', '{esc(name_for(i))}', 2026, 6, {score}.00, '{grade}', "
            f"'{{\"产量\":{score},\"质量\":{score-5}}}')"
        )

    out.append(
        "INSERT INTO `crm_hr_employee` (`employee_no`, `name`, `department`, `position`, `phone`, "
        "`email`, `hire_date`, `status`, `base_salary`) VALUES\n" + ",\n".join(emp_rows) + ";"
    )
    out.append(
        "\nINSERT INTO `crm_hr_attendance` (`employee_id`, `employee_no`, `clock_type`, `clock_at`) VALUES\n"
        + ",\n".join(att_parts) + ";"
    )
    out.append(
        "\nINSERT INTO `crm_hr_payroll` (`payroll_no`, `period_year`, `period_month`, `employee_id`, "
        "`employee_no`, `employee_name`, `base_salary`, `overtime_hours`, `overtime_pay`, `bonus`, "
        "`deduction`, `tax`, `net_salary`, `status`) VALUES\n" + ",\n".join(pay_rows) + ";"
    )
    out.append(
        "\nINSERT INTO `crm_hr_performance` (`employee_id`, `employee_no`, `employee_name`, "
        "`period_year`, `period_month`, `score`, `grade`, `kpi_items`) VALUES\n"
        + ",\n".join(perf_rows) + ";"
    )

    out.append("")
    out.append("SET FOREIGN_KEY_CHECKS = 1;")
    out.append("")
    out.append("SELECT '=== Mock 批量数据统计 ===' AS info;")
    out.append("SELECT COUNT(*) AS mock_orders FROM crm_order WHERE order_no LIKE 'XS-20260601-%';")
    out.append("SELECT COUNT(*) AS mock_workorders FROM crm_workorder WHERE workorder_no LIKE 'GD-20260601-%';")
    out.append("SELECT COUNT(*) AS mock_employees FROM crm_hr_employee WHERE employee_no LIKE 'EM-MOCK-%';")
    out.append("SELECT COUNT(*) AS mock_outsource FROM crm_outsource_order WHERE outsource_no LIKE 'WW-20260601-%';")
    out.append("SELECT COUNT(*) AS mock_po FROM crm_purchase_order WHERE po_no LIKE 'PO-20260401-%';")
    out.append("SELECT COUNT(*) AS mock_inspection FROM crm_quality_inspection WHERE inspection_no LIKE 'QI-20260601-%';")
    return out


def main() -> None:
    content = "\n".join(lines()) + "\n"
    with open(OUT, "w", encoding="utf-8", newline="\n") as f:
        f.write(content)
    kb = len(content.encode("utf-8")) / 1024
    print(f"Wrote {OUT} ({kb:.1f} KB)")


if __name__ == "__main__":
    main()
