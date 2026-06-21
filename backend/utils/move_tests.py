"""Move 6 test files from erp_platform to core (these are core utility class tests)."""
import os
import re
import shutil
from pathlib import Path

TEST_MAP = {
    'AesGcmEncryptorTest.java':        ('web',   'com.btsheng.erp.core.web.AesGcmEncryptor'),  # moved from common-security
    'BcryptStrengthCheckerTest.java':  ('model', 'com.btsheng.erp.core.model.BcryptStrengthChecker'),
    'JwtSignerTest.java':              ('web',   'com.btsheng.erp.core.web.JwtSigner'),
    'PasswordValidatorTest.java':       ('model', 'com.btsheng.erp.core.model.PasswordValidator'),
    'QuoteApprovalRouterTest.java':    ('model', 'com.btsheng.erp.core.model.QuoteApprovalRouter'),
    'UsernameNormalizerTest.java':     ('model', 'com.btsheng.erp.core.model.UsernameNormalizer'),
}

SRC_BASE = Path(r'E:\claude\smart-workshop-erp\backend\src\erp_platform\src\test\java\com\btsheng\erp\platform\auth')
DST_BASE = Path(r'E:\claude\smart-workshop-erp\backend\src\core\src\test\java\com\btsheng\erp\core')

moved = 0
errors = []
for name, (new_sub, new_pkg) in TEST_MAP.items():
    src = SRC_BASE / name
    if not src.exists():
        print(f'  Skip (not found): {src}')
        continue
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

    # Rewrite imports from com.btsheng.erp.platform.auth.* to com.btsheng.erp.core.{new_sub}.*
    # (this is the tricky part - we need to know the original class path)
    # E.g. import com.btsheng.erp.platform.auth.AesGcmEncryptor -> com.btsheng.erp.core.{sub}.AesGcmEncryptor
    sub_path = new_pkg.rsplit('.', 1)[0]  # e.g. com.btsheng.erp.core.web
    text = re.sub(
        r'import\s+com\.btsheng\.erp\.platform\.auth\.(\w+)\s*;',
        lambda m: f'import {sub_path}.{m.group(1)};',
        text
    )

    # Also rewrite com.btsheng.erp.core.common.* legacy imports (if any)
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

    dst_dir = DST_BASE / new_sub
    dst_dir.mkdir(parents=True, exist_ok=True)
    dst = dst_dir / name
    dst.write_text(text, encoding='utf-8')
    src.unlink()
    moved += 1
    print(f'  moved: {name} -> core/{new_sub}/')

# Verify
print()
print('=== After move ===')
all_test = list((Path(r'E:\claude\smart-workshop-erp\backend\src\erp_platform\src\test\java')).rglob('*.java'))
print(f'  erp_platform/test remaining: {len(all_test)}')
core_test = list((Path(r'E:\claude\smart-workshop-erp\backend\src\core\src\test\java')).rglob('*.java'))
print(f'  core/test total: {len(core_test)}')
for t in core_test:
    print(f'    {t.relative_to("backend/src/core/src/test/java")}')

# Also cleanup empty erp_platform/test dirs
test_root = Path(r'E:\claude\smart-workshop-erp\backend\src\erp_platform\src\test')
for root, dirs, files in os.walk(test_root, topdown=False):
    if not dirs and not files:
        try:
            os.rmdir(root)
        except OSError:
            pass
