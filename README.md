# Valle Verde

Simulador de granja 2D **por turnos** (un turno = un día), hecho en Java con Swing y Java2D. El jugador camina con su granjero por un valle con su granja y una aldea; siembra, riega y cosecha cultivos, cría animales, construye edificios, comercia en un mercado con precios dinámicos y cumple encargos de los vecinos, a lo largo de un ciclo de estaciones y clima cambiante, con logros y guardado en 3 ranuras de MongoDB.

> **Nombre provisional.** El nombre visible vive en una sola constante ([`InfoJuego.NOMBRE`](src/main/java/edu/ucc/farmsim/GameInfo.java)).
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
| **5.6** | Avisos (*toasts*), invernadero, efectos de estación y clima, animaciones y carga de sprites con respaldo vectorial (5 historias) | **hecha** |
| **5.7** | Pedido del estudiante: tema FlatLaf, sprites pixel-art propios, avatar que camina, cuatro vecinos con diálogos y encargos, tutorial de inicio, valle de 48 × 24 con granja y aldea y cámara que sigue al avatar, guardado en MongoDB real y arranque en paralelo (8 historias, HU-43 a HU-50) | **hecha** |

### Cómo se juega

- **Inicio:** «Nueva partida», «Cargar» (elige una de las 3 ranuras) o «Salir». En una partida nueva, el **tutorial** del granjero te guía en 5 pasos; «Saltar tutorial» lo cierra.
- **Caminar:** flechas o `W` `A` `S` `D`; con dos teclas perpendiculares camina en diagonal. La cámara sigue al granjero por el valle: la granja al oeste y la aldea al este, unidas por un camino.
- **Herramientas:** `1` Sembrar, `2` Regar, `3` Cosechar, `4` Construir, `5` Alimentar (o la barra de abajo). `Espacio` usa la herramienta en la parcela de enfrente; también se puede hacer clic en una parcela. `Esc` suelta la herramienta.
- **Vecinos:** acércate a Marta (mercado), Don Tomás (granjero veterano), la Dra. Lucía (veterinaria) o Inés (vecina) y pulsa `E` o haz clic sobre ellos: dan consejos y un encargo con recompensa.
- **Ventanas:** `I` inventario, `M` mercado, `L` logros; un clic sobre un edificio abre su panel (animales, curar, recolectar).
- **Días:** `Enter` o «Terminar día» avanza el día; aparece el resumen con ingresos y gastos.
- **Guardar y cargar:** botón «Guardar» (elige la ranura); `F5` guarda y `F9` carga rápido en la última ranura usada.
- **Efectos:** la casilla «Efectos» apaga la lluvia, la nieve, la noche y las animaciones sin cambiar el juego.

## Requisitos

- **MongoDB Community Server 8.0** para guardar y cargar partidas (colección `partidas` de la base `valleverde`). En Windows se instala como servicio que arranca solo con:

  ```powershell
  winget instalar MongoDB.Server --version 8.0.23
  ```

  Se usa la **8.0** y no la 9 porque el *driver* de Java del proyecto admite servidores hasta la 8.x. Por defecto se busca en `localhost:27017`; otra dirección se indica con la variable de entorno `VALLEVERDE_MONGO_URI`. Sin MongoDB el juego funciona igual, pero guardar y cargar responden «No se pudo acceder a las partidas guardadas».
- **JDK 17 o superior** (el proyecto compila con `--release 17`; se probó con JDK 25).
- Una pantalla (el juego es gráfico). Sin pantalla termina con un mensaje.
- No hace falta instalar Maven: el repositorio incluye el *Maven Wrapper*.

## Cómo compilar, ejecutar y probar

En PowerShell (Windows), desde la carpeta del proyecto:

```powershell
.\mvnw.cmd clean verify
```

Compila, ejecuta las 581 pruebas, mide la cobertura (JaCoCo exige ≥ 70 % en dominio y aplicación; hoy 97 % y 96 %) y genera `objetivo\farmsim.jar`. Las pruebas contra MongoDB real (`HU29MongoRealTest`, base `valleverde_test`) se omiten solas si no hay MongoDB, y las de ventanas, si no hay pantalla.

