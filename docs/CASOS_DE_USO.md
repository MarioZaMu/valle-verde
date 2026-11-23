# Casos de uso — Valle Verde

> **Versión 0.1 · 2026-11-22 · Fase 1 (Análisis)**
> Descripción textual (sin diagrama de casos de uso, que no se entrega). Cada caso cita los `RF` que cubre y las `RN` que aplica.
> **Actor primario:** el **Jugador**. **Actor secundario:** **MongoDB** (CU-14 y CU-15) y el **Paquete de assets** (CU-18).
> Convención de los flujos alternos: `A#` = alternativa en el paso indicado; todo rechazo devuelve un código y un mensaje en español (RN-35) y **no modifica el estado**.

## Índice

| ID | Caso de uso | Épica | RF |
|---|---|---|---|
| CU-01 | Iniciar nueva partida | Progresión | RF-28, RF-33 |
| CU-02 | Sembrar cultivo | Cultivo | RF-01, RF-12 |
| CU-03 | Regar cultivo | Cultivo | RF-02 |
| CU-04 | Cosechar o limpiar parcela | Cultivo | RF-04, RF-06 |
| CU-05 | Consultar estado de una parcela | Cultivo | RF-07 |
| CU-06 | Terminar día | Tiempo y clima | RF-03, RF-05, RF-08, RF-09, RF-10, RF-11, RF-12, RF-17, RF-21, RF-22, RF-26 |
| CU-07 | Comprar insumos en el mercado | Economía | RF-14, RF-38 |
| CU-08 | Vender en el mercado | Economía | RF-15, RF-16, RF-38 |
| CU-09 | Construir edificio | Construcción | RF-18, RF-24, RF-25, RF-27 |
| CU-10 | Comprar animal | Construcción | RF-14, RF-19 |
| CU-11 | Alimentar animales | Animales | RF-20, RF-21 |
| CU-12 | Recolectar productos de un edificio | Animales | RF-22 |
| CU-13 | Curar animal | Animales | RF-23 |
| CU-14 | Guardar partida | Persistencia | RF-29, RF-32 |
| CU-15 | Cargar partida | Persistencia | RF-30, RF-31, RF-32 |
| CU-16 | Consultar logros | Progresión | RF-26, RF-27 |
| CU-17 | Consultar inventario | Economía | RF-13, RF-37 |
| CU-18 | Observar la granja | Interfaz | RF-10, RF-34, RF-35, RF-36, RF-39, RF-40, RF-41, RF-42, RF-43, RF-44, RF-48 |
| CU-19 | Caminar por el valle | Mundo | RF-45, RF-50 |
| CU-20 | Hablar con un NPC | Mundo | RF-46, RF-47 |
| CU-21 | Seguir el tutorial de inicio | Mundo | RF-49 |

---

## CU-01 · Iniciar nueva partida
- **Actor:** Jugador · **RF:** RF-28, RF-33 · **RN:** RN-30, RN-13, RN-04
- **Precondición:** la aplicación está en la pantalla de inicio.
- **Flujo principal:**
  1. El Jugador elige *Nueva partida*.
  2. El sistema crea el estado inicial de RN-30 (Año 1, Primavera 1, 500 m, 100 de energía, 10 semillas de Zanahoria y 5 de Papa, mapa de RN-04).
  3. El sistema sortea el pronóstico del día 2 (RN-13).
  4. El sistema muestra la pantalla principal con el HUD y el mapa.
- **Alternativas:** **A3.** Si ya había una partida en curso, la interfaz pide confirmación antes de descartarla.
- **Postcondición:** hay una partida en curso con el estado inicial.

## CU-02 · Sembrar cultivo
- **Actor:** Jugador · **RF:** RF-01, RF-12 · **RN:** RN-02, RN-05, RN-06, RN-15, RN-22
- **Precondición:** hay una partida en curso y la herramienta *Sembrar* con una semilla seleccionada.
- **Flujo principal:**
  1. El Jugador hace clic en una parcela de tierra.
  2. El sistema verifica parcela libre, estación válida, semilla disponible y energía ≥ 2.
  3. El sistema consume 1 semilla y 2 de energía y crea el cultivo en etapa *Semilla*.
  4. El sistema publica el evento de cultivo sembrado y el mapa se actualiza.
