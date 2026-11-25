# Arquitectura y plan técnico — Valle Verde

> **Versión 0.2 · 2026-11-22 · Fase 5.2b (migración a Spring Boot)**
> Formaliza el diseño propuesto en los diagramas ([`DIAGRAMAS.md`](DIAGRAMAS.md)): capas, árbol de paquetes, decisiones técnicas y plan de implementación por fases con sus historias ([`HISTORIAS_DE_USUARIO.md`](HISTORIAS_DE_USUARIO.md) §7). El plan de pruebas está en [`PLAN_DE_PRUEBAS.md`](PLAN_DE_PRUEBAS.md).
> **Estado:** las Fases 5.0 (§10), 5.1 (§11), 5.2 (§12) y 5.2b (§13, migración a servidor Spring Boot con API REST, cliente Swing por HTTP y persistencia en MongoDB) 5.3 (§14), 5.4 (§15) y 5.5 (§16) están hechas. La Fase 5.6 empieza solo con tu visto bueno.

## 1. Estilo arquitectónico

**Cliente-servidor por capas, con inversión de dependencias en la persistencia**, desplegado en un solo equipo: un **servidor Spring Boot** (API REST JSON en 127.0.0.1, lógica del juego y acceso a MongoDB) y un **cliente Swing/Java2D** que lo usa por HTTP. Un solo `java -jar` arranca ambos. La lógica corre en un solo hilo del servidor (una partida por servidor, S-01).

Las capas del servidor tienen los nombres del curso (*Controller → Service → Repository*) y el modelo de dominio va aparte, como «no es una cuarta capa» (PDF Spring p. 21). Hay **dos controladores con papeles distintos**, como separa el taller del bloque 7: el `ControladorRestJuego` (`@RestController`, capa `api`) solo traduce HTTP, y `ControladorJuego` (patrón GRASP *Controller*, capa `application`) orquesta los casos de uso.

```
 CLIENTE (Swing)                         SERVIDOR (Spring Boot, 127.0.0.1)
 ui ──► ComandosJuego ◄── cliente ═HTTP/JSON═► api ──► application ──► domain ◄── infrastructure ──► MongoDB
 (Swing/Java2D)   (ClienteJuegoRemoto)  (@RestController)  (Controller,     (reglas,   (RepositorioGuardadoMongo)
                                                          Services,       estado,
                                                          DTO, BusEvento)  patrones)
```

| Regla | Descripción | Cómo se verifica |
|---|---|---|
| **AR-1** | Servidor: `api → application → domain` e `infrastructure → domain`. Cliente: `ui → application` y `cliente → application`. Ninguna otra dirección. `Main` y `configuracion` son la raíz de composición y los únicos que pueden ver todas las capas | prueba ArchUnit de capas |
| **AR-2** | El dominio no importa Swing, AWT, `org.springframework`, `com.fasterxml.jackson`, `com.mongodb` ni E/S (`java.io`, `java.nio.file`, `java.net`) | prueba ArchUnit |
| **AR-3** | La UI no importa `domain`, `infrastructure`, `api` ni `cliente`: da órdenes por la interfaz `ComandosJuego` y dibuja con DTO inmutables de `application` | prueba ArchUnit |
| **AR-4** | `ControladorJuego` solo orquesta y devuelve un `Resultado`; nunca lanza excepciones hacia la UI ni hacia la API y nunca llama a un repositorio | prueba ArchUnit + prueba de inyección de fallas |
| **AR-5** | Sin ciclos entre paquetes de `domain` | prueba ArchUnit (`slices`) y `tools/check_diagrams.py` |
| **AR-6** | La UI se actualiza por eventos (Observer), no consultando en bucle | revisión y prueba de `HudPanel` con eventos simulados |
| **AR-7** | Toda aleatoriedad recibe un `Random` inyectado | prueba: ningún `new Random()` ni `Math.random()` fuera de `Main` y `configuracion` |
| **AR-8** | Sin números mágicos: las RN viven en `ReglasJuego` y en las tablas de catálogo | revisión en cada historia (DoD 4) |
| **AR-9** | Cada clase de patrón lleva el Javadoc `Pattern: <nombre> — Role: <rol>` | `JavadocPatronTest` con un registro (`src/test/resources/patterns.txt`) que cada fase completa |
| **AR-10** | Toda clase de producción declara en su Javadoc la línea `Stories: HU-xx, …` (o `Stories: ninguno`) | `tools/traceability.py` (alimenta la matriz de trazabilidad) |
| **AR-11** | Los controladores REST (`api`) no importan `domain` ni `infrastructure`: solo traducen HTTP ↔ `ControladorJuego`. El cliente (`cliente`) no importa `api` | prueba ArchUnit |
| **AR-12** | Cada orden del cliente tiene su endpoint documentado en [`API.md`](API.md) y una prueba de integración con el código HTTP de éxito y el de cada rechazo | DoD 11 y pruebas de `api` y `cliente` |

## 2. Responsabilidades por capa

| Capa | Paquete | Responsabilidad | Contiene | No debe |
|---|---|---|---|---|
| Presentación (cliente) | `ui` | Dibujar y capturar entrada del jugador | pantallas, paneles, `PanelMapa` (Java2D), `GestorRecurso`, tema | contener reglas de negocio, importar `domain` ni saber si hay HTTP |
| Cliente HTTP | `cliente` | Implementar `ComandosJuego` enviando cada orden a la API y republicar los eventos recibidos | `ClienteJuegoRemoto`, `DecodificadorEvento` | contener reglas de negocio, importar `domain` o `api` |
| API REST | `api` | Traducir HTTP ↔ `ControladorJuego`: rutas, JSON, códigos de estado, eventos de la respuesta | `ControladorRestJuego`, `RespuestaApi`, `MapeadorEstadoError`, `RecolectorEvento`, manejo global de errores | contener reglas de negocio, importar `domain` o `infrastructure` |
| Aplicación | `application` | Casos de uso: coordinar, validar entradas y traducir | `ComandosJuego`, `ControladorJuego`, servicios, `SesionJuego`, `BusEvento`, DTO, `Resultado` | dibujar, hablar HTTP, tocar MongoDB |
| Dominio | `domain` | Reglas y estado del juego (RN-01 a RN-35) | agregado `EstadoJuego`, cultivos, animales, edificios, mercado, tiempo, logros, patrones | depender de otra capa o de bibliotecas de UI, Spring, JSON, MongoDB y E/S |
| Infraestructura | `infrastructure` | Detalles técnicos: guardar y leer partidas en MongoDB | `RepositorioGuardadoMongo`, `DocumentoGuardado`, `CodecGuardado`, `Checksum` | contener reglas de negocio |
| Configuración | `configuracion` | Arrancar Spring Boot y declarar los beans que no son componentes (`BusEvento`, `SecuenciaDia`); los tiempos de espera de MongoDB van en la URI de `application.properties` | `AplicacionServidor`, `ConfiguracionServidor` | contener reglas de negocio |

## 3. Árbol de paquetes

Paquete raíz `edu.ucc.farmsim`. Las clases entre paréntesis se muestran en los diagramas como un subconjunto representativo ([`04_vista_desarrollo`](diagramas/04_vista_desarrollo.png)).

