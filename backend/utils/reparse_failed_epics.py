"""Reparse the 5 failed epic YAMLs (E1/E3/E5/E6/E11) by extracting story list
from raw text instead of strict YAML parse."""
import re
import sys
from pathlib import Path

SRC = Path(r'E:\claude\smart-workshop-erp\docs\prd\6-epics核心完整-yaml.md')
DST_DIR = Path(r'E:\claude\smart-workshop-erp\docs\prd')

failed = {'E1', 'E3', 'E5', 'E6', 'E11'}

content = SRC.read_text(encoding='utf-8')
pattern = re.compile(
    r'####\s+(E\d+):\s*([^\n]+?)\s*（完整 YAML）\s*\n(.*?)\n```',
    re.DOTALL
)

cmap = {'L': 'high', 'M': 'medium', 'S': 'low', 'H': 'high', 'XL': 'high'}

for epic_tag, title, yaml_block in pattern.findall(content):
    if epic_tag not in failed:
        continue
    epic_num = epic_tag[1:]
    clean_title = re.sub(r'（[^）]+）', '', title).strip()
    # extract stories with regex
    stories = []
    # Pattern: - id: E1-S1 \n title: ... \n repository_type: ... \n ... priority: ...
    story_pattern = re.compile(
        r'-\s*id:\s*(E\d+-S\d+)\s*\n\s*title:\s*([^\n]+)\s*\n\s*repository_type:\s*([^\n]+)\s*\n\s*estimated_complexity:\s*([^\n]+)\s*\n\s*priority:\s*([^\n]+)',
        re.MULTILINE
    )
    for m in story_pattern.finditer(yaml_block):
        sid, stitle, rt, comp, pri = m.groups()
        stories.append((sid, stitle.strip().strip('"').strip(), rt.strip(), comp.strip(), pri.strip()))

    # also extract provides_apis for each story (best-effort)
    apis_per_story = {}
    # Split yaml_block by story id
    parts = re.split(r'(?=-\s*id:\s*E\d+-S\d+)', yaml_block)
    for part in parts:
        m_id = re.match(r'-\s*id:\s*(E\d+-S\d+)', part)
        if not m_id:
            continue
        sid = m_id.group(1)
        apis = re.findall(r'-\s*([A-Z]+\s+/api/[^\s\n]+)', part)
        if apis:
            apis_per_story[sid] = apis

    # Build PO-format yaml
    out = []
    out.append(f'epic_id: {epic_num}')
    out.append(f'title: "{clean_title}"')
    out.append('description: |')
    # Pull first WHY line
    why_m = re.search(r'why_exist:\s*\|\s*\n((?:[^\n]+\n){1,5})', yaml_block)
    if why_m:
        for line in why_m.group(1).strip().split('\n')[:3]:
            out.append('  ' + line)
    else:
        out.append('  ' + clean_title)
    out.append('')
    out.append('stories:')
    for sid, stitle, rt, comp, pri in stories:
        short_id = f"{sid[1]}.{sid[4:]}"  # E1-S1 -> 1.1
        out.append(f'  - id: "{short_id}"')
        out.append(f'    title: "{stitle}"')
        out.append(f'    estimated_complexity: {cmap.get(comp, "medium")}')
        out.append(f'    priority: {pri}')
        primary = rt.split('+')[0] if '+' in rt else rt
        out.append(f'    repository_type: {primary}')
        if sid in apis_per_story and apis_per_story[sid]:
            out.append('    provides_apis:')
            for api in apis_per_story[sid][:5]:
                out.append(f'      - "{api.strip()}"')
        out.append('')

    out_text = '\n'.join(out)
    # Build filename
    slug = re.sub(r'[^\w]+', '-', clean_title).strip('-').lower()
    out_path = DST_DIR / f'epic-{epic_num}-{slug}.yaml'
    out_path.write_text(out_text, encoding='utf-8')
    print(f'  ✅ {out_path.name}  ({len(out_text)} bytes, {len(stories)} stories)')

print('\nFinal epic files:')
for f in sorted(DST_DIR.glob('epic-*.yaml')):
    print(f'  {f.name}  ({f.stat().st_size:,} bytes)')
