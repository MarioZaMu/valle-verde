# Arquitectura y plan técnico — Valle Verde

> **Versión 0.2 · 2026-11-22 · Fase 5.2b (migración a Spring Boot)**
> Formaliza el diseño propuesto en los diagramas ([`DIAGRAMAS.md`](DIAGRAMAS.md)): capas, árbol de paquetes, decisiones técnicas y plan de implementación por fases con sus historias ([`HISTORIAS_DE_USUARIO.md`](HISTORIAS_DE_USUARIO.md) §7). El plan de pruebas está en [`PLAN_DE_PRUEBAS.md`](PLAN_DE_PRUEBAS.md).
> **Estado:** las Fases 5.0 (§10), 5.1 (§11), 5.2 (§12) y 5.2b (§13, migración a servidor Spring Boot con API REST, cliente Swing por HTTP y persistencia en MongoDB) 5.3 (§14), 5.4 (§15) y 5.5 (§16) están hechas. La Fase 5.6 empieza solo con tu visto bueno.

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
│  ├─ components   HudPanel, ToolBar, InventoryGrid, MarketDialog, SlotsDialog, AchievementsDialog, ToastPresenter, ToastQueue, ToastLayer
│  ├─ render       MapPanel, MapRenderer, VisualEffects, Particle, SeasonPalette
│  ├─ assets       AssetManager (el respaldo vectorial vive en `MapRenderer`)
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
| **5.4** Construcción y animales | HU-18 a 24, 17 (**23**) | `Building`, `BuildingBuilder` ×4, `BuildingDirector`, `Animal`, `AnimalState` ×3, `BuildingService`, `AnimalService`, paso de animales, `DaySummary` | diálogo de construcción y panel del edificio (alimentar, recolectar, curar) | Builder, State (animal) | **5** (animal enfermo no produce) |
| **5.5** Progresión y persistencia | HU-26, 27, 29, 30, 31, 32 (**22**) | `AchievementTracker`, `ProgressFacts`, desbloqueos, `GameMemento`, `GameStateValidator`, `SaveRepository`, `MongoSaveRepository`, `SaveCodec`, `Checksum`, `SaveGameService`; **pruebas de rendimiento a plena carga** (HU-10 · CA-3, HU-29 · CA-4, HU-30 · CA-4) | diálogos de ranuras, logros y atajos `F5`/`F9` | Memento (completo: documento en MongoDB) | **6** (guardar → cargar idéntico), **7** (partida dañada o inexistente) |
| **5.6** Interfaz visual y extras *(recortable)* | HU-39, 42, 25, 40, 41 (**19**) | `ToastQueue`, `ToastPresenter`, `ToastLayer`, `AssetManager` y respaldo vectorial, invernadero, efectos (`VisualEffects`, `Particle`, `SeasonPalette`), animaciones con `Timer`; `docs/ASSETS.md`, README final y matriz de trazabilidad completa; decisión sobre FlatLaf | pulido visual | Observer (completado) | — **(hecha)** |

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
| D-13 | Las herramientas «Construir» y «Alimentar» y los atajos `M`, `I`, `L`, `F5`, `F9` se pueden pulsar; los que aún no tienen pantalla responden «estará disponible en una fase posterior» (en 5.3 y 5.4 ya funcionan `M`, `I`, «Construir» y «Alimentar»; quedan `L`, `F5` y `F9` para 5.5) | HU-36 · CA-1 y CA-5 piden que existan |
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

## 14. Estado de la Fase 5.3 (clima y economía)

**Historias cerradas (6 historias, 23 puntos):** HU-12, 14, 15, 16, 37 y 38. `python tools/traceability.py --upto 5.3` termina sin errores.

