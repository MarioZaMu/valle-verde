"""Compares the real imports between domain sub-packages with the arrows drawn in the development view.

Usage (from any folder): python tools/check_imports.py
Fails if the code has a dependency between domain packages that the diagram does not draw.
Arrows drawn but not yet present in the code are listed as information (they belong to later phases).
"""
import glob, os, re, sys
sys.stdout.reconfigure(encoding='utf-8')
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
os.chdir(ROOT)
base = 'src/main/java/edu/ucc/farmsim/'
real = set()
for f in glob.glob(base + 'domain/**/*.java', recursive=True):
    me = os.path.relpath(os.path.dirname(f), base).replace(os.sep, '.')
    for m in re.findall(r'^import edu\.ucc\.farmsim\.(domain\.\w+)\.', open(f, encoding='utf-8').read(), re.M):
        if m != me and m != 'domain.config':
            real.add((me, m))
dev = open('docs/diagramas/04_vista_desarrollo.puml', encoding='utf-8').read()
alias = {}; stack = []
for line in dev.splitlines():
    s = line.strip(); m = re.match(r'package "([^"]+)"\s+as\s+(\w+).*\{$', s)
    if m: stack.append(m.group(1)); alias[m.group(2)] = '.'.join(stack[1:]); continue
    if s == '}' and stack: stack.pop()
edges = re.findall(r'^(\w+) \.(?:down|up)?\.?> (\w+)', dev, re.M) + re.findall(r'^(\w+) \.\.> (\w+)', dev, re.M)
drawn = {(alias[a], alias[b]) for a, b in edges if a in alias and b in alias}
missing = sorted(real - drawn)
print('dependencias reales entre paquetes de dominio:', sorted(real))
print('FALTAN en el diagrama:' if missing else 'Todas las dependencias reales están dibujadas.', missing if missing else '')
planned = sorted(e for e in drawn if e[0].startswith('domain.') and e[1].startswith('domain.') and e not in real)
print('dibujadas y aún sin código (fases siguientes):', planned)
sys.exit(1 if missing else 0)
