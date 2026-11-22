# Reglas de negocio — Valle Verde

> **Versión 0.1 · 2026-11-22 · Fase 1 (Análisis)**
> Fuente única de todos los valores numéricos del juego. Cada valor se implementará como **constante** en una clase de configuración (sin números mágicos) y las historias de usuario y pruebas deben citar estos IDs.
> Convenciones: «m» = monedas · día de la estación `1..28` · todo importe es **entero** · porcentajes con **aritmética entera** (evita errores de punto flotante) · redondeo *half-up*.

## Índice

| Grupo | Reglas |
|---|---|
| Tiempo y energía | RN-01 a RN-03 |
| Mapa | RN-04 |
| Cultivos | RN-05 a RN-10 |
| Clima | RN-11 a RN-13 |
| Economía | RN-14 a RN-19 |
| Construcción | RN-20 a RN-22 |
| Animales | RN-23 a RN-27 |
| Progresión | RN-28 a RN-29 |
| Partida y persistencia | RN-30 a RN-33 |
| Interfaz | RN-34 a RN-35 |

---

## 1. Tiempo y energía

### RN-01 · Calendario
- Cuatro estaciones en orden: **Primavera → Verano → Otoño → Invierno**; cada una dura **28 días**; el año dura **112 días**.
- La partida nueva empieza en **Año 1, Primavera, día 1**.
- Al terminar el día 28 de una estación comienza el día 1 de la siguiente; tras el día 28 de Invierno comienza **Año + 1, Primavera, día 1**.
- No existe fin de partida: el juego continúa indefinidamente.

### RN-02 · Energía diaria
- Energía máxima: **100**. Nunca baja de 0 ni supera 100.
- Una acción se **rechaza** si la energía disponible es menor que su costo.

| Acción | Costo de energía |
|---|---|
| Sembrar | 2 |
| Regar | 1 (**2** si el clima del día es Sequía, RN-12) |
| Cosechar | 2 |
| Limpiar parcela marchita | 1 |
| Alimentar (por animal alimentado) | 1 |
| Recolectar productos de un edificio (por acción) | 1 |
| Curar animal | 1 |
| Construir, comprar, vender, terminar día, guardar, cargar | 0 |

- «Terminar día» **restaura la energía a 100** (paso 7 de RN-03). La energía sobrante no se acumula.

### RN-03 · Orden de resolución del fin de día
Al ejecutar «Terminar día» el sistema aplica, **siempre en este orden**:

1. **Clima del día que termina** (RN-12): la lluvia riega los cultivos al aire libre; la helada marchita los cultivos al aire libre.
2. **Riego y crecimiento** de cada cultivo (RN-08) y **marchitamiento por sequedad** (RN-09-a).
3. **Animales** (RN-25 y RN-26): alimentación, estados y producción.
4. **Mercado**: guardar el precio actual de cada producto como «precio anterior» (RN-19) y luego **recuperar la demanda** (RN-18).
5. **Avanzar el calendario** (RN-01). Si cambia la estación, marchitar los cultivos fuera de estación (RN-09-c).
6. **Clima**: el pronóstico pasa a ser el clima del nuevo día y se sortea el pronóstico siguiente (RN-13).
7. **Restaurar energía** a 100 y borrar la marca «regada» de todas las parcelas.
8. **Evaluar logros** (RN-28).
9. **Publicar** los eventos resultantes (día avanzado, clima, cultivos marchitos, animales enfermos, logros…).

> La ejecución completa del fin de día debe cumplirse en ≤ 200 ms (RNF-01).

---

## 2. Mapa

### RN-04 · Mapa de la granja
- Cuadrícula de **16 columnas × 10 filas = 160 parcelas**. Coordenadas `(columna, fila)`, con `(0,0)` arriba a la izquierda.
- Tipos de terreno y distribución inicial (fija y determinista):

| Terreno | Cantidad | Uso |
|---|---|---|
| **TIERRA** (cultivable) | 40 (bloque de 8 × 5) | Solo aquí se siembra |
| **HIERBA** (construible) | 102 | Solo aquí se construye |
| **AGUA** (estanque 3 × 2) | 6 | Decorativo, no utilizable |
| **OBSTÁCULO** (árboles y rocas) | 12 | Decorativo, no utilizable |

- Una parcela ocupada por un edificio no admite cultivos ni otro edificio.

---

## 3. Cultivos

### RN-05 · Catálogo de cultivos