```
edu.ucc.farmsim
├─ Main, OpcionesLanzamiento                  raíz de composición: arranca el servidor Spring Boot y/o la ventana del cliente (`--server`, `--client`)
├─ InfoJuego                             nombre visible y versión del juego (única constante, RNF-12)
├─ interfaz
│  ├─ pantallas      VentanaPrincipal, PantallaInicio, PantallaJuego, EnrutadorPantalla
│  ├─ componentes   HudPanel, BarraHerramienta, GrillaInventario, DialogoMercado, DialogoRanuras, DialogoLogros, PresentadorAviso, ColaAviso, CapaAviso
│  ├─ dibujo       PanelMapa, DibujanteMapa, EfectosVisuales, Particula, PaletaEstacion
│  ├─ recursos       GestorRecurso (el respaldo vectorial vive en `DibujanteMapa`)
│  └─ tema        Paleta, Textos (cadenas de interfaz en español)
├─ cliente                               (cliente HTTP)
│     ClienteJuegoRemoto (implementa ComandosJuego), DecodificadorEvento
├─ api                                  (servidor: capa REST)
│     ControladorRestJuego, RespuestaApi, DtoEvento, MapeadorEstadoError, RecolectorEvento, ManejadorExcepcionApi
├─ configuracion                               AplicacionServidor, ConfiguracionServidor (Spring)
├─ aplicacion
│  ├─ controlador   ComandosJuego (puerto), ControladorJuego
│  ├─ servicio      SesionJuego, ServicioGranja, ServicioDia, ServicioMercado, ServicioEdificio,
│  │               ServicioAnimal, ServicioProgreso, ServicioJuegoGuardado
│  ├─ evento        BusEvento, OyenteJuego, EventoJuego (interfaz sellada + records)
│  ├─ dto          GameView, VistaHud, VistaParcela, VistaInventario, VistaMercado, VistaEdificio,
│  │               VistaLogro, VistaRanura, DaySummary   (records inmutables)
│  └─ resultado       Resultado<T>, CodigoError, Mensajes (plantillas de RN-35)
├─ dominio
│  ├─ juego         EstadoJuego, MementoJuego, ValidadorEstadoJuego, SecuenciaDia, PasoDia + pasos del día
│  ├─ guardado         RepositorioGuardado (puerto), SaveSlotInfo
│  ├─ granja         MapaGranja, Parcela, Terreno
│  ├─ cultivo         Cultivo, TipoCultivo, EstadoCultivo (+5 estados), FabricaCultivo (+5 creadores)
│  ├─ edificio     Edificio, TipoEdificio, ConstructorEdificio (+4 constructores), DirectorEdificio
│  ├─ animal       Animal, TipoAnimal, EstadoAnimal (+3 estados)
│  ├─ economia      Inventario, TipoArticulo, Mercado, EstrategiaPrecios, PreciosCultivo, PreciosFijos, Tendencia
│  ├─ tiempo         RelojJuego, FechaJuego, Estacion, EstadoEstacion (+4), Clima, EstrategiaClima,
│  │               EstrategiaClimaEstacional, EstrategiaClimaFija, AzarEstado
│  ├─ progreso     SeguidorLogro, Logro, HechosProgreso
│  └─ configuracion       ReglasJuego (constantes de las RN)
└─ infraestructura
   └─ persistencia  RepositorioGuardadoMongo, DocumentoGuardado, CodecGuardado, Checksum
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
| **DT-01** | **Frontera UI ↔ aplicación con DTO y el puerto `ComandosJuego`.** La UI da órdenes por la interfaz `ComandosJuego`, que recibe y devuelve tipos simples (`int`, `String` con ids como `"CARROT"`) y `record` inmutables (`GameView`, `VistaParcela`, …). Los ids de catálogo y los textos para mostrar salen de `application` | La UI no importa `domain` (AR-3); el dominio puede cambiar sin tocar la UI |
| **DT-02** | **`Resultado<T>` + `CodigoError`.** Cada operación del controlador devuelve `Resultado.ok(valor)` o `Resultado.fallar(código, mensaje)` con los textos de RN-35. El controlador envuelve cada llamada en `try/catch` y convierte cualquier excepción inesperada en `ERROR_INTERNO` | Cumple R-03 y RNF-07; ninguna excepción llega a la UI |
| **DT-03** | **Dominio mutable, con transacciones por memento.** `EstadoJuego` es un agregado mutable; antes de operaciones compuestas (terminar día, cargar) el servicio toma una instantánea (`mementoCrear()`) y, si algo falla, la restaura | Reutiliza el patrón Memento para cumplir HU-10 · CA-4 («falla interna: el estado queda idéntico») sin lógica de reversión aparte |
| **DT-04** | **Fin de día como *pipeline* de pasos.** `SecuenciaDia` ejecuta una lista **ordenada** de `PasoDia` (clima, crecimiento, animales, mercado, calendario, pronóstico, energía, logros) y produce un `ReporteDia`; cada historia agrega su propio paso | Materializa RN-03 y el principio de independencia de las historias (HU-10 define el *pipeline*; las demás se registran). El paso de calendario y los pasos de granja operan por métodos de agregado (`MapaGranja.diaAvanzar`, `Mercado.recoverDemand`), sin que `juego` importe `cultivo`, `animal` ni `edificio` directamente |
| **DT-05** | **Eventos síncronos.** `BusEvento` notifica a los observadores en el mismo hilo; los eventos se publican **después** de que la operación tuvo éxito. Un observador que falla no impide notificar a los demás. En el servidor los recoge un `RecolectorEvento` (DT-19); en el cliente notifican a la UI en el EDT | Un solo hilo de lógica (S-01): sin concurrencia, sin bloqueos y con pruebas deterministas |
| **DT-06** | **Un solo hilo de lógica.** En el servidor cada orden se ejecuta bajo un candado (DT-20), incluidos guardar y cargar (≤ 1 s, RNF-03). Las animaciones del cliente usan un `javax.swing.Timer` de la vista y no modifican el estado | Simplicidad; la animación nunca decide el estado (reglas de interfaz) |
| **DT-07** | **Aleatoriedad reproducible: `AzarEstado extends java.util.Random`.** Su estado completo es un `long` (algoritmo SplitMix64 que sobrescribe `next(bits)`), con `estado()` y `restaurar(long)`. Se inyecta desde `Main`; las pruebas usan semillas fijas | `java.util.Random` no permite leer su estado interno. Con `AzarEstado`, guardar y cargar continúa **exactamente** la misma secuencia (RN-32, HU-30 · CA-2), sin recontar sorteos |
| **DT-08** | **Configuración sin números mágicos.** Todos los valores de RN-01 a RN-35 están en `ReglasJuego` (constantes agrupadas); las tablas de catálogo (RN-05, RN-16, RN-20, RN-23, RN-28) viven en los enumerados `TipoCultivo`, `TipoArticulo`, `TipoEdificio`, `TipoAnimal` y `Logro`, que se consideran *tablas de configuración*. El nombre visible del juego está en **una** constante (RNF-12) | Una sola fuente de verdad, igual que en las reglas de negocio |
| **DT-09** | **Singleton `RelojJuego` con aislamiento para pruebas.** `getInstance()`, `restaurar(FechaJuego)` y `reiniciarParaTests()`. El `MementoJuego` guarda la fecha y `EstadoJuego.restaurar` la restablece | El patrón se pidió explícitamente. El riesgo (estado global) se contiene con el método de reinicio y con una regla de pruebas: toda prueba que use el reloj lo reinicia en `@BeforeEach` |
| **DT-10** | **`SesionJuego` como contenedor del estado actual.** Los servicios obtienen el `EstadoJuego` de `SesionJuego`; cargar construye un estado **aparte** y lo asigna con `replace` solo si todo es válido | Garantiza HU-31 («la partida actual no cambia»): el estado vigente no se toca hasta que el nuevo está completo y validado |
| **DT-11** | **Formato de guardado.** Documento `{_id, versionEsquema, archivadoEn, estado, checksum}` (RN-32). El `MementoJuego` contiene instantáneas inmutables (`record` sin dependencias de bibliotecas); `CodecGuardado` (infraestructura) las serializa a JSON con Jackson y `DocumentoGuardado` es el `@Document` de Spring Data MongoDB. El `checksum` es SHA-256 del texto de `estado` | El dominio no conoce Jackson ni MongoDB (AR-2); el versionado permite rechazar partidas de otra versión |
| **DT-12** | **Guardado atómico.** Reemplazar el documento de la ranura en una sola operación (`replaceOne` con *upsert*); MongoDB garantiza la atomicidad por documento | Si falla, el documento previo queda intacto (RN-31, HU-29 · CA-2) |
| **DT-13** | **Validación de carga en el dominio.** `ValidadorEstadoJuego` aplica las invariantes de RN-33 sin E/S; la secuencia de 5 verificaciones se reparte entre infraestructura (existencia, JSON, versión, checksum) y dominio (invariantes) | La regla de negocio vive en el dominio; la infraestructura solo lee el documento |
| **DT-14** | **Interfaz.** `EnrutadorPantalla` con `CardLayout` (inicio / juego); `PanelMapa` extiende `JPanel` y dibuja con Java2D en `paintComponent`; atajos con `InputMap/ActionMap`; textos en `Textos` (español). FlatLaf (Apache 2.0) es **opcional** y se decide en la Fase 5.6 | La UI funcional mínima se entrega desde la 5.1; lo visual es lo último y recortable |
| **DT-15** | **Registro de eventos técnicos** con `java.util.logging` en el cliente y el registro de Spring Boot (SLF4J) en el servidor. Los mensajes al jugador siempre salen de `Mensajes` | Cero dependencias nuevas: ya vienen con Spring Boot |
| **DT-16** | **Spring Boot como servidor.** `Main` arranca un `SpringApplication` con Tomcat embebido en `127.0.0.1` y un puerto libre (RNF-16). `@RestController` en `api`, `@Service` en los servicios de `application` y `@Repository` en `RepositorioGuardadoMongo`; los beans que no son componentes se declaran en `configuracion`. El dominio no lleva anotaciones de Spring | Spring Boot es obligatorio (R-01); el dominio sigue puro y se prueba sin Spring |
| **DT-17** | **Dos controladores, un puerto.** `ControladorRestJuego` (HTTP) delega en `ControladorJuego` (GRASP). `ControladorJuego` y `ClienteJuegoRemoto` implementan la interfaz `ComandosJuego`; la UI depende solo de ella | Las 146 pruebas de aceptación siguen usando el controlador en proceso, sin servidor; la UI no sabe si hay red (AR-3) |
| **DT-18** | **API REST.** Un recurso por sustantivo y verbos HTTP; JSON; sin sesión de usuario (una partida por servidor). `MapeadorEstadoError` traduce cada `CodigoError` a un código HTTP (200/201 éxito; 400 entrada inválida; 404 no existe; 409 regla de negocio; 422 partida dañada; 500 y 503 fallas). El cuerpo de error es `{codigo, mensaje}` con los textos de RN-35. Detalle en [`API.md`](API.md) | Cumple RNF-15; el cliente reconstruye el mismo `Resultado` que devuelve el controlador |
| **DT-19** | **Eventos en el sobre de la respuesta.** `RecolectorEvento` (un `OyenteJuego` del servidor) acumula los eventos que provoca cada orden y `ControladorRestJuego` los devuelve como `eventos` junto a `datos`. `ClienteJuegoRemoto` reconstruye el `Resultado`, y después republica los eventos, en el mismo orden, en su `BusEvento` local | El Observer cruza la red sin WebSockets ni sondeo (AR-6, R-09): un solo viaje por orden |
| **DT-20** | **Concurrencia.** Una partida por servidor (S-01): `ControladorRestJuego` ejecuta cada orden bajo un mismo candado. El cliente envía las órdenes desde el EDT, de forma síncrona (localhost, tiempo de espera de 5 s) | Sin carreras con un solo jugador y sin `SwingWorker`; el riesgo está en RT-08 |
| **DT-21** | **Persistencia en MongoDB.** El puerto `RepositorioGuardado` sigue en el dominio; el adaptador `RepositorioGuardadoMongo` usa Spring Data MongoDB sobre la colección `partidas` (RN-31, RN-32). La URI se configura con `valleverde.mongo.uri` (por defecto `mongodb://localhost:27017/valleverde`) con tiempos de espera de 2 s. Cualquier error de MongoDB se convierte en una excepción sin marcar del dominio que `ServicioJuegoGuardado` traduce a `ERROR_ES` | Jugar no requiere MongoDB (RNF-11); la infraestructura sigue siendo reemplazable (inversión de dependencias) |
| **DT-22** | **Lanzador.** `Main` sin argumentos arranca el servidor y el cliente en el mismo proceso (el cliente usa `ClienteJuegoRemoto` contra el puerto elegido). `--server [--port n]` arranca solo el servidor y `--client --url …` solo la ventana. Spring se inicia con `headless(false)` para poder abrir Swing | RNF-16; permite demostrar la separación y depurar cada lado |

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
- JaCoCo: informe en `target/site/jacoco` y regla `comprobar` con **dos reglas separadas** (paquetes `domain` y `application`, cobertura de líneas ≥ 70 %, RNF-05).
- Surefire ejecuta las pruebas rápidas por defecto; las de rendimiento llevan `@Tag("perf")` y corren con `-Pperf` (y en el cierre de la Fase 5.5).
- Comando de verificación de cada fase: `mvn clean verify` (compila, prueba, mide cobertura y empaqueta).
- Se generará un *Maven Wrapper* (`mvnw`) para que cualquier equipo compile sin instalar Maven.

## 6. Flujo típico de una orden

Como en la secuencia [A1](diagramas/A1_secuencia_vender_cosecha.png):

1. La UI captura el clic o el botón y llama a `ComandosJuego.vender("CARROT", 3)` (tipos simples). La implementación del cliente, `ClienteJuegoRemoto`, envía `POST /api/market/sales` con `{idArticulo, cantidad}`.
2. `ControladorRestJuego` toma el candado y llama a `ControladorJuego.vender`, que delega en `ServicioMercado`: este obtiene el estado de `SesionJuego`, valida (`Inventario.count`), llama al dominio (`Mercado.unidadVender`, `Inventario.remove`, `EstadoJuego.ganar`) y evalúa logros con `HechosProgreso`.
3. El servicio publica el evento (`BusEvento.publish`) y devuelve `Resultado.ok(79)`; el `RecolectorEvento` lo guarda y el controlador REST responde `200` con `{datos: 79, eventos: [...]}`.
4. `ClienteJuegoRemoto` reconstruye `Resultado.ok(79)` y republica los eventos en el `BusEvento` local; `HudPanel` y `CapaAviso` se actualizan y `DialogoMercado` muestra el resultado.
5. Ante un rechazo, el servicio devuelve `Resultado.fallar(código, mensaje)`, la API responde con el código HTTP del error (p. ej. `409`) y el cuerpo `{codigo, mensaje}`, y el cliente reconstruye el mismo `Resultado.fallar`: **sin cambios de estado ni eventos**.