| Resultado | Valor |
|---|---|
| Pruebas | 280 en verde y 2 omitidas con aviso (las de MongoDB real); 194 con prefijo `HUxx_` |
| Cobertura de líneas (JaCoCo) | **dominio 97,7 %**, **aplicación 96,9 %**, UI 90,7 %, API 95,2 %, cliente 89,3 % (barrera: 70 % en dominio y aplicación) |
| Patrones aplicados | Strategy: `PricingStrategy` con `CropPricing` y `FlatPricing`; Observer: `InventoryModel`; todos registrados en `patterns.txt` |
| Pruebas críticas cerradas | **3** `HU12_ca2_frostWithersEveryCropInTheOpenEvenAMatureOne` y **4** `HU15_ca2_afterTenCarrotsTheNextOnePaysOnly22` con `HU15_ca3_theDemandNeverFallsBelow50` |
| Arranque | 4,1 a 4,3 s con la máquina libre (5.2b); al medir de nuevo con el servidor de Java de VS Code indexando el proyecto subió a 5 a 7,5 s: el límite de RNF-02 (≤ 5 s) depende de la carga del equipo (RT-09) y conviene medirlo otra vez en el cierre del proyecto |
| API nueva | `GET /api/market`, `POST /api/market/purchases`, `POST /api/market/sales`, `GET /api/market/quotes`, `GET /api/inventory` (ver [`API.md`](API.md)) |

**Decisiones tomadas al implementar:**

| # | Decisión | Motivo |
|---|---|---|
| D-24 | El paso de efectos del clima (`WeatherEffectsStep`) es el primero del *pipeline* y el de mercado (`MarketStep`) el cuarto | Es exactamente el orden de RN-03 |
| D-25 | `CropType` conoce el precio de semilla y la venta base (tabla de RN-05); `ItemType` sigue sin conocer a `CropType` hacia atrás | Una sola tabla por cultivo; sin ciclo `crop ↔ economy` (D-07) |
| D-26 | Precio = `máx(1, ⌊(base × factor × demanda + 5 000) / 10 000⌋)` con enteros | Es el redondeo half-up de RN-17 sin números decimales |
| D-27 | La tendencia (RN-19) guarda «precio anterior = 0» al inicio, que se muestra como ▬; `MarketSnapshot` lo conserva en el memento | «Al inicio todos muestran ▬» sin inventar un precio previo |
| D-28 | `GET /api/market/quotes` calcula el «total previsto» en el servidor | El total depende de la demanda que baja unidad a unidad (79 m, no 3 × 27) |
| D-29 | Sembrar y cosechar publican `InventoryChanged`; el modelo del inventario recarga y limpia la semilla elegida si se agota | HU-37 · CA-3 por eventos (Observer), sin consultar en bucle |
| D-30 | El inventario y el mercado son diálogos modales (`I`, `M`; `Esc` cierra) y la lógica vive en `InventoryModel` y `MarketPresenter`, sin Swing | RT-04: se prueba sin pantalla; el modal impide clics en el mapa (HU-38 · CA-1) |
| D-31 | Las compras y ventas responden `200` (no `201`) | Son órdenes sobre el mercado, no recursos que se consulten después |
| D-32 | Ante una semilla o un producto que no corresponde (vender una semilla, comprar una cosecha) el servidor responde `500 ERROR_INTERNO` | Como D-22: la UI nunca lo envía y RN-35 no tiene código para ello |

**Criterios cubiertos en parte (se completan donde nace lo que falta):**

| Criterio | Qué falta | Fase |
|---|---|---|
| HU-28 · CA-1 | solo falta que los logros bloqueados empiecen con contadores en 0 (la demanda 100 ya se prueba) | 5.5 |
| HU-37 · CA-2 | la grilla ya refleja la capacidad que informe el inventario; construir el Almacén (+12) | cerrado en 5.4 |
| HU-38 · CA-3 | el ejemplo de la Vaca (800 m) | cerrado en 5.4 |
| HU-10 · CA-3, HU-33 · CA-3 | rendimiento a plena carga y cargar una partida guardada | 5.5 |
| HU-35 · CA-3, HU-36 · CA-4 | dibujar edificios y animales; recolectar sus productos con «Cosechar» | cerrados en 5.4 |
| HU-36 · CA-5 | los atajos `L`, `F5` y `F9` aún responden que llegarán (`M` e `I` ya abren su función) | 5.5 |