| Cultivo | Estaciones válidas | Días a madurar (T) | Semilla (m) | Venta base (m) |
|---|---|---|---|---|
| Zanahoria | Primavera | 4 | 10 | 30 |
| Papa | Primavera | 6 | 20 | 55 |
| Tomate | Verano | 7 | 25 | 65 |
| Maíz | Verano, Otoño | 8 | 30 | 75 |
| Calabaza | Otoño | 10 | 40 | 110 |

- Cada cosecha rinde **1 unidad** del producto del mismo nombre.

### RN-06 · Siembra
Sembrar se permite solo si **todas** las condiciones se cumplen:
1. La parcela es **TIERRA**, está libre (sin cultivo ni edificio) y **no es** parte de un invernadero (esas siguen RN-22).
2. La **estación actual** está entre las estaciones válidas del cultivo (RN-05).
3. Hay **≥ 1 semilla** de ese cultivo en el inventario.
4. Energía ≥ 2.

Efecto: consume 1 semilla, resta 2 de energía y crea el cultivo en etapa *Semilla* con `díasCrecidos = 0`, `díasSinRiego = 0` y `regado = falso`.

### RN-07 · Riego
- Regar requiere un cultivo **vivo** (no marchito) que **no esté regado hoy** y energía ≥ costo (RN-02).
- Efecto: marca el cultivo como `regado = verdadero` hasta el fin del día.
- Regar un cultivo ya regado se rechaza **sin consumir energía**.
- La lluvia (RN-12) riega automáticamente los cultivos al aire libre.

### RN-08 · Crecimiento y etapas
En el paso 2 de RN-03, por cada cultivo vivo y **no maduro**:
- Si `regado`: `díasCrecidos += 1` y `díasSinRiego = 0`.
- Si no: `díasSinRiego += 1` (el cultivo no crece ese día).

Etapa según `d = díasCrecidos` y `T` (RN-05), con `M = ⌈T / 2⌉`:

| Etapa | Condición |
|---|---|
| Semilla | `d = 0` |
| Brote | `1 ≤ d < M` |
| Crecimiento | `M ≤ d < T` |
| **Maduro** | `d ≥ T` |

Ejemplos: Zanahoria (`T = 4`, `M = 2`): Brote con `d = 1`, Crecimiento con `d = 2..3`, Maduro con `d = 4`. Tomate (`T = 7`, `M = 4`): Brote `d = 1..3`, Crecimiento `d = 4..6`, Maduro `d = 7`.
Un cultivo **Maduro** ya no crece ni consume riego.

### RN-09 · Marchitamiento
Un cultivo pasa a **Marchito** (irreversible) cuando ocurre **cualquiera** de estas causas:
- **a) Falta de riego:** `díasSinRiego ≥ 3` (**≥ 2** si el clima del día que termina es Sequía). Solo aplica a etapas Semilla, Brote y Crecimiento.
- **b) Helada:** el clima del día que termina es Helada y el cultivo está **al aire libre**, en **cualquier etapa** (incluido Maduro).
- **c) Fuera de estación:** al comenzar una estación que no figura en las estaciones válidas del cultivo, en cualquier etapa (salvo en invernadero, RN-22).

Un cultivo marchito no crece, no se cosecha y **ocupa la parcela** hasta que se limpie (RN-10).

### RN-10 · Cosecha y limpieza
- **Cosechar** un cultivo **Maduro** requiere energía ≥ 2 y **espacio en el inventario** para 1 unidad (RN-15). Efecto: la unidad entra al inventario y la parcela queda libre.
- Si el inventario está lleno: se rechaza con «Inventario lleno», el cultivo **permanece Maduro** y **no se consume energía**.
- Cosechar un cultivo no maduro y vivo se rechaza («El cultivo aún no está maduro»).
- **Limpiar** un cultivo **Marchito** cuesta 1 de energía, deja la parcela libre y **no** produce nada.

---

## 4. Clima

### RN-11 · Clima por estación
El clima de cada día se sortea con un `Random` **inyectado**. Probabilidades (%) según la estación y el día **del día que se sortea**:

| Periodo | Soleado | Nublado | Lluvioso | Sequía | Helada | Nevado |
|---|---|---|---|---|---|---|
| Primavera | 55 | 15 | 30 | 0 | 0 | 0 |
| Verano | 60 | 10 | 15 | 15 | 0 | 0 |
| Otoño, días 1–21 | 50 | 20 | 30 | 0 | 0 | 0 |
| Otoño, días 22–28 | 35 | 15 | 25 | 0 | 25 | 0 |
| Invierno | 25 | 20 | 0 | 0 | 15 | 40 |