```powershell
java -jar objetivo\farmsim.jar
```

Con un solo comando arranca el **servidor** (Spring Boot, solo en 127.0.0.1, puerto libre) y la **ventana** que lo usa por HTTP. Los dos arrancan a la vez: la pantalla de inicio aparece en unos **2,2 s** y el servidor queda listo a los 3,8–4,9 s, antes de que haga falta (RNF-02 pide ≤ 5 s hasta la pantalla de inicio; medido en la Fase 5.7i, 5 arranques). También se pueden separar: `--server --port 8080` (solo servidor; la API está en [`docs/API.md`](docs/API.md)) y `--client --url=http://127.0.0.1:8080` (solo ventana).

| Objetivo | Comando |
|---|---|
| Solo las pruebas de una historia | `.\mvnw.cmd test "-Dtest=HU01*"` |
| Incluir pruebas de rendimiento | `.\mvnw.cmd verify -Pperf` |
| Informe de cobertura | abrir `objetivo\site\jacoco\indice.html` |
| Comprobar diagramas | `python tools/check_diagrams.py` |
| Comprobar historias de usuario | `python tools/check_hu.py` |
| Comprobar el código contra el diagrama de desarrollo | `python tools/check_imports.py` |
| Matriz de trazabilidad | `python tools/traceability.py` → `docs/TRAZABILIDAD.md` |
| Volver a generar los diagramas | `bash tools/render_diagrams.sh` (requiere Java, `tools/plantuml.jar` y Graphviz) |

## Arquitectura

Servidor Spring Boot (API REST) y cliente Swing por HTTP en el mismo equipo, con partidas en MongoDB. Capas con dependencias en un solo sentido: servidor `api → application → domain` e `infrastructure → domain`, cliente `ui → application` y `cliente → application`. El dominio no conoce Swing, Spring, Jackson, MongoDB ni E/S; la UI dibuja con DTO inmutables y se actualiza por eventos (Observer); el controlador siempre devuelve un `Resultado` y nunca lanza excepciones hacia la UI. Detalle en [`docs/ARQUITECTURA.md`](docs/ARQUITECTURA.md).

```
edu.ucc.farmsim
├─ Main, OpcionesLanzamiento, InfoJuego  raíz de composición y datos del juego
├─ interfaz            pantallas · componentes · dibujo · recursos · tema   (cliente)
├─ cliente        ClienteJuegoRemoto, DecodificadorEvento                   (cliente HTTP)
├─ api           ControladorRestJuego, RespuestaApi, RecolectorEvento… (servidor REST)
├─ configuracion        AplicacionServidor, ConfiguracionServidor                  (Spring)
├─ aplicacion   controlador · servicio · evento · dto · resultado
├─ dominio        juego · guardado · granja · npc · cultivo · edificio · animal · economia · tiempo · progreso · configuracion
└─ infraestructura persistencia
```

## Patrones de diseño

| Patrón | Clases | Historias | Estado |
|---|---|---|---|
| Singleton | `RelojJuego` | HU-09 | hecho (5.1) |
| State | `EstadoEstacion` con `EstadoPrimavera`, `EstadoVerano`, `EstadoOtonio`, `EstadoInvierno` | HU-09 | hecho (5.1) |
| State | `EstadoCultivo` con `EstadoSemilla`, `EstadoBrote`, `EstadoCreciendo`, `EstadoMaduro`, `EstadoMarchito` (contexto `Cultivo`) | HU-03, HU-05 | hecho (5.2) |
| State | `EstadoAnimal` con `EstadoSano`, `EstadoHambriento`, `EstadoEnfermo` (contexto `Animal`) | HU-21 | hecho (5.4) |
| Strategy | `EstrategiaClima` con `EstrategiaClimaEstacional` y `EstrategiaClimaFija` | HU-11 | hecho (5.1) |
| Strategy | `EstrategiaPrecios` con `PreciosCultivo` y `PreciosFijos` | HU-15 | hecho (5.3) |
| Observer | `BusEvento`, `OyenteJuego`, `ModeloHud`, `HudPanel` | HU-10, HU-34 | hecho (5.1) |
| Observer | `PanelMapa` | HU-35 | hecho (5.2) |
| Observer | `PresentadorAviso` (y `ColaAviso`/`CapaAviso` que dibujan lo que recibe) | HU-39 | hecho (5.6) |
| Memento | `EstadoJuego` (originador), `MementoJuego`, `ServicioJuegoGuardado` (*caretaker*) | HU-10, HU-29, HU-30 | hecho (5.5): guardado en MongoDB |
| Observer | `ModeloInventario` | HU-37 | hecho (5.3) |
| Observer | `RecolectorEvento` (el servidor guarda los eventos para la respuesta) | RNF-15 | hecho (5.2b) |
| Controller (GRASP) | `ControladorJuego` (único punto de entrada de la interfaz) | todas | hecho (5.1) |
| Factory Method | `FabricaCultivo` con `FabricaZanahoria`, `FabricaPapa`, `FabricaTomate`, `FabricaMaiz`, `FabricaCalabaza` | HU-01 | hecho (5.2) |
| Builder | `ConstructorEdificio` con `ConstructorGallinero`, `ConstructorEstablo`, `ConstructorAlmacen`, `ConstructorInvernadero` y `DirectorEdificio` | HU-18 | hecho (5.4) |

