"""Cleanup **-prefixed files using Win32 DeleteFileW API."""
import ctypes
from ctypes import wintypes
import os

kernel32 = ctypes.WinDLL('kernel32', use_last_error=True)
kernel32.DeleteFileW.argtypes = [wintypes.LPCWSTR]
kernel32.DeleteFileW.restype = wintypes.BOOL

# Run from project root
PROJECT_ROOT = r'E:\claude\smart-workshop-erp'
os.chdir(PROJECT_ROOT)

for fn in os.listdir('.'):
    if fn.startswith('**'):
        # Use raw string to avoid escape
        full = os.path.abspath(fn)
        # Convert / to \
        full = full.replace('/', '\\')
        ok = kernel32.DeleteFileW(full)
        err = ctypes.get_last_error()
        print(f'  DeleteFileW({fn!r}) -> {ok}, err={err}')

print()
print('Files remaining in project root:')
for f in sorted(os.listdir('.')):
    print(f'  {f}')