## 7. Plan de implementación por fases

> Cada fase termina con `mvn clean verify` en verde, un `.jar` ejecutable y una **demostración posible** (UI funcional mínima de las historias de la fase). Dentro de una historia se trabaja así: (1) pruebas de aceptación `HUxx_…` a partir de los criterios; (2) código mínimo; (3) refactor; (4) commit `feat(area): descripcion (HUxx)`. Al cerrar la fase: resumen, patrones aplicados y qué diagrama o documento actualizar; **se espera tu visto bueno antes de seguir**.

| Fase | Historias (puntos) | Trabajo técnico principal | UI funcional mínima | Patrones | Pruebas críticas |
|---|---|---|---|---|---|
| **5.0** Preparación técnica | — | `pom.xml`, esqueleto de paquetes, `Main` (ventana vacía), pruebas de arquitectura (AR-1 a AR-5), JaCoCo, `mvnw`, README inicial | ventana vacía | — | arquitectura |
| **5.1** Núcleo del turno | HU-13, 09, 08, 11, 28, 10, 33, 34 (**25**) | `ReglasJuego`, `Inventario`, calendario (`RelojJuego`, `EstadoEstacion`), `AzarEstado`, clima (`EstrategiaClima`), `EstadoJuego`, `SecuenciaDia` con pasos de calendario, pronóstico y energía, `SesionJuego`, `ServicioDia`, `Result/Messages`, `BusEvento` | pantalla de inicio, HUD con etiquetas y botón «Terminar día» | Singleton, State (estación), Strategy (clima), Observer, Memento (versión mínima en memoria, para HU-10 · CA-4) | determinismo de 112 días |
| **5.2** Cultivo | HU-01 a 07, 35, 36 (**29**) | `MapaGranja`, `Parcela`, `Cultivo`, `EstadoCultivo` ×5, `FabricaCultivo` ×5, `ServicioGranja`, pasos de crecimiento y marchitamiento, DTO de parcela, mapa Java2D en vectores, barra de herramientas, tooltips | mapa con parcelas, herramientas 1–5 y clic | State (cultivo), Factory Method | **1** (fuera de estación), **2** (inventario lleno) |
| **5.2b** Migración a Spring Boot | — (sin historias nuevas: conserva las 163 pruebas) | `pom.xml` con Spring Boot 3.5.16; interfaz `ComandosJuego`; capa `api` (`ControladorRestJuego`, `MapeadorEstadoError`, `RecolectorEvento`); `cliente` (`ClienteJuegoRemoto`); `configuracion`; `RepositorioGuardadoMongo` en lugar de `JsonSaveRepository`; `Main` con modos; reglas AR-1 a AR-3 y AR-11; `docs/API.md` | la misma de la 5.2, ahora sobre HTTP | — (el Observer cruza la red en el sobre de respuesta) | todas las anteriores en verde; arranque ≤ 5 s (RNF-02) |
| **5.3** Clima y economía | HU-12, 37, 14, 15, 16, 38 (**23**) | efectos del clima en el *pipeline*, `Mercado`, `EstrategiaPrecios` ×2, `ServicioMercado`, paso de mercado, `GrillaInventario`, `DialogoMercado` | inventario en grilla y mercado modal | Strategy (precios) | **3** (helada), **4** (saturación) |
| **5.4** Construcción y animales | HU-18 a 24, 17 (**23**) | `Edificio`, `ConstructorEdificio` ×4, `DirectorEdificio`, `Animal`, `EstadoAnimal` ×3, `ServicioEdificio`, `ServicioAnimal`, paso de animales, `DaySummary` | diálogo de construcción y panel del edificio (alimentar, recolectar, curar) | Builder, State (animal) | **5** (animal enfermo no produce) |
| **5.5** Progresión y persistencia | HU-26, 27, 29, 30, 31, 32 (**22**) | `SeguidorLogro`, `HechosProgreso`, desbloqueos, `MementoJuego`, `ValidadorEstadoJuego`, `RepositorioGuardado`, `RepositorioGuardadoMongo`, `CodecGuardado`, `Checksum`, `ServicioJuegoGuardado`; **pruebas de rendimiento a plena carga** (HU-10 · CA-3, HU-29 · CA-4, HU-30 · CA-4) | diálogos de ranuras, logros y atajos `F5`/`F9` | Memento (completo: documento en MongoDB) | **6** (guardar → cargar idéntico), **7** (partida dañada o inexistente) |
| **5.6** Interfaz visual y extras *(recortable)* | HU-39, 42, 25, 40, 41 (**19**) | `ColaAviso`, `PresentadorAviso`, `CapaAviso`, `GestorRecurso` y respaldo vectorial, invernadero, efectos (`EfectosVisuales`, `Particula`, `PaletaEstacion`), animaciones con `Timer`; `docs/ASSETS.md`, README final y matriz de trazabilidad completa; decisión sobre FlatLaf | pulido visual | Observer (completado) | — **(hecha)** |

**Qué se actualiza al cerrar cada fase:** el diagrama de clases si aparecen clases nuevas con responsabilidad estructural, el de desarrollo si cambia un paquete o una dependencia (lo comprueba `tools/check_diagrams.py`) y la matriz de trazabilidad (RF → HU → diagrama → clases → pruebas).

**Si falta tiempo:** se recorta la Fase 5.6 sin afectar el diseño base (el juego sigue completo con gráficos vectoriales). El MVP son las 30 historias *Must* (109 puntos).

## 8. Riesgos técnicos

| ID | Riesgo | Prob. | Impacto | Mitigación |
|---|---|---|---|---|
| RT-01 | El `secuencia` del fin de día se vuelve un cuello de botella o se rompe al agregar pasos | Media | Alto | Pasos pequeños e independientes con prueba de orden (HU-10 · CA-1); `ReporteDia` único; transacción por memento (DT-03) |
| RT-02 | El estado global del Singleton contamina pruebas | Media | Medio | `reiniciarParaTests()` y regla de `@BeforeEach` (DT-09) |
| RT-03 | Cambios al esquema de guardado rompen partidas viejas | Baja | Medio | `versionEsquema` y rechazo con `VERSION_NO_SOPORTADA`; pruebas con documentos de ejemplo versionados |
| RT-04 | La UI Swing no se puede probar sin pantalla | Media | Bajo | Lógica de presentación en clases sin Swing (presentadores y DTO); pruebas de componentes solo si `!GraphicsEnvironment.isHeadless()` |
| RT-05 | El JDK 25 del equipo produce bytecode incompatible con JRE 17 | Baja | Alto | `--release 17` y una comprobación en 5.0 (`javap` del `.class` generado) |
| RT-06 | Mapas, sprites y efectos consumen el tiempo | Alta | Alto | Orden de fases con la 5.6 recortable; vectoriales desde el inicio |
| RT-07 | Sobreingeniería por tantos patrones | Media | Medio | Cada patrón está justificado por escrito ([`DIAGRAMAS.md`](DIAGRAMAS.md) §3.1); no se agregan más |
| RT-08 | El cliente bloquea el EDT al esperar la respuesta HTTP | Baja | Medio | Servidor local (milisegundos), tiempo de espera de 5 s y mensaje `ERROR_INTERNO` si vence; si en la práctica se nota, las órdenes pasan a un `SwingWorker` sin tocar el servidor |
| RT-09 | El arranque de Spring Boot supera los 5 s de RNF-02 | Media | Medio | Medir en la Fase 5.2b; inicialización perezosa de beans y `primavera.main.banner-mode=off`; la ventana puede abrirse mientras el servidor termina de arrancar |
| RT-10 | MongoDB no está disponible al guardar o cargar | Media | Medio | Jugar no lo requiere (RNF-11); tiempo de espera de 2 s y `ERROR_ES` con la partida intacta (HU-29 · CA-2, HU-31 · CA-5) |
| RT-11 | Las pruebas de MongoDB dependen del equipo donde corren | Media | Bajo | Solo `RepositorioGuardadoMongo` las necesita; el resto usa `RepositorioGuardadoFalso`; se omiten con un aviso si no hay MongoDB |

## 9. Cambios derivados de esta fase (ya aplicados)

| # | Cambio | Dónde |
|---|---|---|
| C-01 | `SeguidorLogro.evaluar` recibe `HechosProgreso` y no `EstadoJuego`: la versión anterior creaba un **ciclo** `juego → progreso → juego` | diagramas 03 y 04; el verificador ahora detecta ciclos |
| C-02 | Se dibuja la dependencia `granja → tiempo` (cultivos y animales necesitan clima y estación) | diagrama 04 |
| C-03 | Se agregan `SesionJuego` (DT-10), `AzarEstado` (DT-07) y `HechosProgreso` | diagramas 03 y 04 |
| C-04 | RN-32: el archivo guarda el **estado interno del generador** (`AzarEstado`) en lugar de «semilla + número de sorteos» | `REGLAS_DE_NEGOCIO.md` |
| C-05 | RN-35: nuevo código `ERROR_INTERNO` para excepciones inesperadas (DT-02) | `REGLAS_DE_NEGOCIO.md` |

## 10. Estado de la Fase 5.0 (preparación técnica)

| Entregable | Resultado |
|---|---|
| `pom.xml` con las versiones de §5, JaCoCo con **dos barreras** (dominio y aplicación ≥ 70 %), perfil `perf` y *shade* | `mvn clean verify` en verde; la barrera se probó en negativo con una clase sin cubrir y el build falló como debía |
| Esqueleto: 26 `package-info.java` del árbol de §3, `Main`, `InfoJuego`, `VentanaPrincipal` | la ventana «Valle Verde» abre en 0,5 s |
| Pruebas: AR-1 a AR-5 y AR-7 (ArchUnit), AR-9 (registro de patrones), `InfoJuegoTest`, `VentanaPrincipalTest` | 12 pruebas en verde |
| `mvnw` (Maven Wrapper 3.9.16) | `./mvnw -v` funciona sin instalar Maven |
| `target/farmsim.jar` ejecutable | bytecode versión 61 (Java 17), `Main-Class` correcto; sin pantalla termina con un mensaje claro (código 1) |
| `tools/traceability.py` y `docs/TRAZABILIDAD.md` | matriz RF → HU → componente → clases → pruebas; el modo `--upto 5.x` exige historias completas |
| `README.md` | cómo compilar, ejecutar y probar; estructura; tabla de patrones |

Ajustes surgidos al implementar: (1) `Main` se declara como capa «Composition» en AR-1, porque ArchUnit trata como violación que una clase fuera de las capas acceda a la UI; (2) `InfoJuego` y `VentanaPrincipal` se agregan al árbol y al diagrama de desarrollo; (3) se añade la regla AR-10 (`Stories:` en el Javadoc).

## 11. Estado de la Fase 5.1 (núcleo del turno)

