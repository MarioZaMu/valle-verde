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
| 5.3 a 5.6 | Economía, animales y edificios, guardado y pulido visual | pendientes |

Hoy el juego abre la **pantalla de inicio** (Nueva partida, Cargar, Salir) y una pantalla de juego con la barra superior (dinero, fecha, clima, pronóstico y energía) y el botón «Terminar día». Desde la fase 5.2 también tiene el **mapa de la granja**: eliges una herramienta (teclas `1` a `5` o la barra), eliges la semilla y haces clic en una parcela de tierra para sembrar, regar y cosechar; `Enter` termina el día. El mercado, los animales y el guardado llegan en las fases siguientes.

## Requisitos

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

Capas con dependencias en un solo sentido: `ui → application → domain` e `infrastructure → domain`. El dominio no conoce Swing, Gson ni archivos; la UI dibuja con DTO inmutables y se actualiza por eventos (Observer); el controlador siempre devuelve un `Result` y nunca lanza excepciones hacia la UI. Detalle en [`docs/ARQUITECTURA.md`](docs/ARQUITECTURA.md).

```
edu.ucc.farmsim
├─ Main, GameInfo                 raíz de composición y datos del juego
├─ ui            screens · components · render · assets · theme
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
| State | `AnimalState` y sus estados concretos | HU-21 | 5.4 |
| Strategy | `WeatherStrategy` con `SeasonalWeatherStrategy` y `FixedWeatherStrategy` | HU-11 | hecho (5.1) |
| Strategy | `PricingStrategy` | HU-15 | 5.3 |
| Observer | `EventBus`, `GameListener`, `HudModel`, `HudPanel` | HU-10, HU-34 | hecho (5.1) |
| Observer | `MapPanel` | HU-35 | hecho (5.2) |
| Observer | `ToastLayer` | HU-39 | 5.6 |
| Memento | `GameState` (originador), `GameMemento` | HU-10 | en memoria (5.1); guardado en archivo en 5.5 |
| Factory Method | `CropFactory` con `CarrotFactory`, `PotatoFactory`, `TomatoFactory`, `CornFactory`, `PumpkinFactory` | HU-01 | hecho (5.2) |
| Builder | `BuildingBuilder`, `BuildingDirector` | HU-18 | 5.4 |

> Cada clase de patrón lleva el Javadoc `Pattern: <nombre> — Role: <rol>` y está registrada en `src/test/resources/patterns.txt` (una prueba lo verifica). La tabla se actualiza al cerrar cada fase.

## Documentación

| Documento | Contenido |
|---|---|
| [`docs/VISION_ALCANCE.md`](docs/VISION_ALCANCE.md) | visión, alcance, actores y riesgos |
| [`docs/REQUERIMIENTOS.md`](docs/REQUERIMIENTOS.md) | 42 requerimientos funcionales (MoSCoW) y 14 no funcionales |
| [`docs/REGLAS_DE_NEGOCIO.md`](docs/REGLAS_DE_NEGOCIO.md) | reglas con valores numéricos (RN-01 a RN-35) |
| [`docs/CASOS_DE_USO.md`](docs/CASOS_DE_USO.md) | 18 casos de uso en texto |
| [`docs/HISTORIAS_DE_USUARIO.md`](docs/HISTORIAS_DE_USUARIO.md) | 42 historias con criterios Dado/Cuando/Entonces y backlog por fases |
| [`docs/DIAGRAMAS.md`](docs/DIAGRAMAS.md) | las 5 vistas del PPTX del curso y una secuencia anexa |
| [`docs/ARQUITECTURA.md`](docs/ARQUITECTURA.md) | capas, paquetes, decisiones técnicas y plan por fases |
| [`docs/PLAN_DE_PRUEBAS.md`](docs/PLAN_DE_PRUEBAS.md) | estrategia de pruebas y criterios de salida |
| [`docs/TRAZABILIDAD.md`](docs/TRAZABILIDAD.md) | RF → HU → componente → clases → pruebas (generado) |

## Assets gráficos

Todavía no hay imágenes: el juego se dibujará con gráficos vectoriales y, cuando se agreguen sprites, serán únicamente de licencia **CC0 o CC-BY** y se registrarán en `docs/ASSETS.md` y en esta sección (Fase 5.6). No se usan sprites ni nombres de juegos comerciales.

## Convenciones

Código, comentarios, nombres de pruebas y commits en inglés; documentación y textos visibles al jugador en español. Commits: `feat(area): description (HUxx)`.