- **Alternativas:**
  - **A2a.** La parcela no es de tierra o está ocupada → `PARCELA_NO_CULTIVABLE` / `PARCELA_OCUPADA`.
  - **A2b.** La estación no es válida para el cultivo (por ejemplo, Calabaza en Primavera) → `FUERA_DE_ESTACION`. En invernadero no aplica (RN-22).
  - **A2c.** No hay semillas → `SIN_SEMILLAS`.
  - **A2d.** Energía < 2 → `SIN_ENERGIA`.
- **Postcondición:** la parcela contiene un cultivo en etapa *Semilla* y el inventario y la energía se redujeron.

## CU-03 · Regar cultivo
- **Actor:** Jugador · **RF:** RF-02 · **RN:** RN-02, RN-07, RN-12
- **Precondición:** hay un cultivo vivo en la parcela y la herramienta *Regar* seleccionada.
- **Flujo principal:**
  1. El Jugador hace clic en la parcela.
  2. El sistema verifica cultivo vivo, no regado y energía suficiente (1; 2 si el clima es Sequía).
  3. El sistema marca el cultivo como regado y descuenta la energía.
- **Alternativas:** **A2a.** Sin cultivo → `SIN_CULTIVO`. **A2b.** Ya regado → `YA_REGADO` (sin gasto de energía). **A2c.** Cultivo marchito → se rechaza. **A2d.** Energía insuficiente → `SIN_ENERGIA`.
- **Postcondición:** el cultivo queda regado hasta el fin del día.

## CU-04 · Cosechar o limpiar parcela
- **Actor:** Jugador · **RF:** RF-04, RF-06 · **RN:** RN-02, RN-10, RN-15, RN-28
- **Precondición:** herramienta *Cosechar* seleccionada.
- **Flujo principal (cultivo maduro):**
  1. El Jugador hace clic en una parcela con cultivo **Maduro**.
  2. El sistema verifica energía ≥ 2 y espacio para 1 unidad en el inventario.
  3. El sistema inserta el producto, libera la parcela, descuenta 2 de energía y cuenta la cosecha para los logros.
- **Flujo alterno (cultivo marchito):** el sistema cobra 1 de energía, libera la parcela y no entrega producto.
- **Alternativas:**
  - **A2a.** Inventario lleno → `INVENTARIO_LLENO`; el cultivo **sigue Maduro** y no se gasta energía.
  - **A2b.** Cultivo no maduro → `NO_MADURO` con los días que faltan.
  - **A2c.** Energía insuficiente → `SIN_ENERGIA`.
- **Postcondición:** la parcela queda libre y el inventario incluye el producto, o bien el estado queda intacto.

## CU-05 · Consultar estado de una parcela
- **Actor:** Jugador · **RF:** RF-07 · **RN:** RN-05, RN-08, RN-09, RN-34
- **Flujo principal:**
  1. El Jugador deja el cursor 300 ms sobre una parcela.
  2. El sistema muestra un *tooltip* con cultivo, etapa, regado sí/no y días para madurar (`T − d`).
- **Alternativas:** **A2.** Parcela sin cultivo → muestra solo el tipo de terreno. Cultivo marchito → muestra «Marchito».
- **Postcondición:** no hay cambios de estado (consulta pura).