**Historias cerradas (8 historias, 25 puntos):** HU-13, 09, 08, 11, 28, 10, 33 y 34. `python tools/traceability.py --upto 5.1` termina sin errores: cada historia tiene sus clases (`Stories:`) y al menos tantas pruebas `HUxx_` como criterios.

| Resultado | Valor |
|---|---|
| Pruebas | 78 en verde (61 con prefijo `HUxx_`), 1 deshabilitada con motivo (HU-10 · CA-3, rendimiento a plena carga, fase 5.5) |
| Cobertura de líneas (JaCoCo) | **dominio 97,4 %**, **aplicación 93,2 %**, infraestructura 100 %, UI 92,8 % (barrera: 70 % en dominio y aplicación) |
| Patrones aplicados | Singleton (`RelojJuego`), State (`EstadoEstacion` y 4 estados), Strategy (`EstrategiaClima`: estacional y fija), Observer (`BusEvento`, `OyenteJuego`, `ModeloHud`, `HudPanel`), Memento mínimo (`EstadoJuego`, `MementoJuego`), todos registrados en `patterns.txt` (AR-9) |
| Prueba de integración Swing | clic en «Nueva partida» y «Terminar día» sobre las pantallas reales: el HUD muestra «500 m», «Día 1» y luego «Día 2» |
| Arranque real | la ventana «Valle Verde» aparece en 0,53 s |

**Decisiones tomadas al implementar (ajustes al plan):**

| # | Decisión | Motivo |
|---|---|---|
| D-01 | El **Memento** se adelanta a la 5.1 en su forma mínima (en memoria) | HU-10 · CA-4 exige que una falla interna deje el estado idéntico; `ServicioDia` toma una instantánea y la restaura. La 5.5 lo completa con archivo, validación y las partes que traigan las fases |
| D-02 | `ServicioProgreso.juegoNuevo` crea las partidas | RF-28 pertenece al componente *Progreso* de la vista funcional |
| D-03 | **Presentadores sin Swing** (`PresentadorPantallaInicio`, `PresentadorPantallaJuego`, `ModeloHud`) | La lógica de las pantallas se prueba sin pantalla (RT-04); Swing solo pinta |
| D-04 | `SecuenciaDia` ordena los pasos por `FaseDia`, sin importar el orden de registro | El orden de RN-03 no puede romperse al agregar un paso |
| D-05 | `RepositorioGuardado` nace con un solo método (`existeRanura`) y `JsonSaveRepository` con su implementación real mínima | HU-33 · CA-3 («Cargar» sin partidas) la necesita; la 5.5 agrega guardar, cargar y listar |
| D-06 | Código `SIN_PARTIDA` agregado a RN-35 | Terminar el día sin partida en curso necesitaba un mensaje propio |

**Criterios cubiertos en parte (se completan donde nace lo que falta):**

| Criterio | Qué falta | Fase |
|---|---|---|
| HU-28 · CA-1 | demanda 100 de todos los productos y logros bloqueados con contadores en 0 | 5.3 y 5.5 |
| HU-10 · CA-2 | marcas de riego de las parcelas limpiadas al terminar el día | 5.2 (HU-03) |
| HU-10 · CA-3 | rendimiento a plena carga (prueba `@Disabled`) | 5.5 |
| HU-08 · CA-2 | el rechazo `SIN_ENERGIA` al sembrar (hoy se prueba en `Energia` y en `Mensajes`) | 5.2 (HU-01) |
| HU-34 · CA-2 | el evento `EnergiaCambiada` lo publicará una acción real (hoy se simula en la prueba) | 5.2 |
| HU-33 · CA-3 | cargar cuando sí hay una partida guardada | 5.5 (HU-30) |

**Diagramas:** el diagrama lógico y el de desarrollo incluyen ahora las clases reales de la fase. Además de `tools/check_diagrams.py`, se comprobó una vez contra los `import` reales del dominio: no falta ninguna dependencia en el dibujo (las flechas dibujadas que aún no existen en el código corresponden a fases siguientes).

## 12. Estado de la Fase 5.2 (cultivo)

**Historias cerradas (9 historias, 29 puntos):** HU-01, 02, 03, 04, 05, 06, 07, 35 y 36. `python tools/traceability.py --upto 5.2` termina sin errores.

| Resultado | Valor |
|---|---|
| Pruebas | 163 en verde (146 con prefijo `HUxx_`) y 1 deshabilitada con motivo (HU-10 · CA-3, fase 5.5) |
| Cobertura de líneas (JaCoCo) | **dominio 97,8 %**, **aplicación 95,5 %**, UI 90,6 %, infraestructura 100 % (barrera: 70 % en dominio y aplicación) |
| Patrones aplicados | State (`EstadoCultivo` con `EstadoSemilla`, `EstadoBrote`, `EstadoCreciendo`, `EstadoMaduro`, `EstadoMarchito`; contexto `Cultivo`), Factory Method (`FabricaCultivo` con 5 creadores), Observer (`PanelMapa`); todos registrados en `patterns.txt` |
| Pruebas críticas cerradas | **1** `HU01_ca2_siembra_fuera_de_estacion_es_rechazado_y_nada_cambia` y **2** `HU04_ca2_harvestingWithAFullInventoryIsRejectedAndTheCropStaysMature` |
| Integración Swing real | herramienta «1 Sembrar» → clic en el mapa → HUD 98/100 → `Enter` termina el día |

**Decisiones tomadas al implementar:**

| # | Decisión | Motivo |
|---|---|---|
| D-07 | `TipoCultivo` no conoce `TipoArticulo`: es `TipoArticulo` quien enlaza cada semilla y cada cosecha con su cultivo | Evita un ciclo `cultivo ↔ economy`: el diagrama de desarrollo ya dibujaba `economy → cultivo` |
| D-08 | `EstadoFaseCreciendo` agrupa lo común de Semilla, Brote y Crecimiento (necesitan agua, no se cosechan) | Una sola implementación de la regla de crecimiento y de marchitamiento por sequedad |
| D-09 | El marchitamiento por cambio de estación vive en `PasoCalendario` (paso 5 de RN-03) y el de sequedad en `PasoCrecimiento` (paso 2) | Es exactamente el orden de RN-03; el crecimiento del último día ocurre antes del cambio de estación |
| D-10 | La herramienta «Cosechar» cosecha, limpia o (en 5.4) recolecta según el contenido de la parcela | RN-34 |
| D-11 | Código nuevo `CULTIVO_MARCHITO` en RN-35 | RN-07 manda rechazar el riego de un cultivo marchito y no había código para ello |
| D-12 | Regar un cultivo maduro se permite (cuesta energía sin efecto) | RN-07 solo exige «vivo y sin regar hoy»; el rechazo necesitaría una regla nueva |
| D-13 | Las herramientas «Construir» y «Alimentar» y los atajos `M`, `I`, `L`, `F5`, `F9` se pueden pulsar; los que aún no tienen pantalla responden «estará disponible en una fase posterior» (en 5.3 y 5.4 ya funcionan `M`, `I`, «Construir» y «Alimentar»; quedan `L`, `F5` y `F9` para 5.5) | HU-36 · CA-1 y CA-5 piden que existan |
| D-14 | `PanelMapa` consulta el mapa solo al recibir un evento (`JuegoIniciado`, `DiaTerminado`, `MapaCambiado`); no tiene `Timer` (lo verifica una prueba) | Regla de interfaz: actualizar por eventos, no por consulta en bucle |
| D-15 | Geometría (`GeometriaMapa`) y dibujo (`DibujanteMapa`) son clases sin componente | Se prueban pintando sobre un `BufferedImage`, sin pantalla |

**Correcciones a documentos aprobados:** (1) las coordenadas de ejemplo de HU-01 (parcela `(2,3)`) y de HU-18 (esquina `(4,6)`) no coincidían con el mapa de RN-04 y se cambiaron por `(5,3)` y `(2,7)`; (2) `CULTIVO_MARCHITO` se agregó a RN-35; (3) los diagramas lógico y de desarrollo incluyen las clases de la fase (`MapaGranja.cultivosCrecer`, `Cultivo.terminarDeDia`, `EstadoCultivo.onFinDia`, `ServicioGranja`).

**Criterios de la 5.1 que se cerraron en esta fase:** HU-10 · CA-2 (se limpia la marca de riego), HU-08 · CA-2 (`SIN_ENERGIA` al sembrar) y HU-34 · CA-2 (el evento `EnergiaCambiada` lo publica una acción real).

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
| Pruebas | 222 (2 se omiten con aviso si no hay MongoDB: `RepositorioGuardadoMongoRealTest`); `mvn clean verify` en verde con las dos barreras de cobertura |
| Nuevas | 22 de la API (una por ruta y rechazo), 11 del cliente contra el servidor real (incluye «mismos resultados y eventos que el controlador en proceso»), 1 de las pantallas Swing sobre HTTP, 8 de reglas ArchUnit |
| Arranque | `java -jar target/farmsim.jar`: la ventana «Valle Verde» aparece en **4,1 a 4,3 s** (RNF-02 pide ≤ 5 s) con servidor, Tomcat y cliente |
| Sin MongoDB | el juego funciona; `GET /api/saves/any` responde `503 ERROR_ES` con el texto de RN-35 |
| Patrón nuevo | Observer: `RecolectorEvento` (el servidor guarda los eventos de cada orden para la respuesta) |

**Decisiones tomadas al implementar:**

| # | Decisión | Motivo |
|---|---|---|
| D-16 | `tieneJuegoEnProgreso()` del cliente se deduce de `GET /api/game` y no tiene ruta propia | Una ruta menos y la misma semántica (sin partida = `404 SIN_PARTIDA`) |
| D-17 | El cliente reconstruye el `Resultado` y **después** republica los eventos | Mismo orden que en proceso: la UI ve los eventos antes de recibir el resultado |
| D-18 | `DtoEvento.datos` es el propio *record* del evento; el cliente elige la clase por el nombre entre los subtipos permitidos de `EventoJuego` | Un evento nuevo no exige tocar el decodificador |
| D-19 | `RepositorioGuardadoMongo` lleva `@Profile("!no-mongo")`; las pruebas de API y cliente usan el perfil `no-mongo` con `RepositorioGuardadoFalso` | Esas pruebas no dependen de tener MongoDB (RT-11) |
| D-20 | Todo error técnico de MongoDB se vuelve `ExcepcionAlmacenamientoGuardado` (dominio) y `ServicioJuegoGuardado` lo traduce a `ERROR_ES`; `tieneGuardadoCualquier()` ahora devuelve `Resultado<Boolean>` | La aplicación no ve tipos de MongoDB y jugar no depende de la base |
| D-21 | Surefire fija `java.awt.headless=false` | Spring Boot activa el modo sin pantalla y las pruebas Swing se omitían en silencio |
| D-22 | Un `idSemilla` desconocido responde `500 ERROR_INTERNO` (no hay código de RN-35 para «semilla inexistente») | La UI nunca lo envía; no se inventó un código nuevo |
| D-23 | `JsonSaveRepository` y Gson se eliminaron; la D-05 de la §11 queda sustituida por `RepositorioGuardadoMongo` | Persistencia en MongoDB (RN-31) |

