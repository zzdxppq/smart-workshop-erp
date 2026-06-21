"""V94 重写 v2：只替换 sys-menu-permission-seed.sql include + V67 嵌入 + V87 嵌入"""
import os

BASELINE = r"E:/claude/smart-workshop-erp/backend/db/init.baseline.sql"
V94 = r"E:/claude/smart-workshop-erp/backend/db/migrations/V94__v21_menu_role_permission.sql"

with open(BASELINE, "r", encoding="utf-8") as f:
    baseline = f.read()
with open(V94, "r", encoding="utf-8") as f:
    v94 = f.read()

# 找 3 段 marker
m1 = baseline.find("-- include: sys-menu-permission-seed.sql")
m2 = baseline.find("-- include: V67__sys_menu_permission.sql")
m3 = baseline.find("-- V94 · V87 · 报价与订单协同设计 V2.1")
set_marker = baseline.find("SET FOREIGN_KEY_CHECKS = 1;", m3)

print(f"m1={m1} m2={m2} m3={m3} set_marker={set_marker}")

# m1 之前：先找 m1 之前的最近一个 -- V 段结尾（V_N include 行），保留到 -- \n 之间
# 简单做法：m1 之前的最后一行可能是 sys_role INSERT 的尾段
# 让 m1 前一个 ";" 行结尾作为我们的起点
pre_m1_end = baseline.rfind(";\n", 0, m1) + 2

# m2 之前：找 m2 前最近一个 "="*60 行（V67 include 前 banner）
# 实际 m2 前面就是 m1 段结尾，连接紧密
pre_m2_end = m1  # m1 段全删，m2 接续

# m3 之前：找 m3 前最近一个 ";" 行（V67 段尾）
pre_m3_end = baseline.rfind(";\n", m2, m3) + 2

# set_marker 之前：找 set_marker 前最近一个空行
pre_set_end = baseline.rfind("\n\n", m3, set_marker)

# 删除 [pre_m1_end, pre_set_end) 之间的所有内容
deleted_range = baseline[pre_m1_end:pre_set_end]
print(f"删除范围长度: {len(deleted_range)} 字符")
print(f"删除起始: {baseline[pre_m1_end:pre_m1_end+100]}")
print(f"删除结束: {deleted_range[-100:]}")

# 准备 V94 内容（在 baseline 当前 USE cnc_platform; 之后插入）
v94_use_idx = v94.index("USE `cnc_platform`;")
v94_body = v94[v94_use_idx:].rstrip()

# 构造新 baseline
new_baseline = baseline[:pre_m1_end] + "\n" + v94_body + "\n\n" + baseline[set_marker:]

with open(BASELINE, "w", encoding="utf-8") as f:
    f.write(new_baseline)

print(f"V94 已替换（旧 menu 段删 + 新 V94 加）")
print(f"baseline: {os.path.getsize(BASELINE) // 1024} KB")
print(f"V_N include 数: {new_baseline.count(chr(10) + '-- include: V')}")