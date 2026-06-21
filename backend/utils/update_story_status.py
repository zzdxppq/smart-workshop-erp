"""Update Story 1.1 status to AwaitingTestDesign + append Change Log row."""
from pathlib import Path
p = Path(r'E:\claude\smart-workshop-erp\docs\stories\1.1-user-and-role-permission.md')
text = p.read_text(encoding='utf-8')

# Update frontmatter status
old1 = "Story:\n  id: 1.1\n  title: 用户与角色权限\n  epic: 1 - 基础设施与权限\n  status: Draft"
new1 = "Story:\n  id: 1.1\n  title: 用户与角色权限\n  epic: 1 - 基础设施与权限\n  status: AwaitingTestDesign"
text = text.replace(old1, new1, 1)

# Update the H2 Status line
text = text.replace("## Status\nDraft", "## Status\nAwaitingTestDesign", 1)

# Append Change Log row
old_row = "| 2026-06-10 | SM | Created → Draft | Story created |"
new_row = (
    old_row + "\n"
    "| 2026-06-10 | architect (鲁班) | Draft → AwaitingArchReview → AwaitingTestDesign | "
    "评审 9.2/10 · APPROVED_WITH_COMMENTS · 3 处 P1 修补 dev 实施时闭环 · "
    "详见 `docs/architecture/story-reviews/1.1-review.md` |"
)
text = text.replace(old_row, new_row, 1)

p.write_text(text, encoding='utf-8')
print('Status:', 'AwaitingTestDesign' in text)
print('Change Log updated:', 'architect (鲁班)' in text)
print('size:', p.stat().st_size, 'bytes')
