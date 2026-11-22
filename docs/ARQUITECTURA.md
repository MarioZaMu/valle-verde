# Arquitectura y plan técnico — Valle Verde

> **Versión 0.2 · 2026-11-22 · Fase 5.2b (migración a Spring Boot)**
> Formaliza el diseño propuesto en los diagramas ([`DIAGRAMAS.md`](DIAGRAMAS.md)): capas, árbol de paquetes, decisiones técnicas y plan de implementación por fases con sus historias ([`HISTORIAS_DE_USUARIO.md`](HISTORIAS_DE_USUARIO.md) §7). El plan de pruebas está en [`PLAN_DE_PRUEBAS.md`](PLAN_DE_PRUEBAS.md).
> **Estado:** las Fases 5.0 (§10), 5.1 (§11), 5.2 (§12) y 5.2b (§13, migración a servidor Spring Boot con API REST, cliente Swing por HTTP y persistencia en MongoDB) están hechas. La Fase 5.3 empieza solo con tu visto bueno.

## 1. Estilo arquitectónico

**Cliente-servidor por capas, con inversión de dependencias en la persistencia**, desplegado en un solo equipo: un **servidor Spring Boot** (API REST JSON en 127.0.0.1, lógica del juego y acceso a MongoDB) y un **cliente Swing/Java2D** que lo usa por HTTP. Un solo `java -jar` arranca ambos. La lógica corre en un solo hilo del servidor (una partida por servidor, S-01).

Las capas del servidor tienen los nombres del curso (*Controller → Service → Repository*) y el modelo de dominio va aparte, como «no es una cuarta capa» (PDF Spring p. 21). Hay **dos controladores con papeles distintos**, como separa el taller del bloque 7: el `GameRestController` (`@RestController`, capa `api`) solo traduce HTTP, y `GameController` (patrón GRASP *Controller*, capa `application`) orquesta los casos de uso.

```
 CLIENTE (Swing)                         SERVIDOR (Spring Boot, 127.0.0.1)
 ui ──► GameCommands ◄── client ═HTTP/JSON═► api ──► application ──► domain ◄── infrastructure ──► MongoDB
 (Swing/Java2D)   (RemoteGameClient)  (@RestController)  (Controller,     (reglas,   (MongoSaveRepository)
                                                          Services,       estado,
                                                          DTO, EventBus)  patrones)
```

| Regla | Descripción | Cómo se verifica |
|---|---|---|
| **AR-1** | Servidor: `api → application → domain` e `infrastructure → domain`. Cliente: `ui → application` y `client → application`. Ninguna otra dirección. `Main` y `config` son la raíz de composición y los únicos que pueden ver todas las capas | prueba ArchUnit de capas |
| **AR-2** | El dominio no importa Swing, AWT, `org.springframework`, `com.fasterxml.jackson`, `com.mongodb` ni E/S (`java.io`, `java.nio.file`, `java.net`) | prueba ArchUnit |
| **AR-3** | La UI no importa `domain`, `infrastructure`, `api` ni `client`: da órdenes por la interfaz `GameCommands` y dibuja con DTO inmutables de `application` | prueba ArchUnit |
| **AR-4** | `GameController` solo orquesta y devuelve un `Result`; nunca lanza excepciones hacia la UI ni hacia la API y nunca llama a un repositorio | prueba ArchUnit + prueba de inyección de fallas |
| **AR-5** | Sin ciclos entre paquetes de `domain` | prueba ArchUnit (`slices`) y `tools/check_diagrams.py` |
| **AR-6** | La UI se actualiza por eventos (Observer), no consultando en bucle | revisión y prueba de `HudPanel` con eventos simulados |
| **AR-7** | Toda aleatoriedad recibe un `Random` inyectado | prueba: ningún `new Random()` ni `Math.random()` fuera de `Main` y `config` |
| **AR-8** | Sin números mágicos: las RN viven en `GameRules` y en las tablas de catálogo | revisión en cada historia (DoD 4) |
| **AR-9** | Cada clase de patrón lleva el Javadoc `Pattern: <nombre> — Role: <rol>` | `PatternJavadocTest` con un registro (`src/test/resources/patterns.txt`) que cada fase completa |
| **AR-10** | Toda clase de producción declara en su Javadoc la línea `Stories: HU-xx, …` (o `Stories: none`) | `tools/traceability.py` (alimenta la matriz de trazabilidad) |
| **AR-11** | Los controladores REST (`api`) no importan `domain` ni `infrastructure`: solo traducen HTTP ↔ `GameController`. El cliente (`client`) no importa `api` | prueba ArchUnit |
| **AR-12** | Cada orden del cliente tiene su endpoint documentado en [`API.md`](API.md) y una prueba de integración con el código HTTP de éxito y el de cada rechazo | DoD 11 y pruebas de `api` y `client` |

## 2. Responsabilidades por capa

