# Requerimientos — Valle Verde

> **Versión 0.1 · 2026-11-22 · Fase 1 (Análisis)**
> IDs trazables: `RF-xx` funcionales · `RNF-xx` no funcionales · `RN-xx` reglas de negocio ([`REGLAS_DE_NEGOCIO.md`](REGLAS_DE_NEGOCIO.md)) · `CU-xx` casos de uso ([`CASOS_DE_USO.md`](CASOS_DE_USO.md)).
> Los valores numéricos viven **solo** en las reglas de negocio; aquí se citan por ID.

## 1. Priorización (MoSCoW)

| Prioridad | Significado | Cantidad |
|---|---|---|
| **Must** | Imprescindible: forma el MVP | 30 |
| **Should** | Importante; se incluye si el plazo lo permite | 8 |
| **Could** | Deseable; primera candidata a recortarse | 4 |
| **Won't** | Fuera de esta versión (ver §5) | — |

Los requerimientos se agrupan en las **6 épicas** que también organizarán las historias de usuario: *Cultivo*, *Tiempo y clima*, *Economía*, *Construcción y animales*, *Progresión y persistencia* e *Interfaz*.

## 2. Requerimientos funcionales

### Épica A · Cultivo

| ID | Requerimiento | Prioridad | RN |
|---|---|---|---|
| RF-01 | El jugador podrá **sembrar** una semilla del inventario en una parcela de tierra libre, siempre que la estación sea válida para ese cultivo, y el sistema consumirá la semilla y la energía correspondientes | Must | RN-02, RN-05, RN-06, RN-15 |
| RF-02 | El jugador podrá **regar** un cultivo vivo; el sistema marcará el cultivo como regado hoy y consumirá la energía que corresponda al clima | Must | RN-02, RN-07, RN-12 |
| RF-03 | El sistema **hará crecer** cada día a los cultivos regados y los hará avanzar por las etapas Semilla, Brote, Crecimiento y Maduro | Must | RN-03, RN-07, RN-08 |
| RF-04 | El jugador podrá **cosechar** un cultivo maduro y recibir el producto en el inventario; si el inventario está lleno, la cosecha se rechazará sin perder el cultivo ni gastar energía | Must | RN-02, RN-10, RN-15 |
| RF-05 | El sistema **marchitará** los cultivos por falta de riego, por helada o por cambio a una estación no válida | Must | RN-01, RN-09, RN-12 |
| RF-06 | El jugador podrá **limpiar** una parcela con cultivo marchito para volver a usarla | Should | RN-02, RN-10 |
| RF-07 | El jugador podrá **consultar el estado de una parcela**: cultivo, etapa, regado y días para madurar | Must | RN-05, RN-08, RN-09 |

### Épica B · Tiempo y clima

| ID | Requerimiento | Prioridad | RN |
|---|---|---|---|
| RF-08 | El jugador podrá **terminar el día**; el sistema resolverá todos los efectos en el orden definido y avanzará un turno | Must | RN-02, RN-03 |
| RF-09 | El sistema **llevará el calendario**: 4 estaciones, años y cambio de estación con sus consecuencias | Must | RN-01, RN-09 |
| RF-10 | El sistema **sorteará el clima** de cada día según la estación y mostrará siempre el **pronóstico** del día siguiente | Must | RN-11, RN-13 |
| RF-11 | El sistema **aplicará los efectos del clima** a los cultivos: lluvia que riega, sequía que reduce la tolerancia y encarece el riego, y helada que marchita | Must | RN-07, RN-09, RN-12 |
| RF-12 | El sistema **administrará la energía diaria**: descontará el costo de cada acción, rechazará las acciones sin energía suficiente y la restaurará al terminar el día | Must | RN-02 |

### Épica C · Economía

