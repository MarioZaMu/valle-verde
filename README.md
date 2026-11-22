# Valle Verde

Simulador de granja 2D **por turnos** (un turno = un día), hecho en Java con Swing y Java2D. El jugador siembra, riega y cosecha cultivos, cría animales, construye edificios y comercia en un mercado con precios dinámicos, a lo largo de un ciclo de estaciones y clima cambiante, con logros y guardado en 3 ranuras.

> **Nombre provisional.** El nombre visible vive en una sola constante ([`GameInfo.NAME`](src/main/java/edu/ucc/farmsim/GameInfo.java)).
> Proyecto final de *Diseño de Software* — Ingeniería de Software, UCC Pasto. El valor del proyecto está en la calidad del diseño y en la coherencia entre documentos, diagramas y código.

## Estado

| Fase | Contenido | Estado |
|---|---|---|
| 0 a 4 | Lectura de insumos, requerimientos, historias de usuario, diagramas y arquitectura | hechas (ver [`docs/`](docs/)) |
| **5.0** | Preparación técnica: `pom.xml`, esqueleto de paquetes, pruebas de arquitectura, cobertura | **hecha** |
| **5.1** | Núcleo del turno: inventario, calendario, energía, clima, nueva partida, terminar día, pantalla de inicio y HUD (8 historias) | **hecha** |
| **5.2** | Cultivo: sembrar, regar, crecer, cosechar, marchitar y limpiar, con el mapa Java2D, herramientas y atajos (9 historias) | **hecha** |
| **5.2b** | Migración a Spring Boot: servidor con API REST, cliente Swing por HTTP y partidas en MongoDB | **hecha** |
| **5.3** | Clima y economía: efectos del clima, mercado con precios dinámicos, tendencia, inventario en grilla y mercado modal (6 historias) | **hecha** |
| **5.4** | Construcción y animales: construir, comprar y alimentar animales, estados, producción, curación, resumen del día y Almacén (8 historias) | **hecha** |
| **5.5** | Progresión y persistencia: logros con recompensa, edificios desbloqueados, guardar, cargar y listar ranuras en MongoDB, rechazo de partidas dañadas y pruebas de rendimiento (6 historias) | **hecha** |
| 5.6 | Pulido visual, notificaciones, invernadero y extras | pendiente |

Hoy el juego abre la **pantalla de inicio** (Nueva partida, Cargar, Salir) y una pantalla de juego con la barra superior (dinero, fecha, clima, pronóstico y energía) y el botón «Terminar día». Desde la fase 5.2 también tiene el **mapa de la granja**: eliges una herramienta (teclas `1` a `5` o la barra), eliges la semilla y haces clic en una parcela de tierra para sembrar, regar y cosechar; `Enter` termina el día. Desde la 5.3 el clima importa (la lluvia riega, la helada marchita), `I` abre el **inventario** en grilla (clic en una semilla para elegirla) y `M` el **mercado** con las pestañas Comprar y Vender, precios que cambian con la temporada y la saturación, y la tendencia ▲ ▼ ▬. Desde la 5.4 puedes **construir** (herramienta `4`: clic en una parcela de hierba libre y eliges el edificio), comprar gallinas y vacas en el mercado, **alimentar** (herramienta `5` o el panel del edificio), **recolectar** (herramienta `3` sobre el edificio) y curar a los animales enfermos desde el panel que se abre al pulsar un edificio; al terminar el día aparece el **resumen del día**. Desde la 5.5 `L` abre los **logros** (con su recompensa y tu avance), `F5` guarda y `F9` carga en la última ranura usada (si no hay ninguna se abre el diálogo de **ranuras**), y «Cargar» en la pantalla de inicio te deja elegir entre tres ranuras. Las partidas se guardan en MongoDB (`valleverde.mongo.uri`); si no responde, el juego sigue y avisa.

## Requisitos

- **MongoDB** (opcional): solo guarda y carga partidas. Sin él, el juego funciona y guardar/cargar responden «No se pudo acceder a las partidas guardadas». Por defecto se busca en `localhost:27017`; otra dirección se indica con la variable de entorno `VALLEVERDE_MONGO_URI`.
- **JDK 17 o superior** (el proyecto compila con `--release 17`; se probó con JDK 25).
- Una pantalla (el juego es gráfico). Sin pantalla termina con un mensaje.
- No hace falta instalar Maven: el repositorio incluye el *Maven Wrapper*.

## Cómo compilar, ejecutar y probar

En PowerShell (Windows), desde la carpeta del proyecto:

```powershell
.\mvnw.cmd clean verify
```

Compila, ejecuta las pruebas, mide la cobertura (JaCoCo exige ≥ 70 % en dominio y aplicación) y genera `target\farmsim.jar`.

```powershell
java -jar target\farmsim.jar
```

Con un solo comando arranca el **servidor** (Spring Boot, solo en 127.0.0.1, puerto libre) y la **ventana** que lo usa por HTTP. También se pueden separar: `--server --port 8080` (solo servidor; la API está en [`docs/API.md`](docs/API.md)) y `--client --url=http://127.0.0.1:8080` (solo ventana).

| Objetivo | Comando |
|---|---|
| Solo las pruebas de una historia | `.\mvnw.cmd test "-Dtest=HU01*"` |
| Incluir pruebas de rendimiento | `.\mvnw.cmd verify -Pperf` |
| Informe de cobertura | abrir `target\site\jacoco\index.html` |
| Comprobar diagramas | `python tools/check_diagrams.py` |
| Comprobar historias de usuario | `python tools/check_hu.py` |
| Comprobar el código contra el diagrama de desarrollo | `python tools/check_imports.py` |
| Matriz de trazabilidad | `python tools/traceability.py` → `docs/TRAZABILIDAD.md` |
| Volver a generar los diagramas | `bash tools/render_diagrams.sh` (requiere Java, `tools/plantuml.jar` y Graphviz) |

