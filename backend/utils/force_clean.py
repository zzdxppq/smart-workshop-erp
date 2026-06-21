"""Final cleanup: use os.scandir to find the ** file and force-delete via Win32."""
import ctypes
from ctypes import wintypes
import os

os.chdir(r'E:\claude\smart-workshop-erp')
kernel32 = ctypes.WinDLL('kernel32', use_last_error=True)
kernel32.DeleteFileW.argtypes = [wintypes.LPCWSTR]
kernel32.DeleteFileW.restype = wintypes.BOOL

# Use scandir to see all entries including hidden
print('All entries:')
for entry in os.scandir('.'):
    print(f'  {entry.name!r}  (is_file={entry.is_file()})')

# Try every name
for fn in ['**结论**：**A.', '**关联**：Story', '**评审人**：architect', '**评审日期**：2026-06-10']:
    full = r'E:\claude\smart-workshop-erp' + '\\' + fn
    ok = kernel32.DeleteFileW(full)
    print(f'  Try {fn!r}: ok={ok} err={ctypes.get_last_error()}')