| Capa | Paquete | Responsabilidad | Contiene | No debe |
|---|---|---|---|---|
| Presentación (cliente) | `ui` | Dibujar y capturar entrada del jugador | pantallas, paneles, `MapPanel` (Java2D), `AssetManager`, tema | contener reglas de negocio, importar `domain` ni saber si hay HTTP |
| Cliente HTTP | `client` | Implementar `GameCommands` enviando cada orden a la API y republicar los eventos recibidos | `RemoteGameClient`, `EventDecoder` | contener reglas de negocio, importar `domain` o `api` |
| API REST | `api` | Traducir HTTP ↔ `GameController`: rutas, JSON, códigos de estado, eventos de la respuesta | `GameRestController`, `ApiResponse`, `ErrorStatusMapper`, `EventCollector`, manejo global de errores | contener reglas de negocio, importar `domain` o `infrastructure` |
| Aplicación | `application` | Casos de uso: coordinar, validar entradas y traducir | `GameCommands`, `GameController`, servicios, `GameSession`, `EventBus`, DTO, `Result` | dibujar, hablar HTTP, tocar MongoDB |
| Dominio | `domain` | Reglas y estado del juego (RN-01 a RN-35) | agregado `GameState`, cultivos, animales, edificios, mercado, tiempo, logros, patrones | depender de otra capa o de bibliotecas de UI, Spring, JSON, MongoDB y E/S |
| Infraestructura | `infrastructure` | Detalles técnicos: guardar y leer partidas en MongoDB | `MongoSaveRepository`, `SaveDocument`, `SaveCodec`, `Checksum` | contener reglas de negocio |
| Configuración | `config` | Arrancar Spring Boot y declarar los beans que no son componentes (`EventBus`, `DayPipeline`); los tiempos de espera de MongoDB van en la URI de `application.properties` | `ServerApplication`, `ServerConfig` | contener reglas de negocio |

## 3. Árbol de paquetes

Paquete raíz `edu.ucc.farmsim`. Las clases entre paréntesis se muestran en los diagramas como un subconjunto representativo ([`04_vista_desarrollo`](diagramas/04_vista_desarrollo.png)).

```
edu.ucc.farmsim
├─ Main, LaunchOptions                  raíz de composición: arranca el servidor Spring Boot y/o la ventana del cliente (`--server`, `--client`)
├─ GameInfo                             nombre visible y versión del juego (única constante, RNF-12)
├─ ui
│  ├─ screens      MainWindow, StartScreen, GameScreen, ScreenRouter
│  ├─ components   HudPanel, ToolBar, InventoryGrid, MarketDialog, SlotDialog, AchievementsDialog, ToastLayer
│  ├─ render       MapPanel, MapRenderer, ParticleSystem, DayNightTint
│  ├─ assets       AssetManager, VectorFallback
│  └─ theme        Palette, Texts (cadenas de interfaz en español)
├─ client                               (cliente HTTP)
│     RemoteGameClient (implementa GameCommands), EventDecoder
├─ api                                  (servidor: capa REST)
│     GameRestController, ApiResponse, EventDto, ErrorStatusMapper, EventCollector, ApiExceptionHandler
├─ config                               ServerApplication, ServerConfig (Spring)
├─ application
│  ├─ controller   GameCommands (puerto), GameController
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
   └─ persistence  MongoSaveRepository, SaveDocument, SaveCodec, Checksum
```

Estructura Maven del repositorio:

```
ProyectoFinal/
├─ pom.xml
├─ src/main/java/edu/ucc/farmsim/...        código (en inglés)
├─ src/main/resources/application.properties   configuración de Spring (puerto, URI de MongoDB)
├─ src/main/resources/assets/               sprites CC0 / CC-BY (opcionales)
├─ src/test/java/edu/ucc/farmsim/...        espejo de los paquetes + acceptance/ + architecture/
├─ docs/                                    documentación y diagramas (en español)
├─ tools/                                   utilidades y Maven portable (no se versionan los binarios)
└─ README.md
```

## 4. Decisiones técnicas

