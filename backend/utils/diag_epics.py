import re
from pathlib import Path
SRC = Path(r'E:\claude\smart-workshop-erp\docs\prd\6-epics核心完整-yaml.md')
content = SRC.read_text(encoding='utf-8')
pattern = re.compile(
    r'####\s+(E\d+):\s*([^\n]+?)\s*\n```yaml\n(.*?)\n```',
    re.DOTALL
)
print('Permissive pattern:')
for m in pattern.findall(content):
    tag, title, body = m
    print(f'  {tag}: {title[:50]!r}  ({len(body)} bytes)')
print()

# E4 / E12 might use ``` ... ``` but with trailing
print('All #### E*:')
for m in re.finditer(r'####\s+(E\d+):\s*([^\n]+)', content):
    print(f'  L{m.start()}: {m.group(1)}: {m.group(2)[:60]!r}')
