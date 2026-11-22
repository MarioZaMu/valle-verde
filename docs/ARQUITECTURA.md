# Arquitectura y plan técnico — Valle Verde

> **Versión 0.1 · 2026-11-22 · Fase 4**
> Formaliza el diseño propuesto en los diagramas ([`DIAGRAMAS.md`](DIAGRAMAS.md)): capas, árbol de paquetes, decisiones técnicas y plan de implementación por fases con sus historias ([`HISTORIAS_DE_USUARIO.md`](HISTORIAS_DE_USUARIO.md) §7). El plan de pruebas está en [`PLAN_DE_PRUEBAS.md`](PLAN_DE_PRUEBAS.md).
> **Aún no hay código**: la Fase 5 empieza solo con tu visto bueno.

## 1. Estilo arquitectónico

**Arquitectura por capas con inversión de dependencias en la persistencia**, en una sola aplicación de escritorio (un proceso, un hilo de lógica). Las capas tienen los nombres del curso (*Controller → Service → Repository*) y el modelo de dominio va aparte, como «no es una cuarta capa» (PDF Spring p. 21).

```
   ui  ───────►  application  ───────►  domain  ◄───────  infrastructure
 (Swing/Java2D)  (Controller,           (reglas,           (JSON, archivos)
                  Services,              estado,
                  DTO, EventBus)         patrones)
```

| Regla | Descripción | Cómo se verifica |
|---|---|---|
| **AR-1** | `ui → application → domain` e `infrastructure → domain`; ninguna otra dirección | prueba ArchUnit de capas |
| **AR-2** | El dominio no importa `javax.swing`, `java.awt`, `com.google.gson` ni E/S de archivos (`java.io`, `java.nio.file`) | prueba ArchUnit |
| **AR-3** | La UI no importa `domain` ni `infrastructure`: dibuja con DTO inmutables de `application` | prueba ArchUnit |
| **AR-4** | `GameController` solo orquesta y devuelve un `Result`; nunca lanza excepciones hacia la UI y nunca llama a un repositorio | prueba ArchUnit + prueba de inyección de fallas |
| **AR-5** | Sin ciclos entre paquetes de `domain` | prueba ArchUnit (`slices`) y `tools/check_diagrams.py` |
| **AR-6** | La UI se actualiza por eventos (Observer), no consultando en bucle | revisión y prueba de `HudPanel` con eventos simulados |
| **AR-7** | Toda aleatoriedad recibe un `Random` inyectado | prueba: ningún `new Random()` ni `Math.random()` fuera de `Main` |
| **AR-8** | Sin números mágicos: las RN viven en `GameRules` y en las tablas de catálogo | revisión en cada historia (DoD 4) |
| **AR-9** | Cada clase de patrón lleva el Javadoc `Pattern: <nombre> — Role: <rol>` | prueba que revisa las fuentes |

## 2. Responsabilidades por capa

| Capa | Paquete | Responsabilidad | Contiene | No debe |
|---|---|---|---|---|
| Presentación | `ui` | Dibujar y capturar entrada del jugador | pantallas, paneles, `MapPanel` (Java2D), `AssetManager`, tema | contener reglas de negocio, importar `domain` |
| Aplicación | `application` | Casos de uso: coordinar, validar entradas y traducir | `GameController`, servicios, `GameSession`, `EventBus`, DTO, `Result` | dibujar, tocar archivos |
| Dominio | `domain` | Reglas y estado del juego (RN-01 a RN-35) | agregado `GameState`, cultivos, animales, edificios, mercado, tiempo, logros, patrones | depender de otra capa o de bibliotecas de UI, JSON y E/S |
| Infraestructura | `infrastructure` | Detalles técnicos | `JsonSaveRepository`, `SaveFileCodec`, `Checksum` | contener reglas de negocio |

## 3. Árbol de paquetes

Paquete raíz `edu.ucc.farmsim`. Las clases entre paréntesis se muestran en los diagramas como un subconjunto representativo ([`04_vista_desarrollo`](diagramas/04_vista_desarrollo.png)).