**Pendiente de la persistencia (5.5):** guardar, cargar y listar (`PUT`/`POST /api/saves/{ranura}`), `CodecGuardado` con Jackson, `Checksum` y `ValidadorEstadoJuego`. El adaptador y el formato de documento (`DocumentoGuardado`) ya existen.

**Diagramas:** las 6 vistas se actualizaron (MongoDB, cliente, API REST, paquetes `cliente`/`api`/`configuracion`, despliegue con servidor y cliente dentro de la JVM, y la secuencia A1 con el salto HTTP); `check_diagrams.py` y `check_imports.py` terminan sin errores.

## 14. Estado de la Fase 5.3 (clima y economía)

**Historias cerradas (6 historias, 23 puntos):** HU-12, 14, 15, 16, 37 y 38. `python tools/traceability.py --upto 5.3` termina sin errores.

| Resultado | Valor |
|---|---|
| Pruebas | 280 en verde y 2 omitidas con aviso (las de MongoDB real); 194 con prefijo `HUxx_` |
| Cobertura de líneas (JaCoCo) | **dominio 97,7 %**, **aplicación 96,9 %**, UI 90,7 %, API 95,2 %, cliente 89,3 % (barrera: 70 % en dominio y aplicación) |
| Patrones aplicados | Strategy: `EstrategiaPrecios` con `PreciosCultivo` y `PreciosFijos`; Observer: `ModeloInventario`; todos registrados en `patterns.txt` |
| Pruebas críticas cerradas | **3** `HU12_ca2_frostWithersEveryCropInTheOpenEvenAMatureOne` y **4** `HU15_ca2_afterTenCarrotsTheNextOnePaysOnly22` con `HU15_ca3_theDemandNeverFallsBelow50` |
| Arranque | 4,1 a 4,3 s con la máquina libre (5.2b); al medir de nuevo con el servidor de Java de VS Code indexando el proyecto subió a 5 a 7,5 s: el límite de RNF-02 (≤ 5 s) depende de la carga del equipo (RT-09) y conviene medirlo otra vez en el cierre del proyecto |
| API nueva | `GET /api/market`, `POST /api/market/purchases`, `POST /api/market/sales`, `GET /api/market/quotes`, `GET /api/inventory` (ver [`API.md`](API.md)) |

**Decisiones tomadas al implementar:**

| # | Decisión | Motivo |
|---|---|---|
| D-24 | El paso de efectos del clima (`PasoEfectosClima`) es el primero del *pipeline* y el de mercado (`PasoMercado`) el cuarto | Es exactamente el orden de RN-03 |
| D-25 | `TipoCultivo` conoce el precio de semilla y la venta base (tabla de RN-05); `TipoArticulo` sigue sin conocer a `TipoCultivo` hacia atrás | Una sola tabla por cultivo; sin ciclo `cultivo ↔ economy` (D-07) |
| D-26 | Precio = `máx(1, ⌊(base × factor × demanda + 5 000) / 10 000⌋)` con enteros | Es el redondeo half-up de RN-17 sin números decimales |
| D-27 | La tendencia (RN-19) guarda «precio anterior = 0» al inicio, que se muestra como ▬; `InstantaneaMercado` lo conserva en el memento | «Al inicio todos muestran ▬» sin inventar un precio previo |
| D-28 | `GET /api/market/quotes` calcula el «total previsto» en el servidor | El total depende de la demanda que baja unidad a unidad (79 m, no 3 × 27) |
| D-29 | Sembrar y cosechar publican `InventarioCambiado`; el modelo del inventario recarga y limpia la semilla elegida si se agota | HU-37 · CA-3 por eventos (Observer), sin consultar en bucle |
| D-30 | El inventario y el mercado son diálogos modales (`I`, `M`; `Esc` cierra) y la lógica vive en `ModeloInventario` y `PresentadorMercado`, sin Swing | RT-04: se prueba sin pantalla; el modal impide clics en el mapa (HU-38 · CA-1) |
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

**Diagramas:** `04_vista_desarrollo` incluye las clases nuevas; el resto de vistas ya tenían el diseño de la economía (Strategy de precios, `ServicioMercado`, `DialogoMercado`). `check_diagrams.py` y `check_imports.py` terminan sin errores.

---

## 15. Estado de la Fase 5.4 (construcción y animales)

**Historias cerradas (8 historias, 23 puntos):** HU-17, 18, 19, 20, 21, 22, 23 y 24. HU-24 (Almacén) se adelantó desde la 5.5 porque HU-37 · CA-2 pedía las 36 casillas. `python tools/traceability.py --upto 5.4` termina sin errores.

| Resultado | Valor |
|---|---|
| Pruebas | 379 en verde y 2 omitidas con aviso (las de MongoDB real) |
| Cobertura de líneas (JaCoCo) | **dominio 98,0 %**, **aplicación 97,8 %**, UI 91,8 %, API 95,7 %, cliente 90,0 % (barrera: 70 % en dominio y aplicación) |
| Patrones aplicados | Builder: `ConstructorEdificio` con `ConstructorGallinero`, `ConstructorEstablo`, `ConstructorAlmacen`, `ConstructorInvernadero`, `DirectorEdificio` y `Edificio` como producto; State: `EstadoAnimal` con `EstadoSano`, `EstadoHambriento`, `EstadoEnfermo` y `Animal` como contexto; todos registrados en `patterns.txt` |
| Prueba crítica cerrada | **5** `HU22_ca2_aSickHenProducesNothing_criticalTest` (y la variante de la gallina hambrienta) |
| API nueva | `GET|POST /api/buildings`, `POST /api/animals`, `POST /api/buildings/{id}/feeding`, `/collection` y `/animals/{n}/treatment`; el resumen del día viaja como evento `ResumenDiaListo` (ver [`API.md`](API.md)) |
| Verificadores | `check_hu.py`, `check_diagrams.py`, `check_imports.py` y `traceability.py` terminan sin errores |

**Decisiones tomadas al implementar:**

| # | Decisión | Motivo |
|---|---|---|
| D-33 | `Logro` (los nueve logros de RN-28) y `SeguidorLogro` ya existen con solo el conjunto de logros desbloqueados | RN-29 y HU-18 · CA-5 necesitan saber qué edificios están bloqueados; los contadores y la evaluación llegan en 5.5 (HU-26) |
| D-34 | Construir comprueba en el orden de RN-21 (ubicación, desbloqueo, límite, saldo); comprar un animal comprueba primero el saldo y luego la plaza | HU-38 · CA-3 pide `SIN_FONDOS` aunque no haya Establo |
| D-35 | Alimentar, recolectar y curar responden `VistaAccionEdificio` (edificio, cantidad y un aviso opcional) | «No hay nada que alimentar», «animales enfermos omitidos» (HU-20 · CA-4) y «nada que recoger» son éxitos con aviso: RN-35 no tiene código para ellos |
| D-36 | `EstadoJuego` lleva un `LibroDia` con ingresos y gastos del día (cada `ganar` y `gastar`), que entra en el memento; `SecuenciaDia` lo copia al `ReporteDia` y lo reinicia | HU-17 · CA-4: los acumulados parten en 0 cada día; el saldo anterior es `saldo nuevo − ingresos + gastos` |
| D-37 | El resumen del día viaja como evento `ResumenDiaListo` después de `DiaTerminado`; el diálogo no es modal | No cambia el cuerpo de `POST /api/game/days` y no bloquea el juego |
| D-38 | Un edificio se identifica por su orden de construcción (no se demuelen) y las rutas son `/api/buildings/{id}/…`; un id o un animal inexistente responde `500 ERROR_INTERNO` | Como D-22 y D-32: la UI nunca los envía y RN-35 no tiene código para ello |
| D-39 | Curar no es herramienta: se hace desde el panel del edificio. «Alimentar» y «Cosechar» actúan sobre el edificio bajo el clic, «Construir» abre el diálogo con la parcela pulsada como esquina y, sin herramienta, pulsar un edificio abre su panel | RN-27 y RN-34 |
| D-40 | El catálogo de construcción incluye los cuatro edificios; el efecto del Invernadero (HU-25) llega en 5.6 | HU-18 lista el catálogo completo; construirlo ya funciona y cuesta lo que dice RN-20 |
| D-41 | `VistaMapa` lleva la lista de edificios (con sus animales) y `VistaMercado` la de animales en venta; los constructores anteriores se conservan | El mapa se dibuja de una sola consulta y la pestaña Comprar ofrece Gallina y Vaca |

**Criterios cubiertos en parte (se completan donde nace lo que falta):**

| Criterio | Qué falta | Fase |
|---|---|---|
| HU-17 · CA-2 | el resumen ya lista cultivos marchitos, animales enfermos y productos; falta la línea del logro desbloqueado | 5.5 |
| HU-28 · CA-1 | solo falta que los logros bloqueados empiecen con contadores en 0 | 5.5 |
| HU-10 · CA-3, HU-33 · CA-3 | rendimiento a plena carga y cargar una partida guardada | 5.5 |
| HU-36 · CA-5 | los atajos `L`, `F5` y `F9` aún responden que llegarán | 5.5 |

**Diagramas:** `03_vista_logica` y `04_vista_desarrollo` incluyen las clases y dependencias nuevas (`domain.game → animal/building`, `edificio → animal/economy/progress`, `animal → economy`); las seis vistas se volvieron a renderizar. `check_diagrams.py` y `check_imports.py` terminan sin errores.

---

## 16. Estado de la Fase 5.5 (progresión y persistencia)

**Historias cerradas (6 historias, 22 puntos):** HU-26, 27, 29, 30, 31 y 32, más los criterios que esperaban a los logros y al guardado (HU-17 · CA-2, HU-28 · CA-1, HU-10 · CA-3, HU-33 · CA-3, HU-36 · CA-5). `python tools/traceability.py --upto 5.5` termina sin errores.

| Resultado | Valor |
|---|---|
| Pruebas | 442 en verde y 2 omitidas con aviso (las de MongoDB real) |
| Cobertura de líneas (JaCoCo) | **dominio 97,2 %**, **aplicación 97,6 %**, infraestructura 94,2 %, UI 91,1 %, API 95,9 %, cliente 90,8 % (barrera: 70 % en dominio y aplicación) |
| Patrones aplicados | Memento completo: `MementoJuego` con todas las partes (logros y contadores incluidos) y `ServicioJuegoGuardado` como *caretaker*; registrado en `patterns.txt` |
| Pruebas críticas cerradas | **6** `HU30_ca1_el_cargado_estado_es_estructuralmente_identico_a_el_archivado_uno_critico_test` y **7** `HU31_ca1_…Inexistent…`, `HU31_ca2_…Corrupt…` (carácter cambiado y JSON truncado) |
| Rendimiento a plena carga | `RendimientoGranjaLlenoTest`: terminar el día 100 veces (p95 ≤ 200 ms), guardar y cargar (≤ 1 s, documento ≤ 200 KB) |
| API nueva | `GET /api/achievements`, `GET /api/saves`, `PUT /api/saves/{ranura}`, `POST /api/saves/{ranura}/load` (ver [`API.md`](API.md)) |
| Verificadores | `check_hu.py`, `check_diagrams.py`, `check_imports.py` y `traceability.py` terminan sin errores |

