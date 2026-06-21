"""Draw CNC cutting-flow swimlane diagram.

6 lanes x 12 steps, with arrows, milestones, and a code-legend.
Output: docs/contract-swimlane-cutting.png
"""
from PIL import Image, ImageDraw, ImageFont
from pathlib import Path

# ---------- Config ----------
OUT = Path(r'E:\claude\smart-workshop-erp\docs\contract-swimlane-cutting.png')

FONT_REG = r'C:\Windows\Fonts\msyh.ttc'
FONT_BOLD = r'C:\Windows\Fonts\msyhbd.ttc'

# Canvas (wider for 6 columns)
W, H = 3200, 1900
HEADER_W = 240
N_COLS = 10
LANE_H = (H - 200 - 120) // 6  # bottom legend 120

# Colors
BG = '#FFFFFF'
HEADER_BG = '#1f2328'
LANE_BG_A = '#f7f9fc'
LANE_BG_B = '#eef3fb'
LANE_BORDER = '#cfd8e3'
STEP_BG = '#0969da'
STEP_FG = '#FFFFFF'
ARROW = '#57606a'
TITLE = '#1f2328'
EMPHASIS_BG = '#fff4e1'
EMPHASIS_BORDER = '#d97706'
DONE = '#1a7f37'

# 6 lanes: (label, bg)
LANES = [
    ('业务员', LANE_BG_A),
    ('工程师', LANE_BG_B),
    ('生管 / 排产', LANE_BG_A),
    ('采购 / 仓管', LANE_BG_B),
    ('下料工 / 操作工', LANE_BG_A),
    ('品检 / 财务', LANE_BG_B),
]

# 18 steps, each in its own (lane, col) - no two steps in same cell
STEPS = [
    # (lane, col, label, code, is_milestone)
    (0, 0, '客户询价\n业务员报价', 'BJ-', False),
    (0, 1, '合同/订单\n评审签订', 'XS-', False),
    (1, 2, '工艺拆分\nBOM + 工艺路线', 'BOM', False),
    (2, 3, 'MRP 计算\n缺料清单', 'MRP', False),
    (2, 4, '排产 / 派工\nGantt 拖拽', 'M2', True),
    (2, 5, '生成下料\n工单', 'GD-', True),
    (3, 0, '采购请购\n询比价', 'CG-', False),
    (3, 1, '采购订单\n厂商送货', 'PO-', False),
    (3, 3, '材料扫码\n入库', 'WL-', True),
    (3, 4, '下料领料\n扫码出库', 'WL-', False),
    (4, 5, '扫工单码\n开工', 'GD-', False),
    (4, 6, '下料切割\n录毛坯/损耗', 'LZ-', True),
    (4, 7, '扫码报工\n合格/报废', 'LZ-', False),
    (4, 8, '转下工序\nLZ- 流转', 'LZ-', True),
    (5, 3, '来料 FA\n品检双签', 'FA', False),
    (5, 6, '首件检验\n品检 + 工程师', 'FA', True),
    (5, 7, '工序接续\n监控损耗', 'QC', False),
    (5, 8, '报废率\n>10% 介入', 'QC', False),
    (5, 9, '成本归集\n料号成本', 'FI', True),
]

# Arrows: list of (from_step_index, to_step_index)
ARROWS = [
    (0, 1),     # 询价 -> 订单
    (1, 2),     # 订单 -> 工艺
    (2, 3),     # 工艺 -> MRP
    (3, 4),     # MRP -> 排产
    (4, 5),     # 排产 -> 工单
    (6, 7),     # 请购 -> 采购单
    (7, 8),     # 采购单 -> 入库
    (8, 9),     # 入库 -> 领料
    (9, 10),    # 领料 -> 扫工单码
    (10, 11),   # 扫工单码 -> 下料
    (11, 12),   # 下料 -> 报工
    (12, 13),   # 报工 -> 转下工序
    (8, 14),    # 入库 -> FA 来料检
    (11, 15),   # 下料 -> FA 首件
    (11, 16),   # 下料 -> 工序接续
    (12, 17),   # 报工 -> 报废率监控
    (13, 18),   # 转下工序 -> 成本归集
    (5, 11),    # 工单 -> 下料
]

