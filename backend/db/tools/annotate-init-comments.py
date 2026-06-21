#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""为 init.sql 中多行 CREATE TABLE 补齐 MySQL 表/字段 COMMENT。"""
from __future__ import annotations

import re
import sys
from pathlib import Path

COLUMN_DICT = {
    "id": "主键ID",
    "status": "状态",
    "remark": "备注",
    "comment": "备注说明",
    "created_at": "创建时间",
    "updated_at": "更新时间",
    "created_by": "创建人用户ID",
    "updated_by": "更新人用户ID",
    "deleted": "逻辑删除标记",
    "is_deleted": "是否删除(0否1是)",
    "sort": "排序号",
    "sort_no": "排序号",
    "version": "版本号",
    "type": "类型",
    "name": "名称",
    "code": "编码",
    "qty": "数量",
    "quantity": "数量",
    "amount": "金额",
    "total_amount": "总金额",
    "unit_price": "单价",
    "unit": "单位",
    "currency": "币种",
    "user_id": "用户ID",
    "owner_user_id": "负责人用户ID",
    "dept_id": "部门ID",
    "customer_id": "客户ID",
    "customer_name": "客户名称",
    "supplier_id": "供应商ID",
    "supplier_name": "供应商名称",
    "vendor_id": "厂商ID",
    "vendor_code": "厂商编码",
    "vendor_name": "厂商名称",
    "order_id": "订单ID",
    "order_no": "订单号",
    "quote_id": "报价单ID",
    "workorder_id": "工单ID",
    "workorder_no": "工单号",
    "material_id": "物料ID",
    "material_code": "物料编码",
    "material_name": "物料名称",
    "drawing_id": "图纸ID",
    "drawing_no": "图号",
    "bom_id": "BOM ID",
    "process_id": "工艺ID",
    "equipment_id": "设备ID",
    "machine_id": "机台ID",
    "po_id": "采购单ID",
    "po_no": "采购单号",
    "purchase_order_id": "采购订单ID",
    "rfq_id": "询价单ID",
    "contract_id": "合同ID",
    "contract_no": "合同号",
    "file_id": "文件ID",
    "biz_id": "业务单据ID",
    "biz_no": "业务单号",
    "biz_type": "业务类型",
    "ref_type": "关联业务类型",
    "ref_id": "关联业务ID",
    "ref_no": "关联业务单号",
    "source_type": "来源类型",
    "approval_status": "审批状态",
    "approval_route": "审批路由",
    "signed_at": "签约日期",
    "plan_date": "计划日期",
    "plan_amount": "计划金额",
    "actual_amount": "实际金额",
    "period_no": "期次",
    "receipt_date": "收款日期",
    "payer": "付款方/方式",
    "revenue": "收入",
    "profit": "利润",
    "profit_rate": "利润率(%)",
    "total_cost": "总成本",
    "cost_id": "成本核算单ID",
    "cost_no": "成本核算单号",
    "alert_level": "告警级别",
    "settled_date": "结算日期",
    "analysis_month": "分析月份",
    "segment_code": "成本段编码",
    "segment_name": "成本段名称",
    "delivery_date": "交期",
    "purchase_reason": "采购理由",
    "notify_channel": "通知渠道",
    "contact_email": "联系邮箱",
    "contact_phone": "联系电话",
    "contact_name": "联系人",
    "credit_level": "信用等级",
    "capabilities_json": "能力标签JSON",
    "step_no": "工序号",
    "step_name": "工序名称",
    "process_seq": "工序序号",
    "process_code": "工序编码",
    "process_name": "工序名称",
    "scan_type": "扫码类型",
    "scan_no": "扫码单号",
    "report_no": "报工单号",
    "transfer_no": "过站单号",
    "run_no": "运算单号",
    "outsource_no": "委外单号",
    "batch_no": "批次号",
    "barcode_no": "条码号",
    "workflow_code": "工作流编码",
    "event_type": "事件类型",
    "operator_user_id": "操作人用户ID",
    "operator_id": "操作人ID",
    "transferred_by": "过站操作人ID",
    "reported_by": "报工人ID",
    "triggered_by": "触发人用户ID",
    "started_at": "开始时间",
    "completed_at": "完成时间",
    "scanned_at": "扫码时间",
    "reported_at": "报工时间",
    "transferred_at": "过站时间",
    "sent_at": "发送时间",
    "plan_start": "计划开始",
    "plan_end": "计划结束",
    "scheduled_start": "排产开始时间",
    "scheduled_end": "排产结束时间",
    "actual_start": "实际开始时间",
    "actual_end": "实际结束时间",
    "estimated_hours": "预计工时",
    "actual_hours": "实际工时",
    "estimated_minutes": "预计分钟",
    "actual_minutes": "实际分钟",
    "reported_qty": "报工数量",
    "priority": "优先级",
    "product_name": "产品名称",
    "product_code": "产品编码",
    "is_fa": "是否首件(0否1是)",
    "is_new": "是否新品(0否1是)",
    "is_urgent": "是否加急(0否1是)",
    "is_outsource": "是否委外(0否1是)",
    "is_abnormal": "是否异常(0否1是)",
    "quality_status": "质检状态",
    "po_item_id": "采购明细ID",
    "arrived_at": "到货时间",
    "channel": "渠道",
    "subject": "主题",
    "body": "正文",
    "attachment_path": "附件路径",
    "retry_count": "重试次数",
    "username": "登录名",
    "password_hash": "密码哈希",
    "real_name": "真实姓名",
    "phone": "手机号",
    "email": "邮箱",
    "role_code": "角色编码",
    "role_name": "角色名称",
    "data_scope": "数据权限范围",
    "amount_threshold": "金额阈值",
    "dept_name": "部门名称",
    "parent_id": "父级ID",
    "dict_type": "字典类型",
    "dict_code": "字典编码",
    "dict_label": "字典标签",
    "bucket": "存储桶",
    "object_key": "对象键",
    "original_name": "原始文件名",
    "size": "文件大小(字节)",
    "mime": "MIME类型",
    "uploader_id": "上传人ID",
    "encryption_meta": "加密元数据",
    "last_login_time": "最后登录时间",
    "position_name": "职位名称",
    "nodes_json": "审批节点JSON",
    "conditions_json": "条件JSON",
    "category_code": "分类编码",
    "category_name": "分类名称",
    "prefix": "前缀",
    "seq_no": "序号",
    "is_active": "是否启用(0否1是)",
    "spec": "规格",
    "category_id": "分类ID",
    "cost_material": "材料成本",
    "cost_labor": "人工成本",
    "cost_machine": "机台成本",
    "cost_overhead": "管理费用",
    "cost_outsource": "委外成本",
    "cost_total": "总成本",
    "client_id": "客户端ID",
    "sync_status": "同步状态",
    "abnormal_type": "异常类型",
    "abnormal_note": "异常说明",
    "from_step_no": "源工序号",
    "to_step_no": "目标工序号",
    "from_equipment_id": "源设备ID",
    "to_equipment_id": "目标设备ID",
    "run_type": "运算类型",
    "date_range_start": "日期范围起",
    "date_range_end": "日期范围止",
    "warehouse_ids": "仓库ID列表",
    "total_shortage": "缺料总数",
    "total_purchase_suggestion": "建议采购总量",
}