## Arquitectura

Servidor Spring Boot (API REST) y cliente Swing por HTTP en el mismo equipo, con partidas en MongoDB. Capas con dependencias en un solo sentido: servidor `api → application → domain` e `infrastructure → domain`, cliente `ui → application` y `client → application`. El dominio no conoce Swing, Spring, Jackson, MongoDB ni E/S; la UI dibuja con DTO inmutables y se actualiza por eventos (Observer); el controlador siempre devuelve un `Result` y nunca lanza excepciones hacia la UI. Detalle en [`docs/ARQUITECTURA.md`](docs/ARQUITECTURA.md).

```
edu.ucc.farmsim
├─ Main, LaunchOptions, GameInfo  raíz de composición y datos del juego
├─ ui            screens · components · render · assets · theme   (cliente)
├─ client        RemoteGameClient, EventDecoder                   (cliente HTTP)
├─ api           GameRestController, ApiResponse, EventCollector… (servidor REST)
├─ config        ServerApplication, ServerConfig                  (Spring)
├─ application   controller · service · event · dto · result
├─ domain        game · save · farm · crop · building · animal · economy · time · progress · config
└─ infrastructure persistence
```

## Patrones de diseño

| Patrón | Clases | Historias | Estado |
|---|---|---|---|
| Singleton | `GameClock` | HU-09 | hecho (5.1) |
| State | `SeasonState` con `SpringState`, `SummerState`, `AutumnState`, `WinterState` | HU-09 | hecho (5.1) |
| State | `CropState` con `SeedState`, `SproutState`, `GrowingState`, `MatureState`, `WitheredState` (contexto `Crop`) | HU-03, HU-05 | hecho (5.2) |
| State | `AnimalState` con `HealthyState`, `HungryState`, `SickState` (contexto `Animal`) | HU-21 | hecho (5.4) |
| Strategy | `WeatherStrategy` con `SeasonalWeatherStrategy` y `FixedWeatherStrategy` | HU-11 | hecho (5.1) |
| Strategy | `PricingStrategy` con `CropPricing` y `FlatPricing` | HU-15 | hecho (5.3) |
| Observer | `EventBus`, `GameListener`, `HudModel`, `HudPanel` | HU-10, HU-34 | hecho (5.1) |
| Observer | `MapPanel` | HU-35 | hecho (5.2) |
| Observer | `ToastLayer` | HU-39 | 5.6 |
| Memento | `GameState` (originador), `GameMemento`, `SaveGameService` (*caretaker*) | HU-10, HU-29, HU-30 | hecho (5.5): guardado en MongoDB |
| Observer | `InventoryModel` | HU-37 | hecho (5.3) |
| Observer | `EventCollector` (el servidor guarda los eventos para la respuesta) | RNF-15 | hecho (5.2b) |
| Factory Method | `CropFactory` con `CarrotFactory`, `PotatoFactory`, `TomatoFactory`, `CornFactory`, `PumpkinFactory` | HU-01 | hecho (5.2) |
| Builder | `BuildingBuilder` con `CoopBuilder`, `BarnBuilder`, `WarehouseBuilder`, `GreenhouseBuilder` y `BuildingDirector` | HU-18 | hecho (5.4) |

> Cada clase de patrón lleva el Javadoc `Pattern: <nombre> — Role: <rol>` y está registrada en `src/test/resources/patterns.txt` (una prueba lo verifica). La tabla se actualiza al cerrar cada fase.

## Documentación

| Documento | Contenido |
|---|---|
| [`docs/VISION_ALCANCE.md`](docs/VISION_ALCANCE.md) | visión, alcance, actores y riesgos |
| [`docs/REQUERIMIENTOS.md`](docs/REQUERIMIENTOS.md) | 42 requerimientos funcionales (MoSCoW) y 16 no funcionales |
| [`docs/REGLAS_DE_NEGOCIO.md`](docs/REGLAS_DE_NEGOCIO.md) | reglas con valores numéricos (RN-01 a RN-35) |
| [`docs/CASOS_DE_USO.md`](docs/CASOS_DE_USO.md) | 18 casos de uso en texto |
| [`docs/HISTORIAS_DE_USUARIO.md`](docs/HISTORIAS_DE_USUARIO.md) | 42 historias con criterios Dado/Cuando/Entonces y backlog por fases |
| [`docs/DIAGRAMAS.md`](docs/DIAGRAMAS.md) | las 5 vistas del PPTX del curso y una secuencia anexa |
| [`docs/API.md`](docs/API.md) | rutas REST, cuerpos, eventos y código HTTP de cada rechazo |
| [`docs/ARQUITECTURA.md`](docs/ARQUITECTURA.md) | capas, paquetes, decisiones técnicas y plan por fases |
| [`docs/PLAN_DE_PRUEBAS.md`](docs/PLAN_DE_PRUEBAS.md) | estrategia de pruebas y criterios de salida |
| [`docs/TRAZABILIDAD.md`](docs/TRAZABILIDAD.md) | RF → HU → componente → clases → pruebas (generado) |

## Assets gráficos

Todavía no hay imágenes: el juego se dibujará con gráficos vectoriales y, cuando se agreguen sprites, serán únicamente de licencia **CC0 o CC-BY** y se registrarán en `docs/ASSETS.md` y en esta sección (Fase 5.6). No se usan sprites ni nombres de juegos comerciales.

## Convenciones

Código, comentarios, nombres de pruebas y commits en inglés; documentación y textos visibles al jugador en español. Commits: `feat(area): description (HUxx)`.