| ID | Decisión | Justificación |
|---|---|---|
| **DT-01** | **Frontera UI ↔ aplicación con DTO y el puerto `GameCommands`.** La UI da órdenes por la interfaz `GameCommands`, que recibe y devuelve tipos simples (`int`, `String` con ids como `"CARROT"`) y `record` inmutables (`GameView`, `PlotView`, …). Los ids de catálogo y los textos para mostrar salen de `application` | La UI no importa `domain` (AR-3); el dominio puede cambiar sin tocar la UI |
| **DT-02** | **`Result<T>` + `ErrorCode`.** Cada operación del controlador devuelve `Result.ok(valor)` o `Result.fail(código, mensaje)` con los textos de RN-35. El controlador envuelve cada llamada en `try/catch` y convierte cualquier excepción inesperada en `ERROR_INTERNO` | Cumple R-03 y RNF-07; ninguna excepción llega a la UI |
| **DT-03** | **Dominio mutable, con transacciones por memento.** `GameState` es un agregado mutable; antes de operaciones compuestas (terminar día, cargar) el servicio toma una instantánea (`createMemento()`) y, si algo falla, la restaura | Reutiliza el patrón Memento para cumplir HU-10 · CA-4 («falla interna: el estado queda idéntico») sin lógica de reversión aparte |
| **DT-04** | **Fin de día como *pipeline* de pasos.** `DayPipeline` ejecuta una lista **ordenada** de `DayStep` (clima, crecimiento, animales, mercado, calendario, pronóstico, energía, logros) y produce un `DayReport`; cada historia agrega su propio paso | Materializa RN-03 y el principio de independencia de las historias (HU-10 define el *pipeline*; las demás se registran). El paso de calendario y los pasos de granja operan por métodos de agregado (`FarmMap.advanceDay`, `Market.recoverDemand`), sin que `game` importe `crop`, `animal` ni `building` directamente |
| **DT-05** | **Eventos síncronos.** `EventBus` notifica a los observadores en el mismo hilo; los eventos se publican **después** de que la operación tuvo éxito. Un observador que falla no impide notificar a los demás. En el servidor los recoge un `EventCollector` (DT-19); en el cliente notifican a la UI en el EDT | Un solo hilo de lógica (S-01): sin concurrencia, sin bloqueos y con pruebas deterministas |
| **DT-06** | **Un solo hilo de lógica.** En el servidor cada orden se ejecuta bajo un candado (DT-20), incluidos guardar y cargar (≤ 1 s, RNF-03). Las animaciones del cliente usan un `javax.swing.Timer` de la vista y no modifican el estado | Simplicidad; la animación nunca decide el estado (reglas de interfaz) |
| **DT-07** | **Aleatoriedad reproducible: `StateRandom extends java.util.Random`.** Su estado completo es un `long` (algoritmo SplitMix64 que sobrescribe `next(bits)`), con `state()` y `restore(long)`. Se inyecta desde `Main`; las pruebas usan semillas fijas | `java.util.Random` no permite leer su estado interno. Con `StateRandom`, guardar y cargar continúa **exactamente** la misma secuencia (RN-32, HU-30 · CA-2), sin recontar sorteos |
| **DT-08** | **Configuración sin números mágicos.** Todos los valores de RN-01 a RN-35 están en `GameRules` (constantes agrupadas); las tablas de catálogo (RN-05, RN-16, RN-20, RN-23, RN-28) viven en los enumerados `CropType`, `ItemType`, `BuildingType`, `AnimalType` y `Achievement`, que se consideran *tablas de configuración*. El nombre visible del juego está en **una** constante (RNF-12) | Una sola fuente de verdad, igual que en las reglas de negocio |
| **DT-09** | **Singleton `GameClock` con aislamiento para pruebas.** `getInstance()`, `restore(GameDate)` y `resetForTests()`. El `GameMemento` guarda la fecha y `GameState.restore` la restablece | El patrón se pidió explícitamente. El riesgo (estado global) se contiene con el método de reinicio y con una regla de pruebas: toda prueba que use el reloj lo reinicia en `@BeforeEach` |
| **DT-10** | **`GameSession` como contenedor del estado actual.** Los servicios obtienen el `GameState` de `GameSession`; cargar construye un estado **aparte** y lo asigna con `replace` solo si todo es válido | Garantiza HU-31 («la partida actual no cambia»): el estado vigente no se toca hasta que el nuevo está completo y validado |
| **DT-11** | **Formato de guardado.** Documento `{_id, schemaVersion, savedAt, state, checksum}` (RN-32). El `GameMemento` contiene instantáneas inmutables (`record` sin dependencias de bibliotecas); `SaveCodec` (infraestructura) las serializa a JSON con Jackson y `SaveDocument` es el `@Document` de Spring Data MongoDB. El `checksum` es SHA-256 del texto de `state` | El dominio no conoce Jackson ni MongoDB (AR-2); el versionado permite rechazar partidas de otra versión |
| **DT-12** | **Guardado atómico.** Reemplazar el documento de la ranura en una sola operación (`replaceOne` con *upsert*); MongoDB garantiza la atomicidad por documento | Si falla, el documento previo queda intacto (RN-31, HU-29 · CA-2) |
| **DT-13** | **Validación de carga en el dominio.** `GameStateValidator` aplica las invariantes de RN-33 sin E/S; la secuencia de 5 verificaciones se reparte entre infraestructura (existencia, JSON, versión, checksum) y dominio (invariantes) | La regla de negocio vive en el dominio; la infraestructura solo lee el documento |
| **DT-14** | **Interfaz.** `ScreenRouter` con `CardLayout` (inicio / juego); `MapPanel` extiende `JPanel` y dibuja con Java2D en `paintComponent`; atajos con `InputMap/ActionMap`; textos en `Texts` (español). FlatLaf (Apache 2.0) es **opcional** y se decide en la Fase 5.6 | La UI funcional mínima se entrega desde la 5.1; lo visual es lo último y recortable |
| **DT-15** | **Registro de eventos técnicos** con `java.util.logging` en el cliente y el registro de Spring Boot (SLF4J) en el servidor. Los mensajes al jugador siempre salen de `Messages` | Cero dependencias nuevas: ya vienen con Spring Boot |
| **DT-16** | **Spring Boot como servidor.** `Main` arranca un `SpringApplication` con Tomcat embebido en `127.0.0.1` y un puerto libre (RNF-16). `@RestController` en `api`, `@Service` en los servicios de `application` y `@Repository` en `MongoSaveRepository`; los beans que no son componentes se declaran en `config`. El dominio no lleva anotaciones de Spring | Spring Boot es obligatorio (R-01); el dominio sigue puro y se prueba sin Spring |
| **DT-17** | **Dos controladores, un puerto.** `GameRestController` (HTTP) delega en `GameController` (GRASP). `GameController` y `RemoteGameClient` implementan la interfaz `GameCommands`; la UI depende solo de ella | Las 146 pruebas de aceptación siguen usando el controlador en proceso, sin servidor; la UI no sabe si hay red (AR-3) |
| **DT-18** | **API REST.** Un recurso por sustantivo y verbos HTTP; JSON; sin sesión de usuario (una partida por servidor). `ErrorStatusMapper` traduce cada `ErrorCode` a un código HTTP (200/201 éxito; 400 entrada inválida; 404 no existe; 409 regla de negocio; 422 partida dañada; 500 y 503 fallas). El cuerpo de error es `{code, message}` con los textos de RN-35. Detalle en [`API.md`](API.md) | Cumple RNF-15; el cliente reconstruye el mismo `Result` que devuelve el controlador |
| **DT-19** | **Eventos en el sobre de la respuesta.** `EventCollector` (un `GameListener` del servidor) acumula los eventos que provoca cada orden y `GameRestController` los devuelve como `events` junto a `data`. `RemoteGameClient` reconstruye el `Result`, y después republica los eventos, en el mismo orden, en su `EventBus` local | El Observer cruza la red sin WebSockets ni sondeo (AR-6, R-09): un solo viaje por orden |
| **DT-20** | **Concurrencia.** Una partida por servidor (S-01): `GameRestController` ejecuta cada orden bajo un mismo candado. El cliente envía las órdenes desde el EDT, de forma síncrona (localhost, tiempo de espera de 5 s) | Sin carreras con un solo jugador y sin `SwingWorker`; el riesgo está en RT-08 |
| **DT-21** | **Persistencia en MongoDB.** El puerto `SaveRepository` sigue en el dominio; el adaptador `MongoSaveRepository` usa Spring Data MongoDB sobre la colección `partidas` (RN-31, RN-32). La URI se configura con `valleverde.mongo.uri` (por defecto `mongodb://localhost:27017/valleverde`) con tiempos de espera de 2 s. Cualquier error de MongoDB se convierte en una excepción sin marcar del dominio que `SaveGameService` traduce a `ERROR_ES` | Jugar no requiere MongoDB (RNF-11); la infraestructura sigue siendo reemplazable (inversión de dependencias) |
| **DT-22** | **Lanzador.** `Main` sin argumentos arranca el servidor y el cliente en el mismo proceso (el cliente usa `RemoteGameClient` contra el puerto elegido). `--server [--port n]` arranca solo el servidor y `--client --url …` solo la ventana. Spring se inicia con `headless(false)` para poder abrir Swing | RNF-16; permite demostrar la separación y depurar cada lado |