**Decisiones tomadas al implementar:**

| # | Decisión | Motivo |
|---|---|---|
| D-42 | `RepositorioGuardado` guarda y lee `MementoJuego` (no texto): `CodecGuardado` y `Checksum` viven en la infraestructura y `ValidadorEstadoJuego` en el dominio; el servicio de guardado decide el código de error | DT-11 y DT-13: el dominio no conoce Jackson ni MongoDB y las invariantes de RN-33 son regla de negocio |
| D-43 | El orden de RN-33 se cumple así: existencia, JSON bien formado, versión, suma de control y datos (en `CodecGuardado`) e invariantes (en el dominio). Un JSON mal formado gana a una versión o suma erróneas | HU-31 · CA-4 |
| D-44 | Cargar construye el estado aparte (`EstadoJuego.desdeMemento`) después de validar y solo entonces reemplaza al de `SesionJuego`; si algo inesperado falla se restaura el anterior | RN-33: la partida actual no cambia en absoluto |
| D-45 | Los logros se evalúan en `ServicioLogro` tras cada acción exitosa (cosechar, vender, construir, comprar animal, curar) y en el paso 8 del día (`PasoLogros`); una recompensa que cumple otra condición encadena la siguiente | RN-28: «después de cada acción exitosa y en el paso 8» |
| D-46 | Construir el primer edificio paga LG-06 (100 m): el saldo tras construir un Gallinero es 200 m, no 100 m | HU-26 · CA-5 manda sobre la cifra de HU-18 · CA-1; las pruebas de HU-18 comprueban los 400 m cobrados y la recompensa |
| D-47 | Los contadores (cosechas, ventas, edificios, curaciones) forman parte del memento y de la validación («logros coherentes con sus contadores») | RN-32 y RN-33 |
| D-48 | `F5` y `F9` guardan y cargan en la última ranura usada; si no hay ninguna abren el diálogo de ranuras. Sobrescribir una ranura ocupada pide confirmación en el diálogo (no en `F5`, que repite la ranura elegida) | HU-36 · CA-5 y HU-29 · CA-3 |
| D-49 | «Cargar» en la pantalla de inicio ofrece las tres ranuras en una lista; una ranura dañada o vacía se rechaza con el mensaje de RN-35 sin salir del inicio | HU-31 |
| D-50 | La prueba de plena carga usa 40 cultivos al aire libre, 6 edificios (el Invernadero incluido) y 16 animales; los 9 cultivos del invernadero llegan con HU-25 (5.6) | El efecto del Invernadero aún no existe |
| D-51 | Las pruebas de guardado usan el `RepositorioGuardadoMongo` real sobre un `MongoTemplate` en memoria (`MongoEnMemoria`); la prueba con MongoDB de verdad sigue omitida si no hay uno | No hay MongoDB en esta máquina (RT-11) |

**Criterios cubiertos en parte (se completan donde nace lo que falta):**

| Criterio | Qué falta | Fase |
|---|---|---|
| HU-10 · CA-3, HU-29 · CA-4, HU-30 · CA-4 | los 9 cultivos del invernadero de la «plena carga» | 5.6 (HU-25) |
| HU-29 · CA-2 | ~~la prueba contra un MongoDB real que rechaza o no responde~~ — **cubierta en la etapa 5.7h** (`HU29MongoRealTest`) | hecha |

**Diagramas:** `03_vista_logica` y `04_vista_desarrollo` incluyen `ServicioLogro`, `ValidadorEstadoJuego`, `CodecGuardado`, `Checksum`, `InfoRanura` y sus dependencias (`domain.game → cultivo`, `domain.save → tiempo`); las seis vistas se volvieron a renderizar. `check_diagrams.py` y `check_imports.py` terminan sin errores.

## 17. Estado de la Fase 5.6 (interfaz visual y extras)

**Historias cerradas (5 historias, 19 puntos):** HU-25, 39, 40, 41 y 42, más los criterios que esperaban al invernadero (HU-10 · CA-3, HU-29 · CA-4 y HU-30 · CA-4 con los 9 cultivos protegidos). `python tools/traceability.py --upto 5.6` termina sin errores. Con esta fase **las 42 historias están implementadas**.

| Resultado | Valor |
|---|---|
| Pruebas | 486 en verde y 2 omitidas con aviso (las de MongoDB real) |
| Cobertura de líneas (JaCoCo) | **dominio 97,3 %**, **aplicación 98,4 %**, infraestructura 96,1 %, UI 91,5 %, API 96,6 %, cliente 94,0 % (barrera: 70 % en dominio y aplicación) |
| Patrones aplicados | Observer completado: `PresentadorAviso` (`ConcreteObserver`, registrado en `patterns.txt`) |
| Rendimiento | un cuadro con partículas activas tarda menos de 33 ms (≥ 30 FPS, `HU40_ca4`); la plena carga ahora incluye los 9 cultivos del invernadero |
| API | `VistaParcela` añade `resguardado` (ver [`API.md`](API.md) v0.5); no hay rutas nuevas |
| Verificadores | `check_hu.py`, `check_diagrams.py`, `check_imports.py` y `traceability.py` terminan sin errores |

**Decisiones tomadas al implementar:**

| # | Decisión | Motivo |
|---|---|---|
| D-52 | Una parcela «protegida» se deduce del edificio que la cubre (`Parcela.esResguardado()`); no se guarda nada nuevo en el memento y el validador acepta cultivos sobre parcelas de un invernadero | RN-22 y RN-32: el estado ya contiene el edificio |
| D-53 | Dentro del invernadero la tolerancia sin riego es siempre 3 días y regar cuesta 1 de energía aunque el clima sea Sequía; la lluvia no riega, la helada y el cambio de estación no marchitan | RN-22 · «inmunes a Helada y Sequía» y HU-25 · CA-3 y CA-4 |
| D-54 | Los avisos son un Observer: `PresentadorAviso` escucha el bus (logros y cambio de estación) y recibe de `PresentadorPantallaJuego` los rechazos y avisos propios; `ColaAviso` (sin Swing, con reloj inyectado) guarda 4 visibles y el resto en cola; `CapaAviso` solo dibuja y no captura el ratón. La barra de estado se mantiene | HU-39; la barra sigue siendo el rastro del último mensaje |
| D-55 | Un aviso dura 3 s (5 s el de logro) a opacidad completa y luego se desvanece 0,4 s; el tiempo cuenta desde que se hace visible, no desde que se publicó | HU-39 · CA-1 y CA-3 |
| D-56 | `EfectosVisuales` (paleta de estación, lluvia, nieve, tinte de noche, cuadros de animales, crecimiento de cultivos) es un modelo sin Swing que solo recibe lo que la vista ya sabe y **nunca llama al juego**; `PanelMapa` lo mueve con un `javax.swing.Timer` que solo corre mientras algo se mueve | HU-40 · CA-3 y HU-41 · CA-3 y CA-4 |
| D-57 | La prueba `HU35_ca5` («no usa un *timer* para sondear») pasó a comprobar lo que de verdad importa: el panel nunca pide el mapa por su cuenta y su temporizador está detenido si nada se mueve | El temporizador de HU-41 es de dibujo, no de sondeo |
| D-58 | `GestorRecurso` lee `crops/<CULTIVO>_<ETAPA>.png` y `animals/<ANIMAL>_<CUADRO>.png` de `assets/`, guarda cada archivo una vez y, si falta, `DibujanteMapa` dibuja el vector. **No se incluye ningún sprite**; el primer faltante se registra como aviso y los demás en nivel FINE | HU-42; evita 30 avisos por cuadro en una instalación sin imágenes |
| D-59 | La casilla «Efectos» apaga a la vez los efectos y las animaciones; con ella apagada el mapa se dibuja con su aspecto base | HU-40 · CA-4 |
| D-60 | **FlatLaf no se adopta:** el aspecto por defecto de Swing con el mapa vectorial basta, y evita una dependencia más que verificar | Alcance y RNF-02 (arranque) |

**Criterios cubiertos en parte:**

| Criterio | Qué falta | Cuándo |
|---|---|---|
| HU-29 · CA-2 | ~~la prueba contra un MongoDB real que rechaza o no responde~~ — **cubierta en la etapa 5.7h** (`HU29MongoRealTest`) | hecha |
| ~~RNF-02 · RT-09~~ | **resuelto en la 5.7i:** la pantalla de inicio aparece en 2,0 a 2,4 s porque servidor y ventana arrancan en paralelo | hecha |

**Diagramas:** `03_vista_logica` y `04_vista_desarrollo` incluyen `PresentadorAviso`, `ColaAviso`, `CapaAviso`, `EfectosVisuales`, `Particula`, `PaletaEstacion` y `GestorRecurso`; la secuencia anexa cambió `CapaAviso` por `PresentadorAviso`. Las vistas se volvieron a renderizar.

## 18. Fase 5.7 (hecha): interfaz profesional, avatar y NPC

**Origen:** el estudiante pidió, tras cerrar la 5.6, FlatLaf, sprites en estilo granja, NPC y un avatar que camina. Esto **revierte** las decisiones de Fase 0 «sin NPC» (W-04) y «sin avatar» (W-05). Los sprites de Stardew Valley son de ConcernedApe y tienen copyright: no se usan; se dibujan sprites **propios** de 32 × 32 px.

**Etapa 5.7a (hecha, solo documentación):** RF-43 a RF-48, RN-36 a RN-38, CU-19 y CU-20, HU-43 a HU-48 (24 puntos), vista funcional con 48 RF y verificadores actualizados (`check_hu.py`, `check_diagrams.py`, `gen_test_plan.py`).

| Etapa | Historias | Contenido |
|---|---|---|
| 5.7b | HU-43 | `pom.xml` con FlatLaf (descarga de Maven Central), `UiTheme`, tipografía, contraste |
| 5.7c | HU-44, HU-48 | `tools/gen_sprites.py`, sprites de 32 × 32, escala entera nítida, iconos en barra, HUD e inventario |
| 5.7d | HU-45 | `Avatar` en `EstadoJuego` y en el memento (esquema 2, el 1 se sigue aceptando), `ComandosJuego.mover`, `POST /api/avatar/moves`, teclas, animación |
| 5.7e | HU-46, HU-47 | `Npc`, `ServicioConversacion`, `GET /api/npcs`, `POST /api/npcs/{id}/talk`, encargos, cuadro de diálogo, interacción con `E` o clic |
| 5.7f | HU-49 (nueva) | tutorial de inicio: en una partida nueva el avatar guía al jugador paso a paso (caminar, sembrar, regar, hablar con un NPC, terminar el día); se puede saltar y su avance se guarda |
| 5.7g | HU-50 (nueva) | el valle: mapa de 48 × 24 con la granja al oeste y la aldea al este unidas por un camino, sin valla; parcelas de 48 px en pantalla con cámara que sigue al avatar; personajes de 1 × 2 parcelas; caminar sin pausas al mantener una tecla |
| 5.7h | HU-29 a HU-32 (corrección) | persistencia en MongoDB: diagnosticar por qué la aplicación empaquetada no guarda nada, corregirlo y cubrirlo con una prueba contra MongoDB real (HU-29 · CA-2 pendiente) |
| 5.7i | — | cierre: arranque ≤ 5 s, cobertura, diagramas, README |

