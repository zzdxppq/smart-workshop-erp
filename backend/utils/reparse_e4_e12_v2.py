"""Reparse E4 and E12 - they're missing because their ```yaml blocks are far apart
or split by other content. Use line-range based extraction instead of regex."""
import re
from pathlib import Path

SRC = Path(r'E:\claude\smart-workshop-erp\docs\prd\6-epics核心完整-yaml.md')
DST_DIR = Path(r'E:\claude\smart-workshop-erp\docs\prd')

cmap = {'L': 'high', 'M': 'medium', 'S': 'low', 'H': 'high', 'XL': 'high'}

# Find line numbers of all "#### E{N}:" headers
content = SRC.read_text(encoding='utf-8')
lines = content.split('\n')
headers = {}  # tag -> line index
for i, ln in enumerate(lines):
    m = re.match(r'####\s+(E\d+):\s*([^\n]+)', ln)
    if m:
        tag = m.group(1)
        title = m.group(2)
        if tag not in headers:
            headers[tag] = (i, title)

print('All #### E* headers found:')
for tag, (i, t) in sorted(headers.items()):
    print(f'  L{i:6d}  {tag}: {t[:60]!r}')

# For each tag, extract from its line to next ``` close OR next #### E{N}
for tag, (start_line, title) in sorted(headers.items()):
    if tag in {'E1','E2','E3','E5','E6','E7','E8','E9','E10','E11'}:
        continue  # already done

    epic_num = tag[1:]
    # Find the next ```yaml after start_line
    yaml_start = None
    for i in range(start_line+1, min(start_line+20, len(lines))):
        if lines[i].strip() == '```yaml':
            yaml_start = i
            break
    if yaml_start is None:
        print(f'  ⚠️ {tag}: no ```yaml found near L{start_line}')
        continue

    # Find the matching ``` close
    yaml_end = None
    for i in range(yaml_start+1, min(yaml_start+20000, len(lines))):
        if lines[i].strip() == '```':
            yaml_end = i
            break
    if yaml_end is None:
        print(f'  ⚠️ {tag}: no close ``` after L{yaml_start}')
        continue

    yaml_block = '\n'.join(lines[yaml_start+1:yaml_end])
    print(f'  {tag}: yaml at L{yaml_start+1}-{yaml_end} ({len(yaml_block)} bytes)')

    # Extract stories
    story_pattern = re.compile(
        r'-\s*id:\s*(E\d+-S\d+)\s*\n\s*title:\s*([^\n]+)\s*\n\s*repository_type:\s*([^\n]+)\s*\n\s*estimated_complexity:\s*([^\n]+)\s*\n\s*priority:\s*([^\n]+)',
        re.MULTILINE
    )
    stories = []
    for m in story_pattern.finditer(yaml_block):
        sid, stitle, rt, comp, pri = m.groups()
        stories.append((sid, stitle.strip().strip('"').strip(), rt.strip(), comp.strip(), pri.strip()))

    if not stories:
        # Some stories might have different structure; show first 1000 chars
        print(f'    stories count 0; first 500 chars of block:')
        print('   ', yaml_block[:500])
        continue

    # Clean title (E12 long title)
    clean_title = re.sub(r'（[^）]+）', '', title).strip()
    short_title = clean_title if len(clean_title) <= 30 else clean_title[:30]

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
        m2 = re.match(r'E(\d+)-S(\d+)', sid)
        short_id = f"{m2.group(1)}.{int(m2.group(2))}" if m2 else sid
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
    print(f'    ✅ {out_path.name}  ({len(out_text)} bytes, {len(stories)} stories)')

print('\nFinal:')
total = 0
for f in sorted(DST_DIR.glob('epic-*.yaml'), key=lambda x: int(x.stem.split('-')[1])):
    n = len(re.findall(r'^\s*-\s*id:', f.read_text(encoding='utf-8'), re.MULTILINE))
    total += n
    print(f'  {f.name:55s}  {n} stories')
print(f'\n📋 Total: {total} stories across 12 epics')
