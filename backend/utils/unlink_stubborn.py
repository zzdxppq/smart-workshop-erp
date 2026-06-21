"""Try os.unlink on the stubborn ** file - scandir saw it as is_file=True."""
import os
os.chdir(r'E:\claude\smart-workshop-erp')

for entry in os.scandir('.'):
    if entry.is_file() and entry.name.startswith('**'):
        print(f'Found: {entry.name!r}')
        try:
            os.unlink(entry.path)
            print(f'  unlink succeeded: {entry.name!r}')
        except Exception as e:
            print(f'  unlink failed: {e}')
            # Try via os.remove with the scandir-supplied name
            try:
                os.remove(entry.name)
                print(f'  os.remove succeeded')
            except Exception as e2:
                print(f'  os.remove failed: {e2}')

print()
print('Final entries:')
for entry in os.scandir('.'):
    if entry.is_file():
        print(f'  {entry.name!r}')