| ID | Requerimiento | Prioridad | RN |
|---|---|---|---|
| RF-13 | El sistema **administrará el inventario** con casillas, pilas máximas y operaciones atómicas de insertar y retirar | Must | RN-15 |
| RF-14 | El jugador podrá **comprar** semillas, heno, medicina y animales a precio fijo, con validación de saldo y de espacio | Must | RN-14, RN-15, RN-16 |
| RF-15 | El jugador podrá **vender** cosechas y productos animales a un **precio dinámico** que depende de la temporada y de la saturación, y que se recupera con los días | Must | RN-14, RN-17, RN-18 |
| RF-16 | El sistema **mostrará la tendencia** de cada precio (sube, baja o igual) | Should | RN-19 |
| RF-17 | El sistema **resumirá el día** al terminarlo: ingresos, gastos y eventos relevantes | Could | RN-03, RN-14 |

### Épica D · Construcción y animales

| ID | Requerimiento | Prioridad | RN |
|---|---|---|---|
| RF-18 | El jugador podrá **construir** edificios (Gallinero y Establo como mínimo) sobre hierba libre, validando saldo, desbloqueo y límite por tipo | Must | RN-04, RN-14, RN-20, RN-21, RN-29 |
| RF-19 | El jugador podrá **comprar animales** que se alojarán en un edificio compatible con plaza libre | Must | RN-14, RN-20, RN-23 |
| RF-20 | El jugador podrá **alimentar** a los animales de un edificio consumiendo heno y energía | Must | RN-02, RN-15, RN-24 |
| RF-21 | El sistema **gestionará el estado** de cada animal (Sano, Hambriento, Enfermo) según se lo alimente | Must | RN-25 |
| RF-22 | El sistema **hará producir** a los animales sanos y alimentados, y el jugador podrá **recolectar** esos productos hacia el inventario | Must | RN-15, RN-26 |
| RF-23 | El jugador podrá **curar** a un animal enfermo con medicina | Should | RN-16, RN-27 |
| RF-24 | El jugador podrá construir un **invernadero** para cultivar en cualquier estación, protegido de helada y sequía | Could | RN-20, RN-22 |
| RF-25 | El jugador podrá construir un **almacén** que amplíe la capacidad del inventario | Should | RN-15, RN-20 |

### Épica E · Progresión y persistencia

| ID | Requerimiento | Prioridad | RN |
|---|---|---|---|
| RF-26 | El sistema **registrará logros** al cumplirse sus condiciones y abonará su recompensa una sola vez | Must | RN-28 |
| RF-27 | El sistema **desbloqueará edificios** al conseguir los logros asociados | Should | RN-29 |
| RF-28 | El jugador podrá iniciar una **nueva partida** con el estado inicial definido | Must | RN-30 |
| RF-29 | El jugador podrá **guardar** la partida en una de 3 ranuras de forma atómica | Must | RN-31, RN-32 |
| RF-30 | El jugador podrá **cargar** una partida guardada y recuperar un estado idéntico al guardado | Must | RN-32, RN-33 |
| RF-31 | El sistema **rechazará** partidas dañadas, de versión no soportada o inexistentes con un mensaje claro, **sin alterar la partida actual** | Must | RN-33 |
| RF-32 | El sistema **listará las ranuras** con sus metadatos (fecha del juego, dinero y fecha real de guardado) | Should | RN-31 |

### Épica F · Interfaz

