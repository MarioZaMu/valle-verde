"""Generates docs/PLAN_DE_PRUEBAS.md from HISTORIAS_DE_USUARIO.md (criteria counts) so numbers never drift."""
import re, sys
sys.stdout.reconfigure(encoding='utf-8')
hu=open('docs/HISTORIAS_DE_USUARIO.md',encoding='utf-8').read()
S={}
for b in re.split(r'^### (?=HU-\d\d ·)',hu,flags=re.M)[1:]:
    b=b.split('\n---')[0]
    m=re.match(r'(HU-\d\d) · (.*)',b); i=m.group(1)
    meta=re.search(r'\*\*Prioridad:\*\* (\w+) · \*\*Puntos:\*\* (\d+) · \*\*RF:\*\* (.*?) · \*\*RN:',b)
    S[i]=dict(title=m.group(2).strip(),pri=meta.group(1),rf=re.findall(r'RF-\d\d',meta.group(3)),n=len(re.findall(r'^- \*\*CA-\d+',b,re.M)))
PH=[('5.1',[13,9,8,11,28,10,33,34]),('5.2',[1,2,3,4,5,6,7,35,36]),('5.3',[12,37,14,15,16,38]),
    ('5.4',[18,19,20,21,22,23,17]),('5.5',[26,27,24,29,30,31,32]),('5.6',[39,42,25,40,41]),('5.7',[43,44,48,45,46,47,49])]
phase={f'HU-{h:02d}':p for p,l in PH for h in l}
CLS={1:'SowCrop',2:'WaterCrop',3:'CropGrowth',4:'HarvestCrop',5:'CropWithering',6:'ClearWitheredPlot',7:'PlotInspection',8:'EnergyManagement',
 9:'CalendarAndSeasons',10:'EndDay',11:'WeatherAndForecast',12:'WeatherEffectsOnCrops',13:'InventoryManagement',14:'BuyFromMarket',
 15:'SellWithDynamicPrice',16:'PriceTrend',17:'DaySummary',18:'BuildBuildings',19:'BuyAnimals',20:'FeedAnimals',21:'AnimalStates',
 22:'ProductionAndCollection',23:'CureAnimals',24:'StorageBuilding',25:'Greenhouse',26:'Achievements',27:'BuildingUnlocks',28:'NewGame',
 29:'SaveGame',30:'LoadGame',31:'RejectInvalidSaves',32:'ListSaveSlots',33:'StartScreen',34:'HudPanel',35:'FarmMapView',36:'ToolsAndClicks',
 37:'InventoryGridView',38:'MarketDialog',39:'Toasts',40:'VisualEffects',41:'Animations',42:'AssetManager',43:'FlatLafTheme',44:'PixelSprites',45:'AvatarMovement',46:'NpcDialogue',47:'NpcErrands',48:'FarmStyleInterface',49:'StartTutorial'}
assert len(CLS)==49 and len(S)==49
UI=set(range(33,44))|{44,48}
rows=''; tot=0; tot_a=0; tot_ui=0
for i in sorted(S):
    n=int(i[3:]); k='UI + visual' if n in UI else 'Aceptación'
    if n in (7,): k='Aceptación + visual'
    pkg='ui' if n in UI else 'acceptance'
    rows+=f'| {i} | {S[i]["title"]} | {S[i]["pri"]} | {phase[i]} | `{pkg}.HU{n:02d}{CLS[n]}Test` | {S[i]["n"]} | {k} |\n'
    tot+=S[i]['n']
    if n in UI: tot_ui+=S[i]['n']
    else: tot_a+=S[i]['n']
perph=''
for p,l in PH:
    ids=[f'HU-{h:02d}' for h in l]; perph+=f'| {p} | {len(ids)} | {sum(S[i]["n"] for i in ids)} |\n'