```
edu.ucc.farmsim
├─ Main                                 raíz de composición: crea Random, repositorio, servicios, EventBus y ventana
├─ ui
│  ├─ screens      StartScreen, GameScreen, ScreenRouter
│  ├─ components   HudPanel, ToolBar, InventoryGrid, MarketDialog, SlotDialog, AchievementsDialog, ToastLayer
│  ├─ render       MapPanel, MapRenderer, ParticleSystem, DayNightTint
│  ├─ assets       AssetManager, VectorFallback
│  └─ theme        Palette, Texts (cadenas de interfaz en español)
├─ application
│  ├─ controller   GameController
│  ├─ service      GameSession, FarmService, DayService, MarketService, BuildingService,
│  │               AnimalService, ProgressService, SaveGameService
│  ├─ event        EventBus, GameListener, GameEvent (interfaz sellada + records)
│  ├─ dto          GameView, HudView, PlotView, InventoryView, MarketView, BuildingView,
│  │               AchievementView, SlotView, DaySummary   (records inmutables)
│  └─ result       Result<T>, ErrorCode, Messages (plantillas de RN-35)
├─ domain
│  ├─ game         GameState, GameMemento, GameStateValidator, DayPipeline, DayStep + pasos del día
│  ├─ save         SaveRepository (puerto), SaveSlotInfo
│  ├─ farm         FarmMap, Plot, Terrain
│  ├─ crop         Crop, CropType, CropState (+5 estados), CropFactory (+5 creadores)
│  ├─ building     Building, BuildingType, BuildingBuilder (+4 constructores), BuildingDirector
│  ├─ animal       Animal, AnimalType, AnimalState (+3 estados)
│  ├─ economy      Inventory, ItemType, Market, PricingStrategy, CropPricing, FlatPricing, Trend
│  ├─ time         GameClock, GameDate, Season, SeasonState (+4), Weather, WeatherStrategy,
│  │               SeasonalWeatherStrategy, FixedWeatherStrategy, StateRandom
│  ├─ progress     AchievementTracker, Achievement, ProgressFacts
│  └─ config       GameRules (constantes de las RN)
└─ infrastructure
   └─ persistence  JsonSaveRepository, SaveFileCodec, Checksum
```

Estructura Maven del repositorio:

```
ProyectoFinal/
├─ pom.xml
├─ src/main/java/edu/ucc/farmsim/...        código (en inglés)
├─ src/main/resources/assets/               sprites CC0 / CC-BY (opcionales)
├─ src/test/java/edu/ucc/farmsim/...        espejo de los paquetes + acceptance/ + architecture/
├─ docs/                                    documentación y diagramas (en español)
├─ tools/                                   utilidades y Maven portable (no se versionan los binarios)
└─ README.md
```

## 4. Decisiones técnicas

