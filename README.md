# Valle Verde

Simulador de granja 2D **por turnos** (un turno = un día), hecho en Java con Swing y Java2D. El jugador siembra, riega y cosecha cultivos, cría animales, construye edificios y comercia en un mercado con precios dinámicos, a lo largo de un ciclo de estaciones y clima cambiante, con logros y guardado en 3 ranuras.

> **Nombre provisional.** El nombre visible vive en una sola constante ([`GameInfo.NAME`](src/main/java/edu/ucc/farmsim/GameInfo.java)).
> Proyecto final de *Diseño de Software* — Ingeniería de Software, UCC Pasto. El valor del proyecto está en la calidad del diseño y en la coherencia entre documentos, diagramas y código.

## Estado

| Fase | Contenido | Estado |
|---|---|---|
| 0 a 4 | Lectura de insumos, requerimientos, historias de usuario, diagramas y arquitectura | hechas (ver [`docs/`](docs/)) |
| **5.0** | Preparación técnica: `pom.xml`, esqueleto de paquetes, pruebas de arquitectura, cobertura | **hecha** |
| 5.1 a 5.6 | Implementación incremental, historia por historia | pendientes |

Hoy el juego solo abre una **ventana vacía**: las historias se implementan desde la fase 5.1.

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

| Patrón | Clases | Historias | Fase |
|---|---|---|---|
| Singleton | `GameClock` | HU-09 | 5.1 |
| State | `SeasonState`, `CropState`, `AnimalState` (y sus estados concretos) | HU-09, HU-03, HU-05, HU-21 | 5.1, 5.2, 5.4 |
| Strategy | `WeatherStrategy`, `PricingStrategy` | HU-11, HU-15 | 5.1, 5.3 |
| Factory Method | `CropFactory` y sus creadores | HU-01 | 5.2 |
| Builder | `BuildingBuilder`, `BuildingDirector` | HU-18 | 5.4 |
| Memento | `GameState`, `GameMemento`, `SaveGameService` | HU-29, HU-30, HU-31 | 5.5 |
| Observer | `EventBus`, `GameListener`, paneles de la UI | HU-34, HU-35, HU-39 | 5.1, 5.6 |

> Las clases se irán creando fase por fase; cada una llevará el Javadoc `Pattern: <nombre> — Role: <rol>` y se registrará en `src/test/resources/patterns.txt`. Esta tabla se completa con los nombres reales al cerrar cada fase.

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