**Diagramas:** `04_vista_desarrollo` incluye las clases nuevas; el resto de vistas ya tenían el diseño de la economía (Strategy de precios, `MarketService`, `MarketDialog`). `check_diagrams.py` y `check_imports.py` terminan sin errores.

---

## 15. Estado de la Fase 5.4 (construcción y animales)

**Historias cerradas (8 historias, 23 puntos):** HU-17, 18, 19, 20, 21, 22, 23 y 24. HU-24 (Almacén) se adelantó desde la 5.5 porque HU-37 · CA-2 pedía las 36 casillas. `python tools/traceability.py --upto 5.4` termina sin errores.

| Resultado | Valor |
|---|---|
| Pruebas | 379 en verde y 2 omitidas con aviso (las de MongoDB real) |
| Cobertura de líneas (JaCoCo) | **dominio 98,0 %**, **aplicación 97,8 %**, UI 91,8 %, API 95,7 %, cliente 90,0 % (barrera: 70 % en dominio y aplicación) |
| Patrones aplicados | Builder: `BuildingBuilder` con `CoopBuilder`, `BarnBuilder`, `WarehouseBuilder`, `GreenhouseBuilder`, `BuildingDirector` y `Building` como producto; State: `AnimalState` con `HealthyState`, `HungryState`, `SickState` y `Animal` como contexto; todos registrados en `patterns.txt` |
| Prueba crítica cerrada | **5** `HU22_ca2_aSickHenProducesNothing_criticalTest` (y la variante de la gallina hambrienta) |
| API nueva | `GET|POST /api/buildings`, `POST /api/animals`, `POST /api/buildings/{id}/feeding`, `/collection` y `/animals/{n}/treatment`; el resumen del día viaja como evento `DaySummaryReady` (ver [`API.md`](API.md)) |
| Verificadores | `check_hu.py`, `check_diagrams.py`, `check_imports.py` y `traceability.py` terminan sin errores |

**Decisiones tomadas al implementar:**

| # | Decisión | Motivo |
|---|---|---|
| D-33 | `Achievement` (los nueve logros de RN-28) y `AchievementTracker` ya existen con solo el conjunto de logros desbloqueados | RN-29 y HU-18 · CA-5 necesitan saber qué edificios están bloqueados; los contadores y la evaluación llegan en 5.5 (HU-26) |
| D-34 | Construir comprueba en el orden de RN-21 (ubicación, desbloqueo, límite, saldo); comprar un animal comprueba primero el saldo y luego la plaza | HU-38 · CA-3 pide `SIN_FONDOS` aunque no haya Establo |
| D-35 | Alimentar, recolectar y curar responden `BuildingActionView` (edificio, cantidad y un aviso opcional) | «No hay nada que alimentar», «animales enfermos omitidos» (HU-20 · CA-4) y «nada que recoger» son éxitos con aviso: RN-35 no tiene código para ellos |
| D-36 | `GameState` lleva un `DayLedger` con ingresos y gastos del día (cada `earn` y `spend`), que entra en el memento; `DayPipeline` lo copia al `DayReport` y lo reinicia | HU-17 · CA-4: los acumulados parten en 0 cada día; el saldo anterior es `saldo nuevo − ingresos + gastos` |
| D-37 | El resumen del día viaja como evento `DaySummaryReady` después de `DayEnded`; el diálogo no es modal | No cambia el cuerpo de `POST /api/game/days` y no bloquea el juego |
| D-38 | Un edificio se identifica por su orden de construcción (no se demuelen) y las rutas son `/api/buildings/{id}/…`; un id o un animal inexistente responde `500 ERROR_INTERNO` | Como D-22 y D-32: la UI nunca los envía y RN-35 no tiene código para ello |
| D-39 | Curar no es herramienta: se hace desde el panel del edificio. «Alimentar» y «Cosechar» actúan sobre el edificio bajo el clic, «Construir» abre el diálogo con la parcela pulsada como esquina y, sin herramienta, pulsar un edificio abre su panel | RN-27 y RN-34 |
| D-40 | El catálogo de construcción incluye los cuatro edificios; el efecto del Invernadero (HU-25) llega en 5.6 | HU-18 lista el catálogo completo; construirlo ya funciona y cuesta lo que dice RN-20 |
| D-41 | `MapView` lleva la lista de edificios (con sus animales) y `MarketView` la de animales en venta; los constructores anteriores se conservan | El mapa se dibuja de una sola consulta y la pestaña Comprar ofrece Gallina y Vaca |