| ID | Decisión | Justificación |
|---|---|---|
| **DT-01** | **Frontera UI ↔ aplicación con DTO.** `GameController` recibe y devuelve tipos simples (`int`, `String` con ids como `"CARROT"`) y `record` inmutables (`GameView`, `PlotView`, …). Los ids de catálogo y los textos para mostrar salen de `application` | La UI no importa `domain` (AR-3); el dominio puede cambiar sin tocar la UI |
| **DT-02** | **`Result<T>` + `ErrorCode`.** Cada operación del controlador devuelve `Result.ok(valor)` o `Result.fail(código, mensaje)` con los textos de RN-35. El controlador envuelve cada llamada en `try/catch` y convierte cualquier excepción inesperada en `ERROR_INTERNO` | Cumple R-03 y RNF-07; ninguna excepción llega a la UI |
| **DT-03** | **Dominio mutable, con transacciones por memento.** `GameState` es un agregado mutable; antes de operaciones compuestas (terminar día, cargar) el servicio toma una instantánea (`createMemento()`) y, si algo falla, la restaura | Reutiliza el patrón Memento para cumplir HU-10 · CA-4 («falla interna: el estado queda idéntico») sin lógica de reversión aparte |
| **DT-04** | **Fin de día como *pipeline* de pasos.** `DayPipeline` ejecuta una lista **ordenada** de `DayStep` (clima, crecimiento, animales, mercado, calendario, pronóstico, energía, logros) y produce un `DayReport`; cada historia agrega su propio paso | Materializa RN-03 y el principio de independencia de las historias (HU-10 define el *pipeline*; las demás se registran). El paso de calendario y los pasos de granja operan por métodos de agregado (`FarmMap.advanceDay`, `Market.recoverDemand`), sin que `game` importe `crop`, `animal` ni `building` directamente |
| **DT-05** | **Eventos síncronos en el hilo de Swing (EDT).** `EventBus` notifica a los observadores en el mismo hilo; los eventos se publican **después** de que la operación tuvo éxito. Un observador que falla no impide notificar a los demás | Un solo hilo de lógica (S-01): sin concurrencia, sin bloqueos y con pruebas deterministas |
| **DT-06** | **Un solo hilo.** Lógica, guardado y carga corren en el EDT (≤ 1 s, RNF-03). Las animaciones usan un `javax.swing.Timer` de la vista y no modifican el estado | Simplicidad; la animación nunca decide el estado (reglas de interfaz) |
| **DT-07** | **Aleatoriedad reproducible: `StateRandom extends java.util.Random`.** Su estado completo es un `long` (algoritmo SplitMix64 que sobrescribe `next(bits)`), con `state()` y `restore(long)`. Se inyecta desde `Main`; las pruebas usan semillas fijas | `java.util.Random` no permite leer su estado interno. Con `StateRandom`, guardar y cargar continúa **exactamente** la misma secuencia (RN-32, HU-30 · CA-2), sin recontar sorteos |
| **DT-08** | **Configuración sin números mágicos.** Todos los valores de RN-01 a RN-35 están en `GameRules` (constantes agrupadas); las tablas de catálogo (RN-05, RN-16, RN-20, RN-23, RN-28) viven en los enumerados `CropType`, `ItemType`, `BuildingType`, `AnimalType` y `Achievement`, que se consideran *tablas de configuración*. El nombre visible del juego está en **una** constante (RNF-12) | Una sola fuente de verdad, igual que en las reglas de negocio |
| **DT-09** | **Singleton `GameClock` con aislamiento para pruebas.** `getInstance()`, `restore(GameDate)` y `resetForTests()`. El `GameMemento` guarda la fecha y `GameState.restore` la restablece | El patrón se pidió explícitamente. El riesgo (estado global) se contiene con el método de reinicio y con una regla de pruebas: toda prueba que use el reloj lo reinicia en `@BeforeEach` |
| **DT-10** | **`GameSession` como contenedor del estado actual.** Los servicios obtienen el `GameState` de `GameSession`; cargar construye un estado **aparte** y lo asigna con `replace` solo si todo es válido | Garantiza HU-31 («la partida actual no cambia»): el estado vigente no se toca hasta que el nuevo está completo y validado |
| **DT-11** | **Formato de guardado.** Envoltura JSON `{schemaVersion, savedAt, state, checksum}`. El `GameMemento` contiene instantáneas inmutables (`record` sin dependencias de Gson); `SaveFileCodec` las serializa con Gson. El `checksum` es SHA-256 del texto canónico del bloque `state` | El dominio no conoce Gson (AR-2); el versionado permite rechazar archivos de otra versión |
| **DT-12** | **Guardado atómico.** Escribir en un archivo temporal del mismo directorio y mover con `Files.move(..., ATOMIC_MOVE, REPLACE_EXISTING)` | Si falla, el archivo previo queda intacto (RN-31, HU-29 · CA-2) |
| **DT-13** | **Validación de carga en el dominio.** `GameStateValidator` aplica las invariantes de RN-33 sin E/S; la secuencia de 5 verificaciones se reparte entre infraestructura (existencia, JSON, versión, checksum) y dominio (invariantes) | La regla de negocio vive en el dominio; la infraestructura solo lee bytes |
| **DT-14** | **Interfaz.** `ScreenRouter` con `CardLayout` (inicio / juego); `MapPanel` extiende `JPanel` y dibuja con Java2D en `paintComponent`; atajos con `InputMap/ActionMap`; textos en `Texts` (español). FlatLaf (Apache 2.0) es **opcional** y se decide en la Fase 5.6 | La UI funcional mínima se entrega desde la 5.1; lo visual es lo último y recortable |
| **DT-15** | **Registro de eventos técnicos** con `java.util.logging` (sin bibliotecas adicionales). Los mensajes al jugador siempre salen de `Messages` | Cero dependencias nuevas |

## 5. Pila tecnológica y construcción

| Componente | Elección | Versión (verificada en Maven Central el 2026-11-22) |
|---|---|---|
| Lenguaje | Java, compilado con `--release 17` (se ejecuta en JRE 17 o superior; el equipo tiene JDK 25) | Java 17 |
| Construcción | Apache Maven | 3.9.16 (portable en `tools/`) |
| Pruebas | JUnit Jupiter (JUnit 5) | 5.14.4 |
| Arquitectura | ArchUnit (`archunit-junit5`), solo en pruebas | 1.5.1 |
| JSON | Gson | 2.15.0 |
| Cobertura | JaCoCo (`jacoco-maven-plugin`) | 0.8.16 |
| Plugins Maven | compiler 3.16.0 · surefire 3.6.1 · shade 3.6.2 (jar ejecutable único) | — |
| Aspecto (opcional) | FlatLaf | 3.8 |