## CU-06 · Terminar día
- **Actor:** Jugador · **RF:** RF-03, RF-05, RF-08, RF-09, RF-10, RF-11, RF-12, RF-17, RF-21, RF-22, RF-26 · **RN:** RN-01, RN-02, RN-03, RN-07 a RN-09, RN-11 a RN-13, RN-18, RN-19, RN-25, RN-26, RN-28
- **Precondición:** hay una partida en curso.
- **Flujo principal:** el sistema ejecuta, en este orden, los 9 pasos de RN-03:
  1. Aplica el clima del día (lluvia riega; helada marchita).
  2. Hace crecer los cultivos regados y marchita los que sufren sequedad.
  3. Resuelve los animales (estado y producción).
  4. Guarda los precios anteriores y recupera la demanda del mercado.
  5. Avanza el calendario; si cambia la estación, marchita los cultivos fuera de estación.
  6. Promueve el pronóstico a clima del nuevo día y sortea el siguiente.
  7. Restaura la energía a 100 y limpia las marcas de riego.
  8. Evalúa los logros y abona recompensas.
  9. Publica los eventos y la interfaz muestra el resumen del día.
- **Alternativas:**
  - **A1.** Día de **helada** → todos los cultivos al aire libre pasan a *Marchito*, incluidos los maduros sin cosechar.
  - **A2.** Tres días seguidos sin regar (dos con Sequía) → el cultivo se marchita.
  - **A3.** Animal sin alimentar 3 días seguidos → pasa a *Enfermo* y deja de producir.
  - **A5.** Día 28 de Invierno → empieza el Año siguiente; se concede LG-09 al llegar al Año 2.
- **Postcondición:** el juego está en el día siguiente con energía 100 y todas las reglas resueltas, o bien nada cambió si hubo un error interno.

## CU-07 · Comprar insumos en el mercado
- **Actor:** Jugador · **RF:** RF-14, RF-38 · **RN:** RN-14, RN-15, RN-16
- **Precondición:** el mercado está abierto en la pestaña *Comprar*.
- **Flujo principal:**
  1. El Jugador elige un producto (semilla, heno o medicina) y ajusta la cantidad (1 a 99).
  2. El sistema calcula el total = precio × cantidad.
  3. El sistema verifica saldo y espacio en el inventario.
  4. El sistema descuenta el dinero e inserta los ítems.
- **Alternativas:** **A3a.** Saldo insuficiente → `SIN_FONDOS` con lo que falta. **A3b.** No cabe toda la cantidad → `INVENTARIO_LLENO` (no se compra nada).
- **Postcondición:** el dinero baja y el inventario sube en la cantidad comprada.

## CU-08 · Vender en el mercado
- **Actor:** Jugador · **RF:** RF-15, RF-16, RF-38 · **RN:** RN-14, RN-17, RN-18, RN-19, RN-28
- **Precondición:** el mercado está abierto en la pestaña *Vender* y el Jugador tiene productos.
- **Flujo principal:**
  1. El Jugador elige un producto y una cantidad no mayor que su existencia.
  2. El sistema muestra el precio actual y su tendencia (▲ ▼ ▬).
  3. El sistema vende unidad por unidad: calcula el precio con la demanda vigente (RN-17) y luego resta 2 puntos de demanda (RN-18).
  4. El sistema retira las unidades del inventario y acredita el total.
  5. El sistema cuenta las unidades para el logro LG-04.
- **Alternativas:** **A2.** Cantidad mayor que la existencia → se limita a lo disponible. **A3.** La demanda llega a su piso de 50 → el precio deja de bajar.
- **Postcondición:** sube el dinero, baja el inventario y baja la demanda del producto vendido.

## CU-09 · Construir edificio
- **Actor:** Jugador · **RF:** RF-18, RF-24, RF-25, RF-27 · **RN:** RN-04, RN-14, RN-20, RN-21, RN-22, RN-29
- **Precondición:** herramienta *Construir* seleccionada.
- **Flujo principal:**
  1. El Jugador abre el catálogo y elige un edificio desbloqueado.
  2. El Jugador hace clic en la parcela que será la esquina superior izquierda.
  3. El sistema verifica área de hierba libre dentro del mapa, límite por tipo y saldo.
  4. El sistema descuenta el costo, ocupa las parcelas (el invernadero las vuelve de cultivo protegido; el almacén suma 12 casillas) y cuenta el logro LG-06.
