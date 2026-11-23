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


# --- functional view: the 50 RF must be covered exactly once by the component labels ---
func=rd('02_vista_funcional.puml')
cov={}
for m in re.finditer(r'RF-(\d\d)(?: a (?:RF-)?(\d\d))?(?:, (?:RF-)?(\d\d))?',func.split("'/")[1]):
    a=int(m.group(1)); b=int(m.group(2) or a)
    for n in range(a,b+1): cov[n]=cov.get(n,0)+1
    if m.group(3): cov[int(m.group(3))]=cov.get(int(m.group(3)),0)+1
for n in range(1,51):
    if cov.get(n,0)!=1: errs.append(f'vista funcional: RF-{n:02d} cubierto {cov.get(n,0)} veces (debe ser 1)')
print(f'RF cubiertos en vista funcional: {sum(1 for n in range(1,51) if cov.get(n)==1)}/50')


# --- development view: no cycles and layer rules (arrows A ..> B between package aliases) ---
edges=re.findall(r'^(\w+) \.(?:down|up)?\.?> (\w+)',desarrollo,re.M)
edges+=re.findall(r'^(\w+) \.\.> (\w+)',desarrollo,re.M)
graph={}
for a,b in set(edges): graph.setdefault(a,set()).add(b)
def find_cycle():
    state={}
    def dfs(n,path):
        state[n]=1
        for m in graph.get(n,()):
            if state.get(m)==1: return path+[n,m]
            if state.get(m) is None:
                r=dfs(m,path+[n])
                if r: return r
        state[n]=2
    for n in list(graph):
        if state.get(n) is None:
            r=dfs(n,[])
            if r: return r
cy=find_cycle()
if cy: errs.append('vista de desarrollo: ciclo de dependencias '+' -> '.join(cy))
layer=lambda n: {'UI':'ui','APP':'application','DOM':'domain','D':'domain','INF':'infrastructure','ROOT':'root','CLI':'client','API':'api','CFG':'root'}.get(n.split('_')[0],'x')
allowed={'ui':{'ui','application'},'client':{'client','application'},'api':{'api','application'},'application':{'application','domain'},'domain':{'domain'},'infrastructure':{'infrastructure','domain'},'root':{'ui','client','api','application','domain','infrastructure','root'}}
for a,b in set(edges):
    la,lb=layer(a),layer(b)
    if a=='Main': la='root'
    if la in allowed and lb not in allowed[la]: errs.append(f'vista de desarrollo: dependencia prohibida {a} ({la}) -> {b} ({lb})')
    if a.startswith('D_') and not b.startswith('D_') : errs.append(f'dominio depende de otra capa: {a} -> {b}')
print(f'dependencias de paquetes analizadas: {len(set(edges))} · ciclos: {"sí" if cy else "no"}')


# --- package dependencies derived from the CLASS diagram: must be acyclic and drawn in the development view ---
alias_path={}; stack=[]
for line in desarrollo.splitlines():
    s=line.strip()
    m=re.match(r'package "([^"]+)"\s+as\s+(\w+).*\{$',s)
    if m: stack.append(m.group(1)); alias_path[m.group(2)]='.'.join(stack[1:]); continue
    if s=='}' and stack: stack.pop()
dev_edges={(alias_path[a],alias_path[b]) for a,b in set(edges) if a in alias_path and b in alias_path}
names=set(classes)
cdeps=set()
# (a) types used in member signatures
for line in logica.splitlines():
    pass
cur=None; depth=[]
for line in logica.splitlines():
    s=line.strip()
    if re.match(r'package ".*\{$',s): depth.append('p'); continue
    m=re.match(r'(?:abstract\s+)?(?:class|interface)\s+(\w+)',s)
    if m:
        cur=m.group(1)
        if s.endswith('{'): depth.append('c')
        continue
    if s=='}':
        if depth and depth.pop()=='c': cur=None
        continue
    if cur and re.match(r'[+\-]',s):
        for tok in set(re.findall(r'\b[A-Z]\w+\b',s)):
            if tok in names and tok!=cur: cdeps.add((cur,tok))
# (b) relationship lines
for a,arrow,b in re.findall(r'^(\w+)\s+(?:"[^"]*"\s+)?([<>|.\-*o]{2,6})\s+(?:"[^"]*"\s+)?(\w+)',logica,re.M):
    if a not in names or b not in names: continue
    if arrow.startswith('<|') or arrow.startswith('<.'): cdeps.add((b,a))      # B hereda/realiza A -> B depende de A
    else: cdeps.add((a,b))
def pkg(c):
    p=classes[c]
    return p if p.split('.')[0] in ('domain',) else p.split('.')[0]
pedges={(pkg(a),pkg(b)) for a,b in cdeps if pkg(a)!=pkg(b)}
g2={}
for a,b in pedges:
    if b!='domain.config': g2.setdefault(a,set()).add(b)
def cyc2():
    st={}
    def d(n,path):
        st[n]=1
        for m in g2.get(n,()):
            if st.get(m)==1: return path+[n,m]
            if st.get(m) is None:
                r=d(m,path+[n])
                if r: return r
        st[n]=2
    for n in list(g2):
        if st.get(n) is None:
            r=d(n,[])
            if r: return r
c2=cyc2()
if c2: errs.append('vista lógica: ciclo de dependencias entre paquetes '+' -> '.join(c2))
for a,b in sorted(pedges):
    if a.startswith('domain.') and b.startswith('domain.') and b!='domain.config' and (a,b) not in dev_edges:
        errs.append(f'la vista lógica implica {a} -> {b} pero la vista de desarrollo no lo dibuja')
    la,lb=a.split('.')[0],b.split('.')[0]
    if la!=lb and (la,lb) not in {('ui','application'),('client','application'),('api','application'),('application','domain'),('infrastructure','domain')}:
        errs.append(f'dependencia entre capas prohibida en la vista lógica: {a} -> {b}')
print(f'dependencias entre paquetes derivadas de las clases: {len(pedges)} · ciclos: {"sí" if c2 else "no"}')

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