# Step dimensions
STEP_W = 220
STEP_H = 90


def get_pos(lane_idx, col):
    """Return top-left (x, y) of a step box."""
    x = HEADER_W + col * ((W - HEADER_W) / N_COLS) + ((W - HEADER_W) / N_COLS - STEP_W) / 2
    y = 200 + lane_idx * LANE_H + (LANE_H - STEP_H) / 2
    return x, y


def get_center(lane_idx, col):
    x, y = get_pos(lane_idx, col)
    return x + STEP_W / 2, y + STEP_H / 2


def draw_arrow(p1, p2, color='#1a7f37', width=4):
    d.line([p1, p2], fill=color, width=width)
    import math
    dx = p2[0] - p1[0]
    dy = p2[1] - p1[1]
    L = math.hypot(dx, dy)
    if L == 0:
        return
    ux, uy = dx / L, dy / L
    al = 14
    angle = 0.45
    p_left = (
        p2[0] - al * (ux * math.cos(angle) + uy * math.sin(angle)),
        p2[1] - al * (uy * math.cos(angle) - ux * math.sin(angle)),
    )
    p_right = (
        p2[0] - al * (ux * math.cos(angle) - uy * math.sin(angle)),
        p2[1] - al * (uy * math.cos(angle) + ux * math.sin(angle)),
    )
    d.polygon([p2, p_left, p_right], fill=color)


def edge_point(idx_from, idx_to, side='auto'):
    fx, fy = step_centers[idx_from]
    tx, ty = step_centers[idx_to]
    bx1, by1, bx2, by2 = step_boxes[idx_from][3]
    if side == 'auto':
        if abs(tx - fx) > abs(ty - fy):
            side = 'right' if tx > fx else 'left'
        else:
            side = 'bottom' if ty > fy else 'top'
    margin = 8
    if side == 'right':
        return (bx2 + margin, fy)
    if side == 'left':
        return (bx1 - margin, fy)
    if side == 'top':
        return (fx, by1 - margin)
    if side == 'bottom':
        return (fx, by2 + margin)
    return (fx, fy)


# ---------- Draw ----------
img = Image.new('RGB', (W, H), BG)
d = ImageDraw.Draw(img)

font_title = ImageFont.truetype(FONT_BOLD, 36)
font_sub = ImageFont.truetype(FONT_REG, 18)
font_lane = ImageFont.truetype(FONT_BOLD, 22)
font_step = ImageFont.truetype(FONT_REG, 16)
font_step_bold = ImageFont.truetype(FONT_BOLD, 18)
font_code = ImageFont.truetype(FONT_BOLD, 14)
font_legend = ImageFont.truetype(FONT_REG, 16)
font_legend_b = ImageFont.truetype(FONT_BOLD, 18)


# Title bar
d.rectangle([0, 0, W, 100], fill=HEADER_BG)
title_text = 'CNC 加工厂下料流程泳道图（昆山佰泰胜专属 ERP V1.0）'
bbox = d.textbbox((0, 0), title_text, font=font_title)
d.text(((W - (bbox[2]-bbox[0])) / 2, 30), title_text, font=font_title, fill='#ffffff')
sub = '一码到底（GD-工单码 / LZ-流转码 / SB-设备码 / WL-物料码 / WW-委外单码）· 一数到底（业务单据一次录入，财务/车间/驾驶舱共用）'
bbox = d.textbbox((0, 0), sub, font=font_sub)
d.text(((W - (bbox[2]-bbox[0])) / 2, 75), sub, font=font_sub, fill='#c9d1d9')


# Lane backgrounds
for i, (label, bg) in enumerate(LANES):
    y0 = 200 + i * LANE_H
    y1 = y0 + LANE_H
    d.rectangle([0, y0, HEADER_W, y1], fill=HEADER_BG)
    # lane label vertical-ish (centered)
    bbox = d.textbbox((0, 0), label, font=font_lane)
    tw, th = bbox[2]-bbox[0], bbox[3]-bbox[1]
    d.text(((HEADER_W - tw) / 2, y0 + (LANE_H - th) / 2 - 4), label, font=font_lane, fill='#ffffff')

    # lane row band
    d.rectangle([HEADER_W, y0, W, y1], fill=bg, outline=LANE_BORDER, width=1)


