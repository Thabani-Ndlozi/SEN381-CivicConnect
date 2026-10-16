#!/usr/bin/env python3
"""Lightweight repository static/security gate (not a substitute for a dedicated SAST)."""
import pathlib,re,sys,subprocess
root=pathlib.Path(__file__).resolve().parents[1]
ignored={'node_modules','.git','target','dist','evidence','archive','docs'}
fail=[]
tracked = None
if (root/'.git').exists():
    cp=subprocess.run(['git','-C',str(root),'ls-files','-z'],capture_output=True,check=False)
    if cp.returncode==0: tracked={root/pathlib.Path(p.decode()) for p in cp.stdout.split(b'\0') if p}
for path in root.rglob('*'):
 if not path.is_file() or any(p in ignored for p in path.relative_to(root).parts):continue
 if tracked is not None and path not in tracked:continue
 if path.name in ('.env','.env.staging') or (path.name.startswith('.env.') and path.name not in ('.env.example','.env.staging.example')):
  if tracked is not None:fail.append(f'Secret environment file tracked by git: {path.relative_to(root)}')
  continue
 if path.suffix not in {'.java','.ts','.tsx','.yml','.yaml','.py','.js'}:continue
 s=path.read_text(errors='replace')
 if re.search(r'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----',s):fail.append(f'Private key detected: {path}')
 if re.search(r'(?i)(?:AKIA[0-9A-Z]{16}|ghp_[0-9A-Za-z]{36})',s):fail.append(f'Credential-like token: {path}')
 if path.suffix=='.java' and 'System.out.println(' in s:fail.append(f'Direct stdout logging: {path}')
 if path.suffix in {'.ts','.tsx'} and 'dangerouslySetInnerHTML' in s:fail.append(f'Unsafe HTML rendering: {path}')
for item in fail:print('FAIL',item)
if fail:sys.exit(1)
print('PASS: limited local static/secret-pattern checks; dedicated dependency audit runs in CI')
