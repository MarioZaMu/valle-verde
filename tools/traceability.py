"""Traceability matrix: requirement -> user story -> functional component -> classes -> tests.

Usage (from any folder):
    python tools/traceability.py              regenerate docs/TRAZABILIDAD.md, fail only on broken references
    python tools/traceability.py --upto 5.2   additionally require every story of phases <= 5.2 to have
                                              at least as many HUxx_ tests as acceptance criteria and a class citing it

Conventions it relies on:
  * test methods are named  HUxx_caN_description  (docs/PLAN_DE_PRUEBAS.md section 3);
  * every production class has a Javadoc line  'Stories: HU-01, HU-03'  (or 'Stories: none' for technical classes).
"""
import os, re, sys, glob
sys.stdout.reconfigure(encoding='utf-8')
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
os.chdir(ROOT)
rd = lambda p: open(p, encoding='utf-8').read()

hu_doc, req_doc, func = rd('docs/HISTORIAS_DE_USUARIO.md'), rd('docs/REQUERIMIENTOS.md'), rd('docs/diagramas/02_vista_funcional.puml')
errors, warnings = [], []

# --- stories: title, RF list, number of acceptance criteria, phase ---
S = {}
for b in re.split(r'^### (?=HU-\d\d ·)', hu_doc, flags=re.M)[1:]:
    b = b.split('\n---')[0]
    m = re.match(r'(HU-\d\d) · (.*)', b)
    meta = re.search(r'\*\*Prioridad:\*\* (\w+) · \*\*Puntos:\*\* (\d+) · \*\*RF:\*\* (.*?) · \*\*RN:', b)
    S[m.group(1)] = dict(title=m.group(2).strip(), pri=meta.group(1), rf=re.findall(r'RF-\d\d', meta.group(3)),
                         n=len(re.findall(r'^- \*\*CA-\d+', b, re.M)), phase='?')
for ph, sec in re.findall(r'^### Fase (5\.\d) · .*?\n(.*?)(?=^### |\Z)', hu_doc, flags=re.M | re.S):
    for h in set(re.findall(r'\| (HU-\d\d) ·', sec)):
        S[h]['phase'] = ph
RF = dict(re.findall(r'^\| (RF-\d\d) \| .*? \| (Must|Should|Could) \|', req_doc, re.M))

# --- functional component of each RF (from the component view) ---
comp = {}
for label in re.findall(r'label="([^"]*RF-[^"]*)"', func):
    name = label.split('\\n')[0].strip()
    for a, b, c in re.findall(r'RF-(\d\d)(?: a (?:RF-)?(\d\d))?(?:, (?:RF-)?(\d\d))?', label):
        for n in range(int(a), int(b or a) + 1): comp[f'RF-{n:02d}'] = name
        if c: comp[f'RF-{int(c):02d}'] = name

# --- production classes and the stories they cite ---
classes = {}   # simple name -> list of HU
for f in glob.glob('src/main/java/**/*.java', recursive=True):
    name = os.path.basename(f)[:-5]
    if name == 'package-info': continue
    text = rd(f)
    m = re.search(r'Stories:\s*([^\n*]*)', text)
    if not m:
        errors.append(f'{f}: falta la línea Javadoc "Stories: HU-xx" (o "Stories: none")'); continue
    ids = re.findall(r'HU-\d\d', m.group(1))
    if not ids and 'none' not in m.group(1).lower(): errors.append(f'{f}: línea Stories ilegible: {m.group(1).strip()}')
    for h in ids:
        if h not in S: errors.append(f'{f}: cita {h}, que no existe en HISTORIAS_DE_USUARIO.md')
    classes[name] = ids

# --- tests named HUxx_... ---
tests = {}     # HU id -> number of test methods
for f in glob.glob('src/test/java/**/*.java', recursive=True):
    for hu in re.findall(r'void\s+(HU\d\d)_', rd(f)):
        h = f'HU-{hu[2:]}'
        if h not in S: errors.append(f'{f}: prueba cita {h}, que no existe')
        tests[h] = tests.get(h, 0) + 1

def cls_for(h): return sorted(c for c, ids in classes.items() if h in ids)

# --- matrix ---
out = ['# Matriz de trazabilidad — Valle Verde', '',
       '> Generada por `tools/traceability.py` (no editar a mano). Cadena: **RF → HU → componente funcional → clases → pruebas**.',
       '> Las clases se enlazan con la línea Javadoc `Stories: HU-xx` y las pruebas con el prefijo `HUxx_` del método.', '',
       '## 1. Por requerimiento funcional', '',
       '| RF | Prioridad | Historias | Componente funcional | Clases | Pruebas |', '|---|---|---|---|---|---|']
for rf in sorted(RF):
    hs = [h for h, s in S.items() if rf in s['rf']]
    cl = sorted({c for h in hs for c in cls_for(h)})
    tc = ', '.join(f'{h}: {tests.get(h, 0)}' for h in hs)
    if not hs: errors.append(f'{rf} no tiene historia')
    out.append(f'| {rf} | {RF[rf]} | {", ".join(hs) or "—"} | {comp.get(rf, "—")} | {", ".join(cl) or "pendiente"} | {tc} |')
out += ['', '## 2. Por historia de usuario', '',
        '| Historia | Fase | Prioridad | Criterios | Pruebas | Estado | Clases |', '|---|---|---|---|---|---|---|']
for h in sorted(S):
    s = S[h]; t = tests.get(h, 0)
    st = 'sin pruebas' if t == 0 else ('completa' if t >= s['n'] else f'parcial ({t}/{s["n"]})')
    out.append(f'| {h} · {s["title"]} | {s["phase"]} | {s["pri"]} | {s["n"]} | {t} | {st} | {", ".join(cls_for(h)) or "pendiente"} |')
done = sum(1 for h in S if tests.get(h, 0) >= S[h]['n'])
out += ['', f'**Resumen:** {done} de {len(S)} historias con todas sus pruebas; {sum(tests.values())} pruebas `HUxx_` en total.', '']
open('docs/TRAZABILIDAD.md', 'w', encoding='utf-8').write('\n'.join(out))

# --- strict mode ---
if '--upto' in sys.argv:
    lim = sys.argv[sys.argv.index('--upto') + 1]
    for h, s in sorted(S.items()):
        if s['phase'] <= lim:
            if tests.get(h, 0) < s['n']: errors.append(f'{h}: {tests.get(h, 0)} pruebas para {s["n"]} criterios (fase {s["phase"]})')
            if not cls_for(h): errors.append(f'{h}: ninguna clase cita esta historia (fase {s["phase"]})')
print(f'historias: {len(S)} · RF: {len(RF)} · clases de producción: {len(classes)} · pruebas HUxx_: {sum(tests.values())}')
print('ERRORES:' if errors else 'SIN ERRORES'); [print(' -', e) for e in errors]
sys.exit(1 if errors else 0)
