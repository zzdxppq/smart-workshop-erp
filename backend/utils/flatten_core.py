"""Flatten backend/src/core/ 9 common-* sub-modules into 1 erp-core.

Strategy:
1. Inventory all .java files in 9 sub-modules
2. Read each file's package declaration
3. Map current package to new package
4. Rewrite package decl + Write to new location
5. Delete 9 old sub-module dirs (pom + src)
6. Update top-level core/pom.xml: keep erp-core as single module
7. Update 4 service poms: change deps to erp-core (already correct)
8. Update top-level backend/pom.xml: <module>core</module> (already correct)
"""
import os
import re
import shutil
from pathlib import Path

ROOT = Path(r'E:\claude\smart-workshop-erp\backend')
CORE = ROOT / 'src' / 'core'
OLD_MODULES = [
    'common-dto',
    'common-entity',
    'common-util',
    'common-web',
    'common-security',
    'common-redis',
    'common-audit',
    'common-oss',
    'common-job',
]

# Step 1: inventory all .java files
print('=== Inventory before flatten ===')
total = 0
for sub in OLD_MODULES:
    java_files = list((CORE / sub / 'src' / 'main' / 'java').rglob('*.java')) if (CORE / sub / 'src').exists() else []
    print(f'  {sub}: {len(java_files)} java files')
    total += len(java_files)
print(f'  TOTAL: {total}')

# Step 2: build package -> new package map by reading each file
# New package scheme (flat 4 root packages):
#   common-dto      -> com.btsheng.erp.core.model
#   common-entity   -> com.btsheng.erp.core.model
#   common-util     -> com.btsheng.erp.core.model  (utilities / crypto / router)
#   common-web      -> com.btsheng.erp.core.web
#   common-security -> com.btsheng.erp.core.web
#   common-redis    -> com.btsheng.erp.core.redis
#   common-audit    -> com.btsheng.erp.core.web
#   common-oss      -> com.btsheng.erp.core.infra
#   common-job      -> com.btsheng.erp.core.infra
PACKAGE_MAP = {
    'common-dto':      'com.btsheng.erp.core.model',
    'common-entity':   'com.btsheng.erp.core.model',
    'common-util':     'com.btsheng.erp.core.model',
    'common-web':      'com.btsheng.erp.core.web',
    'common-security': 'com.btsheng.erp.core.web',
    'common-redis':    'com.btsheng.erp.core.redis',
    'common-audit':    'com.btsheng.erp.core.web',
    'common-oss':      'com.btsheng.erp.core.infra',
    'common-job':      'com.btsheng.erp.core.infra',
}

# Step 3: for each java file, compute target path and write
moved = 0
errors = []
for sub in OLD_MODULES:
    src_root = CORE / sub / 'src' / 'main' / 'java'
    if not src_root.exists():
        continue

    # Detect current package from the file (to preserve sub-package structure)
    for src_file in src_root.rglob('*.java'):
        try:
            text = src_file.read_text(encoding='utf-8')
        except Exception as e:
            errors.append((str(src_file), str(e)))
            continue

        # Extract `package xxx.yyy;` line
        m = re.match(r'package\s+([\w.]+)\s*;', text)
        if not m:
            errors.append((str(src_file), 'no package decl'))
            continue
        old_pkg = m.group(1)
        # old_pkg starts with com.btsheng.erp.{sub}.<sub_path>
        # compute relative sub_path
        prefix = f'com.btsheng.erp.{sub}.'
        if old_pkg.startswith(prefix):
            rel = old_pkg[len(prefix):]  # e.g. "common.security.JwtUtil"
        elif old_pkg == f'com.btsheng.erp.{sub}':
            rel = ''
        else:
            rel = old_pkg  # unusual; just use as is

        new_root_pkg = PACKAGE_MAP[sub]
        if rel:
            new_pkg = f'{new_root_pkg}.{rel}'
        else:
            new_pkg = new_root_pkg

        # Compute target path:
        # old: src/main/java/com/btsheng/erp/{sub}/<rest>/Foo.java
        # new: src/main/java/<new_pkg_path>/Foo.java
        new_pkg_path = new_pkg.replace('.', '/')
        target = CORE / 'src' / 'main' / 'java' / new_pkg_path / src_file.name
        target.parent.mkdir(parents=True, exist_ok=True)

        # Rewrite package decl
        new_text = re.sub(r'package\s+[\w.]+\s*;', f'package {new_pkg};', text, count=1)
        target.write_text(new_text, encoding='utf-8')
        moved += 1

print(f'\nMoved {moved} java files')
if errors:
    print(f'Errors: {len(errors)}')
    for f, e in errors[:5]:
        print(f'  {f}: {e}')

# Step 4: handle test files (move them to src/test/java)
moved_tests = 0
for sub in OLD_MODULES:
    test_root = CORE / sub / 'src' / 'test' / 'java'
    if not test_root.exists():
        continue
    for src_file in test_root.rglob('*.java'):
        try:
            text = src_file.read_text(encoding='utf-8')
        except Exception:
            continue
        m = re.match(r'package\s+([\w.]+)\s*;', text)
        if not m:
            continue
        old_pkg = m.group(1)
        prefix = f'com.btsheng.erp.{sub}.'
        if old_pkg.startswith(prefix):
            rel = old_pkg[len(prefix):]
        elif old_pkg == f'com.btsheng.erp.{sub}':
            rel = ''
        else:
            rel = old_pkg
        new_root_pkg = PACKAGE_MAP[sub]
        new_pkg = f'{new_root_pkg}.{rel}' if rel else new_root_pkg
        new_pkg_path = new_pkg.replace('.', '/')
        target = CORE / 'src' / 'test' / 'java' / new_pkg_path / src_file.name
        target.parent.mkdir(parents=True, exist_ok=True)
        new_text = re.sub(r'package\s+[\w.]+\s*;', f'package {new_pkg};', text, count=1)
        target.write_text(new_text, encoding='utf-8')
        moved_tests += 1
print(f'Moved {moved_tests} test files')

# Step 5: delete 9 old sub-module dirs (preserve any pom.xml in test/pom.xml)
deleted = 0
for sub in OLD_MODULES:
    sub_dir = CORE / sub
    if sub_dir.exists():
        shutil.rmtree(sub_dir)
        deleted += 1
print(f'\nDeleted {deleted} sub-module dirs')

# Step 6: clean up any stray empty src/test dirs left
for d in [CORE / 'src' / 'main', CORE / 'src' / 'test']:
    if d.exists() and not any(d.iterdir()):
        d.rmdir()
        print(f'rmdir empty: {d}')

print()
print('=== Final core/ tree ===')
import os as _os
for root, dirs, files in _os.walk(CORE):
    rel = _os.path.relpath(root, CORE)
    depth = 0 if rel == '.' else rel.count(_os.sep) + 1
    if depth > 3:
        del dirs[:]
        continue
    indent = '  ' * depth
    name = rel if rel != '.' else 'core'
    print(f'{indent}{name}/')
    for f in sorted(files)[:6]:
        print(f'{indent}  {f}')
    if len(files) > 6:
        print(f'{indent}  ... and {len(files)-6} more')