| ID | Requerimiento | Prioridad | RN |
|---|---|---|---|
| RF-33 | El sistema ofrecerá una **pantalla de inicio** con *Nueva partida*, *Cargar* y *Salir* | Must | RN-30, RN-31 |
| RF-34 | La pantalla principal mostrará un **HUD** con dinero, día, estación, clima y barra de energía | Must | RN-01, RN-02, RN-11, RN-14 |
| RF-35 | El sistema **dibujará el mapa** de la granja con parcelas, cultivos por etapa, edificios y animales | Must | RN-04, RN-08, RN-20 |
| RF-36 | El jugador podrá **elegir una herramienta** (por barra o atajo) y **actuar con clic** sobre una parcela, resaltada al pasar el cursor, con *tooltip* de estado | Must | RN-02, RN-34 |
| RF-37 | El sistema mostrará el **inventario como grilla** de casillas con icono y cantidad | Must | RN-15 |
| RF-38 | El sistema ofrecerá un **mercado modal** con pestañas Comprar y Vender, precio actual con tendencia y cantidad ajustable | Must | RN-16, RN-17, RN-19 |
| RF-39 | El sistema mostrará **notificaciones** (*toasts*) para logros, errores y eventos de estación | Should | RN-35 |
| RF-40 | El sistema aplicará **efectos visuales**: tinte día/noche, paleta por estación, partículas de lluvia y nieve, indicadores de sequía y helada | Could | RN-01, RN-11, RN-12 |
| RF-41 | El sistema aplicará **animaciones** suaves (crecimiento, animales de 2 a 3 cuadros y transición de día) sin que el estado del juego dependa de ellas | Could | RN-08, RN-25 |
| RF-42 | El sistema cargará **sprites** mediante un `AssetManager` con caché y usará **gráficos vectoriales de respaldo** si faltan | Should | RN-08, RN-25 |
| RF-43 | La interfaz usará el tema visual **FlatLaf** con una tipografía única y, si el tema no se puede instalar, el aspecto por defecto de Swing | Should | RN-34 |
| RF-44 | El sistema incluirá **sprites pixel-art propios de 32 × 32 px** (terrenos, cultivos por etapa, animales, edificios, ítems, avatar y NPC) y los ampliará por factor entero sin desenfocarlos | Should | RN-08, RN-25 |
| RF-45 | El jugador podrá mover un **avatar** con el teclado por el mapa, con colisiones, y usar la herramienta elegida sobre la parcela que tiene enfrente | Should | RN-36 |
| RF-46 | El jugador podrá **hablar con los NPC** que estén junto a su avatar y leer diálogos en español que dependen del estado del juego | Should | RN-37 |
| RF-47 | Los NPC ofrecerán **encargos** de un solo uso que el jugador cumple entregando artículos a cambio de una recompensa en m | Could | RN-38 |
| RF-48 | La interfaz tendrá aspecto de granja: barra de herramientas con iconos, HUD con iconos e inventario con los sprites de los ítems | Should | RN-34 |
| RF-49 | Una partida nueva empezará con un **tutorial** de 5 pasos guiado por el avatar (caminar, sembrar, regar, hablar con un NPC y terminar el día), que se puede saltar y se guarda con la partida | Should | RN-39 |

**Totales:** 49 RF → 30 *Must* · 14 *Should* · 5 *Could* (los RF-43 a RF-49 se agregaron en la Fase 5.7 por pedido expreso del estudiante).

## 3. Requerimientos no funcionales (medibles)

Referencia de hardware: equipo de desarrollo con Windows 11, ≥ 8 GB de RAM y JRE 17 o superior.