doc=f'''# Plan de pruebas — Valle Verde

> **Versión 0.1 · 2026-11-22 · Fase 4** (generado por `tools/gen_test_plan.py` a partir de las historias: los conteos no se editan a mano)
> Complementa [`ARQUITECTURA.md`](ARQUITECTURA.md) y convierte los criterios Dado/Cuando/Entonces de [`HISTORIAS_DE_USUARIO.md`](HISTORIAS_DE_USUARIO.md) en pruebas automáticas y comprobaciones visuales.

## 1. Estrategia

Las pruebas se escriben **antes** que el código, historia por historia (DoD 1). Hay {tot} criterios de aceptación en las 42 historias: cada uno se convierte en **al menos una prueba**.

| Nivel | Qué verifica | Dónde | Herramienta | Velocidad |
|---|---|---|---|---|
| **Unitarias de dominio** | Reglas de cultivos, animales, mercado, tiempo (RN-01 a RN-35) con `StateRandom` de semilla fija | `src/test/.../domain/**` | JUnit 5 | milisegundos |
| **Aceptación** (aplicación) | Cada criterio de cada historia, **a través de `GameController`** con un repositorio en memoria | `src/test/.../acceptance/**` | JUnit 5 | milisegundos |
| **API REST** | Cada endpoint: código HTTP de éxito y de cada rechazo, cuerpo JSON y eventos de la respuesta | `src/test/.../api/**` | Spring Boot Test + MockMvc | decenas de ms |
| **Cliente HTTP** | `RemoteGameClient` contra el servidor real en un puerto libre: mismos resultados que el controlador en proceso | `src/test/.../client/**` | Spring Boot Test (`RANDOM_PORT`) | cientos de ms |
| **Infraestructura** | `MongoSaveRepository`: reemplazo atómico, documentos dañados, MongoDB que no responde | `src/test/.../infrastructure/**` | JUnit 5 + MongoDB de pruebas | decenas de ms |
| **Arquitectura** | Capas, ciclos, imports prohibidos, Javadoc de patrones (AR-1 a AR-9) | `src/test/.../architecture/**` | ArchUnit | ~1 s |
| **Rendimiento** | Fin de día, guardado y carga a plena carga (RNF-01, RNF-03) | `src/test/.../perf/**` con `@Tag("perf")` | JUnit 5 | segundos |
| **Interfaz** | Presentadores y componentes sin pantalla; el resto con lista de comprobación visual | `src/test/.../ui/**` | JUnit 5 (omitidas si `GraphicsEnvironment.isHeadless()`) | decenas de ms |

## 2. Comandos

| Objetivo | Comando |
|---|---|
| Pruebas rápidas + cobertura + empaquetado (cierre de cada historia y fase) | `mvn clean verify` |
| Solo pruebas de una historia | `mvn test -Dtest='HU01*'` |
| Incluir pruebas de rendimiento | `mvn verify -Pperf` |
| Informe de cobertura | `target/site/jacoco/index.html` |
| Comprobar diagramas, historias, código y trazabilidad | `python tools/check_diagrams.py` · `python tools/check_hu.py` · `python tools/check_imports.py` · `python tools/traceability.py [--upto 5.x]` |

> En este equipo Maven no está en el PATH: se usa `tools/apache-maven-3.9.16/bin/mvn` (o el `mvnw` que se generará en la Fase 5.0).

## 3. Convenciones

- **Nombres:** clases `HUxxNombreDeLaHistoriaTest`; métodos `HUxx_caN_descripcionEnIngles` (por ejemplo `HU01_ca2_rejectsSowingOutOfSeason`). El prefijo `HUxx_` permite filtrar por historia y trazar cada criterio.
- **Estructura Dado/Cuando/Entonces** en comentarios `// Given`, `// When`, `// Then`; una sola aserción de comportamiento por prueba cuando sea posible.
- **Datos de prueba** (clase `GameFixtures`): partida nueva con semilla fija; constructores de estado (`aGame().inSeason(SPRING).withMoney(500)…`); `fullLoadGame()` (40 cultivos al aire libre, 9 en invernadero, 6 edificios y 16 animales, para RNF-01).
- **Aleatoriedad:** nunca `new Random()` en pruebas; siempre `StateRandom` con semilla fija o `FixedWeatherStrategy` para controlar el clima del día.
- **Reloj:** toda prueba que lo use llama a `GameClock.resetForTests()` en `@BeforeEach` (DT-09).
- **Repositorios:** `FakeSaveRepository` en memoria para aceptación, API y cliente; MongoDB real solo en las pruebas de `MongoSaveRepository` (se omiten con un aviso si no hay un MongoDB al que conectarse).
- **Fallas inyectadas:** servicios y repositorios de prueba que lanzan excepciones para verificar que el controlador devuelve `ERROR_INTERNO` y que el estado no cambia (RNF-07, HU-10 · CA-4).
- **Trazabilidad:** cada clase de producción lleva `Stories: HU-xx` en su Javadoc y cada método de prueba el prefijo `HUxx_`; `python tools/traceability.py` (con `--upto 5.x` al cerrar una fase) comprueba que cada historia tenga clases y al menos tantas pruebas como criterios.
- **Mensajes:** las pruebas comparan el **código** de error y, en pocas pruebas dedicadas, el texto en español de RN-35.

## 4. Pruebas de aceptación por historia

Total: **{tot} criterios** → **{tot_a}** de dominio/aplicación y **{tot_ui}** de interfaz (HU-33 a HU-42). Los de interfaz se prueban con presentadores y componentes sin pantalla cuando es posible, y el resto con la lista de la §7.

| Historia | Título | Prioridad | Fase | Clase de prueba | Criterios | Tipo |
|---|---|---|---|---|---|---|
{rows}
**Criterios por fase:**

| Fase | Historias | Criterios |
|---|---|---|
{perph}
## 5. Pruebas críticas obligatorias

| # | Prueba crítica | Historia · criterio | Método propuesto | Fase |
|---|---|---|---|---|
| 1 | Sembrar fuera de estación | HU-01 · CA-2 | `HU01_ca2_rejectsSowingOutOfSeason` | 5.2 |
| 2 | Cosechar con inventario lleno | HU-04 · CA-2 | `HU04_ca2_keepsCropMatureWhenInventoryIsFull` | 5.2 |
| 3 | Helada que marchita cultivos | HU-12 · CA-2 | `HU12_ca2_frostWithersEveryCropInTheOpenEvenAMatureOne` | 5.3 |
| 4 | Saturación de precios al vender mucho | HU-15 · CA-2 y CA-3 | `HU15_ca2_afterTenCarrotsTheNextOnePaysOnly22`, `HU15_ca3_theDemandNeverFallsBelow50` | 5.3 |
| 5 | Animal enfermo que no produce | HU-22 · CA-2 (y HU-21 · CA-3) | `HU22_ca2_sickAnimalDoesNotProduce` | 5.4 |
| 6 | Guardar → cargar devuelve estado idéntico | HU-30 · CA-1 | `HU30_ca1_loadedStateEqualsSavedState` | 5.5 |
| 7 | Cargar una partida dañada o inexistente sin alterar la partida | HU-31 · CA-1 y CA-2 | `HU31_ca1_missingSaveLeavesGameUntouched`, `HU31_ca2_corruptSaveLeavesGameUntouched` | 5.5 |

## 6. Pruebas de arquitectura y de calidad transversal

| ID | Prueba | Verifica |
|---|---|---|
| AR-1 | `layersAreRespected` | `ui → application → domain`; `infrastructure → domain` |
| AR-2 | `domainHasNoFrameworkOrIoImports` | el dominio no importa Swing, AWT, Spring, Jackson, MongoDB, `java.io` ni `java.nio.file` |
| AR-3 | `uiDoesNotImportDomainOrInfrastructure` | la UI solo conoce `application` |
| AR-4 | `controllerNeverTouchesRepositories` y `controllerNeverThrows` | el controlador no usa repositorios y siempre devuelve `Result` (inyección de fallas) |
| AR-5 | `domainPackagesHaveNoCycles` | sin ciclos entre subpaquetes de `domain` |
| AR-7 | `noUnseededRandomnessOutsideMain` | no hay `new Random()` ni `Math.random()` fuera de `Main` |
| AR-8 | `noMagicNumbersInDomain` | revisión automática simple de literales numéricos fuera de `GameRules` y catálogos (se acepta 0, 1 y −1) |
| AR-9 | `patternClassesHavePatternJavadoc` | las clases listadas como patrón llevan `Pattern: … — Role: …` |
| Q-1 | `everyErrorCodeHasSpanishMessage` | cada `ErrorCode` tiene su plantilla de RN-35 |
| Q-2 | `assetsAreRegisteredInAssetsMd` | todo archivo de `resources/assets` figura en `docs/ASSETS.md` con licencia CC0 o CC-BY |
| Q-3 | `gameRunsWithoutAnyImage` | el juego arranca y se puede jugar con respaldo vectorial |

## 7. Requerimientos no funcionales y comprobaciones visuales

| RNF | Cómo se verifica | Cuándo |
|---|---|---|
| RNF-01 (fin de día ≤ 200 ms p95) | prueba `perf` con `fullLoadGame()`: 20 ejecuciones de calentamiento y 100 medidas | 5.5 |
| RNF-02 (arranque ≤ 5 s) | medición manual con cronómetro sobre `java -jar farmsim.jar` y registro en el README | 5.1 y 5.6 |
| RNF-03 (guardar/cargar ≤ 1 s, ≤ 200 KB) | prueba `perf` + comprobación del tamaño del documento | 5.5 |
| RNF-04 (≥ 30 FPS, clic ≤ 100 ms) | contador de cuadros de `MapPanel` en modo de depuración; comprobación manual | 5.6 |
| RNF-05 (cobertura ≥ 70 %) | `jacoco:check` en `mvn verify`: **dominio** y **aplicación** por separado | cada fase |
| RNF-06 y RNF-07 | pruebas AR-1 a AR-5 y `controllerNeverThrows` | cada fase |
| RNF-08 (determinismo) | misma semilla + mismas acciones → mismo estado tras 112 días | 5.1 |
| RNF-09 (persistencia fiable) | pruebas de HU-29, HU-30 y HU-31 | 5.5 |
| RNF-10 (usabilidad, español) | Q-1 y lista visual: toda acción en ≤ 2 clics o 1 atajo | 5.2 en adelante |
| RNF-11 (portabilidad) | `mvn clean package` y `java -jar target/farmsim.jar` en un equipo limpio | cada fase |
| RNF-12 y RNF-13 | AR-8, AR-9 y revisión de commits (`HUxx`) | cada fase |
| RNF-14 (licencias y contraste) | Q-2, Q-3 y revisión de contraste ≥ 4,5 : 1 | 5.6 |

**Lista de comprobación visual** (historias de interfaz, ejecutada al cerrar cada fase con el `.jar`):

| Historia | Pasos | Resultado esperado |
|---|---|---|
| HU-33 | Ejecutar el `.jar`; pulsar «Cargar» con 3 ranuras vacías; pulsar «Salir» | Aparece el inicio en ≤ 5 s; mensaje de que no hay partidas; el proceso termina sin errores |
| HU-34 | Nueva partida; sembrar; terminar día | HUD con «500 m», «Año 1 · Primavera · Día 1», energía «100/100», luego «98/100» y el día siguiente |
| HU-35 | Nueva partida; colocar cultivos en cada etapa | 160 parcelas legibles desde 1024 × 768; las 5 etapas se distinguen |
| HU-36 | Teclas `1` a `5`, `Esc`, `Enter`, `M`, `I`, `L`, `F5`, `F9`; clics fuera del mapa | Cada atajo hace lo definido en RN-34; el clic fuera del mapa no hace nada |
| HU-37 y HU-38 | Abrir inventario y mercado; vender 3 Zanahorias; comprar sin fondos | Grilla de 24 casillas; total previsto 79 m; botón deshabilitado sin fondos |
| HU-39 | Provocar 5 eventos a la vez | Máximo 4 *toasts* visibles; los logros duran 5 s |
| HU-40 y HU-41 | Cambiar de estación y clima; activar y desactivar efectos | Paletas distintas; partículas de lluvia y nieve; el estado es idéntico con efectos activados o no |
| HU-42 | Borrar la carpeta de imágenes y ejecutar | El juego funciona completo con gráficos vectoriales |

## 8. Cobertura

- `jacoco:check` con **dos reglas**: `edu/ucc/farmsim/domain/**` y `edu/ucc/farmsim/application/**`, cobertura de líneas **≥ 70 %** cada una (RNF-05); la construcción falla si no se cumple.
- Se excluyen del cálculo `ui` e `infrastructure`, que se prueban con sus propios niveles.
- El informe se revisa al cerrar cada fase; si una clase de dominio queda sin cubrir, falta una prueba de una regla.

## 9. Criterios de salida por fase

Una fase se da por cerrada cuando:

1. `mvn clean verify` termina en verde (pruebas, cobertura ≥ 70 % y empaquetado).
2. Todas las pruebas `HUxx_` de las historias de la fase existen y pasan; las pruebas críticas de la fase (§5) también.
3. Las pruebas de arquitectura (AR-1 a AR-9) pasan.
4. `java -jar target/farmsim.jar` ejecuta y permite demostrar cada historia de la fase.
5. `python tools/check_diagrams.py` termina con `SIN ERRORES` y los diagramas reflejan el código.
6. Hay un resumen de fase (historias cerradas, patrones aplicados, documentos actualizados) y se espera tu visto bueno.

**Excepción declarada:** tres criterios de rendimiento a plena carga necesitan todas las entidades del juego y solo pueden verificarse en la Fase 5.5: **HU-10 · CA-3** (fin de día ≤ 200 ms), **HU-29 · CA-4** (guardar ≤ 1 s y ≤ 200 KB) y **HU-30 · CA-4** (cargar ≤ 1 s). Hasta entonces sus pruebas existen con `@Disabled("needs full-load fixture, see PLAN_DE_PRUEBAS §9")` y el resumen de cada fase las lista como pendientes; en la 5.5 se activan y se vuelven obligatorias.

## 10. Trazabilidad requisito → historia → diagrama → clases → pruebas

La matriz completa se genera al final de cada fase con un script (`tools/traceability.py`, Fase 5.0) a partir de: los `RF` de cada historia, las vistas de [`DIAGRAMAS.md`](DIAGRAMAS.md) §9, las clases reales del código y las pruebas `HUxx_`. El script falla si un RF no tiene historia, si una historia no tiene pruebas o si una prueba cita una historia inexistente.
'''
open('docs/PLAN_DE_PRUEBAS.md','w',encoding='utf-8').write(doc)
print('criterios totales',tot,'aceptación',tot_a,'ui',tot_ui)
