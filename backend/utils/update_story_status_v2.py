"""Update Story 1.1 status to AwaitingTestDesign + append Change Log row (v2)."""
from pathlib import Path
import re

p = Path(r'E:\claude\smart-workshop-erp\docs\stories\1.1-user-and-role-permission.md')
text = p.read_text(encoding='utf-8')

# Update ## Status line
old_status = '`Draft`（SM 萧何 起草）→ AwaitingArchReview'
new_status = '`AwaitingTestDesign`（architect 鲁班 评审通过 · APPROVED_WITH_COMMENTS）'
text = text.replace(old_status, new_status, 1)

# Also patch the frontmatter status field if present
text = re.sub(r'(- 基础设施与权限\n  status: )Draft', r'\1AwaitingTestDesign', text, count=1)

# Find the Change Log table and append row
# Look for the line: "| 2026-06-09 | SM | ..."
log_lines = re.findall(r'^\| [^|]+ \| [^|]+ \| Created[^\n]*\n', text, re.MULTILINE)
print('Found log lines:', len(log_lines))
print('First log line:', log_lines[0] if log_lines else 'none')

# Append after the Created row
created_row = log_lines[0].rstrip('\n') if log_lines else None
if created_row:
    arch_row = (
        '| 2026-06-10 | architect (鲁班) | AwaitingArchReview → AwaitingTestDesign | '
        '评审 9.2/10 · APPROVED_WITH_COMMENTS · 3 处 P1 修补（DEK 启动兜底 / 审批路由 candidates 字段 / 审计查询 admin 标注）dev 实施时闭环 · '
        '详见 `docs/architecture/story-reviews/1.1-review.md` |'
    )
    text = text.replace(created_row, created_row + '\n' + arch_row, 1)

p.write_text(text, encoding='utf-8')
print()
print('--- After update ---')
print('Status updated:', new_status in text)
print('Arch row added:', 'architect (鲁班)' in text)
print('Size:', p.stat().st_size, 'bytes')