- **Alternativas:** **A1.** Edificio bloqueado → `EDIFICIO_BLOQUEADO` con el logro requerido. **A3a.** Área inválida → `UBICACION_INVALIDA`. **A3b.** Máximo alcanzado → `LIMITE_EDIFICIOS`. **A3c.** Saldo insuficiente → `SIN_FONDOS`.
- **Postcondición:** el edificio existe en el mapa y el dinero bajó; si falló, no cambió nada.

## CU-10 · Comprar animal
- **Actor:** Jugador · **RF:** RF-14, RF-19 · **RN:** RN-14, RN-20, RN-23
- **Precondición:** existe al menos un edificio compatible (Gallinero para gallinas, Establo para vacas).
- **Flujo principal:**
  1. El Jugador elige el animal en la pestaña *Comprar* del mercado.
  2. El sistema busca el primer edificio compatible con plaza libre.
  3. El sistema descuenta el precio y aloja al animal (Sano, `díasSinAlimento = 0`).
- **Alternativas:** **A2.** No hay edificio compatible o todos están llenos → `SIN_ESPACIO_ANIMALES`. **A3.** Saldo insuficiente → `SIN_FONDOS`.
- **Postcondición:** el edificio aloja un animal más; si es el cuarto animal, se puede conceder LG-07.

## CU-11 · Alimentar animales
- **Actor:** Jugador · **RF:** RF-20, RF-21 · **RN:** RN-02, RN-24, RN-25
- **Precondición:** herramienta *Alimentar* seleccionada y un edificio con animales.
- **Flujo principal:**
  1. El Jugador hace clic en el edificio.
  2. El sistema cuenta los animales **no enfermos** que aún no comieron hoy (`n`).
  3. El sistema verifica heno ≥ `n` y energía ≥ `n`.
  4. El sistema consume `n` heno y `n` de energía y marca a esos animales como alimentados hoy.
- **Alternativas:** **A3a.** Falta heno → `SIN_HENO`. **A3b.** Falta energía → `SIN_ENERGIA`. En ambos casos no se consume nada. **A2.** Hay animales enfermos → el sistema los omite y avisa con `ANIMAL_ENFERMO`. **A2b.** `n = 0` → informa que ya comieron.
- **Postcondición:** los animales alimentados producirán al terminar el día si están sanos (CU-06).

## CU-12 · Recolectar productos de un edificio
- **Actor:** Jugador · **RF:** RF-22 · **RN:** RN-02, RN-15, RN-26
- **Precondición:** herramienta *Cosechar* seleccionada y un edificio con productos pendientes.
- **Flujo principal:**
  1. El Jugador hace clic en el edificio.
  2. El sistema suma los productos pendientes y verifica espacio en el inventario y energía ≥ 1.
  3. El sistema pasa todos los productos al inventario y descuenta 1 de energía.
- **Alternativas:** **A2a.** No cabe todo → `INVENTARIO_LLENO` (no se mueve nada). **A2b.** No hay pendientes → informa que no hay nada que recoger. **A2c.** Energía insuficiente → `SIN_ENERGIA`.
- **Postcondición:** el edificio queda sin pendientes y el inventario aumenta.

## CU-13 · Curar animal
- **Actor:** Jugador · **RF:** RF-23 · **RN:** RN-02, RN-16, RN-27, RN-28
- **Precondición:** un animal está **Enfermo** y el panel del edificio está abierto.
- **Flujo principal:**
  1. El Jugador elige *Curar* sobre el animal enfermo.
  2. El sistema verifica 1 Medicina en el inventario y energía ≥ 1.
  3. El sistema consume la Medicina y la energía, devuelve al animal al estado *Sano* y reinicia `díasSinAlimento`.
  4. El sistema cuenta el logro LG-08.