### RN-12 · Efectos del clima
| Clima | Efecto |
|---|---|
| Soleado, Nublado | Ninguno |
| Lluvioso | Riega todos los cultivos **al aire libre** (paso 1 de RN-03) |
| Sequía | La tolerancia sin riego baja a 2 días (RN-09-a) y regar cuesta 2 de energía |
| Helada | Marchita todos los cultivos **al aire libre** (RN-09-b) |
| Nevado | Sin efecto sobre cultivos (efecto visual) |

Los cultivos del invernadero son inmunes a Helada y Sequía y no reciben lluvia (RN-22).

### RN-13 · Pronóstico y clima inicial
- El primer día (Año 1, Primavera 1) es **Soleado** (fijo). El pronóstico del día 2 se sortea al crear la partida.
- Al terminar cada día, el pronóstico se convierte en el clima del nuevo día y se sortea el del día siguiente (paso 6 de RN-03).
- El pronóstico es **100 % fiable** y siempre visible para el jugador.
- Tanto el clima actual como el pronóstico se guardan en la partida (RN-32).

---

## 5. Economía

### RN-14 · Dinero
- El saldo es un **entero ≥ 0**; nunca hay deuda.
- Una operación cuyo costo supere el saldo se **rechaza** («Fondos insuficientes») sin modificar nada.
- Saldo inicial: **500 m** (RN-30).

### RN-15 · Inventario
- **24 casillas** base; **+12** con el Almacén (RN-20), para un máximo de **36**.
- Cada casilla contiene **un solo tipo de ítem**; pila máxima: **99** unidades.
- Ítems: 5 semillas, 5 cosechas, Huevo, Leche, Heno y Medicina.
- **Insertar:** primero completa las pilas del mismo tipo y luego usa casillas vacías. Si no cabe **toda** la cantidad, se rechaza con «Inventario lleno» y **no se inserta nada** (operación atómica).
- **Retirar:** la cantidad no puede superar la existente.

### RN-16 · Catálogo de compra (precio fijo, disponible todo el año)

| Producto | Precio (m) |
|---|---|
| Semilla de Zanahoria / Papa / Tomate / Maíz / Calabaza | 10 / 20 / 25 / 30 / 40 |
| Heno (1 ración) | 5 |
| Medicina | 40 |
| Gallina | 150 |
| Vaca | 800 |

- Cantidad por transacción: de **1 a 99**. Total = precio × cantidad.
- Se valida el **saldo** (RN-14) y el **espacio**: de inventario para ítems (RN-15) o de capacidad del edificio para animales (RN-23).
- Las semillas se pueden **comprar en cualquier estación**; la restricción de estación se aplica al **sembrar** (RN-06).

### RN-17 · Precio de venta dinámico
Precio de **una unidad** (en m, entero):

```
precio = máx(1, redondear_half_up( base × factorTemporada × demanda / 10 000 ))
```

con `factorTemporada` y `demanda` expresados como **porcentajes enteros**.

| Producto | Base (m) | factorTemporada |
|---|---|---|
| Cosechas (RN-05) | tabla de RN-05 | **90** si la estación actual ∈ estaciones válidas del cultivo; **120** en caso contrario |
| Huevo | 20 | 100 |
| Leche | 60 | 100 |

- Al vender varias unidades, **cada unidad se calcula con la demanda vigente en ese momento** y después baja la demanda (RN-18).
- **Ejemplo:** 3 Zanahorias en Primavera (base 30, factor 90, demanda 100 → 98 → 96): `27`, `26` (26,46) y `26` (25,92) → **79 m**; la demanda queda en **94**.

### RN-18 · Saturación y recuperación del mercado
- Cada producto vendible tiene su **propia demanda**: entero entre **50 y 100**, inicial **100**.
- **Cada unidad vendida resta 2** (mínimo 50).
- **Al terminar el día suma 5** (máximo 100), paso 4 de RN-03. Recuperar de 50 a 100 toma 10 días.
- Comprar no modifica la demanda.

### RN-19 · Tendencia de precio
- En el paso 4 de RN-03 se guarda, por producto, el **precio anterior** = precio de la próxima unidad *antes* de recuperar la demanda.
- Indicador: **▲** si el precio actual es mayor que el anterior, **▼** si es menor, **▬** si es igual. Al inicio de la partida todos muestran ▬.
- Vender durante el día baja el precio actual y, por tanto, puede mostrar ▼.

