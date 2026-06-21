"""Dedupe old common.* sub-package files - they're duplicated by new model/web/redis/infra."""
import shutil
from pathlib import Path

OLD_PKG = Path(r'E:\claude\smart-workshop-erp\backend\src\core\src\main\java\com\btsheng\erp\core\common')

if OLD_PKG.exists():
    # count files
    files = list(OLD_PKG.rglob('*.java'))
    print(f'Old common.* files: {len(files)}')
    for f in files:
        # check if new package version exists
        rel = f.relative_to(OLD_PKG)
        # rel: dto/BaseDTO.java
        # new path: model/BaseDTO.java
        new_rel = Path(str(rel).replace('dto', 'model', 1)
                                .replace('entity', 'model', 1)
                                .replace('util', 'model', 1)
                                .replace('web', 'web', 1)
                                .replace('security', 'web', 1)
                                .replace('audit', 'web', 1)
                                .replace('redis', 'redis', 1)
                                .replace('oss', 'infra', 1)
                                .replace('job', 'infra', 1), )
        new_path = OLD_PKG.parent.parent / new_rel
        if new_path.exists():
            # check if contents equal
            if f.read_bytes() == new_path.read_bytes():
                f.unlink()
                # also delete the now-empty parent
                pass
            else:
                # keep both; manual review needed
                print(f'  DIFF: {f} vs {new_path}')
    # remove empty common dir tree
    shutil.rmtree(OLD_PKG)
    # walk up removing empty
    parent = OLD_PKG.parent
    while parent != OLD_PKG.parent.parent and parent.exists() and not any(parent.iterdir()):
        parent.rmdir()
        parent = parent.parent
    print(f'Removed old common.* dir')
else:
    print('old common.* not found')

print()
print('=== Final core/ src/main/java/ tree ===')
import os
for root, dirs, files in os.walk(r'E:\claude\smart-workshop-erp\backend\src\core\src\main\java'):
    rel = os.path.relpath(root, r'E:\claude\smart-workshop-erp\backend\src\core\src\main\java')
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