- **Alternativas:** **A2a.** Sin Medicina → `SIN_MEDICINA`. **A2b.** Energía insuficiente → `SIN_ENERGIA`.
- **Postcondición:** el animal está sano; si se lo alimenta, producirá al terminar el día.

## CU-14 · Guardar partida
- **Actor:** Jugador; secundario: MongoDB · **RF:** RF-29, RF-32 · **RN:** RN-31, RN-32
- **Precondición:** hay una partida en curso.
- **Flujo principal:**
  1. El Jugador abre *Guardar* y la interfaz lista las 3 ranuras con sus metadatos.
  2. El Jugador elige una ranura (con confirmación si está ocupada).
  3. El sistema crea una instantánea del estado, calcula el checksum y arma el documento de la ranura.
  4. El sistema reemplaza el documento en MongoDB en una sola operación y confirma con un *toast*.
- **Alternativas:** **A3.** MongoDB no responde o rechaza la escritura → `ERROR_ES`; el documento previo **queda intacto** y la partida sigue en juego.
- **Postcondición:** la ranura contiene la partida actual.

## CU-15 · Cargar partida
- **Actor:** Jugador; secundario: MongoDB · **RF:** RF-30, RF-31, RF-32 · **RN:** RN-32, RN-33
- **Precondición:** la pantalla de inicio o el juego en curso.
- **Flujo principal:**
  1. El Jugador abre *Cargar* y ve las 3 ranuras con metadatos.
  2. El Jugador elige una ranura ocupada.
  3. El sistema lee el documento y valida, en orden: existencia, JSON, versión, checksum e invariantes (RN-33).
  4. El sistema construye el nuevo estado aparte y, solo si todo es válido, **reemplaza** el estado actual.
  5. El sistema muestra la partida cargada.
- **Alternativas:** **A3a.** La ranura está vacía o el documento falta → `ARCHIVO_INEXISTENTE`. **A3b.** JSON mal formado o checksum distinto → `ARCHIVO_CORRUPTO`. **A3e.** MongoDB no responde → `ERROR_ES`. **A3c.** Versión desconocida → `VERSION_NO_SOPORTADA`. **A3d.** Invariante violada → `ARCHIVO_CORRUPTO`. En todos los casos la **partida actual no cambia**.
- **Postcondición:** el estado en memoria equivale al guardado, o bien sigue idéntico al anterior si hubo error.

## CU-16 · Consultar logros
- **Actor:** Jugador · **RF:** RF-26, RF-27 · **RN:** RN-28, RN-29
- **Flujo principal:**
  1. El Jugador abre *Logros* (`L`).
  2. El sistema lista los 9 logros con su condición, su progreso (por ejemplo, 12 / 25 cosechas) y su recompensa; los desbloqueados aparecen marcados.
  3. Los logros que habilitan edificios indican cuál habilitan.
- **Alternativas:** **A1.** Un logro se acaba de cumplir → un *toast* lo anuncia durante 5 s (RF-39).
- **Postcondición:** consulta pura.

## CU-17 · Consultar inventario
- **Actor:** Jugador · **RF:** RF-13, RF-37 · **RN:** RN-15
- **Flujo principal:**
  1. El Jugador abre el inventario (`I`).
  2. El sistema muestra la grilla de casillas (24, o 36 con Almacén) con icono, nombre y cantidad (máximo 99 por pila).
  3. El Jugador puede seleccionar una semilla para la herramienta *Sembrar*.
- **Alternativas:** **A2.** Casilla vacía → se muestra vacía.
- **Postcondición:** consulta; la única modificación posible es la semilla seleccionada.