# Time column dividers (vertical light lines)
col_w = (W - HEADER_W) / N_COLS
col_labels = [
    '①询价', '②订单', '③工艺', '④MRP/入库', '⑤排产/领料',
    '⑥下料工单', '⑦下料切割', '⑧报工', '⑨流转', '⑩归档',
]
for c in range(N_COLS + 1):
    x = HEADER_W + c * col_w
    d.line([(x, 200), (x, H - 100)], fill='#d0d7de', width=1)
# column header strip
d.rectangle([HEADER_W, 110, W, 200], fill='#f6f8fa')
d.line([(HEADER_W, 200), (W, 200)], fill='#57606a', width=2)
for c, lbl in enumerate(col_labels):
    x = HEADER_W + c * col_w + col_w / 2
    bbox = d.textbbox((0, 0), lbl, font=font_lane)
    tw = bbox[2]-bbox[0]
    d.text((x - tw / 2, 140), lbl, font=font_lane, fill=TITLE)

# Lane separator emphasis
for i in range(1, 6):
    y = 200 + i * LANE_H
    d.line([(0, y), (W, y)], fill='#a0a8b3', width=2)


# ---------- Pre-compute step geometry ----------
step_centers = {}
step_boxes = []
for idx, (lane, col, label, code, ms) in enumerate(STEPS):
    x, y = get_pos(lane, col)
    box = (x, y, x + STEP_W, y + STEP_H)
    step_boxes.append((idx, lane, col, box, code, ms))
    step_centers[idx] = get_center(lane, col)


# ---------- Draw arrows first (under boxes) ----------
for (a, b) in ARROWS:
    # Use center-to-center as a guide, but back off to box edge.
    cx_a, cy_a = step_centers[a]
    cx_b, cy_b = step_centers[b]
    bx1a, by1a, bx2a, by2a = step_boxes[a][3]
    bx1b, by1b, bx2b, by2b = step_boxes[b][3]
    if cx_b > cx_a:
        # going right
        p1 = (bx2a + 4, cy_a)
        p2 = (bx1b - 4, cy_b)
    elif cx_b < cx_a:
        p1 = (bx1a - 4, cy_a)
        p2 = (bx2b + 4, cy_b)
    else:
        p1 = (cx_a, by2a + 4)
        p2 = (cx_b, by1b - 4)
    draw_arrow(p1, p2, color='#1a7f37', width=4)


# ---------- Draw steps ----------
for idx, (lane, col, label, code, ms) in enumerate(STEPS):
    box = step_boxes[idx][3]

    fill = EMPHASIS_BG if ms else STEP_BG
    border = EMPHASIS_BORDER if ms else STEP_BG
    text_color = TITLE if ms else STEP_FG
    label_color = TITLE if ms else STEP_FG
    code_color = '#a04040' if ms else '#ffd966'

    d.rounded_rectangle(box, radius=10, fill=fill, outline=border, width=3 if ms else 2)

    # code badge top-right
    if code:
        cb_w, cb_h = 60, 22
        cb_x = box[2] - cb_w - 6
        cb_y = box[1] + 6
        d.rounded_rectangle([cb_x, cb_y, cb_x + cb_w, cb_y + cb_h], radius=4, fill=border)
        cbbox = d.textbbox((0, 0), code, font=font_code)
        ctw = cbbox[2]-cbbox[0]
        d.text((cb_x + (cb_w - ctw) / 2, cb_y + 2), code, font=font_code, fill='#ffffff')

    # label multi-line
    lines = label.split('\n')
    line_h = 22
    total_h = line_h * len(lines)
    y0 = box[1] + (STEP_H - total_h) / 2
    for li, ln in enumerate(lines):
        is_bold = (li == 0)
        f = font_step_bold if is_bold else font_step
        bbox = d.textbbox((0, 0), ln, font=f)
        tw = bbox[2]-bbox[0]
        d.text((box[0] + (STEP_W - tw) / 2, y0 + li * line_h), ln, font=f, fill=label_color)

    # Milestone star
    if ms:
        # little star in top-left
        sx, sy = box[0] + 12, box[1] + 12
        d.text((sx, sy - 4), '★', font=font_step_bold, fill=EMPHASIS_BORDER)