## 5. Pila tecnológica y construcción

| Componente | Elección | Versión (verificada en Maven Central el 2026-11-22) |
|---|---|---|
| Lenguaje | Java, compilado con `--release 17` (se ejecuta en JRE 17 o superior; el equipo tiene JDK 25) | Java 17 |
| Construcción | Apache Maven | 3.9.16 (portable en `tools/`) |
| Pruebas | JUnit Jupiter (JUnit 5) | 5.14.4 |
| Arquitectura | ArchUnit (`archunit-junit5`), solo en pruebas | 1.5.1 |
| Servidor | Spring Boot (`spring-boot-starter-parent`) con los starters `web` (Tomcat embebido y Jackson), `data-mongodb`, `validation` y `test`; el BOM fija las versiones de Spring, Jackson y el *driver* de MongoDB | 3.5.16 (requiere Java 17) |
| Base de datos | MongoDB (servidor local gratuito) | el que tenga instalado el equipo |
| Cobertura | JaCoCo (`jacoco-maven-plugin`) | 0.8.16 |
| Plugins Maven | compiler 3.16.0 · surefire 3.6.1 · `spring-boot-maven-plugin` 3.5.16 (jar ejecutable único; reemplaza a *shade*) | — |
| Aspecto (opcional) | FlatLaf | 3.8 |

> JUnit ya publica la serie 6, pero se mantiene **JUnit 5** como pide el stack. Todo es gratuito (Apache 2.0, EPL 2.0 o similar).

Decisiones del `pom.xml` (se crea en la Fase 5.0):
- `maven.compiler.release=17`; `finalName=farmsim`; el `spring-boot-maven-plugin` (`repackage`) produce un único `farmsim.jar` con Spring, Jackson, el *driver* de MongoDB y los assets; la clase de arranque es `edu.ucc.farmsim.Main`.
- JaCoCo: informe en `target/site/jacoco` y regla `check` con **dos reglas separadas** (paquetes `domain` y `application`, cobertura de líneas ≥ 70 %, RNF-05).
- Surefire ejecuta las pruebas rápidas por defecto; las de rendimiento llevan `@Tag("perf")` y corren con `-Pperf` (y en el cierre de la Fase 5.5).
- Comando de verificación de cada fase: `mvn clean verify` (compila, prueba, mide cobertura y empaqueta).
- Se generará un *Maven Wrapper* (`mvnw`) para que cualquier equipo compile sin instalar Maven.

## 6. Flujo típico de una orden

Como en la secuencia [A1](diagramas/A1_secuencia_vender_cosecha.png):

1. La UI captura el clic o el botón y llama a `GameCommands.sell("CARROT", 3)` (tipos simples). La implementación del cliente, `RemoteGameClient`, envía `POST /api/market/sales` con `{itemId, quantity}`.
2. `GameRestController` toma el candado y llama a `GameController.sell`, que delega en `MarketService`: este obtiene el estado de `GameSession`, valida (`Inventory.count`), llama al dominio (`Market.sellUnit`, `Inventory.remove`, `GameState.earn`) y evalúa logros con `ProgressFacts`.
3. El servicio publica el evento (`EventBus.publish`) y devuelve `Result.ok(79)`; el `EventCollector` lo guarda y el controlador REST responde `200` con `{data: 79, events: [...]}`.
4. `RemoteGameClient` reconstruye `Result.ok(79)` y republica los eventos en el `EventBus` local; `HudPanel` y `ToastLayer` se actualizan y `MarketDialog` muestra el resultado.
5. Ante un rechazo, el servicio devuelve `Result.fail(código, mensaje)`, la API responde con el código HTTP del error (p. ej. `409`) y el cuerpo `{code, message}`, y el cliente reconstruye el mismo `Result.fail`: **sin cambios de estado ni eventos**.

## 7. Plan de implementación por fases

> Cada fase termina con `mvn clean verify` en verde, un `.jar` ejecutable y una **demostración posible** (UI funcional mínima de las historias de la fase). Dentro de una historia se trabaja así: (1) pruebas de aceptación `HUxx_…` a partir de los criterios; (2) código mínimo; (3) refactor; (4) commit `feat(area): description (HUxx)`. Al cerrar la fase: resumen, patrones aplicados y qué diagrama o documento actualizar; **se espera tu visto bueno antes de seguir**.