---

## 6. Construcción

### RN-20 · Catálogo de edificios

| Edificio | Tamaño (col × fila) | Costo (m) | Efecto / capacidad | Desbloqueo | Máx. |
|---|---|---|---|---|---|
| Gallinero | 2 × 2 | 400 | 4 gallinas | Desde el inicio | 2 |
| Establo | 3 × 2 | 1.000 | 4 vacas | Logro LG-02 | 2 |
| Almacén | 2 × 2 | 300 | +12 casillas de inventario | Logro LG-01 | 1 |
| Invernadero | 3 × 3 | 1.500 | 9 parcelas de cultivo protegidas | Logro LG-05 | 1 |

- La construcción se completa **al instante** y cuesta **0** de energía.

### RN-21 · Colocación y límites
Construir se permite solo si:
1. Todas las parcelas del área (desde la esquina superior izquierda elegida) están **dentro del mapa**, son **HIERBA** y están **libres**.
2. El edificio está **desbloqueado** (RN-29).
3. No se supera el **máximo por tipo** (RN-20).
4. El saldo alcanza (RN-14).

Efecto: descuenta el costo y marca las parcelas como ocupadas. Si falla una condición, no se modifica nada.

### RN-22 · Invernadero
- Al construirse, sus 9 parcelas se convierten en **parcelas de cultivo protegidas**.
- Dentro de él: **no** rige la restricción de estación (RN-06 punto 2), los cultivos son **inmunes a Helada y Sequía** (tolerancia fija de 3 días sin riego), la lluvia **no** los riega (se riegan a mano, 1 de energía) y no se marchitan por cambio de estación.
- Siembra, riego, crecimiento y cosecha siguen las demás reglas de cultivos.

---

## 7. Animales

### RN-23 · Catálogo de animales

| Animal | Edificio | Costo (m) | Producto | Precio base (m) |
|---|---|---|---|---|
| Gallina | Gallinero | 150 | Huevo | 20 |
| Vaca | Establo | 800 | Leche | 60 |

- Cada edificio aloja hasta **4** animales. Al comprar, el animal se asigna al **primer edificio compatible con plaza libre**; sin plaza se rechaza («No hay espacio…»).

### RN-24 · Alimentación
- Cada animal necesita **1 Heno por día**.
- **Alimentar** se aplica a un **edificio** y es **atómico**: alimenta a todos los animales **no enfermos** del edificio que aún no hayan comido hoy. Requiere **1 Heno y 1 energía por animal** a alimentar; si falta alguno de los dos, se rechaza completo sin consumir nada.
- Un animal **enfermo no se alimenta** (no consume heno): necesita Medicina (RN-27).

### RN-25 · Estados del animal
Cada animal tiene el estado **Sano**, **Hambriento** o **Enfermo**, la marca `alimentadoHoy` y el contador `díasSinAlimento`. Se resuelve en el paso 3 de RN-03:

| Situación al terminar el día | Resultado |
|---|---|
| Está **Enfermo** | Sigue Enfermo, **no produce**; el contador no cambia |
| No enfermo y `alimentadoHoy` | `díasSinAlimento = 0`, estado **Sano**, **produce** (RN-26) |
| No enfermo y **no** alimentado | `díasSinAlimento += 1`: si es **≥ 3** pasa a **Enfermo**; si es 1 o 2 queda **Hambriento**. **No produce** |

Después se pone `alimentadoHoy = falso`.

### RN-26 · Producción y recolección
- Un animal **Sano y alimentado** ese día genera **1 unidad** de su producto, que queda **pendiente** en el edificio.
- Cada animal acumula como máximo **2 unidades pendientes**; con 2 pendientes **no produce** más hasta que se recolecten.
- **Recolectar** se aplica a un edificio: pasa **todos** los pendientes al inventario, cuesta 1 de energía por acción y es **atómico** (si no cabe todo, «Inventario lleno» y no se mueve nada).

### RN-27 · Curación
- **Curar** se aplica a un animal **Enfermo** desde el panel del edificio (no es una herramienta de la barra).
- Consume **1 Medicina** y 1 de energía. El animal pasa a **Sano** y `díasSinAlimento = 0` de inmediato.
- Si ese día se lo alimenta, produce normalmente al terminar el día.

---

## 8. Progresión

### RN-28 · Logros