**Criterios cubiertos en parte (se completan donde nace lo que falta):**

| Criterio | Qué falta | Fase |
|---|---|---|
| HU-17 · CA-2 | el resumen ya lista cultivos marchitos, animales enfermos y productos; falta la línea del logro desbloqueado | 5.5 |
| HU-28 · CA-1 | solo falta que los logros bloqueados empiecen con contadores en 0 | 5.5 |
| HU-10 · CA-3, HU-33 · CA-3 | rendimiento a plena carga y cargar una partida guardada | 5.5 |
| HU-36 · CA-5 | los atajos `L`, `F5` y `F9` aún responden que llegarán | 5.5 |

**Diagramas:** `03_vista_logica` y `04_vista_desarrollo` incluyen las clases y dependencias nuevas (`domain.game → animal/building`, `building → animal/economy/progress`, `animal → economy`); las seis vistas se volvieron a renderizar. `check_diagrams.py` y `check_imports.py` terminan sin errores.

---

## 16. Estado de la Fase 5.5 (progresión y persistencia)

**Historias cerradas (6 historias, 22 puntos):** HU-26, 27, 29, 30, 31 y 32, más los criterios que esperaban a los logros y al guardado (HU-17 · CA-2, HU-28 · CA-1, HU-10 · CA-3, HU-33 · CA-3, HU-36 · CA-5). `python tools/traceability.py --upto 5.5` termina sin errores.

| Resultado | Valor |
|---|---|
| Pruebas | 442 en verde y 2 omitidas con aviso (las de MongoDB real) |
| Cobertura de líneas (JaCoCo) | **dominio 97,2 %**, **aplicación 97,6 %**, infraestructura 94,2 %, UI 91,1 %, API 95,9 %, cliente 90,8 % (barrera: 70 % en dominio y aplicación) |
| Patrones aplicados | Memento completo: `GameMemento` con todas las partes (logros y contadores incluidos) y `SaveGameService` como *caretaker*; registrado en `patterns.txt` |
| Pruebas críticas cerradas | **6** `HU30_ca1_theLoadedStateIsStructurallyIdenticalToTheSavedOne_criticalTest` y **7** `HU31_ca1_…Inexistent…`, `HU31_ca2_…Corrupt…` (carácter cambiado y JSON truncado) |
| Rendimiento a plena carga | `FullFarmPerformanceTest`: terminar el día 100 veces (p95 ≤ 200 ms), guardar y cargar (≤ 1 s, documento ≤ 200 KB) |
| API nueva | `GET /api/achievements`, `GET /api/saves`, `PUT /api/saves/{slot}`, `POST /api/saves/{slot}/load` (ver [`API.md`](API.md)) |
| Verificadores | `check_hu.py`, `check_diagrams.py`, `check_imports.py` y `traceability.py` terminan sin errores |

**Decisiones tomadas al implementar:**