| Fase | Historias (puntos) | Trabajo técnico principal | UI funcional mínima | Patrones | Pruebas críticas |
|---|---|---|---|---|---|
| **5.0** Preparación técnica | — | `pom.xml`, esqueleto de paquetes, `Main` (ventana vacía), pruebas de arquitectura (AR-1 a AR-5), JaCoCo, `mvnw`, README inicial | ventana vacía | — | arquitectura |
| **5.1** Núcleo del turno | HU-13, 09, 08, 11, 28, 10, 33, 34 (**25**) | `GameRules`, `Inventory`, calendario (`GameClock`, `SeasonState`), `StateRandom`, clima (`WeatherStrategy`), `GameState`, `DayPipeline` con pasos de calendario, pronóstico y energía, `GameSession`, `DayService`, `Result/Messages`, `EventBus` | pantalla de inicio, HUD con etiquetas y botón «Terminar día» | Singleton, State (estación), Strategy (clima), Observer, Memento (versión mínima en memoria, para HU-10 · CA-4) | determinismo de 112 días |
| **5.2** Cultivo | HU-01 a 07, 35, 36 (**29**) | `FarmMap`, `Plot`, `Crop`, `CropState` ×5, `CropFactory` ×5, `FarmService`, pasos de crecimiento y marchitamiento, DTO de parcela, mapa Java2D en vectores, barra de herramientas, tooltips | mapa con parcelas, herramientas 1–5 y clic | State (cultivo), Factory Method | **1** (fuera de estación), **2** (inventario lleno) |
| **5.2b** Migración a Spring Boot | — (sin historias nuevas: conserva las 163 pruebas) | `pom.xml` con Spring Boot 3.5.16; interfaz `GameCommands`; capa `api` (`GameRestController`, `ErrorStatusMapper`, `EventCollector`); `client` (`RemoteGameClient`); `config`; `MongoSaveRepository` en lugar de `JsonSaveRepository`; `Main` con modos; reglas AR-1 a AR-3 y AR-11; `docs/API.md` | la misma de la 5.2, ahora sobre HTTP | — (el Observer cruza la red en el sobre de respuesta) | todas las anteriores en verde; arranque ≤ 5 s (RNF-02) |
| **5.3** Clima y economía | HU-12, 37, 14, 15, 16, 38 (**23**) | efectos del clima en el *pipeline*, `Market`, `PricingStrategy` ×2, `MarketService`, paso de mercado, `InventoryGrid`, `MarketDialog` | inventario en grilla y mercado modal | Strategy (precios) | **3** (helada), **4** (saturación) |
| **5.4** Construcción y animales | HU-18 a 23, 17 (**21**) | `Building`, `BuildingBuilder` ×4, `BuildingDirector`, `Animal`, `AnimalState` ×3, `BuildingService`, `AnimalService`, paso de animales, `DaySummary` | diálogo de construcción y panel del edificio (alimentar, recolectar, curar) | Builder, State (animal) | **5** (animal enfermo no produce) |
| **5.5** Progresión y persistencia | HU-26, 27, 24, 29, 30, 31, 32 (**24**) | `AchievementTracker`, `ProgressFacts`, desbloqueos, `GameMemento`, `GameStateValidator`, `SaveRepository`, `MongoSaveRepository`, `SaveCodec`, `Checksum`, `SaveGameService`; **pruebas de rendimiento a plena carga** (HU-10 · CA-3, HU-29 · CA-4, HU-30 · CA-4) | diálogos de ranuras, logros y atajos `F5`/`F9` | Memento (completo: documento en MongoDB) | **6** (guardar → cargar idéntico), **7** (partida dañada o inexistente) |
| **5.6** Interfaz visual y extras *(recortable)* | HU-39, 42, 25, 40, 41 (**19**) | `ToastLayer`, `AssetManager` y respaldo vectorial, invernadero, efectos (`ParticleSystem`, `DayNightTint`), animaciones con `Timer`; `docs/ASSETS.md`, README final y matriz de trazabilidad completa; decisión sobre FlatLaf | pulido visual | Observer (completado) | — |

**Qué se actualiza al cerrar cada fase:** el diagrama de clases si aparecen clases nuevas con responsabilidad estructural, el de desarrollo si cambia un paquete o una dependencia (lo comprueba `tools/check_diagrams.py`) y la matriz de trazabilidad (RF → HU → diagrama → clases → pruebas).

**Si falta tiempo:** se recorta la Fase 5.6 sin afectar el diseño base (el juego sigue completo con gráficos vectoriales). El MVP son las 30 historias *Must* (109 puntos).

## 8. Riesgos técnicos