> JUnit ya publica la serie 6, pero se mantiene **JUnit 5** como pide el stack. Todo es gratuito (Apache 2.0, EPL 2.0 o similar).

Decisiones del `pom.xml` (se crea en la Fase 5.0):
- `maven.compiler.release=17`; `finalName=farmsim`; `Main-Class: edu.ucc.farmsim.Main`; el *shade* produce un único `farmsim.jar` con Gson y los assets.
- JaCoCo: informe en `target/site/jacoco` y regla `check` con **dos reglas separadas** (paquetes `domain` y `application`, cobertura de líneas ≥ 70 %, RNF-05).
- Surefire ejecuta las pruebas rápidas por defecto; las de rendimiento llevan `@Tag("perf")` y corren con `-Pperf` (y en el cierre de la Fase 5.5).
- Comando de verificación de cada fase: `mvn clean verify` (compila, prueba, mide cobertura y empaqueta).
- Se generará un *Maven Wrapper* (`mvnw`) para que cualquier equipo compile sin instalar Maven.

## 6. Flujo típico de una orden

Como en la secuencia [A1](diagramas/A1_secuencia_vender_cosecha.png):

1. La UI captura el clic o el botón y llama a `GameController.sell("CARROT", 3)` (tipos simples).
2. El controlador delega en `MarketService`, que obtiene el estado de `GameSession`, valida (`Inventory.count`), llama al dominio (`Market.sellUnit`, `Inventory.remove`, `GameState.earn`) y evalúa logros con `ProgressFacts`.
3. El servicio publica el evento (`EventBus.publish`) y devuelve `Result.ok(79)`.
4. `HudPanel` y `ToastLayer` reciben el evento y se actualizan; `MarketDialog` muestra el resultado.
5. Ante un rechazo el flujo termina con `Result.fail(código, mensaje)` y **sin cambios de estado ni eventos**.

## 7. Plan de implementación por fases

> Cada fase termina con `mvn clean verify` en verde, un `.jar` ejecutable y una **demostración posible** (UI funcional mínima de las historias de la fase). Dentro de una historia se trabaja así: (1) pruebas de aceptación `HUxx_…` a partir de los criterios; (2) código mínimo; (3) refactor; (4) commit `feat(area): description (HUxx)`. Al cerrar la fase: resumen, patrones aplicados y qué diagrama o documento actualizar; **se espera tu visto bueno antes de seguir**.

| Fase | Historias (puntos) | Trabajo técnico principal | UI funcional mínima | Patrones | Pruebas críticas |
|---|---|---|---|---|---|
| **5.0** Preparación técnica | — | `pom.xml`, esqueleto de paquetes, `Main` (ventana vacía), pruebas de arquitectura (AR-1 a AR-5), JaCoCo, `mvnw`, README inicial | ventana vacía | — | arquitectura |
| **5.1** Núcleo del turno | HU-13, 09, 08, 11, 28, 10, 33, 34 (**25**) | `GameRules`, `Inventory`, calendario (`GameClock`, `SeasonState`), `StateRandom`, clima (`WeatherStrategy`), `GameState`, `DayPipeline` con pasos de calendario, pronóstico y energía, `GameSession`, `DayService`, `Result/Messages`, `EventBus` | pantalla de inicio, HUD con etiquetas y botón «Terminar día» | Singleton, State (estación), Strategy (clima), Observer | determinismo de 112 días |
| **5.2** Cultivo | HU-01 a 07, 35, 36 (**29**) | `FarmMap`, `Plot`, `Crop`, `CropState` ×5, `CropFactory` ×5, `FarmService`, pasos de crecimiento y marchitamiento, DTO de parcela, mapa Java2D en vectores, barra de herramientas, tooltips | mapa con parcelas, herramientas 1–5 y clic | State (cultivo), Factory Method | **1** (fuera de estación), **2** (inventario lleno) |
| **5.3** Clima y economía | HU-12, 37, 14, 15, 16, 38 (**23**) | efectos del clima en el *pipeline*, `Market`, `PricingStrategy` ×2, `MarketService`, paso de mercado, `InventoryGrid`, `MarketDialog` | inventario en grilla y mercado modal | Strategy (precios) | **3** (helada), **4** (saturación) |
| **5.4** Construcción y animales | HU-18 a 23, 17 (**21**) | `Building`, `BuildingBuilder` ×4, `BuildingDirector`, `Animal`, `AnimalState` ×3, `BuildingService`, `AnimalService`, paso de animales, `DaySummary` | diálogo de construcción y panel del edificio (alimentar, recolectar, curar) | Builder, State (animal) | **5** (animal enfermo no produce) |
| **5.5** Progresión y persistencia | HU-26, 27, 24, 29, 30, 31, 32 (**24**) | `AchievementTracker`, `ProgressFacts`, desbloqueos, `GameMemento`, `GameStateValidator`, `SaveRepository`, `JsonSaveRepository`, `SaveFileCodec`, `Checksum`, `SaveGameService`; **pruebas de rendimiento a plena carga** (HU-10 · CA-3, HU-29 · CA-4, HU-30 · CA-4) | diálogos de ranuras, logros y atajos `F5`/`F9` | Memento | **6** (guardar → cargar idéntico), **7** (archivo corrupto o inexistente) |
| **5.6** Interfaz visual y extras *(recortable)* | HU-39, 42, 25, 40, 41 (**19**) | `ToastLayer`, `AssetManager` y respaldo vectorial, invernadero, efectos (`ParticleSystem`, `DayNightTint`), animaciones con `Timer`; `docs/ASSETS.md`, README final y matriz de trazabilidad completa; decisión sobre FlatLaf | pulido visual | Observer (completado) | — |