| ID | Logro | Condición | Recompensa |
|---|---|---|---|
| LG-01 | Primera cosecha | Cosechar 1 cultivo | 50 m · desbloquea Almacén |
| LG-02 | Manos a la tierra | Cosechar 25 cultivos (acumulado) | 150 m · desbloquea Establo |
| LG-03 | Gran cosechador | Cosechar 100 cultivos | 400 m |
| LG-04 | Comerciante | Vender 50 unidades (acumulado) | 100 m |
| LG-05 | Capitalista | Tener un saldo ≥ 3.000 m | 200 m · desbloquea Invernadero |
| LG-06 | Constructor | Construir 1 edificio | 100 m |
| LG-07 | Granjero de corral | Tener 4 animales a la vez | 150 m |
| LG-08 | Veterinario | Curar 1 animal | 80 m |
| LG-09 | Un año en el valle | Llegar al Año 2 (Primavera, día 1) | 500 m |

- Solo cuentan las **cosechas** (no las limpiezas de parcelas marchitas).
- Se evalúan **después de cada acción exitosa** y en el paso 8 de RN-03.
- Cada logro se concede **una sola vez**; la recompensa se abona al desbloquearse y el logro no se revoca.

### RN-29 · Desbloqueo de edificios
- El **Gallinero** está disponible desde el inicio. Almacén, Establo e Invernadero requieren los logros LG-01, LG-02 y LG-05.
- Intentar construir un edificio bloqueado se rechaza: «Edificio bloqueado: requiere el logro "…"».

---

## 9. Partida y persistencia

### RN-30 · Partida nueva
| Elemento | Valor inicial |
|---|---|
| Calendario | Año 1, Primavera, día 1 |
| Clima / pronóstico | Soleado (fijo) / sorteado (RN-13) |
| Dinero / energía | 500 m / 100 |
| Inventario | 10 semillas de Zanahoria y 5 de Papa |
| Edificios y animales | Ninguno |
| Mapa | Distribución de RN-04 |
| Mercado | Demanda 100 en todos los productos; tendencia ▬ |
| Logros y contadores | Todos bloqueados y en 0 |
| Aleatoriedad | Semilla del generador (configurable; por defecto la hora actual) |

### RN-31 · Ranuras de guardado
- Hay **3 ranuras** (`slot1`, `slot2`, `slot3`). Cada una es un **documento** de la colección `partidas` de MongoDB, con `_id` igual al nombre de la ranura, en la base `valleverde` (la URI es configurable; por defecto `mongodb://localhost:27017`).
- Guardar en una ranura ocupada la **sobrescribe** (la interfaz pide confirmación).
- El guardado es **atómico**: el documento completo se reemplaza en **una sola operación** (`replaceOne` con *upsert*). Si falla, el documento previo **queda intacto**.
- **MongoDB no es necesario para jugar.** Si no responde en 2 s, guardar y cargar devuelven `ERROR_ES` y la partida en curso sigue sin cambios.
- Metadatos visibles por ranura: Año, estación y día, dinero, fecha y hora real del guardado.

### RN-32 · Formato e integridad del documento
- Documento con los campos `_id`, `schemaVersion` (= **1**), `savedAt`, `state` (el estado como texto JSON UTF-8) y `checksum` (**SHA-256** en hexadecimal de `state`).
- `state` incluye: calendario, energía, dinero, inventario, parcelas y cultivos, edificios y animales (con pendientes), mercado (demanda y precio anterior), clima y pronóstico, logros y contadores, y el **estado interno del generador aleatorio** (un entero de 64 bits que expone `StateRandom`), para continuar exactamente la misma secuencia al cargar.
- Tamaño típico ≤ 200 KB (el límite de un documento de MongoDB es 16 MB).
- **Guardar → cargar devuelve un estado idéntico** al original (igualdad estructural, incluida la secuencia futura de sorteos).

### RN-33 · Validación de carga
La carga verifica **en este orden** y se detiene en el primer fallo:
1. El documento **existe** (y MongoDB responde).
2. El JSON de `state` está **bien formado**.
3. `schemaVersion` es **soportada**.
4. El **checksum** coincide.
5. **Invariantes:** dinero ≥ 0; energía 0–100; día 1–28; inventario dentro de su capacidad y pilas ≤ 99; cultivos solo en parcelas de TIERRA o de invernadero; edificios dentro del mapa y sin solapar; animales ≤ capacidad; logros coherentes con sus contadores.

