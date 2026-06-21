"""Fix broken string literals introduced by encoding corruption (scoped files only)."""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

REPLACEMENTS = [
    (re.compile(r'"items [^"]*?\?\);'), '"items 至少 1 项");'),
    (re.compile(r'"单次最[^"]*?" \+ PDF_MAX_ITEMS \+ " [^"]*?\?\);'),
     '"单次最多 " + PDF_MAX_ITEMS + " 标签/页（A4 排版）");'),
    (re.compile(r'"PRINT_REPLAY_FORBIDDEN: [^"]*?\?\);'),
     '"PRINT_REPLAY_FORBIDDEN: 仅 SUCCESS 可补打");'),
    (re.compile(r'"PRINT_REPLAY_FORBIDDEN: 补打记录不可再次补打[^"]*?\?\);'),
     '"PRINT_REPLAY_FORBIDDEN: 补打记录不可再次补打（防递归）");'),
    (re.compile(r'"日志不存[^"]*?\?\);'), '"日志不存在");'),
    (re.compile(r'"请求体为[^"]*?\?\);'), '"请求体为空");'),
    (re.compile(r'"lines 最[^"]*?\?\);'), '"lines 最多 6 行");'),
    (re.compile(r'"ip 未配[^"]*?\?\);'), '"ip 未配置");'),
    (re.compile(r'"type 不支[^"]*?\?\);'), '"type 不支持");'),
    (re.compile(r'new String\{"工单[^"]*?, "GD-"'), 'new String[]{"工单码", "GD-"'),
    (re.compile(r'new String\{"流转[^"]*?, "LZ-"'), 'new String[]{"流转码", "LZ-"'),
    (re.compile(r'new String\{"设备[^"]*?, "SB-"'), 'new String[]{"设备码", "SB-"'),
    (re.compile(r'new String\{"物料[^"]*?, "WL-"'), 'new String[]{"物料码", "WL-"'),
    (re.compile(r'Set\.of\("原材[^"]*?, "粗加[^"]*?, "精加[^"]*?, "表面处理", "检[^"]*?\)'),
     'Set.of("原材料", "粗加工", "精加工", "表面处理", "检验")'),
    (re.compile(r'"工程[^"]*? \+ operatorUserId'), '"工程师" + operatorUserId'),
    (re.compile(r'"处理动作[^"]*? \+ req\.getActionType\(\)'), '"处理动作 " + req.getActionType()'),
    (re.compile(r'\{"原材[^"]*?, "粗加[^"]*?, "精加[^"]*?, "表面处理", "检[^"]*?\}'),
     '{"原材料", "粗加工", "精加工", "表面处理", "检验"}'),
    (re.compile(r'"原材[^"]*? : s\.getSegment\(\)'), '"原材料" : s.getSegment()'),
    (re.compile(r'"原材[^"]*? : item\.getSegment\(\)'), '"原材料" : item.getSegment()'),
    (re.compile(r'"原材[^"]*? : in\.getSegment\(\)'), '"原材料" : in.getSegment()'),
    (re.compile(r'"原材[^"]*? : req\.getSegment\(\)'), '"原材料" : req.getSegment()'),
    (re.compile(r'private static final String\[\] FIVE_SEGMENT_NAMES = \{"原材[^"]*?, "粗加[^"]*?, "精加[^"]*?, "表面处理", "检[^"]*?\};'),
     'private static final String[] FIVE_SEGMENT_NAMES = {"原材料", "粗加工", "精加工", "表面处理", "检验"};'),
    (re.compile(r'private static final String\[\] FIVE_SEGMENTS = \{"原材[^"]*?, "粗加[^"]*?, "精加[^"]*?, "表面处理", "检[^"]*?\};'),
     'private static final String[] FIVE_SEGMENTS = {"原材料", "粗加工", "精加工", "表面处理", "检验"};'),
    (re.compile(r'REWORK_LEVEL_INFO = 1;     //[^\n]*REWORK_LEVEL_WARN = 2;[^\n]*REWORK_LEVEL_CRITICAL = 3;[^\n]*REWORK_LEVEL_EXCEED = 4;'),
     'REWORK_LEVEL_INFO = 1;\n    public static final int REWORK_LEVEL_WARN = 2;\n    public static final int REWORK_LEVEL_CRITICAL = 3;\n    public static final int REWORK_LEVEL_EXCEED = 4;'),
    (re.compile(r'"主工[^"]*?\?\);'), '"主工序");'),
    (re.compile(r'"开[^"]*?\?\);'), '"开始");'),
    (re.compile(r'"创建委外[^"]*?\?\);'), '"创建委外单");'),
    (re.compile(r'"供应商接[^"]*?\?\);'), '"供应商接单");'),
    (re.compile(r'"不良品登[^"]*?\?\);'), '"不良品登记");'),
    (re.compile(r'"该图纸仅 ENGINEER 可下[^"]*?\?\);'), '"该图纸仅 ENGINEER 可下载");'),
    (re.compile(r'unit\) != null \? req\.getUnit\(\) : "[^"]*?\?\);'),
     'unit) != null ? req.getUnit() : "件");'),
    (re.compile(r'getUnit\(\) != null \? itemReq\.getUnit\(\) : "[^"]*?\?\);'),
     'getUnit() != null ? itemReq.getUnit() : "件");'),
    (re.compile(r'"P1 修补 3：逾期 " \+ Math\.abs\(days\) \+ " [^"]*?\?\);'),
     '"P1 修补 3：逾期 " + Math.abs(days) + " 天");'),
    (re.compile(r'"P1 修补 2：距离到[^"]*?" \+ days \+ " [^"]*?\?\);'),
     '"P1 修补 2：距离到货 " + days + " 天");'),
    (re.compile(r'"距离到货还有 " \+ days \+ " [^"]*?\?\);'),
     '"距离到货还有 " + days + " 天");'),
    (re.compile(r'"上海某精[^"]*?\?\);'), '"上海某精密");'),
]

TARGETS = list((ROOT / "src" / "erp-business" / "src" / "main").rglob("*.java"))


def main() -> None:
    changed = 0
    for path in TARGETS:
        text = path.read_text(encoding="utf-8", errors="replace")
        original = text
        for pattern, repl in REPLACEMENTS:
            text = pattern.sub(repl, text)
        text = re.sub(r'(//[^\n]*?)\s{4,}([A-Za-z_@].*)', r'\1\n            \2', text)
        if text != original:
            path.write_text(text, encoding="utf-8", newline="\n")
            changed += 1
    print(f"fixed {changed} files")


if __name__ == "__main__":
    main()