**Qué se actualiza al cerrar cada fase:** el diagrama de clases si aparecen clases nuevas con responsabilidad estructural, el de desarrollo si cambia un paquete o una dependencia (lo comprueba `tools/check_diagrams.py`) y la matriz de trazabilidad (RF → HU → diagrama → clases → pruebas).

**Si falta tiempo:** se recorta la Fase 5.6 sin afectar el diseño base (el juego sigue completo con gráficos vectoriales). El MVP son las 30 historias *Must* (109 puntos).

## 8. Riesgos técnicos

| ID | Riesgo | Prob. | Impacto | Mitigación |
|---|---|---|---|---|
| RT-01 | El `pipeline` del fin de día se vuelve un cuello de botella o se rompe al agregar pasos | Media | Alto | Pasos pequeños e independientes con prueba de orden (HU-10 · CA-1); `DayReport` único; transacción por memento (DT-03) |
| RT-02 | El estado global del Singleton contamina pruebas | Media | Medio | `resetForTests()` y regla de `@BeforeEach` (DT-09) |
| RT-03 | Cambios al esquema de guardado rompen archivos viejos | Baja | Medio | `schemaVersion` y rechazo con `VERSION_NO_SOPORTADA`; pruebas con archivos de ejemplo versionados |
| RT-04 | La UI Swing no se puede probar sin pantalla | Media | Bajo | Lógica de presentación en clases sin Swing (presentadores y DTO); pruebas de componentes solo si `!GraphicsEnvironment.isHeadless()` |
| RT-05 | El JDK 25 del equipo produce bytecode incompatible con JRE 17 | Baja | Alto | `--release 17` y una comprobación en 5.0 (`javap` del `.class` generado) |
| RT-06 | Mapas, sprites y efectos consumen el tiempo | Alta | Alto | Orden de fases con la 5.6 recortable; vectoriales desde el inicio |
| RT-07 | Sobreingeniería por tantos patrones | Media | Medio | Cada patrón está justificado por escrito ([`DIAGRAMAS.md`](DIAGRAMAS.md) §3.1); no se agregan más |

## 9. Cambios derivados de esta fase (ya aplicados)

| # | Cambio | Dónde |
|---|---|---|
| C-01 | `AchievementTracker.evaluate` recibe `ProgressFacts` y no `GameState`: la versión anterior creaba un **ciclo** `game → progress → game` | diagramas 03 y 04; el verificador ahora detecta ciclos |
| C-02 | Se dibuja la dependencia `farm → time` (cultivos y animales necesitan clima y estación) | diagrama 04 |
| C-03 | Se agregan `GameSession` (DT-10), `StateRandom` (DT-07) y `ProgressFacts` | diagramas 03 y 04 |
| C-04 | RN-32: el archivo guarda el **estado interno del generador** (`StateRandom`) en lugar de «semilla + número de sorteos» | `REGLAS_DE_NEGOCIO.md` |
| C-05 | RN-35: nuevo código `ERROR_INTERNO` para excepciones inesperadas (DT-02) | `REGLAS_DE_NEGOCIO.md` |