| ID | Riesgo | Prob. | Impacto | Mitigación |
|---|---|---|---|---|
| RT-01 | El `pipeline` del fin de día se vuelve un cuello de botella o se rompe al agregar pasos | Media | Alto | Pasos pequeños e independientes con prueba de orden (HU-10 · CA-1); `DayReport` único; transacción por memento (DT-03) |
| RT-02 | El estado global del Singleton contamina pruebas | Media | Medio | `resetForTests()` y regla de `@BeforeEach` (DT-09) |
| RT-03 | Cambios al esquema de guardado rompen partidas viejas | Baja | Medio | `schemaVersion` y rechazo con `VERSION_NO_SOPORTADA`; pruebas con documentos de ejemplo versionados |
| RT-04 | La UI Swing no se puede probar sin pantalla | Media | Bajo | Lógica de presentación en clases sin Swing (presentadores y DTO); pruebas de componentes solo si `!GraphicsEnvironment.isHeadless()` |
| RT-05 | El JDK 25 del equipo produce bytecode incompatible con JRE 17 | Baja | Alto | `--release 17` y una comprobación en 5.0 (`javap` del `.class` generado) |
| RT-06 | Mapas, sprites y efectos consumen el tiempo | Alta | Alto | Orden de fases con la 5.6 recortable; vectoriales desde el inicio |
| RT-07 | Sobreingeniería por tantos patrones | Media | Medio | Cada patrón está justificado por escrito ([`DIAGRAMAS.md`](DIAGRAMAS.md) §3.1); no se agregan más |
| RT-08 | El cliente bloquea el EDT al esperar la respuesta HTTP | Baja | Medio | Servidor local (milisegundos), tiempo de espera de 5 s y mensaje `ERROR_INTERNO` si vence; si en la práctica se nota, las órdenes pasan a un `SwingWorker` sin tocar el servidor |
| RT-09 | El arranque de Spring Boot supera los 5 s de RNF-02 | Media | Medio | Medir en la Fase 5.2b; inicialización perezosa de beans y `spring.main.banner-mode=off`; la ventana puede abrirse mientras el servidor termina de arrancar |
| RT-10 | MongoDB no está disponible al guardar o cargar | Media | Medio | Jugar no lo requiere (RNF-11); tiempo de espera de 2 s y `ERROR_ES` con la partida intacta (HU-29 · CA-2, HU-31 · CA-5) |
| RT-11 | Las pruebas de MongoDB dependen del equipo donde corren | Media | Bajo | Solo `MongoSaveRepository` las necesita; el resto usa `FakeSaveRepository`; se omiten con un aviso si no hay MongoDB |

## 9. Cambios derivados de esta fase (ya aplicados)

| # | Cambio | Dónde |
|---|---|---|
| C-01 | `AchievementTracker.evaluate` recibe `ProgressFacts` y no `GameState`: la versión anterior creaba un **ciclo** `game → progress → game` | diagramas 03 y 04; el verificador ahora detecta ciclos |
| C-02 | Se dibuja la dependencia `farm → time` (cultivos y animales necesitan clima y estación) | diagrama 04 |
| C-03 | Se agregan `GameSession` (DT-10), `StateRandom` (DT-07) y `ProgressFacts` | diagramas 03 y 04 |
| C-04 | RN-32: el archivo guarda el **estado interno del generador** (`StateRandom`) en lugar de «semilla + número de sorteos» | `REGLAS_DE_NEGOCIO.md` |
| C-05 | RN-35: nuevo código `ERROR_INTERNO` para excepciones inesperadas (DT-02) | `REGLAS_DE_NEGOCIO.md` |

## 10. Estado de la Fase 5.0 (preparación técnica)

| Entregable | Resultado |
|---|---|
| `pom.xml` con las versiones de §5, JaCoCo con **dos barreras** (dominio y aplicación ≥ 70 %), perfil `perf` y *shade* | `mvn clean verify` en verde; la barrera se probó en negativo con una clase sin cubrir y el build falló como debía |
| Esqueleto: 26 `package-info.java` del árbol de §3, `Main`, `GameInfo`, `MainWindow` | la ventana «Valle Verde» abre en 0,5 s |
| Pruebas: AR-1 a AR-5 y AR-7 (ArchUnit), AR-9 (registro de patrones), `GameInfoTest`, `MainWindowTest` | 12 pruebas en verde |
| `mvnw` (Maven Wrapper 3.9.16) | `./mvnw -v` funciona sin instalar Maven |
| `target/farmsim.jar` ejecutable | bytecode versión 61 (Java 17), `Main-Class` correcto; sin pantalla termina con un mensaje claro (código 1) |
| `tools/traceability.py` y `docs/TRAZABILIDAD.md` | matriz RF → HU → componente → clases → pruebas; el modo `--upto 5.x` exige historias completas |
| `README.md` | cómo compilar, ejecutar y probar; estructura; tabla de patrones |

Ajustes surgidos al implementar: (1) `Main` se declara como capa «Composition» en AR-1, porque ArchUnit trata como violación que una clase fuera de las capas acceda a la UI; (2) `GameInfo` y `MainWindow` se agregan al árbol y al diagrama de desarrollo; (3) se añade la regla AR-10 (`Stories:` en el Javadoc).

## 11. Estado de la Fase 5.1 (núcleo del turno)

**Historias cerradas (8 historias, 25 puntos):** HU-13, 09, 08, 11, 28, 10, 33 y 34. `python tools/traceability.py --upto 5.1` termina sin errores: cada historia tiene sus clases (`Stories:`) y al menos tantas pruebas `HUxx_` como criterios.

| Resultado | Valor |
|---|---|
| Pruebas | 78 en verde (61 con prefijo `HUxx_`), 1 deshabilitada con motivo (HU-10 · CA-3, rendimiento a plena carga, fase 5.5) |
| Cobertura de líneas (JaCoCo) | **dominio 97,4 %**, **aplicación 93,2 %**, infraestructura 100 %, UI 92,8 % (barrera: 70 % en dominio y aplicación) |
| Patrones aplicados | Singleton (`GameClock`), State (`SeasonState` y 4 estados), Strategy (`WeatherStrategy`: estacional y fija), Observer (`EventBus`, `GameListener`, `HudModel`, `HudPanel`), Memento mínimo (`GameState`, `GameMemento`), todos registrados en `patterns.txt` (AR-9) |
| Prueba de integración Swing | clic en «Nueva partida» y «Terminar día» sobre las pantallas reales: el HUD muestra «500 m», «Día 1» y luego «Día 2» |
| Arranque real | la ventana «Valle Verde» aparece en 0,53 s |

**Decisiones tomadas al implementar (ajustes al plan):**

