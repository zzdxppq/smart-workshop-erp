"""Append architect row to Story 1.2 Change Log via simple find/replace."""
from pathlib import Path

p = Path(r'E:\claude\smart-workshop-erp\backend\docs\stories\1.2-审批工作流配置.md')
text = p.read_text(encoding='utf-8')

# Use simpler markers - just find "状态：AwaitingArchReview。" and append after
old_marker = '状态：AwaitingArchReview。 |'
new_block = old_marker + (
    '\n| 2026-06-10 | 鲁班 (architect) | 评审 9.0/10 → APPROVED_WITH_COMMENTS；7 维度评分（架构 9.0/API 9.0/DB 9.5/安全 9.0/可实现 8.5/跨Epic 9.5/可测 9.0）；4 处 P1 修补全部闭环；5 风险点（1 路由双向/3 Feign 与通道/1 低）；6 条 P2 反馈。状态：AwaitingArchReview → AwaitingTestDesign。详见 `docs/architecture/story-reviews/1.2-review.md` |'
)

if old_marker in text:
    text = text.replace(old_marker, new_block, 1)
    p.write_text(text, encoding='utf-8')
    print('OK: arch row appended')
else:
    print('NOT FOUND - search by other means')
    import re
    # Find any line starting with 2026-06-10
    for m in re.finditer(r'\| 2026-06-10 \|', text):
        print(f'  match at {m.start()}: ...{text[m.start():m.start()+200]}...')

print(f'Size: {p.stat().st_size} bytes')