Si algo falla se informa en español la causa (partida inexistente, dañada, versión no soportada, datos inválidos) y la **partida actual no cambia en absoluto**: el nuevo estado se construye aparte y solo reemplaza al actual cuando **todas** las verificaciones pasan.

---

## 10. Interfaz

### RN-34 · Herramientas y atajos
| Herramienta | Tecla | Acción sobre la parcela o edificio |
|---|---|---|
| Sembrar | `1` | Siembra la semilla seleccionada en el inventario (RN-06) |
| Regar | `2` | Riega el cultivo (RN-07) |
| Cosechar | `3` | Cosecha un cultivo maduro, limpia uno marchito (RN-10) y recolecta los productos de un edificio (RN-26) |
| Construir | `4` | Abre el catálogo de edificios (RN-20) y coloca el elegido |
| Alimentar | `5` | Alimenta a los animales de un edificio (RN-24) |

Atajos adicionales: `Esc` deselecciona herramienta · `M` Mercado · `I` Inventario · `L` Logros · `Enter` Terminar día · `F5` guardar rápido (última ranura usada) · `F9` cargar rápido.
El *tooltip* de una parcela aparece tras **300 ms** de reposo del cursor y muestra: cultivo, etapa, regado sí/no y días para madurar (`T − d`).

### RN-35 · Mensajes y notificaciones
- Cada rechazo devuelve un **código** y un **mensaje en español** que explica la causa y qué hacer:

| Código | Mensaje (plantilla) |
|---|---|
| `FUERA_DE_ESTACION` | «No se puede sembrar {cultivo} en {estación}: solo en {estaciones válidas}.» |
| `SIN_ENERGIA` | «Energía insuficiente: necesitas {n} y tienes {m}.» |
| `SIN_FONDOS` | «Fondos insuficientes: faltan {n} m.» |
| `INVENTARIO_LLENO` | «Inventario lleno: libera espacio o construye un Almacén.» |
| `SIN_SEMILLAS` | «No tienes semillas de {cultivo}.» |
| `CANTIDAD_INVALIDA` | «La cantidad debe estar entre 1 y {máx}.» (compra: máx 99; venta: máx = existencia) |
| `ERROR_INTERNO` | «Ocurrió un error inesperado. Tu partida no cambió.» |
| `SIN_PARTIDA` | «No hay una partida en curso.» |
| `PARCELA_NO_CULTIVABLE` / `PARCELA_OCUPADA` | «Esta parcela no es de tierra de cultivo.» / «La parcela ya está ocupada.» |
| `YA_REGADO` | «El cultivo ya está regado hoy.» |
| `NO_MADURO` | «El cultivo aún no está maduro: faltan {n} días.» |
| `SIN_CULTIVO` | «No hay ningún cultivo en esta parcela.» |
| `CULTIVO_MARCHITO` | «El cultivo está marchito: límpialo para poder sembrar de nuevo.» |
| `EDIFICIO_BLOQUEADO` | «Edificio bloqueado: requiere el logro «{logro}».» |
| `UBICACION_INVALIDA` | «No se puede construir aquí: el área debe ser hierba libre dentro del mapa.» |
| `LIMITE_EDIFICIOS` | «Ya tienes el máximo de {edificio}.» |
| `SIN_ESPACIO_ANIMALES` | «No hay espacio para más {animal}: construye o amplía el edificio.» |
| `SIN_HENO` / `SIN_MEDICINA` | «Heno insuficiente: necesitas {n}.» / «No tienes Medicina.» |
| `ANIMAL_ENFERMO` | «Hay animales enfermos: cúralos con Medicina.» |
| `ARCHIVO_INEXISTENTE` | «No existe una partida en la ranura {n}.» |
| `ARCHIVO_CORRUPTO` | «La partida está dañada y no se puede cargar. Tu partida actual no cambió.» |
| `VERSION_NO_SOPORTADA` | «La partida es de una versión no soportada.» |
| `ERROR_ES` | «No se pudo acceder a las partidas guardadas: {detalle}.» |

- **Toasts:** duración **3 s** (desvanecimiento de 0,4 s; los logros duran **5 s**), máximo **4 visibles** a la vez, tres tipos: éxito, información y error.
- La interfaz **nunca** recibe excepciones: solo recibe un `Result` con código y mensaje.

---

## 11. Trazabilidad hacia atrás (RN → RF)

Cada RN se usa por al menos un requerimiento funcional; la tabla completa RF ↔ RN está en [`REQUERIMIENTOS.md`](REQUERIMIENTOS.md) §4.