| # | Decisión | Motivo |
|---|---|---|
| D-01 | El **Memento** se adelanta a la 5.1 en su forma mínima (en memoria) | HU-10 · CA-4 exige que una falla interna deje el estado idéntico; `DayService` toma una instantánea y la restaura. La 5.5 lo completa con archivo, validación y las partes que traigan las fases |
| D-02 | `ProgressService.newGame` crea las partidas | RF-28 pertenece al componente *Progreso* de la vista funcional |
| D-03 | **Presentadores sin Swing** (`StartScreenPresenter`, `GameScreenPresenter`, `HudModel`) | La lógica de las pantallas se prueba sin pantalla (RT-04); Swing solo pinta |
| D-04 | `DayPipeline` ordena los pasos por `DayPhase`, sin importar el orden de registro | El orden de RN-03 no puede romperse al agregar un paso |
| D-05 | `SaveRepository` nace con un solo método (`slotExists`) y `JsonSaveRepository` con su implementación real mínima | HU-33 · CA-3 («Cargar» sin partidas) la necesita; la 5.5 agrega guardar, cargar y listar |
| D-06 | Código `SIN_PARTIDA` agregado a RN-35 | Terminar el día sin partida en curso necesitaba un mensaje propio |

**Criterios cubiertos en parte (se completan donde nace lo que falta):**

| Criterio | Qué falta | Fase |
|---|---|---|
| HU-28 · CA-1 | demanda 100 de todos los productos y logros bloqueados con contadores en 0 | 5.3 y 5.5 |
| HU-10 · CA-2 | marcas de riego de las parcelas limpiadas al terminar el día | 5.2 (HU-03) |
| HU-10 · CA-3 | rendimiento a plena carga (prueba `@Disabled`) | 5.5 |
| HU-08 · CA-2 | el rechazo `SIN_ENERGIA` al sembrar (hoy se prueba en `Energy` y en `Messages`) | 5.2 (HU-01) |
| HU-34 · CA-2 | el evento `EnergyChanged` lo publicará una acción real (hoy se simula en la prueba) | 5.2 |
| HU-33 · CA-3 | cargar cuando sí hay una partida guardada | 5.5 (HU-30) |

**Diagramas:** el diagrama lógico y el de desarrollo incluyen ahora las clases reales de la fase. Además de `tools/check_diagrams.py`, se comprobó una vez contra los `import` reales del dominio: no falta ninguna dependencia en el dibujo (las flechas dibujadas que aún no existen en el código corresponden a fases siguientes).

## 12. Estado de la Fase 5.2 (cultivo)

**Historias cerradas (9 historias, 29 puntos):** HU-01, 02, 03, 04, 05, 06, 07, 35 y 36. `python tools/traceability.py --upto 5.2` termina sin errores.

| Resultado | Valor |
|---|---|
| Pruebas | 163 en verde (146 con prefijo `HUxx_`) y 1 deshabilitada con motivo (HU-10 · CA-3, fase 5.5) |
| Cobertura de líneas (JaCoCo) | **dominio 97,8 %**, **aplicación 95,5 %**, UI 90,6 %, infraestructura 100 % (barrera: 70 % en dominio y aplicación) |
| Patrones aplicados | State (`CropState` con `SeedState`, `SproutState`, `GrowingState`, `MatureState`, `WitheredState`; contexto `Crop`), Factory Method (`CropFactory` con 5 creadores), Observer (`MapPanel`); todos registrados en `patterns.txt` |
| Pruebas críticas cerradas | **1** `HU01_ca2_sowingOutOfSeasonIsRejectedAndNothingChanges` y **2** `HU04_ca2_harvestingWithAFullInventoryIsRejectedAndTheCropStaysMature` |
| Integración Swing real | herramienta «1 Sembrar» → clic en el mapa → HUD 98/100 → `Enter` termina el día |

**Decisiones tomadas al implementar:**

| # | Decisión | Motivo |
|---|---|---|
| D-07 | `CropType` no conoce `ItemType`: es `ItemType` quien enlaza cada semilla y cada cosecha con su cultivo | Evita un ciclo `crop ↔ economy`: el diagrama de desarrollo ya dibujaba `economy → crop` |
| D-08 | `GrowingPhaseState` agrupa lo común de Semilla, Brote y Crecimiento (necesitan agua, no se cosechan) | Una sola implementación de la regla de crecimiento y de marchitamiento por sequedad |
| D-09 | El marchitamiento por cambio de estación vive en `CalendarStep` (paso 5 de RN-03) y el de sequedad en `GrowthStep` (paso 2) | Es exactamente el orden de RN-03; el crecimiento del último día ocurre antes del cambio de estación |
| D-10 | La herramienta «Cosechar» cosecha, limpia o (en 5.4) recolecta según el contenido de la parcela | RN-34 |
| D-11 | Código nuevo `CULTIVO_MARCHITO` en RN-35 | RN-07 manda rechazar el riego de un cultivo marchito y no había código para ello |
| D-12 | Regar un cultivo maduro se permite (cuesta energía sin efecto) | RN-07 solo exige «vivo y sin regar hoy»; el rechazo necesitaría una regla nueva |
| D-13 | Las herramientas «Construir» y «Alimentar» y los atajos `M`, `I`, `L`, `F5`, `F9` se pueden pulsar y responden «estará disponible en una fase posterior» | HU-36 · CA-1 y CA-5 piden que existan; sus pantallas llegan en 5.3, 5.4 y 5.5 |
| D-14 | `MapPanel` consulta el mapa solo al recibir un evento (`GameStarted`, `DayEnded`, `MapChanged`); no tiene `Timer` (lo verifica una prueba) | Regla de interfaz: actualizar por eventos, no por consulta en bucle |
| D-15 | Geometría (`MapGeometry`) y dibujo (`MapRenderer`) son clases sin componente | Se prueban pintando sobre un `BufferedImage`, sin pantalla |