TOKEN_DICT = {
    "sys": "系统", "crm": "CRM", "mdm": "主数据", "sales": "销售", "outsub": "委外",
    "prod": "生产", "wms": "仓储", "qc": "质检", "po": "采购单", "rfq": "询价",
    "mrp": "MRP", "bom": "BOM", "vendor": "厂商", "order": "订单", "item": "明细",
    "workorder": "工单", "outsource": "委外", "rework": "返修", "scan": "扫码",
    "report": "报工", "station": "过站", "schedule": "排产", "batch": "批次",
    "material": "物料", "drawing": "图纸", "process": "工艺", "workflow": "工作流",
    "audit": "审计", "dict": "字典", "file": "文件", "user": "用户", "role": "角色",
    "dept": "部门", "payment": "回款", "receipt": "收款", "contract": "合同",
    "profit": "利润", "cost": "成本", "segment": "成本段", "inspection": "检验",
    "incoming": "来料", "allocation": "工序分配", "shortage": "缺料", "result": "结果",
    "run": "运算", "history": "历史", "alert": "告警", "quality": "质量",
    "defect": "不良", "eta": "交期预估", "actual": "实际", "state": "状态",
    "link": "关联", "permission": "权限", "printer": "打印机", "label": "标签",
    "employee": "员工", "payroll": "薪酬", "attendance": "考勤", "dashboard": "看板",
    "snapshot": "快照", "reconcile": "对账", "barcode": "条码", "warehouse": "仓库",
    "location": "库位", "inventory": "库存", "quote": "报价", "customer": "客户",
    "supplier": "供应商", "category": "分类", "shadow": "影子", "shadow": "影子",
}