| # | Decisión | Motivo |
|---|---|---|
| D-42 | `SaveRepository` guarda y lee `GameMemento` (no texto): `SaveCodec` y `Checksum` viven en la infraestructura y `GameStateValidator` en el dominio; el servicio de guardado decide el código de error | DT-11 y DT-13: el dominio no conoce Jackson ni MongoDB y las invariantes de RN-33 son regla de negocio |
| D-43 | El orden de RN-33 se cumple así: existencia, JSON bien formado, versión, suma de control y datos (en `SaveCodec`) e invariantes (en el dominio). Un JSON mal formado gana a una versión o suma erróneas | HU-31 · CA-4 |
| D-44 | Cargar construye el estado aparte (`GameState.fromMemento`) después de validar y solo entonces reemplaza al de `GameSession`; si algo inesperado falla se restaura el anterior | RN-33: la partida actual no cambia en absoluto |
| D-45 | Los logros se evalúan en `AchievementService` tras cada acción exitosa (cosechar, vender, construir, comprar animal, curar) y en el paso 8 del día (`AchievementsStep`); una recompensa que cumple otra condición encadena la siguiente | RN-28: «después de cada acción exitosa y en el paso 8» |
| D-46 | Construir el primer edificio paga LG-06 (100 m): el saldo tras construir un Gallinero es 200 m, no 100 m | HU-26 · CA-5 manda sobre la cifra de HU-18 · CA-1; las pruebas de HU-18 comprueban los 400 m cobrados y la recompensa |
| D-47 | Los contadores (cosechas, ventas, edificios, curaciones) forman parte del memento y de la validación («logros coherentes con sus contadores») | RN-32 y RN-33 |
| D-48 | `F5` y `F9` guardan y cargan en la última ranura usada; si no hay ninguna abren el diálogo de ranuras. Sobrescribir una ranura ocupada pide confirmación en el diálogo (no en `F5`, que repite la ranura elegida) | HU-36 · CA-5 y HU-29 · CA-3 |
| D-49 | «Cargar» en la pantalla de inicio ofrece las tres ranuras en una lista; una ranura dañada o vacía se rechaza con el mensaje de RN-35 sin salir del inicio | HU-31 |
| D-50 | La prueba de plena carga usa 40 cultivos al aire libre, 6 edificios (el Invernadero incluido) y 16 animales; los 9 cultivos del invernadero llegan con HU-25 (5.6) | El efecto del Invernadero aún no existe |
| D-51 | Las pruebas de guardado usan el `MongoSaveRepository` real sobre un `MongoTemplate` en memoria (`InMemoryMongo`); la prueba con MongoDB de verdad sigue omitida si no hay uno | No hay MongoDB en esta máquina (RT-11) |

**Criterios cubiertos en parte (se completan donde nace lo que falta):**

| Criterio | Qué falta | Fase |
|---|---|---|
| HU-10 · CA-3, HU-29 · CA-4, HU-30 · CA-4 | los 9 cultivos del invernadero de la «plena carga» | 5.6 (HU-25) |
| HU-29 · CA-2 | la prueba contra un MongoDB real que rechaza o no responde (la simulada sí está) | cuando haya MongoDB |

**Diagramas:** `03_vista_logica` y `04_vista_desarrollo` incluyen `AchievementService`, `GameStateValidator`, `SaveCodec`, `Checksum`, `SlotInfo` y sus dependencias (`domain.game → crop`, `domain.save → time`); las seis vistas se volvieron a renderizar. `check_diagrams.py` y `check_imports.py` terminan sin errores.

## 17. Estado de la Fase 5.6 (interfaz visual y extras)

**Historias cerradas (5 historias, 19 puntos):** HU-25, 39, 40, 41 y 42, más los criterios que esperaban al invernadero (HU-10 · CA-3, HU-29 · CA-4 y HU-30 · CA-4 con los 9 cultivos protegidos). `python tools/traceability.py --upto 5.6` termina sin errores. Con esta fase **las 42 historias están implementadas**.

