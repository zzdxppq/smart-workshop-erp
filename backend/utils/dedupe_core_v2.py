"""Hard dedupe: remove ALL com.btsheng.erp.core.common.* duplicates."""
import os
import shutil
from pathlib import Path

CORE_MAIN = Path(r'E:\claude\smart-workshop-erp\backend\src\core\src\main\java')

# Old common.* subdir
OLD_COMMON = CORE_MAIN / 'com' / 'btsheng' / 'erp' / 'core' / 'common'

print('OLD_COMMON exists:', OLD_COMMON.exists())
if OLD_COMMON.exists():
    files = list(OLD_COMMON.rglob('*.java'))
    print(f'Files in OLD_COMMON: {len(files)}')
    # Build map of (sub, name) -> new path
    SUB_MAP = {'dto': 'model', 'entity': 'model', 'util': 'model',
               'web': 'web', 'security': 'web', 'audit': 'web',
               'redis': 'redis', 'oss': 'infra', 'job': 'infra'}

    diff_count = 0
    deleted_count = 0
    for f in files:
        rel = f.relative_to(OLD_COMMON)  # dto/BaseDTO.java
        parts = rel.parts
        if len(parts) != 2:
            # has sub-dir; keep manual review
            print(f'  Skip (nested): {f}')
            continue
        sub, name = parts
        if sub not in SUB_MAP:
            print(f'  Skip (unknown sub): {f}')
            continue
        new_sub = SUB_MAP[sub]
        new_path = CORE_MAIN / 'com' / 'btsheng' / 'erp' / 'core' / new_sub / name
        if new_path.exists():
            if f.read_bytes() == new_path.read_bytes():
                f.unlink()
                deleted_count += 1
            else:
                print(f'  DIFF: old {f.relative_to(CORE_MAIN)} vs new {new_path.relative_to(CORE_MAIN)}')
                diff_count += 1
        else:
            # new doesn't exist, just move
            new_path.parent.mkdir(parents=True, exist_ok=True)
            shutil.move(str(f), str(new_path))
            deleted_count += 1

    print(f'Deleted: {deleted_count}, DIFF (manual review): {diff_count}')

    # Remove the common/ dir tree (any remaining empty)
    shutil.rmtree(OLD_COMMON, ignore_errors=True)
    # walk up
    parent = OLD_COMMON.parent
    while parent != CORE_MAIN and parent.exists() and not any(parent.iterdir()):
        parent.rmdir()
        parent = parent.parent

# verify
import re
remaining_common = list(CORE_MAIN.rglob('com/btsheng/erp/core/common/**/*.java'))
print(f'Remaining common.* files: {len(remaining_common)}')

# Final tree
print()
print('=== core/src/main/java tree (depth 4) ===')
for root, dirs, files in os.walk(CORE_MAIN):
    rel = os.path.relpath(root, CORE_MAIN)
    depth = 0 if rel == '.' else rel.count(os.sep) + 1
    if depth > 4:
        del dirs[:]
        continue
    indent = '  ' * depth
    name = rel if rel != '.' else '.'
    print(f'{indent}{name}/')
    for f in sorted(files)[:6]:
        print(f'{indent}  {f}')
    if len(files) > 6:
        print(f'{indent}  ... and {len(files)-6} more')