TABLE_SUFFIX = {
    "log": "日志表", "item": "明细表", "history": "历史表", "plan": "计划表",
    "link": "关联表", "shadow": "影子表", "config": "配置表", "node": "节点表",
    "record": "记录表", "alert": "告警表", "segment": "明细表",
}


def humanize_token(token: str) -> str:
    if not token:
        return ""
    return TOKEN_DICT.get(token, token)


def guess_column_comment(col_name: str) -> str:
    if col_name in COLUMN_DICT:
        return COLUMN_DICT[col_name]
    if col_name.endswith("_id"):
        return f"{humanize_token(col_name[:-3])}ID"
    if col_name.endswith("_no"):
        return f"{humanize_token(col_name[:-3])}编号"
    if col_name.endswith("_at"):
        return f"{humanize_token(col_name[:-3])}时间"
    if col_name.endswith("_date"):
        return f"{humanize_token(col_name[:-5])}日期"
    if col_name.endswith("_name"):
        return f"{humanize_token(col_name[:-5])}名称"
    if col_name.endswith("_code"):
        return f"{humanize_token(col_name[:-5])}编码"
    if col_name.endswith("_amount"):
        return f"{humanize_token(col_name[:-7])}金额"
    if col_name.endswith("_qty"):
        return f"{humanize_token(col_name[:-4])}数量"
    if col_name.startswith("is_"):
        return f"是否{humanize_token(col_name[3:])}(0否1是)"
    return "".join(humanize_token(p) for p in col_name.split("_") if p)


def guess_table_comment(table_name: str, section_hints: list[str]) -> str:
    for hint in reversed(section_hints):
        h = hint.strip(" -=\t")
        if not h or h.startswith("#") or "include:" in h:
            continue
        if any(x in h for x in ("迁移", "Seed", "种子", "测例", "校验", "红线检查")):
            continue
        if "·" in h or "Story" in h or "表" in h or len(h) <= 60:
            return h if h.endswith("表") or "表" in h else f"{h}表"
    bare = table_name.strip("`").split(".")[-1]
    words = [humanize_token(p) for p in bare.split("_") if p]
    return f"{''.join(words)}表"


def extract_table_name(create_line: str) -> str:
    m = re.search(
        r"CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?(?:`?(?:[\w$]+)`?\.)?`?([\w$]+)`?",
        create_line,
        re.I,
    )
    return m.group(1) if m else "unknown_table"


def is_orphan_comment_line(line: str) -> bool:
    """Multi-line COMMENT '...' continuation from migration source (invalid after annotator)."""
    s = line.strip()
    return bool(re.match(r"^COMMENT\s+'", s, re.I)) and not re.match(r"^`?[\w$]+`?\s+", s)


def merge_multiline_column_comment(lines: list[str], start: int) -> tuple[str, int]:
    """Merge `col DEF\n    COMMENT '...'` into one line."""
    merged = lines[start].rstrip()
    j = start + 1
    while j < len(lines):
        nxt = lines[j].strip()
        if re.match(r"^COMMENT\s+'", nxt, re.I):
            m = re.search(r"COMMENT\s+'((?:[^'\\]|\\.)*)'", nxt, re.I)
            if m:
                detail = m.group(1).replace("'", "''")
                merged = merged.rstrip(",").rstrip() + f" COMMENT '{detail}',"
            j += 1
            continue
        break
    return merged, j


def is_constraint_line(line: str) -> bool:
    s = line.strip().upper()
    return s.startswith(("PRIMARY KEY", "UNIQUE KEY", "KEY ", "INDEX ", "CONSTRAINT ", "FOREIGN KEY", "CHECK "))


def has_comment(s: str) -> bool:
    return bool(re.search(r"\bCOMMENT\s*(?:'|=)", s, re.I))


