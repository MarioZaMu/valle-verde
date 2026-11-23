import os, re, sys
os.chdir(os.path.join(os.path.dirname(os.path.abspath(__file__)),'..','docs'))
sys.stdout.reconfigure(encoding='utf-8')
P='HISTORIAS_DE_USUARIO.md'
hu=open(P,encoding='utf-8').read()
req=open('REQUERIMIENTOS.md',encoding='utf-8').read()
rn=open('REGLAS_DE_NEGOCIO.md',encoding='utf-8').read()
cu=open('CASOS_DE_USO.md',encoding='utf-8').read()
rn_ids=set(re.findall(r'^### (RN-\d\d) ·',rn,re.M))
rf_info={a:(b,c) for a,b,c in re.findall(r'^\| (RF-\d\d) \| (.*?) \| (Must|Should|Could) \|',req,re.M)}
PH=[('5.1','Núcleo del turno',[13,9,8,11,28,10,33,34]),
    ('5.2','Cultivo',[1,2,3,4,5,6,7,35,36]),
    ('5.3','Clima y economía',[12,37,14,15,16,38]),
    ('5.4','Construcción y animales',[18,19,20,21,22,23,17]),
    ('5.5','Progresión y persistencia',[26,27,24,29,30,31,32]),
    ('5.6','Interfaz visual y extras (recortable)',[39,42,25,40,41]),
    ('5.7','Interfaz profesional, avatar y NPC (pedido del estudiante)',[43,44,48,45,46,47,49])]
pos={}; phase={}
for i,(pid,_,l) in enumerate(PH):
    for j,h in enumerate(l): pos[f'HU-{h:02d}']=(i,j); phase[f'HU-{h:02d}']=pid
S={}
blocks=re.split(r'^### (?=HU-\d\d ·)',hu,flags=re.M)[1:]
errs=[]
for b in blocks:
    b=b.split('\n---')[0]
    m=re.match(r'(HU-\d\d) · (.*)',b); id_,title=m.group(1),m.group(2).strip()
    meta=re.search(r'\*\*Épica:\*\* (.*?) · \*\*Prioridad:\*\* (\w+) · \*\*Puntos:\*\* (\d+) · \*\*RF:\*\* (.*?) · \*\*RN:\*\* (.*?) · \*\*Depende de:\*\* (.*)',b)
    ep,pri,pts,rfs,rns,dep=meta.groups()
    cas=re.findall(r'^- \*\*CA-(\d+) · (.*?)\.\*\*',b,re.M)
    S[id_]=dict(title=title,ep=ep,pri=pri,pts=int(pts),rf=re.findall(r'RF-\d\d',rfs),rn=re.findall(r'RN-\d\d',rns),dep=re.findall(r'HU-\d\d',dep),ca=cas,body=b)
n=len(S); print('historias:',n)
for k,s in S.items():
    if s['pts'] not in (1,2,3,5): errs.append(f'{k} puntos {s["pts"]}')
    if len(s['ca'])<3: errs.append(f'{k} <3 criterios')
    if not any(re.search(r'negativo|borde',t) for _,t in s['ca']): errs.append(f'{k} sin negativo/borde')
    for r in s['rf']:
        if r not in rf_info: errs.append(f'{k} RF inexistente {r}')
        elif rf_info[r][1]!=s['pri'] and True: pass
    for r in s['rn']:
        if r not in rn_ids: errs.append(f'{k} RN inexistente {r}')
    for d in s['dep']:
        if d not in S: errs.append(f'{k} dep inexistente {d}'); continue
        if pos[d]>=pos[k]: errs.append(f'{k} depende de {d} que va después/igual en el orden')
    if k not in pos: errs.append(f'{k} sin fase')
    # CA numbering consecutive
    nums=[int(a) for a,_ in s['ca']]
    if nums!=list(range(1,len(nums)+1)): errs.append(f'{k} numeración CA {nums}')
cov={r:[] for r in rf_info}
for k,s in S.items():
    for r in s['rf']:
        if r in cov: cov[r].append(k)
miss=[r for r,v in cov.items() if not v]
if miss: errs.append(f'RF sin HU: {miss}')
# MoSCoW coherence: HU priority = max priority of its RF? report mismatches
order={'Must':0,'Should':1,'Could':2}
for k,s in S.items():
    rp=[rf_info[r][1] for r in s['rf'] if r in rf_info]
    if rp and s['pri'] not in rp: errs.append(f'{k} prioridad {s["pri"]} no coincide con RF {rp}')