**Códigos de error nuevos (RN-35):** `MOVIMIENTO_BLOQUEADO`, `NPC_LEJOS`, `ENCARGO_INCOMPLETO` (los tres `409`).

**Etapa 5.7b (hecha): tema FlatLaf (HU-43).** 491 pruebas en verde (2 omitidas); `UiTheme.install()` instala FlatLaf 3.6.1 con una sola tipografía de 14 pt y esquinas redondeadas, y si falla deja el aspecto por defecto con un aviso (`Main` lo llama antes de crear la ventana). `Contraste` calcula el contraste WCAG y una prueba exige ≥ 4,5 : 1 en los pares de texto y fondo de la `Paleta` (avisos, HUD y diálogos). Los colores de los avisos pasaron de `CapaAviso` a `Paleta`. Decisión **D-61:** FlatLaf reemplaza a la decisión D-60 (que lo descartaba) por pedido del estudiante.

**Etapa 5.7c (hecha): sprites de 32 × 32 e interfaz de granja (HU-44, HU-48).** 503 pruebas en verde (2 omitidas); cobertura de dominio y aplicación sobre la barrera. `tools/gen_sprites.py` dibuja 90 PNG originales (terrenos por estación, 25 etapas de cultivo, animales de 3 cuadros, 4 edificios, 14 ítems, avatar, 4 NPC e iconos) y `--check` comprueba que el repositorio no se desactualice. `GeometriaMapa` ajusta la parcela a un múltiplo de 32 cuando eso desperdicia a lo sumo un cuarto del espacio (64 px a 1024 × 640) y `DibujanteMapa` dibuja con vecino más cercano; la ventana abre en 1100 × 860 para que quepa ×2. Barra de herramientas, HUD e inventario muestran iconos (`GestorRecurso.icono`) y, sin sprites, solo texto. Decisiones: **D-62** el tamaño de parcela no es siempre un múltiplo entero (en ventanas pequeñas se usa la escala que cabe, sin suavizar); **D-63** los animales con sprite se dibujan delante del edificio con una insignia si están hambrientos o enfermos.

**Revisión visual de la 5.7c (HU-44, HU-48 · CA-6 a CA-8).** El estudiante comparó la interfaz con capturas de juegos de granja y pidió más nivel: sin cuadrícula, mejor perspectiva, animaciones y una pantalla de inicio más rica. Cambios:

- **Arte:** `gen_sprites.py` se reescribió con rampas de color, contornos del tono vecino (no negros) y texturas que se repiten sin costura; 118 PNG: 4 variantes de hierba por estación, 3 cuadros de agua, roble y pino por estación (64 × 64), arbusto, valla, y edificios con un tile de tejado encima (Gallinero 64 × 96, Establo 96 × 96, Almacén 64 × 96, Invernadero 96 × 128 con cristal translúcido).
- **Mapa en vista 3/4:** `CapaSuelo` pinta el suelo de la granja y del terreno que la rodea como una sola imagen (en caché mientras no cambian terreno, riego, estación ni cuadro del agua), con bordes irregulares de hierba sobre la tierra y el agua y una sombra corta. `DibujanteMapa` ordena por profundidad árboles, edificios, cultivos y animales, dibuja sus sombras, la valla y el bosque alrededor, y vuelve translúcido lo que tapa un cultivo o la parcela bajo el cursor. El cursor son esquinas de píxel. `EscaladorPixel` hace el *escalado nítido* (entero con vecino más cercano y ajuste final), también con la escala de Windows.
- **Vida ambiental:** `EfectosVisuales.ambienteFijar` (lo activa `PanelMapa`): agua que ondula, copas que se mecen, animales que pasean, sombras de nubes, mariposas u hojas (`PintorAmbiente`, todo función del reloj, sin estado). El temporizador solo corre mientras el panel está en pantalla.
- **Interfaz de madera:** `EstiloMadera`, `CajaMadera`, `BotonMadera`, `RanuraMadera` y `BarraEnergia`; el HUD es un recuadro flotante arriba a la derecha y la barra de herramientas flota abajo. `PanelMapa.agregarFlotante` coloca las parcelas para que ninguna quede debajo de una barra. `UiTheme` aplica la misma paleta de pergamino y madera a todos los diálogos.
- **Pantalla de inicio:** `EscenaTitulo` (cielo, sol, nubes y colinas con paralaje, pájaros, granja con gallinas y granjero que caminan, fundido de entrada) con el nombre del juego animado y botones de madera.
- **Ventana:** abre maximizada y nunca más grande que la parte útil de la pantalla (antes 1100 × 860 no cabía en 1536 × 816 con la escala de 125 %).

Decisiones: **D-64** la vista 3/4 se logra en la UI sin tocar el dominio (los árboles y tejados ocupan visualmente la parcela de arriba, pero las reglas siguen usando solo la huella); **D-65** la animación ambiental no cambia el estado y se apaga con la casilla «Efectos» (RN-08, RN-25).

**Etapa 5.7d (hecha): avatar que camina (HU-45).** 526 pruebas en verde (2 omitidas); cobertura de dominio y aplicación sobre la barrera; verificadores sin errores.

- **Dominio:** `Avatar` (posición, mirada, `paso`), `Direccion` y `InstantaneaAvatar` en `domain.farm`; `MapaGranja.esTransitable` (hierba o tierra, dentro del mapa y sin edificio) y `cercanoTransitable`. `EstadoJuego` contiene al avatar y `avatarPaso`; el `MementoJuego` lo guarda. `ValidadorEstadoJuego` rechaza una partida con el avatar sobre agua, obstáculo o edificio.
- **Guardado:** `CodecGuardado.VERSION_ESQUEMA = 2`; se siguen leyendo documentos de versión 1 (sin `avatar`), que dejan al avatar en (7, 7) o en la parcela transitable más cercana.
- **Aplicación y red:** `ServicioAvatar.mover`, `ComandosJuego.mover(direccion)`, `VistaAvatar`, evento `AvatarMovido`, `VistaMapa.avatar`, `POST /api/avatar/moves` y `MOVIMIENTO_BLOQUEADO` (409). La prueba de equivalencia cubre el paso por HTTP y en proceso.
- **Interfaz:** flechas y `W` `A` `S` `D` caminan (un paso cada 180 ms al mantener la tecla); `Espacio` usa la herramienta en la parcela de enfrente; el clic sigue igual. `EfectosVisuales` anima el paso con 3 cuadros y lo interpola entre parcelas; en reposo muestra un cuadro. `DibujanteMapa` lo ordena por profundidad con su sombra (y un dibujo vectorial si no hay sprites).

Decisiones: **D-66** un paso bloqueado se rechaza con `MOVIMIENTO_BLOQUEADO` pero **cambia la mirada**, por pedido de RN-36; es la única orden rechazada que cambia algo, por eso no publica eventos y la vista vuelve a pedir el mapa; **D-67** no se construye sobre la parcela del avatar (`UBICACION_INVALIDA`), para que nunca quede encerrado dentro de un edificio; **D-68** los NPC aún no bloquean el paso: se suman a `esTransitable` en la 5.7e.

**Etapa 5.7e (hecha): NPC, diálogos y encargos (HU-46, HU-47).** 544 pruebas en verde (2 omitidas); cobertura de dominio y aplicación sobre la barrera; verificadores sin errores.

- **Dominio:** paquete nuevo `domain.npc` con `Npc` (los cuatro personajes, su parcela y su encargo, tomados de `ReglasJuego`) y `LibroEncargo` (encargos cumplidos). `MapaGranja.esTransitable` excluye las parcelas de los NPC; `EstadoJuego` guarda el libro de encargos y el `MementoJuego` lo incluye (`encargosHechos`). `ValidadorEstadoJuego` rechaza una lista con vacíos o repetidos. Dependencias nuevas sin ciclos: `granja → npc`, `juego → npc`, `npc → economy`.
- **Aplicación y red:** `ServicioConversacion` escribe el texto de cada NPC solo a partir del estado (precios, estación y clima, animales, pronóstico) y la línea del encargo; `ServicioNpc` atiende `npcs`, `hablar` y `entregar` (`NPC_LEJOS`, `ENCARGO_INCOMPLETO`, recompensa como ingreso del día y evento `EncargoCompletado`). Rutas `GET /api/npcs`, `POST /api/npcs/{id}/talk` y `POST /api/npcs/{id}/errand`; `VistaMapa.npcs`. La prueba de equivalencia cubre las tres órdenes por HTTP y en proceso.
- **Interfaz:** `E` habla con el NPC de enfrente (o con uno vecino) y el clic sobre un NPC habla con él sea cual sea la herramienta. `DialogoConversacion` es un cuadro de madera abajo, como en un juego de rol: retrato, nombre y rol, texto, encargo y los botones Entregar y Cerrar (lógica en `PresentadorConversacion`). El mapa dibuja a cada NPC con su sombra y, mientras su encargo está pendiente, un globo con el artículo que pide; el aviso «Encargo de … cumplido» sale como notificación.

Decisiones: **D-69** entregar un encargo ya cumplido no es un rechazo: responde con el agradecimiento y no paga (RN-38 «no se repite»); **D-70** no se construye sobre un NPC, pero esa regla vive en `ServicioEdificio` y no en `MapaGranja.puedeColocar`, para que una partida antigua con un edificio en esa parcela siga cargando; **D-71** el esquema sigue en la versión 2: un documento sin `encargosHechos` se lee como «ningún encargo cumplido».

**Etapa 5.7f (hecha): tutorial de inicio guiado por el avatar (HU-49).** 557 pruebas en verde (2 omitidas); cobertura de dominio y aplicación sobre la barrera; verificadores sin errores. Antes del código se documentaron RF-49, RN-39, CU-21 y HU-49 (49 historias, 168 puntos).