def extract_inline_comment(line: str) -> tuple[str, str | None]:
    if has_comment(line):
        return line, None
    m = re.search(r"(.*?)(,\s*)--\s*(.+?)\s*$", line)
    if m:
        return m.group(1) + (m.group(2) or ""), m.group(3).strip()
    m2 = re.search(r"(.*?)\s+--\s*(.+?)\s*$", line)
    if m2 and not m2.group(1).strip().startswith("--"):
        return m2.group(1).rstrip(), m2.group(2).strip()
    return line, None


def inject_column_comment(line: str, comment: str) -> str:
    comment = comment.replace("'", "''")
    base = line.rstrip()
    if base.endswith(","):
        base = base[:-1].rstrip()
    return f"{base} COMMENT '{comment}',"


def process_engine_line(line: str, table_comment: str) -> str:
    if has_comment(line):
        return line
    if "ENGINE" not in line.upper():
        return line
    comment = table_comment.replace("'", "''")
    line = line.rstrip()
    had_semi = line.endswith(";")
    if had_semi:
        line = line[:-1]
    if re.search(r"ENGINE\s*=\s*InnoDB", line, re.I):
        return f"{line} COMMENT='{comment}';" if had_semi else f"{line} COMMENT='{comment}'"
    return line + (";" if had_semi else "")


def is_like_create(line: str) -> bool:
    return bool(re.search(r"CREATE\s+TABLE\b", line, re.I) and re.search(r"\bLIKE\b", line, re.I))


def annotate_sql(text: str) -> str:
    lines = text.splitlines()
    out: list[str] = []
    i = 0
    recent_section: list[str] = []

    while i < len(lines):
        line = lines[i]
        stripped = line.strip()

        if stripped.startswith("--") and "include:" not in stripped:
            recent_section.append(stripped.lstrip("- ").strip())
            if len(recent_section) > 10:
                recent_section.pop(0)

        if is_like_create(stripped):
            out.append(line)
            i += 1
            continue

        if re.match(r"^CREATE\s+TABLE\s+", stripped, re.I) and "(" in stripped:
            table_name = extract_table_name(stripped)
            table_comment = guess_table_comment(table_name, recent_section)
            block = [line]
            i += 1
            while i < len(lines):
                block.append(lines[i])
                if re.search(r"\)\s*ENGINE\s*=", lines[i], re.I):
                    break
                i += 1
            new_block = [block[0]]
            j = 1
            while j < len(block) - 1:
                bl = block[j]
                if is_orphan_comment_line(bl):
                    j += 1
                    continue
                if is_constraint_line(bl) or not bl.strip() or bl.strip().startswith("--"):
                    new_block.append(bl)
                    j += 1
                    continue
                merged, j = merge_multiline_column_comment(block, j)
                body, inline = extract_inline_comment(merged)
                if has_comment(body):
                    new_block.append(body if inline is None else inject_column_comment(body, inline))
                    continue
                col_m = re.match(r"^\s*`?([\w$]+)`?\s+", body)
                col_name = col_m.group(1) if col_m else ""
                comment = inline or guess_column_comment(col_name)
                new_block.append(inject_column_comment(body, comment))
            new_block.append(process_engine_line(block[-1], table_comment))
            out.extend(new_block)
            i += 1
            continue

        out.append(line)
        i += 1

    return "\n".join(out) + ("\n" if text.endswith("\n") else "")


def main() -> int:
    path = Path(sys.argv[1] if len(sys.argv) > 1 else Path(__file__).resolve().parent.parent / "init.sql")
    text = path.read_text(encoding="utf-8")
    # strip broken COMMENT artifacts from prior bad run
    text = re.sub(r";\s*COMMENT\s+'(?:CREATE|INSERT)',\s*$", ";", text, flags=re.M)
    annotated = annotate_sql(text)
    path.write_text(annotated, encoding="utf-8", newline="\n")
    tbl = len(re.findall(r"CREATE\s+TABLE\s+", annotated, re.I))
    like = len(re.findall(r"CREATE\s+TABLE\s+.*\bLIKE\b", annotated, re.I))
    tbl_c = len(re.findall(r"ENGINE\s*=\s*InnoDB[^;]*COMMENT\s*=", annotated, re.I))
    col_c = len(re.findall(r"COMMENT\s+'", annotated))
    bare_engine = len(re.findall(r"^\)\s*ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;\s*$", annotated, re.M))
    print(f"Annotated {path.name}: create_table={tbl}, like_clone={like}, table_comment={tbl_c}, column_comment={col_c}, bare_close={bare_engine}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
