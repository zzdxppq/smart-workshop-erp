"""Print final backend tree."""
import os
print('=== Final backend/ tree ===')
for root, dirs, files in os.walk(r'E:\claude\smart-workshop-erp\backend'):
    rel = os.path.relpath(root, r'E:\claude\smart-workshop-erp\backend')
    depth = 0 if rel == '.' else rel.count(os.sep) + 1
    if depth > 4:
        del dirs[:]
        continue
    indent = '  ' * depth
    name = rel if rel != '.' else 'backend'
    print(f'{indent}{name}/')
    for f in sorted(files)[:6]:
        print(f'{indent}  {f}')
    if len(files) > 6:
        print(f'{indent}  ... and {len(files)-6} more')

print()
print('=== sizes ===')
import subprocess
r = subprocess.run(['du', '-sh', 'backend'], capture_output=True, text=True, cwd=r'E:\claude\smart-workshop-erp')
print(r.stdout.strip())