- **Dominio:** `Tutorial` y `PasoTutorial` en `domain.progress` (caminar, sembrar, regar, hablar, terminar el día, y `DONE` o `SKIPPED`). Una acción solo cuenta si es la del paso actual; terminado o saltado no vuelve. `EstadoJuego` lo contiene y el `MementoJuego` guarda el paso (`tutorial`).
- **Aplicación y red:** `ServicioTutorial` (texto de cada paso, `tutorial`, `saltar`, `hecho`), `VistaTutorial`, evento `TutorialAvanzado`, `ComandosJuego.tutorial()` y `tutorialSaltar()`, rutas `GET /api/tutorial` y `POST /api/tutorial/skip`. La prueba de equivalencia cubre el tutorial por HTTP y en proceso.
- **Interfaz:** `CajaTutorial` (lógica en `PresentadorTutorial`) flota arriba a la izquierda con el retrato del granjero, «Paso n de 5», la indicación y «Saltar tutorial»; se oculta al terminar y `PresentadorAviso` muestra «¡Listo! Ya sabes lo básico.». `PanelMapa.agregarFlotante` evita que tape parcelas; la prueba de 1024 × 768 sin texto cortado se repite con el cuadro visible.

Decisiones: **D-72** el tutorial avanza desde `ControladorJuego`, después de que la orden fue aceptada y publicó sus eventos, y no desde cada servicio: así ningún servicio depende del tutorial y una orden rechazada nunca lo mueve; **D-73** el esquema sigue en la versión 2: un documento sin `tutorial` se carga con el tutorial **terminado**, porque es una partida que ya se estaba jugando; **D-74** `AplicacionTest.juegoIniciado()` salta el tutorial, para que las pruebas de las demás historias vean solo los eventos de su propia orden; las pruebas de HU-49 crean la partida con el tutorial en curso.

**Etapa 5.7g (hecha): el valle, la cámara y el caminar continuo (HU-50).** 569 pruebas en verde (2 omitidas); cobertura de dominio y aplicación sobre la barrera; verificadores sin errores. El estudiante pidió quitar la zona cercada, que los NPC vivan en una aldea fuera de la granja, parcelas más pequeñas en pantalla y que el avatar siga caminando al mantener la tecla. Antes del código se documentaron RF-50, RN-40, HU-50 (50 historias, 173 puntos) y se reescribió RN-04.

- **Dominio:** `ReglasJuego` describe un valle de 48 × 24 = 1152 parcelas: la granja de antes queda igual en la esquina noroeste (tierra, estanque y los 12 obstáculos en sus parcelas), con 18 obstáculos más, un camino (`RUTAS`) y cinco casas (`CASAS`). `Terreno` suma `PATH` (se camina, no se siembra ni se construye) y `HOUSE` (bloquea); el enum `Casa` da la huella de cada casa. Marta y Don Tomás esperan frente a sus casas al norte del camino; la Dra. Lucía e Inés, en la puerta sur de las suyas.
- **Guardado:** `CodecGuardado.VERSION_ESQUEMA = 3`. Un documento de versión 1 o 2 trae 160 parcelas: `InstantaneaMapaGranja.ensanchado` las pone en la esquina noroeste del valle, de modo que cultivos, edificios y avatar conservan sus coordenadas.
- **Aplicación y red:** `VistaCasa` y `VistaMapa.casas`; `GET /api/farm/map` las incluye. El texto del paso «hablar» del tutorial indica que los vecinos están al este por el camino.
- **Interfaz:** `GeometriaMapa.celdaPara` da parcelas de 48 píxeles de pantalla (38 lógicos con Windows al 125 %), nunca menos de 12 × 8 parcelas visibles; `GeometriaMapa.seguir` centra la cámara en el avatar sin mostrar fuera del valle y la deja dentro de la zona libre de barras. `PanelMapa` sigue la posición interpolada del paso (`EfectosVisuales.posicionAvatar`), así la cámara se desliza sin saltos. `DibujanteMapa` dibuja camino, casas (`village/*.png`, con tejado una parcela por encima) y personas de 32 × 64 con los pies en su parcela; el bosque rodea el valle y ya no hay valla. El suelo se escala con un `EscaladorPixel` propio de 4 copias, para no acumular imágenes grandes. `TeclasCaminar` recuerda las teclas pulsadas: el primer paso sale al pulsar y, mientras se mantiene, uno cada 180 ms exactos sin la pausa de la repetición del sistema; manda la última tecla pulsada y al soltarla se sigue con la que aún esté abajo.

Decisiones: **D-75** las casas del valle son terreno (`HOUSE`) y no edificios del jugador: no se compran, no guardan animales y no entran en el memento, porque son fijas como el estanque; **D-76** el esquema sube a la versión 3 porque cambia el tamaño del mapa guardado, y los documentos viejos se ensanchan al cargar (no se reescriben hasta el siguiente guardado); **D-77** la Dra. Lucía e Inés pasaron a la puerta sur de sus casas: en la vista 3/4 el tejado ocupa la parcela norte y las taparía; **D-78** el ritmo del paso lo marca la interfaz (`TeclasCaminar` con el tiempo de `EfectosVisuales.MS_PASO`), no el dominio: cada paso sigue siendo una orden `mover` normal, así que el modo por red funciona igual.

**Etapa 5.7h (hecha): las partidas se guardan en MongoDB (HU-29 a HU-32).** 576 pruebas en verde; cobertura de dominio y aplicación sobre la barrera; verificadores sin errores.

- **Diagnóstico de «no guarda nada»:** el código cumplía RN-31: en esta máquina **no había ningún MongoDB** (nada en el puerto 27017, sin servicio ni Docker), así que guardar devolvía `ERROR_ES` y la partida seguía. Además, la pantalla de juego no tenía ninguna entrada visible para guardar: solo `F5` y `F9`, y «Menú» vuelve al inicio sin guardar.
- **Corrección:** se instaló **MongoDB Community Server 8.0.23** como servicio de Windows de arranque automático (`winget instalar MongoDB.Server --version 8.0.23`). La 8.0 y no la 9 porque el driver de Java de Spring Boot 3.5 (5.5.2) admite servidores hasta la 8.x. La URI por defecto (`mongodb://localhost:27017/valleverde`) no cambia. La pantalla de juego suma un botón **«Guardar»** junto a «Terminar día» y «Menú» que siempre abre las ranuras (CU-14 paso 1), con `F5`/`F9` en su ayuda.
- **Prueba con MongoDB real:** `HU29MongoRealTest` trabaja en la base `valleverde_test` (se borra antes y después de cada prueba, así nunca toca las partidas del jugador) y se omite sola si no hay MongoDB en el equipo. Cubre: el documento queda de verdad en `partidas` con versión, fecha y checksum; un MongoDB que **rechaza** la escritura (un validador de colección) da `ERROR_ES` y deja el documento previo intacto; uno que **no responde** falla en menos de 3 s; y una partida guardada se carga idéntica en otra sesión.
- **Verificación manual:** con el `.jar`, se guardó una partida en la ranura 1, se cerró la aplicación, se volvió a abrir y «Cargar» devolvió el mismo día, clima y dinero.

Decisiones: **D-79** no se agrega un respaldo en archivos: el estudiante eligió instalar MongoDB, que es el almacenamiento que pide RN-31; sin MongoDB el comportamiento sigue siendo el de RN-31 (`ERROR_ES`, la partida sigue); **D-80** la prueba real se omite, en vez de fallar, en un equipo sin MongoDB, igual que las pruebas que necesitan pantalla.

**Etapa 5.7i (hecha): cierre de la Fase 5.7.** 581 pruebas en verde; cobertura de dominio 97 % y de aplicación 96 % (barrera 70 %); `check_hu`, `check_diagrams`, `check_imports`, `traceability --upto 5.7` y `gen_sprites --check` sin errores; README al día (estado, cómo se juega, MongoDB, totales de la documentación).

- **Arranque (RNF-02, RT-09):** medido con 5 arranques del `.jar`, la pantalla de inicio tardaba **5,5 a 6,7 s**. Con marcas de tiempo se vio que el marco estaba listo a los 1,5 s, pero `EnrutadorPantalla` construía la pantalla de juego desde el principio y sus paneles (`PanelMapa`, `PresentadorTutorial`) le piden el mapa y el tutorial al servidor, así que la ventana esperaba a Spring (3,3 a 4,9 s). Cambios:
  - `Main` arranca el servidor en un hilo propio (`server-start`) y abre la ventana a la vez; si el servidor no puede arrancar, avisa y termina con código 3.
  - `ClienteJuegoRemoto` acepta la dirección del servidor como `CompletableFuture<URI>`: una orden enviada antes de que el servidor esté listo espera hasta 30 s, y un servidor que falla es `ERROR_INTERNO`, nunca una espera infinita.
  - `EnrutadorPantalla` muestra la pantalla de inicio sin consultar al servidor y construye la pantalla de juego cuando el servidor responde, o antes si el jugador pulsa «Nueva partida» o «Cargar» (`NavegadorPantalla.juegoPreparar()`, que `PresentadorPantallaInicio` llama antes de su primera orden), para que el HUD reciba el `JuegoIniciado`.
  - Resultado: la pantalla de inicio aparece en **2,0 a 2,4 s** (5 arranques) y el servidor está listo a los 3,8 a 4,9 s, en paralelo. Un clic en «Nueva partida» apenas aparece la ventana funciona: espera al servidor y abre la partida con el HUD completo.

Decisiones: **D-81** RNF-02 se mide hasta la pantalla de inicio, como dice su texto, y no hasta «servidor listo»: la pantalla de inicio no necesita al servidor hasta el primer clic; **D-82** la pantalla de juego se construye cuando hay servidor en vez de hacer asíncronas las lecturas iniciales de cada panel: un solo punto de cambio (`EnrutadorPantalla`) y los paneles no cambian.

**Corrección posterior (caminar trabado):** la causa no era `TeclasCaminar` sino que cada paso, al recibir `AvatarMovido`, volvía a pedir el valle completo al servidor (`GET /api/farm/map`: unos 131 KB de JSON y ~100 ms en el hilo de Swing, cada 180 ms), y caminar contra un obstáculo lo pedía en cada intento. **D-83** `PanelMapa` mueve o gira solo al avatar de la vista que ya tiene (`VistaMapa.conAvatar`, `PanelMapa.cara`), porque un paso no cambia nada más del valle; el mapa completo se sigue pidiendo cuando cambian parcelas, edificios, el día o los encargos. Prueba `HU50_ca5_aStepMovesTheAvatarOnTheShownMapWithoutDownloadingTheWholeValleyAgain`. Al caminar **hacia arriba y hacia abajo** seguía el tirón: el suelo se pintaba como una imagen de las parcelas visibles y se guardaba según esos límites, así que cada fila que avanzaba la cámara obligaba a repintarlo y reescalarlo (la vista se quedaba quieta ~100 ms y luego saltaba hasta 20 px, a veces hacia atrás; en horizontal casi no se notaba porque al inicio la cámara está pegada al borde oeste). **D-84** el suelo se pinta una vez para todo el valle más un borde de 8 parcelas de bosque y la cámara solo lo desliza (`HU50_ca5_aMovingCameraSlidesTheGroundAlreadyPaintedInsteadOfPaintingItAgainOnEveryRow`); **D-85** mientras el avatar camina el mapa se redibuja cada 16 ms (60 cuadros/s; un cuadro tarda ~7 ms) y en reposo cada 33 ms. Medido con grabación de pantalla: antes saltos de 8–20 px con pausas; después 4–5 px cada ~14 ms.