| Resultado | Valor |
|---|---|
| Pruebas | 486 en verde y 2 omitidas con aviso (las de MongoDB real) |
| Cobertura de líneas (JaCoCo) | **dominio 97,3 %**, **aplicación 98,4 %**, infraestructura 96,1 %, UI 91,5 %, API 96,6 %, cliente 94,0 % (barrera: 70 % en dominio y aplicación) |
| Patrones aplicados | Observer completado: `ToastPresenter` (`ConcreteObserver`, registrado en `patterns.txt`) |
| Rendimiento | un cuadro con partículas activas tarda menos de 33 ms (≥ 30 FPS, `HU40_ca4`); la plena carga ahora incluye los 9 cultivos del invernadero |
| API | `PlotView` añade `sheltered` (ver [`API.md`](API.md) v0.5); no hay rutas nuevas |
| Verificadores | `check_hu.py`, `check_diagrams.py`, `check_imports.py` y `traceability.py` terminan sin errores |

**Decisiones tomadas al implementar:**

| # | Decisión | Motivo |
|---|---|---|
| D-52 | Una parcela «protegida» se deduce del edificio que la cubre (`Plot.isSheltered()`); no se guarda nada nuevo en el memento y el validador acepta cultivos sobre parcelas de un invernadero | RN-22 y RN-32: el estado ya contiene el edificio |
| D-53 | Dentro del invernadero la tolerancia sin riego es siempre 3 días y regar cuesta 1 de energía aunque el clima sea Sequía; la lluvia no riega, la helada y el cambio de estación no marchitan | RN-22 · «inmunes a Helada y Sequía» y HU-25 · CA-3 y CA-4 |
| D-54 | Los avisos son un Observer: `ToastPresenter` escucha el bus (logros y cambio de estación) y recibe de `GameScreenPresenter` los rechazos y avisos propios; `ToastQueue` (sin Swing, con reloj inyectado) guarda 4 visibles y el resto en cola; `ToastLayer` solo dibuja y no captura el ratón. La barra de estado se mantiene | HU-39; la barra sigue siendo el rastro del último mensaje |
| D-55 | Un aviso dura 3 s (5 s el de logro) a opacidad completa y luego se desvanece 0,4 s; el tiempo cuenta desde que se hace visible, no desde que se publicó | HU-39 · CA-1 y CA-3 |
| D-56 | `VisualEffects` (paleta de estación, lluvia, nieve, tinte de noche, cuadros de animales, crecimiento de cultivos) es un modelo sin Swing que solo recibe lo que la vista ya sabe y **nunca llama al juego**; `MapPanel` lo mueve con un `javax.swing.Timer` que solo corre mientras algo se mueve | HU-40 · CA-3 y HU-41 · CA-3 y CA-4 |
| D-57 | La prueba `HU35_ca5` («no usa un *timer* para sondear») pasó a comprobar lo que de verdad importa: el panel nunca pide el mapa por su cuenta y su temporizador está detenido si nada se mueve | El temporizador de HU-41 es de dibujo, no de sondeo |
| D-58 | `AssetManager` lee `crops/<CULTIVO>_<ETAPA>.png` y `animals/<ANIMAL>_<CUADRO>.png` de `assets/`, guarda cada archivo una vez y, si falta, `MapRenderer` dibuja el vector. **No se incluye ningún sprite**; el primer faltante se registra como aviso y los demás en nivel FINE | HU-42; evita 30 avisos por cuadro en una instalación sin imágenes |
| D-59 | La casilla «Efectos» apaga a la vez los efectos y las animaciones; con ella apagada el mapa se dibuja con su aspecto base | HU-40 · CA-4 |
| D-60 | **FlatLaf no se adopta:** el aspecto por defecto de Swing con el mapa vectorial basta, y evita una dependencia más que verificar | Alcance y RNF-02 (arranque) |

**Criterios cubiertos en parte:**

| Criterio | Qué falta | Cuándo |
|---|---|---|
| HU-29 · CA-2 | la prueba contra un MongoDB real que rechaza o no responde (la simulada sí está) | cuando haya MongoDB |
| RNF-02 · RT-09 | el arranque medido del `.jar` (servidor listo) fue de **4,96 s** en esta máquina, justo bajo el límite de 5 s; repetir en una máquina limpia | demostración final |

