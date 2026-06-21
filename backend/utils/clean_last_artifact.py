import ctypes
from ctypes import wintypes
import os
os.chdir(r'E:\claude\smart-workshop-erp')
kernel32 = ctypes.WinDLL('kernel32', use_last_error=True)
kernel32.DeleteFileW.argtypes = [wintypes.LPCWSTR]
kernel32.DeleteFileW.restype = wintypes.BOOL

# Walk all files
for root, dirs, files in os.walk('.'):
    for fn in files:
        if fn.startswith('**'):
            full = os.path.abspath(os.path.join(root, fn))
            full = full.replace('/', '\\')
            ok = kernel32.DeleteFileW(full)
            err = ctypes.get_last_error()
            print(f'  DeleteFileW({full}) -> {ok}, err={err}')