## CU-18 · Observar la granja
- **Actor:** Jugador; secundario: Paquete de assets · **RF:** RF-10, RF-34, RF-35, RF-36, RF-39, RF-40, RF-41, RF-42, RF-43, RF-44, RF-48 · **RN:** RN-01, RN-02, RN-04, RN-08, RN-11, RN-12, RN-25, RN-34, RN-35
- **Flujo principal:**
  1. El sistema muestra el HUD (dinero, día, estación, clima, pronóstico y energía) y dibuja el mapa con las parcelas, los cultivos según su etapa, los edificios y los animales.
  2. El Jugador mueve el cursor: se resalta la parcela y se muestra el *tooltip* (CU-05).
  3. El Jugador elige herramienta con la barra o con las teclas `1` a `5` y actúa con clic (CU-02, CU-03, CU-04, CU-09, CU-11).
  4. El sistema redibuja al recibir **eventos**, no por consulta en bucle; las animaciones y los efectos son solo visuales.
  5. El sistema anuncia los eventos con *toasts*.
- **Alternativas:** **A1.** Si no hay sprites disponibles, el sistema dibuja gráficos vectoriales de respaldo. **A4.** Los efectos (tinte, partículas) cambian con la estación y el clima, sin alterar el estado del juego.
- **Postcondición:** la pantalla refleja siempre el estado actual del juego.

## CU-19 · Caminar por el valle
- **Actor:** Jugador · **RF:** RF-45, RF-50 · **RN:** RN-36, RN-40
- **Precondición:** hay una partida en curso.
- **Flujo principal:**
  1. El Jugador pulsa una flecha o `W` `A` `S` `D`.
  2. El sistema mueve al avatar una parcela en esa dirección, actualiza su mirada, lo anima y la cámara lo sigue; mientras la tecla siga apretada da un paso cada 180 ms sin pausas (RN-40).
  3. El Jugador elige una herramienta y pulsa `Espacio`: el sistema la aplica a la parcela que tiene enfrente (CU-02, CU-03, CU-04, CU-11).
- **Alternativas:** **A2.** El destino es agua, obstáculo, edificio, NPC o el borde: se rechaza con `MOVIMIENTO_BLOQUEADO` y el avatar solo cambia su mirada. **A3.** El Jugador hace clic sobre una parcela: la herramienta actúa sobre ella como siempre.
- **Postcondición:** la posición y la mirada quedan en el estado y se guardan con la partida.

## CU-20 · Hablar con un NPC
- **Actor:** Jugador · **RF:** RF-46, RF-47 · **RN:** RN-37, RN-38, RN-14
- **Precondición:** el avatar está en una parcela vecina del NPC.
- **Flujo principal:**
  1. El Jugador pulsa `E` (o hace clic sobre el NPC).
  2. El sistema abre el cuadro de diálogo con el nombre del NPC y un texto que depende del estado del juego.
  3. Si el Jugador tiene el encargo completo, elige «Entregar»: el sistema retira los artículos, paga la recompensa y marca el encargo como cumplido.
- **Alternativas:** **A1.** El avatar no está junto al NPC: `NPC_LEJOS`. **A3.** Faltan artículos: `ENCARGO_INCOMPLETO` y no cambia nada. **A3b.** El encargo ya fue cumplido: el NPC lo agradece y no paga de nuevo.
- **Postcondición:** el encargo cumplido y el dinero quedan guardados con la partida.

## CU-21 · Seguir el tutorial de inicio
- **Actor:** Jugador · **RF:** RF-49 · **RN:** RN-39, RN-36, RN-37
- **Precondición:** el Jugador acaba de empezar una partida nueva (CU-01).
- **Flujo principal:**
  1. El sistema muestra el cuadro del avatar con «Paso 1 de 5» y la indicación de caminar.
  2. El Jugador hace lo que pide el paso (caminar, sembrar, regar, hablar con un NPC o terminar el día; CU-19, CU-02, CU-03, CU-20, CU-06).
  3. El sistema pasa al paso siguiente y cambia la indicación. Se repiten 2 y 3 hasta el paso 5.
  4. Al terminar el paso 5, el sistema muestra «¡Listo! Ya sabes lo básico.» y oculta el cuadro.
- **Alternativas:** **A2a.** La acción es de otro paso o fue rechazada: el paso no cambia. **A2b.** El Jugador pulsa «Saltar tutorial»: el sistema da el tutorial por terminado y oculta el cuadro.
- **Postcondición:** el paso actual (o el tutorial terminado) queda en el estado y se guarda con la partida.