**Diagramas:** `03_vista_logica` y `04_vista_desarrollo` incluyen `ToastPresenter`, `ToastQueue`, `ToastLayer`, `VisualEffects`, `Particle`, `SeasonPalette` y `AssetManager`; la secuencia anexa cambió `ToastLayer` por `ToastPresenter`. Las vistas se volvieron a renderizar.

## 18. Fase 5.7 (en curso): interfaz profesional, avatar y NPC

**Origen:** el estudiante pidió, tras cerrar la 5.6, FlatLaf, sprites en estilo granja, NPC y un avatar que camina. Esto **revierte** las decisiones de Fase 0 «sin NPC» (W-04) y «sin avatar» (W-05). Los sprites de Stardew Valley son de ConcernedApe y tienen copyright: no se usan; se dibujan sprites **propios** de 32 × 32 px.

**Etapa 5.7a (hecha, solo documentación):** RF-43 a RF-48, RN-36 a RN-38, CU-19 y CU-20, HU-43 a HU-48 (24 puntos), vista funcional con 48 RF y verificadores actualizados (`check_hu.py`, `check_diagrams.py`, `gen_test_plan.py`).

| Etapa | Historias | Contenido |
|---|---|---|
| 5.7b | HU-43 | `pom.xml` con FlatLaf (descarga de Maven Central), `UiTheme`, tipografía, contraste |
| 5.7c | HU-44, HU-48 | `tools/gen_sprites.py`, sprites de 32 × 32, escala entera nítida, iconos en barra, HUD e inventario |
| 5.7d | HU-45 | `Avatar` en `GameState` y en el memento (esquema 2, el 1 se sigue aceptando), `GameCommands.move`, `POST /api/avatar/moves`, teclas, animación |
| 5.7e | HU-46, HU-47 | `Npc`, `DialogueService`, `GET /api/npcs`, `POST /api/npcs/{id}/talk`, encargos, cuadro de diálogo |
| 5.7f | — | cierre: arranque ≤ 5 s, cobertura, diagramas, README |

**Códigos de error nuevos (RN-35):** `MOVIMIENTO_BLOQUEADO`, `NPC_LEJOS`, `ENCARGO_INCOMPLETO` (los tres `409`).

**Etapa 5.7b (hecha): tema FlatLaf (HU-43).** 491 pruebas en verde (2 omitidas); `UiTheme.install()` instala FlatLaf 3.6.1 con una sola tipografía de 14 pt y esquinas redondeadas, y si falla deja el aspecto por defecto con un aviso (`Main` lo llama antes de crear la ventana). `Contrast` calcula el contraste WCAG y una prueba exige ≥ 4,5 : 1 en los pares de texto y fondo de la `Palette` (avisos, HUD y diálogos). Los colores de los avisos pasaron de `ToastLayer` a `Palette`. Decisión **D-61:** FlatLaf reemplaza a la decisión D-60 (que lo descartaba) por pedido del estudiante.

**Etapa 5.7c (hecha): sprites de 32 × 32 e interfaz de granja (HU-44, HU-48).** 503 pruebas en verde (2 omitidas); cobertura de dominio y aplicación sobre la barrera. `tools/gen_sprites.py` dibuja 90 PNG originales (terrenos por estación, 25 etapas de cultivo, animales de 3 cuadros, 4 edificios, 14 ítems, avatar, 4 NPC e iconos) y `--check` comprueba que el repositorio no se desactualice. `MapGeometry` ajusta la parcela a un múltiplo de 32 cuando eso desperdicia a lo sumo un cuarto del espacio (64 px a 1024 × 640) y `MapRenderer` dibuja con vecino más cercano; la ventana abre en 1100 × 860 para que quepa ×2. Barra de herramientas, HUD e inventario muestran iconos (`AssetManager.icon`) y, sin sprites, solo texto. Decisiones: **D-62** el tamaño de parcela no es siempre un múltiplo entero (en ventanas pequeñas se usa la escala que cabe, sin suavizar); **D-63** los animales con sprite se dibujan delante del edificio con una insignia si están hambrientos o enfermos.

