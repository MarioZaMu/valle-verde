"""Coherence checks between the PlantUML diagrams (run from the project root: python tools/check_diagrams.py)."""
import re, sys
sys.stdout.reconfigure(encoding='utf-8')
D='docs/diagramas/'
rd=lambda f: open(D+f,encoding='utf-8').read()
logica=rd('03_vista_logica.puml'); desarrollo=rd('04_vista_desarrollo.puml'); seq=rd('A1_secuencia_vender_cosecha.puml')
errs=[]

# --- classes of the logical view with their package path ---
classes={}; ops={}; stack=[]
for line in logica.splitlines():
    s=line.strip()
    m=re.match(r'package "([^"]+?)"',s)
    if m: stack.append(re.split(r'\s',m.group(1))[0]); continue
    m=re.match(r'(?:abstract\s+)?(?:class|interface)\s+([A-Za-z]+)',s)
    if m:
        cur=m.group(1); classes[cur]='.'.join(stack); ops.setdefault(cur,set()); continue
    m=re.match(r'[+\-]\{?\w*\}?\s*\{?\w*\}?\s*([A-Za-z]+)\(',s)
    if m and 'cur' in dir(): ops[cur].add(m.group(1))
    if s=='}' and stack and not s.startswith('}') is False: pass
# package stack handling: closing braces pop only package braces -> recompute with brace depth
classes={}; ops={}; stack=[]; depth=[]; cur=None
for line in logica.splitlines():
    s=line.strip()
    m=re.match(r'package "([^"]+?)".*\{$',s)
    if m: stack.append(re.split(r'\s',m.group(1))[0]); depth.append('p'); continue
    m=re.match(r'(?:abstract\s+)?(?:class|interface)\s+([A-Za-z]+)(.*)',s)
    if m:
        cur=m.group(1); classes[cur]='.'.join(stack); ops.setdefault(cur,set())
        if s.endswith('{'): depth.append('c')
        continue
    if s=='}':
        if depth:
            k=depth.pop()
            if k=='p': stack.pop()
            else: cur=None
        continue
    m=re.match(r'[+\-]\s*(?:\{\w+\}\s*)?([A-Za-z]+)\(',s)
    if m and cur: ops[cur].add(m.group(1))
# realizations: X ..|> Y  and  Y <|.. X
real={}
for a,b in re.findall(r'^(\w+) \.\.\|> (\w+)',logica,re.M): real.setdefault(a,set()).add(b)
for b,a in re.findall(r'^(\w+) <\|\.\. (\w+)',logica,re.M): real.setdefault(a,set()).add(b)
def has_op(c,o):
    return o in ops.get(c,set()) or any(has_op(p,o) for p in real.get(c,()))

# --- development view: package of each class ---
dev={}; stack=[]; depth=[]
for line in desarrollo.splitlines():
    s=line.strip()
    m=re.match(r'package "([^"]+)"\s+as\s+\w+.*\{$',s)
    if m: stack.append(m.group(1)); depth.append(1); continue
    m=re.match(r'class (\w+)',s)
    if m: dev[m.group(1)]='.'.join(stack[1:]); continue   # drop root edu.ucc.farmsim
    if s=='}' and stack: stack.pop()
# logical package -> dev package (logical 'domain.game' == dev 'domain.game'; ui/application are layers in dev with subpackages)
for c,p in classes.items():
    if c not in dev: continue
    layer=p.split('.')[0]; dp=dev[c]
    if p.startswith('domain'): ok=(dp==p)
    else: ok=dp.split('.')[0]==layer
    if not ok: errs.append(f'paquete incoherente: {c} lógica={p} desarrollo={dp}')
# classes of the dev view that claim to be domain/ui but absent in logical are fine (dto, services...), but report counts
common=[c for c in classes if c in dev]
print(f'clases en vista lógica: {len(classes)} · en desarrollo: {len(dev)} · comunes: {len(common)}')

# --- sequence: every solid message must be an operation of the target class ---
parts=set(re.findall(r'^(?:participant|actor)\s+(\w+)',seq,re.M))
msgs=re.findall(r'^\s*(\w+)\s+->\s+(\w+)\s*:\s*(?:<b>)?([A-Za-z]+)\(',seq,re.M)
for a,b,op in msgs:
    if b not in classes: errs.append(f'secuencia: participante {b} no está en la vista lógica'); continue
    if not has_op(b,op): errs.append(f'secuencia: {b}.{op}() no existe en el diagrama de clases')
for a in parts-{'Jugador'}:
    if a not in classes: errs.append(f'secuencia: participante {a} sin clase')
print(f'mensajes de secuencia verificados: {len(msgs)}')


# --- functional view: the 42 RF must be covered exactly once by the component labels ---
func=rd('02_vista_funcional.puml')
cov={}
for m in re.finditer(r'RF-(\d\d)(?: a (?:RF-)?(\d\d))?(?:, (?:RF-)?(\d\d))?',func.split("'/")[1]):
    a=int(m.group(1)); b=int(m.group(2) or a)
    for n in range(a,b+1): cov[n]=cov.get(n,0)+1
    if m.group(3): cov[int(m.group(3))]=cov.get(int(m.group(3)),0)+1
for n in range(1,43):
    if cov.get(n,0)!=1: errs.append(f'vista funcional: RF-{n:02d} cubierto {cov.get(n,0)} veces (debe ser 1)')
print(f'RF cubiertos en vista funcional: {sum(1 for n in range(1,43) if cov.get(n)==1)}/42')

# --- IDs cited in the .puml headers exist in docs ---
req=open('docs/REQUERIMIENTOS.md',encoding='utf-8').read(); hu=open('docs/HISTORIAS_DE_USUARIO.md',encoding='utf-8').read()
import glob
for f in glob.glob(D+'*.puml'):
    t=open(f,encoding='utf-8').read().split('*/')[0] if False else open(f,encoding='utf-8').read()
    head=t[:t.find("'/")+2] if "'/" in t else ''
    for rf in set(re.findall(r'RF-\d\d',head)):
        if f'| {rf} |' not in req: errs.append(f'{f}: {rf} inexistente')
    for h in set(re.findall(r'HU-\d\d',head)):
        if f'### {h} ·' not in hu: errs.append(f'{f}: {h} inexistente')
print('ERRORES:' if errs else 'SIN ERRORES'); [print(' -',e) for e in errs]
sys.exit(1 if errs else 0)
