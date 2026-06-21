"""Flatten 9 common.* sub-packages in core/ to 4 root sub-packages.

Source:  com.btsheng.erp.core.common.{dto,entity,util,web,security,redis,audit,oss,job}.*
Target:  com.btsheng.erp.core.{model,web,redis,infra}.*

Mapping:
  common.dto      -> model
  common.entity   -> model
  common.util     -> model    (Money, SnowflakeIdGenerator, DictCache, AesGcmUtil, BcryptStrengthChecker, QuoteApprovalRouter, JwtSigner, PasswordValidator, UsernameNormalizer)
  common.web      -> web
  common.security -> web      (JwtUtil, SecurityConfig, DataScope, AesGcmTypeHandler, DekLoader, DekHealthController)
  common.audit    -> web      (AuditLog, AuditAspect, AuditErrorEntity)
  common.redis    -> redis
  common.oss      -> infra
  common.job      -> infra
"""
import os
import re
import shutil
from pathlib import Path
from collections import defaultdict

CORE_SRC = Path(r'E:\claude\smart-workshop-erp\backend\src\core\src\main\java')
CORE_TEST = Path(r'E:\claude\smart-workshop-erp\backend\src\core\src\test\java')

PACKAGE_MAP = {
    'dto':      'model',
    'entity':   'model',
    'util':     'model',
    'web':      'web',
    'security': 'web',
    'audit':    'web',
    'redis':    'redis',
    'oss':      'infra',
    'job':      'infra',
}

OLD_ROOT = 'com.btsheng.erp.core.common'
NEW_ROOT = 'com.btsheng.erp.core'


def flatten(base: Path) -> tuple:
    """Returns (moved_count, error_count)."""
    if not base.exists():
        return 0, 0

    moved = 0
    errors = []
    for src_file in list(base.rglob('*.java')):
        try:
            text = src_file.read_text(encoding='utf-8')
        except Exception as e:
            errors.append((str(src_file), str(e)))
            continue

        m = re.match(r'package\s+([\w.]+)\s*;', text)
        if not m:
            errors.append((str(src_file), 'no package decl'))
            continue

        old_pkg = m.group(1)
        if not old_pkg.startswith(OLD_ROOT):
            continue  # skip non-core.common files

        # parse: com.btsheng.erp.core.common.<sub>.<rest>
        rel = old_pkg[len(OLD_ROOT):].lstrip('.')  # "<sub>.<rest>" or just "<sub>"
        parts = rel.split('.')
        if not parts:
            continue
        sub = parts[0]
        rest = parts[1:]
        if sub not in PACKAGE_MAP:
            errors.append((str(src_file), f'unknown sub {sub}'))
            continue
        new_sub = PACKAGE_MAP[sub]
        new_pkg = NEW_ROOT + '.' + new_sub + ('.' + '.'.join(rest) if rest else '')

        # Compute target path: <base>/<new_pkg_path>/<name>
        new_pkg_path = new_pkg.replace('.', os.sep)
        target = base / new_pkg_path / src_file.name
        target.parent.mkdir(parents=True, exist_ok=True)

        # Rewrite package decl + any cross-references
        new_text = re.sub(
            r'package\s+com\.btsheng\.erp\.core\.common\.\w+((?:\.\w+)*)\s*;',
            lambda mm: f'package {NEW_ROOT}.{new_sub}{mm.group(1)};',
            text, count=1
        )
        # Also rewrite import statements referencing old packages
        new_text = re.sub(
            r'import\s+com\.btsheng\.erp\.core\.common\.(\w+)((?:\.\w+)*)\s*;',
            lambda mm: f'import {NEW_ROOT}.{PACKAGE_MAP[mm.group(1)]}{mm.group(2)};',
            new_text
        )
        target.write_text(new_text, encoding='utf-8')
        moved += 1

    # Cleanup: remove now-empty com/btsheng/erp/core/common/ tree
    common_dir = base / 'com' / 'btsheng' / 'erp' / 'core' / 'common'
    if common_dir.exists():
        shutil.rmtree(common_dir)
        # walk up removing any empty parents
        parent = common_dir.parent
        while parent != base and parent.exists() and not any(parent.iterdir()):
            parent.rmdir()
            parent = parent.parent

    return moved, len(errors)


print('=== Flattening main src ===')
moved_main, err_main = flatten(CORE_SRC)
print(f'  Moved: {moved_main}, Errors: {err_main}')

print('=== Flattening test src ===')
moved_test, err_test = flatten(CORE_TEST)
print(f'  Moved: {moved_test}, Errors: {err_test}')

# Final inventory
print()
print('=== Final core/ structure ===')
import os
for root, dirs, files in os.walk(CORE_SRC):
    rel = os.path.relpath(root, CORE_SRC)
    depth = 0 if rel == '.' else rel.count(os.sep) + 1
    if depth > 3:
        del dirs[:]
        continue
    indent = '  ' * depth
    name = rel if rel != '.' else 'src/main/java'
    print(f'{indent}{name}/')
    for f in sorted(files)[:6]:
        print(f'{indent}  {f}')
    if len(files) > 6:
        print(f'{indent}  ... and {len(files)-6} more')
