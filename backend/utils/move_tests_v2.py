"""Move 6 test files from erp-platform to core (use rglob to find actual paths)."""
import os
import re
import shutil
from pathlib import Path

TEST_MAP = {
    'AesGcmEncryptorTest.java':        ('web',   'com.btsheng.erp.core.web.AesGcmEncryptor'),
    'BcryptStrengthCheckerTest.java':  ('model', 'com.btsheng.erp.core.model.BcryptStrengthChecker'),
    'JwtSignerTest.java':              ('web',   'com.btsheng.erp.core.web.JwtSigner'),
    'PasswordValidatorTest.java':       ('model', 'com.btsheng.erp.core.model.PasswordValidator'),
    'QuoteApprovalRouterTest.java':    ('model', 'com.btsheng.erp.core.model.QuoteApprovalRouter'),
    'UsernameNormalizerTest.java':     ('model', 'com.btsheng.erp.core.model.UsernameNormalizer'),
}

# Use rglob to find files regardless of dash vs underscore
print('=== Finding test files via rglob ===')
SRC_ROOT = Path(r'E:\claude\smart-workshop-erp\backend\src')
TEST_BASE = SRC_ROOT
all_java = list(TEST_BASE.rglob('*.java'))

moved = 0
errors = []
for name, (new_sub, new_pkg) in TEST_MAP.items():
    matches = [j for j in all_java if j.name == name]
    if not matches:
        print(f'  Skip: {name} (not found anywhere)')
        continue
    src = matches[0]
    try:
        text = src.read_text(encoding='utf-8')
    except Exception as e:
        errors.append((str(src), str(e)))
        continue

    # Rewrite package
    text = re.sub(
        r'package\s+com\.btsheng\.erp\.platform\.auth\s*;',
        f'package {new_pkg};',
        text, count=1
    )

    # Rewrite imports
    sub_path = new_pkg.rsplit('.', 1)[0]
    text = re.sub(
        r'import\s+com\.btsheng\.erp\.platform\.auth\.(\w+)\s*;',
        lambda m: f'import {sub_path}.{m.group(1)};',
        text
    )
    # Legacy common.* imports
    text = re.sub(
        r'import\s+com\.btsheng\.erp\.core\.common\.util\.(\w+)\s*;',
        lambda m: f'import com.btsheng.erp.core.model.{m.group(1)};',
        text
    )
    text = re.sub(
        r'import\s+com\.btsheng\.erp\.core\.common\.security\.(\w+)\s*;',
        lambda m: f'import com.btsheng.erp.core.web.{m.group(1)};',
        text
    )

    dst_dir = Path(r'E:\claude\smart-workshop-erp\backend\src\core\src\test\java\com\btsheng\erp\core') / new_sub
    dst_dir.mkdir(parents=True, exist_ok=True)
    dst = dst_dir / name
    dst.write_text(text, encoding='utf-8')
    src.unlink()
    moved += 1
    rel_src = src.relative_to(SRC_ROOT)
    print(f'  moved: {rel_src} -> core/test/{new_sub}/{name}')

print()
print(f'Total moved: {moved}, errors: {len(errors)}')

# Verify
print()
print('=== Final test locations ===')
for t in sorted(SRC_ROOT.rglob('*Test.java')):
    rel = t.relative_to(SRC_ROOT)
    print(f'  {rel}')

# Cleanup empty dirs in erp-platform test
print()
print('=== Cleanup erp-platform test empty dirs ===')
for root, dirs, files in os.walk(r'E:\claude\smart-workshop-erp\backend\src\erp-platform\src\test', topdown=False):
    if not dirs and not files:
        try:
            os.rmdir(root)
            print(f'  rmdir: {root}')
        except OSError:
            pass