---

## Trazabilidad RF → CU

| RF | Requerimiento (resumen) | Prioridad | Casos de uso | Historias de usuario |
|---|---|---|---|---|
| RF-01 | El jugador podrá sembrar una semilla del inventario en una parcela de tierra libre, siempr… | Must | CU-02 | HU-01 |
| RF-02 | El jugador podrá regar un cultivo vivo; el sistema marcará el cultivo como regado hoy y co… | Must | CU-03 | HU-02 |
| RF-03 | El sistema hará crecer cada día a los cultivos regados y los hará avanzar por las etapas S… | Must | CU-06 | HU-03 |
| RF-04 | El jugador podrá cosechar un cultivo maduro y recibir el producto en el inventario; si el … | Must | CU-04 | HU-04 |
| RF-05 | El sistema marchitará los cultivos por falta de riego, por helada o por cambio a una estac… | Must | CU-06 | HU-05, HU-12 |
| RF-06 | El jugador podrá limpiar una parcela con cultivo marchito para volver a usarla | Should | CU-04 | HU-06 |
| RF-07 | El jugador podrá consultar el estado de una parcela: cultivo, etapa, regado y días para ma… | Must | CU-05 | HU-07 |
| RF-08 | El jugador podrá terminar el día; el sistema resolverá todos los efectos en el orden defin… | Must | CU-06 | HU-10 |
| RF-09 | El sistema llevará el calendario: 4 estaciones, años y cambio de estación con sus consecue… | Must | CU-06 | HU-09 |
| RF-10 | El sistema sorteará el clima de cada día según la estación y mostrará siempre el pronóstic… | Must | CU-06, CU-18 | HU-11 |
| RF-11 | El sistema aplicará los efectos del clima a los cultivos: lluvia que riega, sequía que red… | Must | CU-06 | HU-12 |
| RF-12 | El sistema administrará la energía diaria: descontará el costo de cada acción, rechazará l… | Must | CU-02, CU-06 | HU-08 |
| RF-13 | El sistema administrará el inventario con casillas, pilas máximas y operaciones atómicas d… | Must | CU-17 | HU-13 |
| RF-14 | El jugador podrá comprar semillas, heno, medicina y animales a precio fijo, con validación… | Must | CU-07, CU-10 | HU-14 |
| RF-15 | El jugador podrá vender cosechas y productos animales a un precio dinámico que depende de … | Must | CU-08 | HU-15 |
| RF-16 | El sistema mostrará la tendencia de cada precio (sube, baja o igual) | Should | CU-08 | HU-16 |
| RF-17 | El sistema resumirá el día al terminarlo: ingresos, gastos y eventos relevantes | Could | CU-06 | HU-17 |
| RF-18 | El jugador podrá construir edificios (Gallinero y Establo como mínimo) sobre hierba libre,… | Must | CU-09 | HU-18 |
| RF-19 | El jugador podrá comprar animales que se alojarán en un edificio compatible con plaza libr… | Must | CU-10 | HU-19 |
| RF-20 | El jugador podrá alimentar a los animales de un edificio consumiendo heno y energía | Must | CU-11 | HU-20 |
| RF-21 | El sistema gestionará el estado de cada animal (Sano, Hambriento, Enfermo) según se lo ali… | Must | CU-06, CU-11 | HU-21 |
| RF-22 | El sistema hará producir a los animales sanos y alimentados, y el jugador podrá recolectar… | Must | CU-06, CU-12 | HU-22 |
| RF-23 | El jugador podrá curar a un animal enfermo con medicina | Should | CU-13 | HU-23 |
| RF-24 | El jugador podrá construir un invernadero para cultivar en cualquier estación, protegido d… | Could | CU-09 | HU-25 |
| RF-25 | El jugador podrá construir un almacén que amplíe la capacidad del inventario | Should | CU-09 | HU-24 |
| RF-26 | El sistema registrará logros al cumplirse sus condiciones y abonará su recompensa una sola… | Must | CU-06, CU-16 | HU-26 |
| RF-27 | El sistema desbloqueará edificios al conseguir los logros asociados | Should | CU-09, CU-16 | HU-27 |
| RF-28 | El jugador podrá iniciar una nueva partida con el estado inicial definido | Must | CU-01 | HU-28 |
| RF-29 | El jugador podrá guardar la partida en una de 3 ranuras de forma atómica | Must | CU-14 | HU-29 |
| RF-30 | El jugador podrá cargar una partida guardada y recuperar un estado idéntico al guardado | Must | CU-15 | HU-30 |
| RF-31 | El sistema rechazará archivos corruptos, de versión no soportada o inexistentes con un men… | Must | CU-15 | HU-31 |
| RF-32 | El sistema listará las ranuras con sus metadatos (fecha del juego, dinero y fecha real de … | Should | CU-14, CU-15 | HU-32 |
| RF-33 | El sistema ofrecerá una pantalla de inicio con *Nueva partida*, *Cargar* y *Salir* | Must | CU-01 | HU-33 |
| RF-34 | La pantalla principal mostrará un HUD con dinero, día, estación, clima y barra de energía | Must | CU-18 | HU-34 |
| RF-35 | El sistema dibujará el mapa de la granja con parcelas, cultivos por etapa, edificios y ani… | Must | CU-18 | HU-35 |
| RF-36 | El jugador podrá elegir una herramienta (por barra o atajo) y actuar con clic sobre una pa… | Must | CU-18 | HU-36 |
| RF-37 | El sistema mostrará el inventario como grilla de casillas con icono y cantidad | Must | CU-17 | HU-37 |
| RF-38 | El sistema ofrecerá un mercado modal con pestañas Comprar y Vender, precio actual con tend… | Must | CU-07, CU-08 | HU-38 |
| RF-39 | El sistema mostrará notificaciones (*toasts*) para logros, errores y eventos de estación | Should | CU-18 | HU-39 |
| RF-40 | El sistema aplicará efectos visuales: tinte día/noche, paleta por estación, partículas de … | Could | CU-18 | HU-40 |
| RF-41 | El sistema aplicará animaciones suaves (crecimiento, animales de 2 a 3 cuadros y transició… | Could | CU-18 | HU-41 |
| RF-42 | El sistema cargará sprites mediante un `AssetManager` con caché y usará gráficos vectorial… | Should | CU-18 | HU-42 |
| RF-43 | La interfaz usará el tema visual FlatLaf con una tipografía única y, si el tema no se puede instalar, … | Should | CU-18 | HU-43 |
| RF-44 | El sistema incluirá sprites pixel-art propios de 32 × 32 px y los ampliará por factor entero … | Should | CU-18 | HU-44 |
| RF-45 | El jugador podrá mover un avatar con el teclado por el mapa, con colisiones, y usar la herramienta … | Should | CU-19 | HU-45 |
| RF-46 | El jugador podrá hablar con los NPC que estén junto a su avatar y leer diálogos en español … | Should | CU-20 | HU-46 |
| RF-47 | Los NPC ofrecerán encargos de un solo uso con recompensa en m | Could | CU-20 | HU-47 |
| RF-48 | La interfaz tendrá aspecto de granja: barra de herramientas con iconos, HUD con iconos e inventario … | Should | CU-18 | HU-48 |
| RF-49 | Una partida nueva empezará con un tutorial de 5 pasos guiado por el avatar, que se puede saltar … | Should | CU-21 | HU-49 |
| RF-50 | El jugador recorrerá un valle de 48 × 24 con su granja y una aldea, con cámara que sigue al avatar … | Should | CU-19 | HU-50 |

> Las historias de usuario están en [`HISTORIAS_DE_USUARIO.md`](HISTORIAS_DE_USUARIO.md).