**Correcciones a documentos aprobados:** (1) las coordenadas de ejemplo de HU-01 (parcela `(2,3)`) y de HU-18 (esquina `(4,6)`) no coincidían con el mapa de RN-04 y se cambiaron por `(5,3)` y `(2,7)`; (2) `CULTIVO_MARCHITO` se agregó a RN-35; (3) los diagramas lógico y de desarrollo incluyen las clases de la fase (`FarmMap.growCrops`, `Crop.endOfDay`, `CropState.onDayEnd`, `FarmService`).

**Criterios de la 5.1 que se cerraron en esta fase:** HU-10 · CA-2 (se limpia la marca de riego), HU-08 · CA-2 (`SIN_ENERGIA` al sembrar) y HU-34 · CA-2 (el evento `EnergyChanged` lo publica una acción real).

**Criterios cubiertos en parte (se completan donde nace lo que falta):**

| Criterio | Qué falta | Fase |
|---|---|---|
| HU-28 · CA-1 | demanda 100 de todos los productos y logros bloqueados | 5.3 y 5.5 |
| HU-10 · CA-3 | rendimiento a plena carga (prueba `@Disabled`) | 5.5 |
| HU-33 · CA-3 | cargar cuando sí hay una partida guardada | 5.5 |
| HU-35 · CA-3 | dibujar edificios y animales | 5.4 |
| HU-36 · CA-4 | recolectar los productos de un edificio con «Cosechar» | 5.4 |
| HU-36 · CA-5 | `M`, `I`, `L`, `F5` y `F9` abren su función (hoy solo responden que llegará) | 5.3 y 5.5 |

**Diagramas:** además de `tools/check_diagrams.py`, la nueva herramienta `tools/check_imports.py` compara los `import` reales entre paquetes del dominio con las flechas del diagrama de desarrollo: no falta ninguna.

## 13. Estado de la Fase 5.2b (migración a Spring Boot)

**Sin historias nuevas:** se migró la arquitectura exigida (Spring Boot) conservando todas las pruebas de aceptación. `python tools/traceability.py --upto 5.2` sigue sin errores.

| Resultado | Valor |
|---|---|
| Pruebas | 222 (2 se omiten con aviso si no hay MongoDB: `MongoSaveRepositoryRealMongoTest`); `mvn clean verify` en verde con las dos barreras de cobertura |
| Nuevas | 22 de la API (una por ruta y rechazo), 11 del cliente contra el servidor real (incluye «mismos resultados y eventos que el controlador en proceso»), 1 de las pantallas Swing sobre HTTP, 8 de reglas ArchUnit |
| Arranque | `java -jar target/farmsim.jar`: la ventana «Valle Verde» aparece en **4,1 a 4,3 s** (RNF-02 pide ≤ 5 s) con servidor, Tomcat y cliente |
| Sin MongoDB | el juego funciona; `GET /api/saves/any` responde `503 ERROR_ES` con el texto de RN-35 |
| Patrón nuevo | Observer: `EventCollector` (el servidor guarda los eventos de cada orden para la respuesta) |

**Decisiones tomadas al implementar:**

| # | Decisión | Motivo |
|---|---|---|
| D-16 | `hasGameInProgress()` del cliente se deduce de `GET /api/game` y no tiene ruta propia | Una ruta menos y la misma semántica (sin partida = `404 SIN_PARTIDA`) |
| D-17 | El cliente reconstruye el `Result` y **después** republica los eventos | Mismo orden que en proceso: la UI ve los eventos antes de recibir el resultado |
| D-18 | `EventDto.data` es el propio *record* del evento; el cliente elige la clase por el nombre entre los subtipos permitidos de `GameEvent` | Un evento nuevo no exige tocar el decodificador |
| D-19 | `MongoSaveRepository` lleva `@Profile("!no-mongo")`; las pruebas de API y cliente usan el perfil `no-mongo` con `FakeSaveRepository` | Esas pruebas no dependen de tener MongoDB (RT-11) |
| D-20 | Todo error técnico de MongoDB se vuelve `SaveStorageException` (dominio) y `SaveGameService` lo traduce a `ERROR_ES`; `hasAnySave()` ahora devuelve `Result<Boolean>` | La aplicación no ve tipos de MongoDB y jugar no depende de la base |
| D-21 | Surefire fija `java.awt.headless=false` | Spring Boot activa el modo sin pantalla y las pruebas Swing se omitían en silencio |
| D-22 | Un `seedId` desconocido responde `500 ERROR_INTERNO` (no hay código de RN-35 para «semilla inexistente») | La UI nunca lo envía; no se inventó un código nuevo |
| D-23 | `JsonSaveRepository` y Gson se eliminaron; la D-05 de la §11 queda sustituida por `MongoSaveRepository` | Persistencia en MongoDB (RN-31) |

**Pendiente de la persistencia (5.5):** guardar, cargar y listar (`PUT`/`POST /api/saves/{slot}`), `SaveCodec` con Jackson, `Checksum` y `GameStateValidator`. El adaptador y el formato de documento (`SaveDocument`) ya existen.

**Diagramas:** las 6 vistas se actualizaron (MongoDB, cliente, API REST, paquetes `client`/`api`/`config`, despliegue con servidor y cliente dentro de la JVM, y la secuencia A1 con el salto HTTP); `check_diagrams.py` y `check_imports.py` terminan sin errores.