| ID | Categoría | Requerimiento | Métrica / criterio de verificación |
|---|---|---|---|
| RNF-01 | Rendimiento | «Terminar día» es rápido incluso con la granja a plena carga (40 cultivos al aire libre, 9 en invernadero, 6 edificios y 16 animales) | ≤ 200 ms (percentil 95 sobre 100 ejecuciones, prueba automática) |
| RNF-02 | Rendimiento | El juego arranca rápido | ≤ 5 s desde `java -jar` (incluye el arranque del servidor Spring Boot) hasta la pantalla de inicio |
| RNF-03 | Rendimiento | Guardar y cargar son rápidos y ligeros | ≤ 1 s por operación; documento ≤ 200 KB |
| RNF-04 | Rendimiento | La interfaz es fluida | ≥ 30 FPS en el mapa con animaciones; la respuesta visible a un clic llega en ≤ 100 ms |
| RNF-05 | Calidad | Pruebas suficientes y verdes | Cobertura de líneas ≥ 70 % (JaCoCo) en dominio y aplicación; las 7 pruebas críticas en verde con `mvn test` |
| RNF-06 | Arquitectura | Dependencias en un solo sentido: servidor `api → application → domain` e `infrastructure → domain`; cliente `ui → application` y `client → application` | 0 violaciones en una prueba automática que revisa los `import`: el dominio no importa Swing, AWT, Spring, Jackson, MongoDB ni E/S; la UI no importa `domain`, `infrastructure` ni `api`; los controladores REST no importan `domain` |
| RNF-07 | Arquitectura | Ninguna excepción llega a la interfaz: `GameController` devuelve siempre un `Result` | 0 excepciones no controladas en las pruebas de aceptación; todo rechazo trae código y mensaje (RN-35) |
| RNF-08 | Determinismo | Toda aleatoriedad usa un `Random` inyectado | Misma semilla + mismas acciones → mismo estado final tras simular 112 días (prueba automática) |
| RNF-09 | Fiabilidad | La persistencia no pierde ni daña datos | Una carga fallida deja la partida actual con 0 diferencias; el guardado es atómico (el documento previo sobrevive a un fallo) |
| RNF-10 | Usabilidad | Interfaz clara y en español | Toda acción de juego alcanzable en ≤ 2 clics o 1 atajo; 100 % de los textos visibles en español; todo error indica causa y qué hacer |
| RNF-11 | Portabilidad | Se ejecuta en otros equipos | `mvn clean package` genera un `.jar` ejecutable con `java -jar` en Windows 10+ y JRE 17+; usable desde 1024 × 768 sin recortes; **jugar no requiere MongoDB** (solo guardar y cargar: sin conexión responden `ERROR_ES` y la partida sigue) |
| RNF-12 | Mantenibilidad | Sin números mágicos y nombre configurable | 0 valores de RN fuera de constantes o de la clase de configuración; el nombre visible del juego está en **una** constante |
| RNF-13 | Mantenibilidad | Los patrones de diseño están documentados | 100 % de las clases que implementan un patrón llevan el Javadoc `Pattern: <nombre> — Role: <rol>`; 100 % de los commits de funcionalidad citan una `HUxx` |
| RNF-14 | Legal y accesibilidad | Assets con licencia libre y lectura cómoda | 100 % de los assets CC0 o CC-BY, registrados en `docs/ASSETS.md` y en el README; el juego ejecuta **sin ningún archivo de imagen** (respaldo vectorial); contraste de texto ≥ 4,5 : 1 y ninguna información transmitida solo por color |
| RNF-15 | Interoperabilidad | La lógica se expone como API REST JSON: recursos con sustantivos, verbos HTTP y códigos de estado coherentes (200, 201, 400, 404, 409, 422, 500, 503) | Cada orden del cliente corresponde a un endpoint documentado en `docs/API.md`; una prueba de integración por endpoint verifica el código de éxito y el de cada rechazo |
| RNF-16 | Despliegue | Servidor y cliente corren en el mismo equipo y el servidor solo acepta conexiones locales | El servidor escucha en 127.0.0.1; un solo `java -jar` arranca ambos; admite también `--server` y `--client` por separado |

## 4. Trazabilidad RN → RF

> Cada regla de negocio la usa al menos un requerimiento; ninguna queda huérfana.