> Cada clase de patrón lleva el Javadoc `Pattern: <nombre> — Role: <rol>` y está registrada en `src/test/resources/patterns.txt` (una prueba lo verifica). La tabla se actualiza al cerrar cada fase.

## Documentación

| Documento | Contenido |
|---|---|
| [`docs/VISION_ALCANCE.md`](docs/VISION_ALCANCE.md) | visión, alcance, actores y riesgos |
| [`docs/REQUERIMIENTOS.md`](docs/REQUERIMIENTOS.md) | 50 requerimientos funcionales (MoSCoW) y 16 no funcionales |
| [`docs/REGLAS_DE_NEGOCIO.md`](docs/REGLAS_DE_NEGOCIO.md) | reglas con valores numéricos (RN-01 a RN-40) |
| [`docs/CASOS_DE_USO.md`](docs/CASOS_DE_USO.md) | 21 casos de uso en texto |
| [`docs/HISTORIAS_DE_USUARIO.md`](docs/HISTORIAS_DE_USUARIO.md) | 50 historias con criterios Dado/Cuando/Entonces y backlog por fases |
| [`docs/DIAGRAMAS.md`](docs/DIAGRAMAS.md) | las 5 vistas del PPTX del curso y una secuencia anexa |
| [`docs/API.md`](docs/API.md) | rutas REST, cuerpos, eventos y código HTTP de cada rechazo |
| [`docs/ASSETS.md`](docs/ASSETS.md) | recursos gráficos, nombres de sprites y licencias (todos propios, CC0) |
| [`docs/ARQUITECTURA.md`](docs/ARQUITECTURA.md) | capas, paquetes, decisiones técnicas y plan por fases |
| [`docs/PLAN_DE_PRUEBAS.md`](docs/PLAN_DE_PRUEBAS.md) | estrategia de pruebas y criterios de salida |
| [`docs/TRAZABILIDAD.md`](docs/TRAZABILIDAD.md) | RF → HU → componente → clases → pruebas (generado) |

## Assets gráficos

Los 122 sprites (32 × 32 px; personajes de 32 × 64, árboles de 64 × 64 y casas del valle de hasta 160 × 128; estilo pixel-art de granja) son **trabajo propio**, generados por `tools/gen_sprites.py`, y constan en [`docs/ASSETS.md`](docs/ASSETS.md); no hay ningún sprite de otro juego. Si falta alguno, el `GestorRecurso` lo avisa y el mapa dibuja el gráfico vectorial de respaldo. No se usan sprites ni nombres de juegos comerciales.

## Convenciones

Clases, métodos, variables y paquetes, comentarios, documentación y textos visibles al jugador en español. Siguen en inglés los valores de las enumeraciones (identificadores de datos como `CARROT_SEED`, que también dan nombre a los sprites), las rutas de la API (`/api/...`), los nombres de los métodos de prueba y los commits. Las líneas `Stories:` y `Pattern:` del Javadoc se dejan tal cual: las leen las pruebas y los scripts. Commits: `feat(area): description (HUxx)`.
