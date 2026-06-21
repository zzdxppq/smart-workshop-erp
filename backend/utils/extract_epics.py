"""Extract epic YAML blocks from docs/prd/6-epics核心完整-yaml.md and write
to docs/prd/epic-N-*.yaml in PO-compatible format.

Source format (in prd.md):
    #### E1: 基础设施与权限（完整 YAML）
    ```yaml
    epic:
      id: E1
      title: 基础设施与权限
      ...
      stories:
        - id: E1-S1
          title: ...
    ```

Target format (PO spec):
    ```yaml
    epic_id: 1
    title: "..."
    description: "..."
    stories:
      - id: "1.1"
        title: "..."
        ...
    ```
"""
import re
import sys
from pathlib import Path

SRC = Path(r'E:\claude\smart-workshop-erp\docs\prd\6-epics核心完整-yaml.md')
DST_DIR = Path(r'E:\claude\smart-workshop-erp\docs\prd')


def slugify_zh(text):
    """Convert Chinese title to safe ASCII filename slug."""
    s = re.sub(r'[（(][^）)]*[）)]', '', text)
    s = re.sub(r'[\s·:：/\\|]+', '-', s)
    s = re.sub(r'-+', '-', s).strip('-')
    return s


def main():
    if not SRC.exists():
        print(f'❌ Source not found: {SRC}')
        sys.exit(1)

    content = SRC.read_text(encoding='utf-8')

    # Pattern: #### E{N}: ...（完整 YAML） followed by ```yaml ... ```
    # Note: E1, E2, ... E13 in V1.3.7 PRD
    pattern = re.compile(
        r'####\s+(E\d+):\s*([^\n]+?)\s*（完整 YAML）\s*\n```yaml\n(.*?)\n```',
        re.DOTALL
    )

    matches = pattern.findall(content)
    print(f'Found {len(matches)} epic YAML blocks')

    for epic_tag, title, yaml_block in matches:
        epic_num = epic_tag[1:]  # "1" from "E1"
        # Clean title: remove trailing parenthetical
        clean_title = re.sub(r'（[^）]+）', '', title).strip()
        slug = slugify_zh(clean_title)

        # Parse the inner yaml to extract title and stories
        try:
            import yaml
            inner = yaml.safe_load(yaml_block)
        except Exception as e:
            print(f'  ⚠️ YAML parse fail for E{epic_num}: {e}; writing raw')
            inner = None

        # Build PO-format epic yaml
        if inner and 'epic' in inner:
            ep = inner['epic']
            out_lines = []
            out_lines.append(f"epic_id: {epic_num}")
            out_lines.append(f'title: "{ep.get("title", clean_title)}"')
            description = ep.get('why_exist', ep.get('description', ''))
            if description:
                # Use | for multi-line
                out_lines.append('description: |')
                for line in str(description).strip().split('\n'):
                    out_lines.append('  ' + line)
            out_lines.append('')
            out_lines.append('stories:')
            for story in ep.get('stories', []):
                sid = story.get('id', '')
                # Convert E1-S1 -> 1.1
                m = re.match(r'E(\d+)-S(\d+)', sid)
                if m:
                    short_id = f"{m.group(1)}.{m.group(2)}"
                else:
                    short_id = sid
                out_lines.append(f'  - id: "{short_id}"')
                out_lines.append(f'    title: "{story.get("title", "")}"')
                # Map complexity L/M/S -> medium/low/high
                cmap = {'L': 'high', 'M': 'medium', 'S': 'low', 'H': 'high', 'XL': 'high'}
                comp = cmap.get(story.get('estimated_complexity', 'M'), 'medium')
                out_lines.append(f'    estimated_complexity: {comp}')
                out_lines.append(f'    priority: {story.get("priority", "P0")}')
                # repository_type
                rt = story.get('repository_type', 'backend+web')
                # Map multi-target to primary backend (multi-repo product)
                if '+' in rt:
                    primary = rt.split('+')[0]
                else:
                    primary = rt
                out_lines.append(f'    repository_type: {primary}')
                # AC count
                acs = story.get('acceptance_criteria', [])
                if acs and isinstance(acs, list):
                    out_lines.append(f'    acceptance_criteria_count: {len(acs)}')
                    out_lines.append('    acceptance_criteria_summary: |')
                    for ac in acs[:3]:  # first 3 only
                        if isinstance(ac, dict):
                            out_lines.append(f"      - {ac.get('id', '?')}: {ac.get('title', ac.get('description', '?'))}")
                # provides_apis
                apis = story.get('provides_apis', [])
                if apis:
                    out_lines.append('    provides_apis:')
                    for api in apis:
                        out_lines.append(f'      - "{api}"')
                out_lines.append('')

            out_text = '\n'.join(out_lines)
        else:
            out_text = f'epic_id: {epic_num}\ntitle: "{clean_title}"\ndescription: |\n  {yaml_block[:500]}\n'

        # Write to file
        out_path = DST_DIR / f'epic-{epic_num}-{slug}.yaml'
        out_path.write_text(out_text, encoding='utf-8')
        print(f'  ✅ {out_path.name}  ({len(out_text)} bytes, {len(inner.get("epic", {}).get("stories", [])) if inner and "epic" in inner else "?"} stories)')

    # Summary
    yaml_files = sorted(DST_DIR.glob('epic-*.yaml'))
    print(f'\n📋 Total epic YAML files: {len(yaml_files)}')
    for f in yaml_files:
        print(f'   {f.name}  ({f.stat().st_size:,} bytes)')

    # Update core-config.yaml
    cfg = Path(r'E:\claude\smart-workshop-erp\.orchestrix-core\core-config.yaml')
    if cfg.exists():
        text = cfg.read_text(encoding='utf-8')
        text = text.replace('prdSharded: false', 'prdSharded: true')
        cfg.write_text(text, encoding='utf-8')
        print(f'\n✅ Updated {cfg.name}: prdSharded: true')


if __name__ == '__main__':
    main()