| RN | Regla | Requerimientos que la usan |
|---|---|---|
| RN-01 | Calendario | RF-05, RF-09, RF-34, RF-40 |
| RN-02 | Energía diaria | RF-01, RF-02, RF-04, RF-06, RF-08, RF-12, RF-20, RF-34, RF-36 |
| RN-03 | Orden de resolución del fin de día | RF-03, RF-08, RF-17 |
| RN-04 | Mapa de la granja | RF-18, RF-35 |
| RN-05 | Catálogo de cultivos | RF-01, RF-07 |
| RN-06 | Siembra | RF-01 |
| RN-07 | Riego | RF-02, RF-03, RF-11 |
| RN-08 | Crecimiento y etapas | RF-03, RF-07, RF-35, RF-41, RF-42, RF-44 |
| RN-09 | Marchitamiento | RF-05, RF-07, RF-09, RF-11 |
| RN-10 | Cosecha y limpieza | RF-04, RF-06 |
| RN-11 | Clima por estación | RF-10, RF-34, RF-40 |
| RN-12 | Efectos del clima | RF-02, RF-05, RF-11, RF-40 |
| RN-13 | Pronóstico y clima inicial | RF-10 |
| RN-14 | Dinero | RF-14, RF-15, RF-17, RF-18, RF-19, RF-34 |
| RN-15 | Inventario | RF-01, RF-04, RF-13, RF-14, RF-20, RF-22, RF-25, RF-37 |
| RN-16 | Catálogo de compra (precio fijo, disponible todo el año) | RF-14, RF-23, RF-38 |
| RN-17 | Precio de venta dinámico | RF-15, RF-38 |
| RN-18 | Saturación y recuperación del mercado | RF-15 |
| RN-19 | Tendencia de precio | RF-16, RF-38 |
| RN-20 | Catálogo de edificios | RF-18, RF-19, RF-24, RF-25, RF-35 |
| RN-21 | Colocación y límites | RF-18 |
| RN-22 | Invernadero | RF-24 |
| RN-23 | Catálogo de animales | RF-19 |
| RN-24 | Alimentación | RF-20 |
| RN-25 | Estados del animal | RF-21, RF-41, RF-42, RF-44 |
| RN-26 | Producción y recolección | RF-22 |
| RN-27 | Curación | RF-23 |
| RN-28 | Logros | RF-26 |
| RN-29 | Desbloqueo de edificios | RF-18, RF-27 |
| RN-30 | Partida nueva | RF-28, RF-33 |
| RN-31 | Ranuras de guardado | RF-29, RF-32, RF-33 |
| RN-32 | Formato e integridad del documento | RF-29, RF-30 |
| RN-33 | Validación de carga | RF-30, RF-31 |
| RN-34 | Herramientas y atajos | RF-36, RF-43, RF-48 |
| RN-35 | Mensajes y notificaciones | RF-39 |
| RN-36 | Avatar y movimiento | RF-45 |
| RN-37 | Personajes (NPC) y diálogos | RF-46 |
| RN-38 | Encargos de los NPC | RF-47 |
| RN-39 | Tutorial de inicio | RF-49 |

## 5. Fuera de alcance (*Won't*)

| ID | Elemento | Motivo |
|---|---|---|
| W-01 | Multijugador y juego por Internet | Fuera del alcance de diseño; servidor y cliente en un solo equipo |
| W-02 | Audio (música y efectos de sonido) | No aporta a la evaluación del diseño |
| W-03 | Servicios externos y nube | Solo MongoDB guarda partidas; sin cuentas ni otros servicios |
| ~~W-04~~ | ~~NPC, diálogos y misiones de personajes~~ | **Incluido desde la Fase 5.7** por pedido expreso del estudiante (RF-46, RF-47) |
| ~~W-05~~ | ~~Avatar que camina por el mapa~~ | **Incluido desde la Fase 5.7** por pedido expreso del estudiante (RF-45); el clic sobre la parcela sigue funcionando |
| W-06 | Tiempo real | El juego avanza únicamente por turnos |
| W-07 | Cuentas de usuario, *mods*, tienda en línea | Sin cuentas ni servicios externos |

## 6. Siguiente paso

Con este documento y las reglas aprobadas, la **Fase 2** redactará el backlog de historias de usuario (épicas, MoSCoW, puntos Fibonacci, criterios Dado/Cuando/Entonces) y completará la trazabilidad `RF → HU → caso de uso`.
