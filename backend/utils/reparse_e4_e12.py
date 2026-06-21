"""Reparse E4 and E12 (missing from previous run) by allowing flexible title pattern."""
import re
import sys
from pathlib import Path

SRC = Path(r'E:\claude\smart-workshop-erp\docs\prd\6-epics核心完整-yaml.md')
DST_DIR = Path(r'E:\claude\smart-workshop-erp\docs\prd')

# Match #### E{N}: ... (no need for "（完整 YAML）")
pattern = re.compile(
    r'####\s+(E\d+):\s*([^\n]+?)\s*\n```yaml\n(.*?)\n```',
    re.DOTALL
)

cmap = {'L': 'high', 'M': 'medium', 'S': 'low', 'H': 'high', 'XL': 'high'}

for epic_tag, title, yaml_block in pattern.findall(SRC.read_text(encoding='utf-8')):
    # Skip if already parsed
    if epic_tag in {'E1', 'E2', 'E3', 'E5', 'E6', 'E7', 'E8', 'E9', 'E10', 'E11'}:
        continue

    epic_num = epic_tag[1:]
    clean_title = re.sub(r'（[^）]+）', '', title).strip()
    # E12 title is long; use first 20 chars
    short_title = clean_title if len(clean_title) <= 30 else clean_title[:30]

    # extract stories via regex
    story_pattern = re.compile(
        r'-\s*id:\s*(E\d+-S\d+)\s*\n\s*title:\s*([^\n]+)\s*\n\s*repository_type:\s*([^\n]+)\s*\n\s*estimated_complexity:\s*([^\n]+)\s*\n\s*priority:\s*([^\n]+)',
        re.MULTILINE
    )
    stories = []
    for m in story_pattern.finditer(yaml_block):
        sid, stitle, rt, comp, pri = m.groups()
        stories.append((sid, stitle.strip().strip('"').strip(), rt.strip(), comp.strip(), pri.strip()))

    if not stories:
        print(f'  ⚠️ {epic_tag}: no stories found in raw block')
        continue

    # Build yaml
    out = []
    out.append(f'epic_id: {epic_num}')
    out.append(f'title: "{short_title}"')
    out.append('description: |')
    why_m = re.search(r'why_exist:\s*\|\s*\n((?:[^\n]+\n){1,5})', yaml_block)
    if why_m:
        for line in why_m.group(1).strip().split('\n')[:3]:
            out.append('  ' + line)
    else:
        out.append('  ' + short_title)
    out.append('')
    out.append('stories:')
    for sid, stitle, rt, comp, pri in stories:
        # E12-S1 -> 12.1
        m = re.match(r'E(\d+)-S(\d+)', sid)
        short_id = f"{m.group(1)}.{int(m.group(2))}" if m else sid
        out.append(f'  - id: "{short_id}"')
        out.append(f'    title: "{stitle}"')
        out.append(f'    estimated_complexity: {cmap.get(comp, "medium")}')
        out.append(f'    priority: {pri}')
        primary = rt.split('+')[0] if '+' in rt else rt
        out.append(f'    repository_type: {primary}')
        out.append('')

    out_text = '\n'.join(out)
    slug = re.sub(r'[^\w]+', '-', short_title).strip('-').lower()
    out_path = DST_DIR / f'epic-{epic_num}-{slug}.yaml'
    out_path.write_text(out_text, encoding='utf-8')
    print(f'  ✅ {out_path.name}  ({len(out_text)} bytes, {len(stories)} stories)')

# Final summary
print('\nAll 12 epic files:')
total_stories = 0
for f in sorted(DST_DIR.glob('epic-*.yaml'), key=lambda x: int(x.stem.split('-')[1])):
    content = f.read_text(encoding='utf-8')
    n = len(re.findall(r'^\s*-\s*id:', content, re.MULTILINE))
    total_stories += n
    print(f'  {f.name:50s}  {n} stories  ({f.stat().st_size:,} bytes)')
print(f'\n📋 Total: 12 epics / {total_stories} stories')