print('ERRORES:' if errs else 'SIN ERRORES'); [print(' -',e) for e in errs]
if '--write' in sys.argv and not errs:
    # backlog
    out=''; tot=0; tp={'Must':0,'Should':0,'Could':0}
    for pid,name,l in PH:
        ids=[f'HU-{h:02d}' for h in l]; sp=sum(S[i]['pts'] for i in ids); tot+=sp
        for i in ids: tp[S[i]['pri']]+=S[i]['pts']
        out+=f'### Fase {pid} · {name} — {sp} puntos ({len(ids)} historias)\n\n| Orden | Historia | Épica | Prioridad | Puntos | Depende de |\n|---|---|---|---|---|---|\n'
        for j,i in enumerate(ids,1): out+=f'| {j} | {i} · {S[i]["title"]} | {S[i]["ep"]} | {S[i]["pri"]} | {S[i]["pts"]} | {", ".join(S[i]["dep"]) or "—"} |\n'
        out+='\n'
    out+='### Resumen de puntos\n\n| Fase | Puntos | Acumulado | Must | Should | Could |\n|---|---|---|---|---|---|\n'; acc=0
    for pid,name,l in PH:
        ids=[f'HU-{h:02d}' for h in l]; c={'Must':0,'Should':0,'Could':0}
        for i in ids: c[S[i]['pri']]+=S[i]['pts']
        sp=sum(c.values()); acc+=sp
        out+=f'| {pid} · {name} | {sp} | {acc} | {c["Must"]} | {c["Should"]} | {c["Could"]} |\n'
    out+=f'| **Total** | **{tot}** | **{tot}** | **{tp["Must"]}** | **{tp["Should"]}** | **{tp["Could"]}** |\n\n'
    out+=f'> **MVP (todas las *Must*): {tp["Must"]} puntos** en {sum(1 for s in S.values() if s["pri"]=="Must")} historias. Lo *Should* y *Could* ({tp["Should"]+tp["Could"]} puntos) se puede recortar sin afectar el diseño base.\n'
    hu=hu.replace('<!-- BACKLOG -->',out.rstrip())
    # map
    eps=[]; 
    for s in S.values():
        if s['ep'] not in eps: eps.append(s['ep'])
    mp='| Épica | Historias | Puntos |\n|---|---|---|\n'
    for e in eps:
        ids=[k for k,s in S.items() if s['ep']==e]
        mp+=f'| {e} | {", ".join(i+" "+S[i]["title"] for i in ids)} | {sum(S[i]["pts"] for i in ids)} |\n'
    hu=hu.replace('<!-- MAP -->',mp.rstrip())
    # CU map
    cumap={}
    for rf,cus in re.findall(r'^\| (RF-\d\d) \| .*? \| (?:Must|Should|Could) \| (CU-[^|]*) \|$',cu,re.M): cumap[rf]=cus.strip()
    tr='| RF | Prioridad | Historias de usuario | Casos de uso |\n|---|---|---|---|\n'
    for rf in sorted(rf_info): tr+=f'| {rf} | {rf_info[rf][1]} | {", ".join(cov[rf])} | {cumap.get(rf,"—")} |\n'
    hu=hu.replace('<!-- TRACE -->',tr.rstrip())
    open(P,'w',encoding='utf-8').write(hu)
    # CU doc: add HU column
    def row(m):
        rf=m.group(1); return f'| {rf} | {m.group(2)} | {m.group(3)} | {m.group(4)} | {", ".join(cov[rf])} |'
    cu2=re.sub(r'^\| (RF-\d\d) \| (.*?) \| (Must|Should|Could) \| (CU-[^|]*?) \|$',row,cu,flags=re.M)
    cu2=cu2.replace('| RF | Requerimiento (resumen) | Prioridad | Casos de uso |\n|---|---|---|---|','| RF | Requerimiento (resumen) | Prioridad | Casos de uso | Historias de usuario |\n|---|---|---|---|---|')
    cu2=cu2.replace('> La columna de historias de usuario (`HU`) se añadirá en la **Fase 2**.','> Las historias de usuario están en [`HISTORIAS_DE_USUARIO.md`](HISTORIAS_DE_USUARIO.md).')
    open('CASOS_DE_USO.md','w',encoding='utf-8').write(cu2)
    print('escrito. total puntos',tot,tp)
