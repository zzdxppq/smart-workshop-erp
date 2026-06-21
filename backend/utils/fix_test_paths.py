"""Fix backend/src/test wrong path nesting."""
from pathlib import Path
import shutil

# The path backend/src/test/src/test/java/... is wrong
# It should be backend/src/test/java/...
wrong_path = Path(r'E:\claude\smart-workshop-erp\backend\src\test\src\test\java\com\btsheng\erp\e2e\.gitkeep')
correct_dir = Path(r'E:\claude\smart-workshop-erp\backend\src\test\java\com\btsheng\erp\e2e')

if wrong_path.exists():
    correct_dir.mkdir(parents=True, exist_ok=True)
    target = correct_dir / '.gitkeep'
    shutil.move(str(wrong_path), str(target))
    print(f'moved: {wrong_path} -> {target}')

# Now remove empty parent dirs of wrong path (bottom-up)
for p_str in [
    r'E:\claude\smart-workshop-erp\backend\src\test\src\test\java\com\btsheng\erp\e2e',
    r'E:\claude\smart-workshop-erp\backend\src\test\src\test\java\com\btsheng\erp',
    r'E:\claude\smart-workshop-erp\backend\src\test\src\test\java\com\btsheng',
    r'E:\claude\smart-workshop-erp\backend\src\test\src\test\java\com',
    r'E:\claude\smart-workshop-erp\backend\src\test\src\test\java',
    r'E:\claude\smart-workshop-erp\backend\src\test\src\test',
    r'E:\claude\smart-workshop-erp\backend\src\test\src',
]:
    p = Path(p_str)
    if p.exists() and not any(p.iterdir()):
        p.rmdir()
        print(f'rmdir: {p}')

print()
print('Final backend/src/test:')
import os
for root, dirs, files in os.walk(r'E:\claude\smart-workshop-erp\backend\src\test'):
    rel = root.replace(r'E:\claude\smart-workshop-erp\backend\src\test', '.') or '.'
    depth = rel.count(os.sep)
    print('  ' + '  ' * depth + rel + '/')
    for f in files:
        print('  ' + '  ' * (depth+1) + f)