# ---------- Draw arrows ----------
def draw_arrow(p1, p2, color=ARROW, width=3, dashed=False):
    d.line([p1, p2], fill=color, width=width)
    # arrowhead
    import math
    dx = p2[0] - p1[0]
    dy = p2[1] - p1[1]
    L = math.hypot(dx, dy)
    if L == 0:
        return
    ux, uy = dx / L, dy / L
    # arrowhead
    al = 12
    angle = 0.5
    p_left = (
        p2[0] - al * (ux * math.cos(angle) + uy * math.sin(angle)),
        p2[1] - al * (uy * math.cos(angle) - ux * math.sin(angle)),
    )
    p_right = (
        p2[0] - al * (ux * math.cos(angle) - uy * math.sin(angle)),
        p2[1] - al * (uy * math.cos(angle) + ux * math.sin(angle)),
    )
    d.polygon([p2, p_left, p_right], fill=color)


# smart arrow endpoints (per edge of step box)
def edge_point(idx_from, idx_to, side='auto'):
    fx, fy = step_centers[idx_from]
    tx, ty = step_centers[idx_to]
    bx1, by1, bx2, by2 = step_boxes[idx_from][3]
    # Decide exit side based on direction
    if side == 'auto':
        if abs(tx - fx) > abs(ty - fy):
            side = 'right' if tx > fx else 'left'
        else:
            side = 'bottom' if ty > fy else 'top'
    margin = 8
    if side == 'right':
        return (bx2 + margin, fy)
    if side == 'left':
        return (bx1 - margin, fy)
    if side == 'top':
        return (fx, by1 - margin)
    if side == 'bottom':
        return (fx, by2 + margin)
    return (fx, fy)


# ---------- Legend ----------
LEG_Y = H - 100
d.rectangle([0, LEG_Y, W, H], fill='#f6f8fa')
d.line([(0, LEG_Y), (W, LEG_Y)], fill='#57606a', width=2)

# 5 items
items = [
    ('标准步骤', STEP_BG, '#ffffff'),
    ('里程碑 (★)', EMPHASIS_BG, EMPHASIS_BORDER),
    ('扫码触发', '#1a7f37', '#ffffff'),
    ('跨泳道联动', '#6e40c9', '#ffffff'),
    ('一码到底：GD/LZ/SB/WL/WW', '#1f2328', '#ffffff'),
]
ix = 40
for label, fill, fg in items:
    d.rounded_rectangle([ix, LEG_Y + 20, ix + 32, LEG_Y + 52], radius=4, fill=fill, outline=fill)
    bbox = d.textbbox((0, 0), label, font=font_legend_b)
    d.text((ix + 44, LEG_Y + 22), label, font=font_legend_b, fill=TITLE)
    ix += 60 + (bbox[2] - bbox[0])

# Right side: code legend
code_items = [
    ('GD-', '工单码'),
    ('LZ-', '流转码'),
    ('SB-', '设备码'),
    ('WL-', '物料码'),
    ('WW-', '委外单码'),
]
ix2 = W - 720
d.text((ix2, LEG_Y + 12), '三码体系（V1.3.5 收回区域码）', font=font_legend_b, fill=TITLE)
ix2 += 250
for code, desc in code_items:
    d.rounded_rectangle([ix2, LEG_Y + 22, ix2 + 60, LEG_Y + 50], radius=4, fill='#1f2328')
    bbox = d.textbbox((0, 0), code, font=font_code)
    ctw = bbox[2]-bbox[0]
    d.text((ix2 + (60 - ctw) / 2, LEG_Y + 26), code, font=font_code, fill='#ffffff')
    bbox = d.textbbox((0, 0), desc, font=font_legend)
    d.text((ix2 + 70, LEG_Y + 26), desc, font=font_legend, fill=TITLE)
    ix2 += 70 + (bbox[2] - bbox[0]) + 24


# ---------- Save ----------
img.save(str(OUT), 'PNG', optimize=True)
print(f'[OK] Swimlane PNG: {OUT}')
print(f'     Size: {OUT.stat().st_size:,} bytes')
print(f'     Dimensions: {W} x {H}')
