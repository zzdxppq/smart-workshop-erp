"""Final flatten: handle hyphen-named common-* packages too.

Structure to fix:
  OLD (hyphen):
    com.btsheng.erp.core.common-{sub}/Foo.java   (dev agent wrote these as Maven module names in package decl)
  NEW (dot):
    com.btsheng.erp.core.{new_sub}/Foo.java

Mapping:
  common-audit          -> web         (AuditLog + AuditAspect)
  common-cost-aggregator -> infra       (V1.3.4 料号 5 段)
  common-dto            -> model
  common-email          -> infra       (V1.3.7 163 SMTP)
  common-entity         -> model
  common-job            -> infra
  common-oss            -> infra
  common-outsub-eta     -> infra       (V1.3.4 历史交期)
  common-redis          -> redis
  common-security       -> web
  common-state-machine  -> infra       (V1.3.4 7 状态机)
  common-util           -> model
  common-web            -> web
"""
import os
import re
import shutil
from pathlib import Path

CORE_MAIN = Path(r'E:\claude\smart-workshop-erp\backend\src\core\src\main\java')
CORE_TEST = Path(r'E:\claude\smart-workshop-erp\backend\src\core\src\test\java')

# Old hyphen dirs (from dev agent's misnamed packages)
HYPHEN_DIRS = [
    ('common-audit', 'web', 'AuditAspect.java/AuditLog.java'),
    ('common-cost-aggregator', 'infra', 'V1.3.4 料号 5 段'),
    ('common-dto', 'model', ''),
    ('common-email', 'infra', 'V1.3.7 163 SMTP'),
    ('common-entity', 'model', ''),
    ('common-job', 'infra', ''),
    ('common-oss', 'infra', ''),
    ('common-outsub-eta', 'infra', 'V1.3.4 历史交期'),
    ('common-redis', 'redis', ''),
    ('common-security', 'web', ''),
    ('common-state-machine', 'infra', 'V1.3.4 7 状态机'),
    ('common-util', 'model', ''),
    ('common-web', 'web', ''),
]

def flatten_dir(core_base: Path, src_subpath: str) -> tuple:
    """src_subpath is 'main' or 'test'."""
    src_root = core_base / 'src' / src_subpath / 'java'
    if not src_root.exists():
        return 0, 0

    base = src_root / 'com' / 'btsheng' / 'erp' / 'core'
    moved = 0
    errors = []

    for old_hyphen, new_sub, note in HYPHEN_DIRS:
        old_dir = base / old_hyphen
        if not old_dir.exists():
            continue

        new_dir = base / new_sub
        new_dir.mkdir(parents=True, exist_ok=True)

        for f in list(old_dir.rglob('*.java')):
            try:
                text = f.read_text(encoding='utf-8')
            except Exception as e:
                errors.append((str(f), str(e)))
                continue

            # Rewrite package: com.btsheng.erp.core.common-X(.Y)? -> com.btsheng.erp.core.{new_sub}(.Y)?
            old_pkg_pattern = re.compile(
                r'package\s+com\.btsheng\.erp\.core\.' + re.escape(old_hyphen) + r'((?:\.\w+)*)\s*;'
            )
            text = old_pkg_pattern.sub(
                lambda m: f'package com.btsheng.erp.core.{new_sub}{m.group(1)};',
                text, count=1
            )

            # Rewrite imports too
            old_imp_pattern = re.compile(
                r'import\s+com\.btsheng\.erp\.core\.' + re.escape(old_hyphen) + r'((?:\.\w+)*)\s*;'
            )
            text = old_imp_pattern.sub(
                lambda m: f'import com.btsheng.erp.core.{new_sub}{m.group(1)};',
                text
            )

            # Determine relative sub-package preserved
            rel = f.relative_to(old_dir)
            if rel.parent == Path('.'):
                # direct child of old_dir
                target = new_dir / f.name
            else:
                # has sub-path
                target = new_dir / rel
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(text, encoding='utf-8')
            moved += 1

        # Remove the now-empty old hyphen dir
        try:
            shutil.rmtree(old_dir)
        except OSError:
            pass

    return moved, len(errors)


# main
moved_main, err_main = flatten_dir(CORE_MAIN, 'main')
print(f'main: moved={moved_main} errors={err_main}')

# test
moved_test, err_test = flatten_dir(CORE_TEST, 'test')
print(f'test: moved={moved_test} errors={err_test}')

# Walk up removing empty com/btsheng/erp/core/ parents if needed
for core_base in [CORE_MAIN, CORE_TEST]:
    if not core_base.exists():
        continue
    com_dir = core_base / 'com'
    # Don't rmdir com/; only rmdir empty sub-dirs of core
    core_dir = com_dir / 'btsheng' / 'erp' / 'core'
    if core_dir.exists():
        for d in sorted(core_dir.iterdir(), reverse=True):
            if d.is_dir() and d.name.startswith('common-'):
                # should already be removed above
                if d.exists():
                    shutil.rmtree(d, ignore_errors=True)
            elif d.is_dir() and not any(d.iterdir()):
                d.rmdir()

# Verify
import re
java_files = list(CORE_MAIN.rglob('*.java'))
print(f'\nTotal java files in main: {len(java_files)}')

# Group by package
from collections import Counter
pkgs = Counter()
for j in java_files:
    try:
        text = j.read_text(encoding='utf-8')
        m = re.match(r'package\s+([\w.]+)\s*;', text)
        if m:
            pkgs[m.group(1)] += 1
    except:
        pass

# Show packages
print('\nBy package:')
for p, n in sorted(pkgs.items()):
    print(f'  {p}: {n}')

# Check for any remaining common-* dir
print('\ncommon-* dirs remaining:')
for d in CORE_MAIN.rglob('common-*'):
    print(f'  {d}')

# Show final tree
print('\n=== Final core/src/main/java/ tree ===')
for root, dirs, files in os.walk(CORE_MAIN):
    rel = os.path.relpath(root, CORE_MAIN)
    depth = 0 if rel == '.' else rel.count(os.sep) + 1
    if depth > 4:
        del dirs[:]
        continue
    indent = '  ' * depth
    name = rel if rel != '.' else '.'
    print(f'{indent}{name}/')
    for f in sorted(files)[:5]:
        print(f'{indent}  {f}')
    if len(files) > 5:
        print(f'{indent}  ... and {len(files)-5} more')
