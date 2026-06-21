"""V94 重写：删除 init.baseline.sql 中 sys-menu-permission-seed.sql include + V67 嵌入 + V87 嵌入，
   在末尾 SET FOREIGN_KEY_CHECKS = 1 之前 append V94__v21_menu_role_permission.sql 完整内容"""
import os

BASELINE = r"E:/claude/smart-workshop-erp/backend/db/init.baseline.sql"
V94 = r"E:/claude-smatWorkshop-erp/backend/db/migrations/V94__v21_menu_role_permission.sql"

# 用正确路径
V94 = r"E:/claude/smart-workshop-erp/backend/db/migrations/V94__v21_menu_role_permission.sql"

with open(BASELINE, "r", encoding="utf-8") as f:
    baseline = f.read()
with open(V94, "r", encoding="utf-8") as f:
    v94 = f.read()

# 找要删除的范围：
# 1) sys-menu-permission-seed.sql include 段（line 962 开始到下一 include/V_N）
# 2) V67 嵌入段（line 6142 开始到 line 6392 V87 marker 前）
# 3) V87 嵌入段（line 6393 到 SET FOREIGN_KEY_CHECKS = 1 前）

# 找 marker
m1 = baseline.find("-- include: sys-menu-permission-seed.sql")
m2 = baseline.find("-- include: V67__sys_menu_permission.sql")
m3 = baseline.find("-- V94 · V87")
set_marker = baseline.find("SET FOREIGN_KEY_CHECKS = 1;", m3)

print(f"m1={m1}, m2={m2}, m3={m3}, set_marker={set_marker}")

# 删除 [m1, set_marker) 之间的所有内容
# 保留 m1 之前的 "="*60 banner
pre_banner_end = baseline.rfind("=" * 60, 0, m1) + 60  # 到 banner 结束
# 但 banner 在 m1 之前多行，需找 banner 的实际开始位置
banner_start = baseline.rfind("=" * 60, 0, m1)
# banner_start 可能不在 m1 之前最近——保守从 m1 前的最后一行 = 找 m1 之前的最后一个换行
pre_end = baseline.rfind("\n", 0, m1) + 1

# 新内容 = pre_end + V94 完整内容 + set_marker 起始处
v94_use_idx = v94.index("USE `cnc_platform`;")
v94_body = v94[:v94_use_idx].rstrip() + "\n\n" + v94[v94_use_idx:].rstrip() + "\n\n"

new_baseline = baseline[:pre_end] + v94_body + baseline[set_marker:]

with open(BASELINE, "w", encoding="utf-8") as f:
    f.write(new_baseline)

print(f"V94 已替换 sys-menu-permission-seed + V67 + V87 嵌入段")
print(f"baseline: {os.path.getsize(BASELINE) // 1024} KB")