# Historias de usuario — Valle Verde

> **Versión 0.1 · 2026-11-22 · Fase 2**
> Backlog completo del proyecto. Cada historia apunta al **RF** que cubre ([`REQUERIMIENTOS.md`](REQUERIMIENTOS.md)) y a las **RN** cuyos valores usa ([`REGLAS_DE_NEGOCIO.md`](REGLAS_DE_NEGOCIO.md)). Los criterios de aceptación usan los valores numéricos de las RN y se convertirán en pruebas `HUxx_descripcion` antes de escribir el código (Fase 5).

## 1. Cómo leer este documento

| Elemento | Convención |
|---|---|
| ID | `HU-01` … `HU-42`. El orden numérico sigue las épicas; el **orden de implementación** está en §6 |
| Prioridad | MoSCoW heredada de los RF: **Must** (MVP), **Should**, **Could** |
| Puntos | Fibonacci (1, 2, 3, 5). **Ninguna historia supera 5**: si una pasaba de 5, se dividió |
| Rol | Todas las historias son del único actor humano, el **Jugador** |
| Criterios | `CA-n` en formato **Dado / Cuando / Entonces** con valores concretos; cada historia tiene **≥ 3** y al menos un caso **negativo o de borde** |
| Códigos de error | Los de `RN-35` (por ejemplo `SIN_ENERGIA`); el estado **no cambia** en ningún rechazo |
| Valores | Se citan de las RN; si una RN cambia, se actualizan estos criterios (única fuente de verdad: las RN) |

## 2. Definición de terminado (DoD) — común a todas las historias

Una historia está **terminada** solo si se cumple todo lo siguiente:

1. Existe una prueba `HUxx_descripcion` por **cada criterio de aceptación**, escrita **antes** que el código, y todas pasan en verde con `mvn test`.
2. El código respeta las capas: `ui → application → domain` e `infrastructure → domain`; el dominio no importa Swing, Gson ni E/S (verificado por la prueba de arquitectura, RNF-06).
3. Todo rechazo devuelve un `Result` con **código y mensaje en español** (RN-35); ninguna excepción llega a la UI (RNF-07).
4. No hay números mágicos: los valores de las RN están en constantes o en la clase de configuración (RNF-12). Toda aleatoriedad usa un `Random` inyectado (RNF-08).
5. Cada clase que implemente un patrón lleva el Javadoc `Pattern: <nombre> — Role: <rol>` (RNF-13).
6. La cobertura de dominio y aplicación se mantiene **≥ 70 %** (JaCoCo, RNF-05).
7. Hay una **interfaz funcional mínima** que permite demostrar la historia (botón o panel simple), salvo las historias puramente de dominio, que se demuestran por la capa de aplicación.
8. Los diagramas y documentos afectados se actualizan **en el mismo commit** si el código cambia el diseño.
9. Un commit en inglés `feat(area): description (HUxx)` por historia o fase, sin errores de compilación (`mvn clean package` en verde).

## 3. Aplicación de INVEST

| Criterio | Cómo se aplicó |
|---|---|
| **I**ndependiente | Cada historia declara sus dependencias y solo apunta a historias de la **misma fase o anteriores**. «Terminar día» (HU-10) entrega el *pipeline* ordenado de RN-03 y cada historia posterior registra su propio paso, de modo que ninguna depende de una posterior |
| **N**egociable | Las historias dicen *qué* y no *cómo*; no fijan clases ni pantallas concretas. Las Could (efectos, animaciones, invernadero, resumen) son negociables o recortables |
| **V**aliosa | Cada historia produce un cambio observable por el Jugador (ver, hacer o decidir algo nuevo) |
| **E**stimable | Todas tienen valores numéricos y reglas cerradas en las RN |
| **S**mall | Máximo 5 puntos. Divisiones realizadas: helada separada del marchitamiento por riego (HU-05 / HU-12); guardar, cargar y rechazar archivos inválidos como tres historias (HU-29 / HU-30 / HU-31); interfaz mínima separada de sprites, efectos y animaciones (HU-35 / HU-42 / HU-40 / HU-41); logros separados de su desbloqueo de edificios (HU-26 / HU-27) |
| **T**estable | Cada criterio usa valores concretos y se verifica con una prueba automática (o una comprobación visual definida para la UI) |

## 4. Brechas y supuestos detectados al redactar

| # | Tema | Tratamiento |
|---|---|---|
| B-01 | `RN-35` no tenía un código para cantidades fuera de rango (comprar o vender con 0, 100 o más de lo disponible) | Se **agrega `CANTIDAD_INVALIDA`** a RN-35 (HU-14 y HU-15) |
| B-02 | `RF-17` (resumen del día) no tenía definición de «ingresos» y «gastos» | Para HU-17: **ingresos** = ventas + recompensas de logros; **gastos** = compras + construcciones. Es una historia *Could* |
| B-03 | `RN-35` limita a 4 *toasts* visibles pero no dice qué pasa con el quinto | HU-39: el quinto **espera en cola** hasta que desaparezca uno |
| B-04 | La duración de la animación de transición entre etapas no figura en las RN | HU-41: **≤ 500 ms** (solo visual; *Could*) |

---

## 5. Historias de usuario

### Épica A · Cultivo

### HU-01 · Sembrar cultivo
**Épica:** Cultivo · **Prioridad:** Must · **Puntos:** 3 · **RF:** RF-01 · **RN:** RN-02, RN-05, RN-06, RN-15 · **Depende de:** HU-08, HU-13, HU-28

**Como** jugador **quiero** sembrar una semilla de mi inventario en una parcela de tierra libre **para** empezar a producir cosechas que pueda vender.

**Criterios de aceptación**
- **CA-1 · Siembra exitosa.** **Dado** que es Primavera, tengo 10 semillas de Zanahoria, 100 de energía y la parcela (2,3) es TIERRA libre, **Cuando** siembro Zanahoria en ella, **Entonces** la parcela contiene una Zanahoria en etapa *Semilla* (`díasCrecidos = 0`), me quedan 9 semillas y mi energía es 98.
- **CA-2 · Fuera de estación (negativo).** **Dado** que es Primavera y tengo 5 semillas de Calabaza (válida solo en Otoño), **Cuando** intento sembrarla, **Entonces** se rechaza con `FUERA_DE_ESTACION` («No se puede sembrar Calabaza en Primavera: solo en Otoño.»), conservo las 5 semillas y mi energía sigue en 100.
- **CA-3 · Parcela no apta (negativo).** **Dado** una parcela de HIERBA y otra de TIERRA que ya tiene un cultivo, **Cuando** intento sembrar en cada una, **Entonces** se rechaza con `PARCELA_NO_CULTIVABLE` y `PARCELA_OCUPADA` respectivamente, sin gastar semillas ni energía.
- **CA-4 · Sin semillas (borde).** **Dado** que tengo 0 semillas de Maíz en Verano, **Cuando** intento sembrar Maíz, **Entonces** se rechaza con `SIN_SEMILLAS` y la parcela sigue libre.

### HU-02 · Regar cultivo
**Épica:** Cultivo · **Prioridad:** Must · **Puntos:** 2 · **RF:** RF-02 · **RN:** RN-02, RN-07, RN-12 · **Depende de:** HU-01, HU-11

**Como** jugador **quiero** regar un cultivo vivo **para** que crezca ese día.

**Criterios de aceptación**
- **CA-1 · Riego exitoso.** **Dado** un cultivo vivo sin regar, clima Soleado y 100 de energía, **Cuando** lo riego, **Entonces** queda marcado como regado hasta el fin del día y mi energía es 99.
- **CA-2 · Riego en Sequía.** **Dado** que el clima del día es Sequía y tengo 100 de energía, **Cuando** riego un cultivo, **Entonces** mi energía baja a 98 (costo 2).
- **CA-3 · Ya regado (negativo).** **Dado** un cultivo ya regado hoy, **Cuando** intento regarlo de nuevo, **Entonces** se rechaza con `YA_REGADO` y mi energía **no cambia**.
- **CA-4 · Sin cultivo o marchito (negativo).** **Dado** una parcela sin cultivo y otra con un cultivo Marchito, **Cuando** intento regarlas, **Entonces** se rechaza con `SIN_CULTIVO` en la primera y se rechaza sin cambios en la segunda.

### HU-03 · Crecimiento por etapas
**Épica:** Cultivo · **Prioridad:** Must · **Puntos:** 3 · **RF:** RF-03 · **RN:** RN-03, RN-07, RN-08 · **Depende de:** HU-02, HU-10

**Como** jugador **quiero** que mis cultivos regados avancen cada día por Semilla, Brote, Crecimiento y Maduro **para** saber cuándo podré cosechar.

**Criterios de aceptación**
- **CA-1 · Primer día.** **Dado** una Zanahoria (`T = 4`) recién sembrada y regada, **Cuando** termino el día, **Entonces** tiene `díasCrecidos = 1`, está en etapa *Brote* y `díasSinRiego = 0`.
- **CA-2 · Umbrales de etapa.** **Dado** una Zanahoria (`T = 4`, `M = 2`) regada todos los días, **Cuando** pasan 2 y luego 4 días, **Entonces** está en *Crecimiento* con `d = 2` y *Maduro* con `d = 4`; y un Tomate (`T = 7`, `M = 4`) está en *Brote* con `d = 3` y en *Crecimiento* con `d = 4`.
- **CA-3 · Sin riego no crece (negativo).** **Dado** un cultivo en *Brote* que no se regó, **Cuando** termino el día, **Entonces** `díasCrecidos` no cambia y `díasSinRiego` pasa de 0 a 1.
- **CA-4 · Maduro no consume riego (borde).** **Dado** una Zanahoria Madura (`d = 4`), **Cuando** pasan 5 días sin regarla, **Entonces** sigue *Madura* con `d = 4` y no se marchita por sequedad.

### HU-04 · Cosechar cultivo maduro
**Épica:** Cultivo · **Prioridad:** Must · **Puntos:** 3 · **RF:** RF-04 · **RN:** RN-02, RN-10, RN-15 · **Depende de:** HU-03, HU-13

**Como** jugador **quiero** cosechar un cultivo maduro y recibirlo en mi inventario **para** venderlo o conservarlo.

**Criterios de aceptación**
- **CA-1 · Cosecha exitosa.** **Dado** una Zanahoria Madura, inventario con espacio y 100 de energía, **Cuando** la cosecho, **Entonces** recibo 1 Zanahoria, la parcela queda libre y mi energía es 98.
- **CA-2 · Inventario lleno (negativo, prueba crítica).** **Dado** que las 24 casillas contienen otros ítems en pilas de 99, **Cuando** intento cosechar una Zanahoria Madura, **Entonces** se rechaza con `INVENTARIO_LLENO`, el cultivo **sigue Maduro** y mi energía sigue en 100.
- **CA-3 · No maduro (negativo).** **Dado** una Zanahoria en *Brote* con `d = 1` y `T = 4`, **Cuando** intento cosecharla, **Entonces** se rechaza con `NO_MADURO` («faltan 3 días») sin consumir energía.
- **CA-4 · Apilado (borde).** **Dado** una casilla de Zanahoria con 98 unidades y las otras 23 casillas llenas, **Cuando** cosecho una Zanahoria, **Entonces** la pila queda en 99; y si ya tenía 99, se rechaza con `INVENTARIO_LLENO`.

### HU-05 · Marchitamiento por falta de riego y por estación
**Épica:** Cultivo · **Prioridad:** Must · **Puntos:** 5 · **RF:** RF-05 · **RN:** RN-01, RN-03, RN-09 · **Depende de:** HU-03, HU-09, HU-11

**Como** jugador **quiero** que los cultivos descuidados o fuera de estación se marchiten **para** que regar a tiempo y planificar la siembra tenga consecuencias.

**Criterios de aceptación**
- **CA-1 · Tres días sin riego.** **Dado** un cultivo en *Crecimiento*, **Cuando** pasan 2 días sin regarlo, **Entonces** sigue vivo con `díasSinRiego = 2`; y **cuando** pasa el tercero, **Entonces** pasa a *Marchito*.
- **CA-2 · Tolerancia en Sequía.** **Dado** que el clima del día que termina es Sequía y el cultivo lleva 1 día sin riego, **Cuando** termino el día, **Entonces** `díasSinRiego = 2` y el cultivo pasa a *Marchito*.
- **CA-3 · Cambio de estación.** **Dado** una Zanahoria (válida en Primavera) viva el día 28 de Primavera, **Cuando** termino el día, **Entonces** al comenzar el Verano pasa a *Marchito* aunque estuviera *Madura*; y un Maíz (Verano y Otoño) sigue vivo al pasar de Verano a Otoño y se marchita al pasar a Invierno.
- **CA-4 · El riego reinicia el contador (borde).** **Dado** un cultivo con `díasSinRiego = 2`, **Cuando** lo riego y termino el día, **Entonces** `díasSinRiego = 0` y no se marchita.
- **CA-5 · El marchito ocupa la parcela (negativo).** **Dado** un cultivo Marchito, **Cuando** intento sembrar en su parcela, **Entonces** se rechaza con `PARCELA_OCUPADA` y el cultivo ya no crece ni se puede cosechar.

### HU-06 · Limpiar parcela marchita
**Épica:** Cultivo · **Prioridad:** Should · **Puntos:** 1 · **RF:** RF-06 · **RN:** RN-02, RN-10 · **Depende de:** HU-05

**Como** jugador **quiero** limpiar una parcela con un cultivo marchito **para** volver a sembrar en ella.

**Criterios de aceptación**
- **CA-1 · Limpieza exitosa.** **Dado** un cultivo Marchito y 100 de energía, **Cuando** limpio la parcela, **Entonces** queda libre, mi energía es 99 y mi inventario no cambia (no hay cosecha).
- **CA-2 · Sin energía (negativo).** **Dado** 0 de energía, **Cuando** intento limpiarla, **Entonces** se rechaza con `SIN_ENERGIA` y el cultivo sigue marchito.
- **CA-3 · Sin cultivo (negativo).** **Dado** una parcela libre, **Cuando** uso la herramienta Cosechar sobre ella, **Entonces** se rechaza con `SIN_CULTIVO`.
- **CA-4 · Reutilización (borde).** **Dado** una parcela recién limpiada, **Cuando** siembro una nueva semilla válida, **Entonces** la siembra se acepta como en una parcela libre.

### HU-07 · Consultar el estado de una parcela
**Épica:** Cultivo · **Prioridad:** Must · **Puntos:** 2 · **RF:** RF-07 · **RN:** RN-05, RN-08, RN-09, RN-34 · **Depende de:** HU-01

**Como** jugador **quiero** ver el estado de una parcela **para** decidir si regar, esperar o cosechar.

**Criterios de aceptación**
- **CA-1 · Parcela con cultivo.** **Dado** un Tomate con `d = 2` (`T = 7`), regado hoy, **Cuando** dejo el cursor 300 ms sobre la parcela, **Entonces** el *tooltip* muestra «Tomate · Brote · Regado: sí · Faltan 5 días».
- **CA-2 · Parcela libre.** **Dado** una parcela de TIERRA sin cultivo, **Cuando** la consulto, **Entonces** muestra «Tierra de cultivo (libre)».
- **CA-3 · Cultivo marchito.** **Dado** un cultivo Marchito, **Cuando** lo consulto, **Entonces** muestra «Marchito» y no muestra días para madurar.
- **CA-4 · Consulta pura (borde).** **Dado** cualquier parcela, **Cuando** la consulto 10 veces seguidas, **Entonces** el estado del juego es idéntico antes y después (no se consume energía ni se modifica nada).

---

### Épica B · Tiempo y clima

### HU-08 · Administrar la energía diaria
**Épica:** Tiempo y clima · **Prioridad:** Must · **Puntos:** 2 · **RF:** RF-12 · **RN:** RN-02, RN-03 · **Depende de:** —

**Como** jugador **quiero** que cada acción consuma energía y que se restaure cada día **para** tener que priorizar mis tareas.

**Criterios de aceptación**
- **CA-1 · Consumo.** **Dado** 100 de energía, **Cuando** realizo 50 siembras (2 c/u), **Entonces** mi energía llega a 0.
- **CA-2 · Energía insuficiente (negativo).** **Dado** 1 de energía, **Cuando** intento sembrar (costo 2), **Entonces** se rechaza con `SIN_ENERGIA` («necesitas 2 y tienes 1») y mi energía sigue en 1.
- **CA-3 · Restauración.** **Dado** 37 de energía restante, **Cuando** termino el día, **Entonces** mi energía es 100 (la sobrante no se acumula).
- **CA-4 · Límites (borde).** **Dado** una acción de costo 0 (comprar, vender, guardar), **Cuando** la realizo con 0 de energía, **Entonces** se acepta y la energía sigue en 0; y la energía nunca es menor que 0 ni mayor que 100.

### HU-09 · Calendario y estaciones
**Épica:** Tiempo y clima · **Prioridad:** Must · **Puntos:** 3 · **RF:** RF-09 · **RN:** RN-01 · **Depende de:** —

**Como** jugador **quiero** que el tiempo avance por días, estaciones y años **para** planificar mis cultivos a largo plazo.

**Criterios de aceptación**
- **CA-1 · Avance diario.** **Dado** Año 1, Primavera, día 1, **Cuando** termino el día, **Entonces** es Año 1, Primavera, día 2.
- **CA-2 · Cambio de estación.** **Dado** el día 28 de Primavera, **Cuando** termino el día, **Entonces** es Verano, día 1 del mismo año; y el orden es Primavera → Verano → Otoño → Invierno.
- **CA-3 · Cambio de año.** **Dado** el día 28 de Invierno del Año 1, **Cuando** termino el día, **Entonces** es Año 2, Primavera, día 1.
- **CA-4 · Ciclo completo (borde).** **Dado** una partida nueva, **Cuando** termino 112 días seguidos, **Entonces** estoy en Año 2, Primavera, día 1 (el año dura 112 días) y el juego sigue sin fin de partida.

### HU-10 · Terminar el día
**Épica:** Tiempo y clima · **Prioridad:** Must · **Puntos:** 5 · **RF:** RF-08 · **RN:** RN-02, RN-03 · **Depende de:** HU-08, HU-09, HU-11, HU-28

**Como** jugador **quiero** terminar el día y que todos sus efectos se resuelvan en un orden fijo **para** que el resultado sea predecible.

**Criterios de aceptación**
- **CA-1 · Orden de resolución.** **Dado** una partida con pasos registrados, **Cuando** termino el día, **Entonces** se ejecutan exactamente en este orden: clima del día, crecimiento y marchitamiento, animales, mercado, calendario, pronóstico, energía, logros y publicación de eventos (RN-03).
- **CA-2 · Reinicio diario.** **Dado** cultivos regados y energía en 40, **Cuando** termino el día, **Entonces** la energía es 100 y ninguna parcela queda marcada como regada.
- **CA-3 · Rendimiento (borde).** **Dado** una granja a plena carga (40 cultivos al aire libre, 9 en invernadero, 6 edificios y 16 animales), **Cuando** termino el día 100 veces, **Entonces** el percentil 95 es ≤ 200 ms (RNF-01).
- **CA-4 · Falla interna (negativo).** **Dado** un paso que falla internamente, **Cuando** termino el día, **Entonces** se devuelve un `Result` de error y el estado completo queda **idéntico** al de antes (no hay un día a medias).

### HU-11 · Sorteo de clima y pronóstico
**Épica:** Tiempo y clima · **Prioridad:** Must · **Puntos:** 3 · **RF:** RF-10 · **RN:** RN-11, RN-13 · **Depende de:** —

**Como** jugador **quiero** ver el clima de hoy y el pronóstico de mañana **para** anticipar lluvia, sequía o heladas.

**Criterios de aceptación**
- **CA-1 · Inicio.** **Dado** una partida nueva, **Cuando** la inicio, **Entonces** el día 1 es Soleado (fijo) y ya existe un pronóstico sorteado para el día 2.
- **CA-2 · Promoción del pronóstico.** **Dado** el pronóstico «Lluvioso», **Cuando** termino el día, **Entonces** el clima de hoy es Lluvioso y se sortea un nuevo pronóstico visible.
- **CA-3 · Determinismo.** **Dado** dos partidas con la misma semilla, **Cuando** avanzo 112 días en ambas, **Entonces** las secuencias de clima son idénticas día por día.
- **CA-4 · Probabilidades por periodo (borde).** **Dado** 10.000 sorteos, **Entonces** Primavera nunca produce Sequía, Helada ni Nevado; Invierno nunca produce Lluvioso; Otoño produce Helada solo en los días 22 a 28; y el Soleado de Verano queda en 60 % ± 3 %.

### HU-12 · Efectos del clima en los cultivos
**Épica:** Tiempo y clima · **Prioridad:** Must · **Puntos:** 5 · **RF:** RF-05, RF-11 · **RN:** RN-07, RN-09, RN-12 · **Depende de:** HU-05, HU-11

**Como** jugador **quiero** que la lluvia riegue, la sequía castigue y la helada marchite mis cultivos al aire libre **para** que el clima importe en mis decisiones.

**Criterios de aceptación**
- **CA-1 · Lluvia.** **Dado** 5 cultivos al aire libre sin regar y clima del día Lluvioso, **Cuando** termino el día, **Entonces** los 5 quedan regados y crecen `+1` día.
- **CA-2 · Helada (negativo, prueba crítica).** **Dado** clima del día Helada y 3 cultivos al aire libre (uno de ellos Maduro sin cosechar), **Cuando** termino el día, **Entonces** los 3 pasan a *Marchito*.
- **CA-3 · Sequía.** **Dado** clima del día Sequía, **Cuando** riego un cultivo, **Entonces** cuesta 2 de energía; y un cultivo con 1 día sin riego se marchita al terminar ese día (tolerancia 2).
- **CA-4 · Sin efecto (borde).** **Dado** clima Soleado, Nublado o Nevado, **Cuando** termino el día, **Entonces** un cultivo sin regar solo aumenta `díasSinRiego` y ninguno se riega ni se marchita por el clima.

---

### Épica C · Economía

### HU-13 · Administrar el inventario
**Épica:** Economía · **Prioridad:** Must · **Puntos:** 3 · **RF:** RF-13 · **RN:** RN-15 · **Depende de:** —

**Como** jugador **quiero** un inventario con espacio limitado **para** tener que decidir qué conservar y qué vender.

**Criterios de aceptación**
- **CA-1 · Apilado.** **Dado** un inventario vacío de 24 casillas, **Cuando** inserto 120 Zanahorias, **Entonces** ocupan 2 casillas: una de 99 y otra de 21.
- **CA-2 · Inserción atómica (negativo).** **Dado** una casilla de Zanahoria con 98 y las otras 23 casillas llenas con otros ítems, **Cuando** intento insertar 3 Zanahorias, **Entonces** se rechaza con `INVENTARIO_LLENO` y la pila **sigue en 98** (no se inserta ni una).
- **CA-3 · Retiro.** **Dado** 120 Zanahorias, **Cuando** retiro 30, **Entonces** quedan 90; y si intento retirar 130, se rechaza y quedan 120.
- **CA-4 · Capacidad y tipos (borde).** **Dado** un inventario nuevo, **Entonces** tiene 24 casillas, cada una admite un solo tipo de ítem y existen 14 tipos (5 semillas, 5 cosechas, Huevo, Leche, Heno y Medicina) con pila máxima de 99.

### HU-14 · Comprar en el mercado
**Épica:** Economía · **Prioridad:** Must · **Puntos:** 3 · **RF:** RF-14 · **RN:** RN-14, RN-15, RN-16, RN-35 · **Depende de:** HU-13

**Como** jugador **quiero** comprar semillas, heno y medicina a precio fijo **para** abastecer mi granja.

**Criterios de aceptación**
- **CA-1 · Compra exitosa.** **Dado** 500 m, **Cuando** compro 5 semillas de Maíz (30 m c/u), **Entonces** mi saldo es 350 m y tengo 5 semillas más.
- **CA-2 · Fondos insuficientes (negativo).** **Dado** 100 m, **Cuando** intento comprar 3 Medicinas (120 m), **Entonces** se rechaza con `SIN_FONDOS` («faltan 20 m») y nada cambia.
- **CA-3 · Sin espacio (negativo).** **Dado** un inventario sin espacio para 10 Heno, **Cuando** intento comprarlos, **Entonces** se rechaza con `INVENTARIO_LLENO` y mi saldo no cambia.
- **CA-4 · Cantidad fuera de rango (borde).** **Dado** una compra de 0 o de 100 unidades, **Cuando** la intento, **Entonces** se rechaza con `CANTIDAD_INVALIDA` (rango 1–99); y comprar semillas de Calabaza en Primavera **sí** se permite (solo se restringe el sembrar).

### HU-15 · Vender con precio dinámico
**Épica:** Economía · **Prioridad:** Must · **Puntos:** 5 · **RF:** RF-15 · **RN:** RN-14, RN-17, RN-18 · **Depende de:** HU-04, HU-13

**Como** jugador **quiero** vender mis cosechas y productos a un precio que cambia con la temporada y con lo mucho que vendo **para** decidir cuándo y cuánto vender.

**Criterios de aceptación**
- **CA-1 · Venta con saturación.** **Dado** Primavera, demanda de Zanahoria en 100 y 3 Zanahorias, **Cuando** las vendo, **Entonces** recibo 79 m (27 + 26 + 26) y la demanda queda en 94.
- **CA-2 · Saturación fuerte (borde, prueba crítica).** **Dado** que ya vendí 10 Zanahorias en el día (demanda 80), **Cuando** consulto el precio de la siguiente, **Entonces** es 22 m y no 27 m.
- **CA-3 · Piso de demanda (borde).** **Dado** demanda 50 de Zanahoria en Primavera, **Cuando** vendo una más, **Entonces** el precio es 14 m y la demanda **permanece en 50** (nunca baja de 50).
- **CA-4 · Recuperación.** **Dado** demanda 80, **Cuando** termino el día, **Entonces** la demanda es 85; y si estaba en 98, queda en 100 (tope).
- **CA-5 · Factor de temporada.** **Dado** demanda 100, **Cuando** vendo 1 Calabaza en Primavera (fuera de estación, factor 120), **Entonces** recibo 132 m; y en Otoño (factor 90) recibo 99 m.
- **CA-6 · Cantidad superior a la existencia (negativo).** **Dado** 2 Zanahorias, **Cuando** intento vender 5, **Entonces** se rechaza con `CANTIDAD_INVALIDA` y el inventario, el saldo y la demanda no cambian.

### HU-16 · Tendencia de precio
**Épica:** Economía · **Prioridad:** Should · **Puntos:** 2 · **RF:** RF-16 · **RN:** RN-19 · **Depende de:** HU-10, HU-15

**Como** jugador **quiero** ver si el precio de un producto sube, baja o sigue igual **para** elegir el mejor momento de venta.

**Criterios de aceptación**
- **CA-1 · Inicio.** **Dado** una partida nueva, **Entonces** todos los productos muestran ▬ (igual).
- **CA-2 · Sube.** **Dado** Zanahoria en Primavera con demanda 80 (precio 22 m), **Cuando** termino el día (precio anterior 22, demanda 85), **Entonces** el precio actual es 23 m y se muestra ▲.
- **CA-3 · Baja.** **Dado** precio anterior 23 m y demanda 85, **Cuando** vendo 10 unidades en el día (demanda 65, precio 18 m), **Entonces** se muestra ▼.
- **CA-4 · Igual (borde).** **Dado** demanda 100 antes y después de terminar el día, **Cuando** consulto la tendencia, **Entonces** se muestra ▬ y el indicador incluye texto o símbolo, no solo color.

### HU-17 · Resumen del día
**Épica:** Economía · **Prioridad:** Could · **Puntos:** 2 · **RF:** RF-17 · **RN:** RN-03, RN-14 · **Depende de:** HU-10, HU-15

**Como** jugador **quiero** un resumen al terminar el día **para** entender qué ganó, gastó y qué ocurrió.

**Criterios de aceptación**
- **CA-1 · Ingresos y gastos.** **Dado** un día con una venta de 79 m y una compra de 150 m, **Cuando** termino el día, **Entonces** el resumen muestra ingresos 79 m, gastos 150 m y saldo anterior y nuevo.
- **CA-2 · Eventos relevantes.** **Dado** que un cultivo se marchitó y se desbloqueó un logro, **Cuando** termino el día, **Entonces** el resumen los lista con su descripción.
- **CA-3 · Día sin novedades (borde).** **Dado** un día sin movimientos, **Cuando** termino el día, **Entonces** muestra ingresos 0 m, gastos 0 m y «Sin novedades».
- **CA-4 · Reinicio (borde).** **Dado** el resumen del día anterior, **Cuando** termino el siguiente día, **Entonces** los acumulados parten en 0 (no se mezclan días).

---

### Épica D · Construcción y animales

### HU-18 · Construir edificios
**Épica:** Construcción y animales · **Prioridad:** Must · **Puntos:** 5 · **RF:** RF-18 · **RN:** RN-04, RN-14, RN-20, RN-21, RN-29 · **Depende de:** HU-28

**Como** jugador **quiero** construir edificios sobre hierba libre **para** ampliar mi granja.

**Criterios de aceptación**
- **CA-1 · Construcción exitosa.** **Dado** 500 m y un área de 2 × 2 de HIERBA libre con esquina en (4,6), **Cuando** construyo un Gallinero (400 m), **Entonces** mi saldo es 100 m y esas 4 parcelas quedan ocupadas.
- **CA-2 · Ubicación inválida (negativo).** **Dado** un área que incluye una parcela de TIERRA, o que sobresale del mapa (esquina en la columna 15 con ancho 2), **Cuando** intento construir, **Entonces** se rechaza con `UBICACION_INVALIDA` y nada cambia.
- **CA-3 · Fondos insuficientes (negativo).** **Dado** 399 m, **Cuando** intento construir un Gallinero, **Entonces** se rechaza con `SIN_FONDOS` («faltan 1 m»).
- **CA-4 · Límite por tipo (borde).** **Dado** 2 Gallineros ya construidos, **Cuando** intento un tercero, **Entonces** se rechaza con `LIMITE_EDIFICIOS`.
- **CA-5 · Edificio bloqueado (negativo).** **Dado** que no tengo el logro LG-02, **Cuando** intento construir un Establo, **Entonces** se rechaza con `EDIFICIO_BLOQUEADO` («requiere el logro «Manos a la tierra»»).
- **CA-6 · Solape (negativo).** **Dado** un Gallinero existente, **Cuando** intento construir otro cuya área toca una de sus parcelas, **Entonces** se rechaza con `UBICACION_INVALIDA`.

### HU-19 · Comprar animales
**Épica:** Construcción y animales · **Prioridad:** Must · **Puntos:** 3 · **RF:** RF-19 · **RN:** RN-14, RN-20, RN-23 · **Depende de:** HU-14, HU-18

**Como** jugador **quiero** comprar gallinas y vacas y alojarlas en mis edificios **para** obtener productos animales.

**Criterios de aceptación**
- **CA-1 · Compra exitosa.** **Dado** un Gallinero vacío y 500 m, **Cuando** compro una Gallina (150 m), **Entonces** mi saldo es 350 m y una Gallina *Sana* con `díasSinAlimento = 0` queda alojada en ese Gallinero.
- **CA-2 · Sin edificio compatible (negativo).** **Dado** que no tengo Gallinero (o no tengo Establo), **Cuando** intento comprar una Gallina (o una Vaca), **Entonces** se rechaza con `SIN_ESPACIO_ANIMALES` y mi saldo no cambia.
- **CA-3 · Capacidad (borde).** **Dado** un Gallinero con 4 Gallinas, **Cuando** compro una quinta, **Entonces** se rechaza con `SIN_ESPACIO_ANIMALES`; y con un segundo Gallinero con plaza, se aloja en este (primer edificio con plaza libre).
- **CA-4 · Fondos (negativo).** **Dado** 149 m, **Cuando** intento comprar una Gallina (150 m), **Entonces** se rechaza con `SIN_FONDOS` («faltan 1 m»).

### HU-20 · Alimentar animales
**Épica:** Construcción y animales · **Prioridad:** Must · **Puntos:** 3 · **RF:** RF-20 · **RN:** RN-02, RN-15, RN-24 · **Depende de:** HU-19, HU-13

**Como** jugador **quiero** alimentar a todos los animales de un edificio de una vez **para** mantenerlos sanos y productivos.

**Criterios de aceptación**
- **CA-1 · Alimentación completa.** **Dado** un Gallinero con 3 Gallinas sanas sin comer, 5 Heno y 100 de energía, **Cuando** uso Alimentar sobre él, **Entonces** me quedan 2 Heno, mi energía es 97 y las 3 quedan alimentadas hoy.
- **CA-2 · Falta de heno (negativo).** **Dado** 2 Heno y 3 animales por alimentar, **Cuando** uso Alimentar, **Entonces** se rechaza con `SIN_HENO` y **no se consume nada**.
- **CA-3 · Falta de energía (negativo).** **Dado** 2 de energía y 3 animales por alimentar, **Cuando** uso Alimentar, **Entonces** se rechaza con `SIN_ENERGIA` y no se consume nada.
- **CA-4 · Animales enfermos (borde).** **Dado** 2 Gallinas sanas y 1 Enferma, **Cuando** uso Alimentar, **Entonces** se alimentan 2 (−2 Heno, −2 energía), la enferma se omite y se avisa con `ANIMAL_ENFERMO`.
- **CA-5 · Ya alimentados (borde).** **Dado** que todos comieron hoy, **Cuando** uso Alimentar, **Entonces** se informa que no hay nada que alimentar y no se consume nada.

### HU-21 · Estados del animal
**Épica:** Construcción y animales · **Prioridad:** Must · **Puntos:** 3 · **RF:** RF-21 · **RN:** RN-03, RN-25 · **Depende de:** HU-10, HU-20

**Como** jugador **quiero** que mis animales se pongan hambrientos y enfermen si los descuido **para** que cuidarlos sea una decisión con consecuencias.

**Criterios de aceptación**
- **CA-1 · Hambre y enfermedad.** **Dado** una Gallina sana que no se alimenta, **Cuando** pasan 1, 2 y 3 días, **Entonces** está *Hambrienta* con `díasSinAlimento = 1`, *Hambrienta* con 2 y *Enferma* con 3.
- **CA-2 · Recuperación al alimentar.** **Dado** una Gallina *Hambrienta* con `díasSinAlimento = 2` que se alimentó hoy, **Cuando** termino el día, **Entonces** está *Sana* con `díasSinAlimento = 0`.
- **CA-3 · Enfermo no se alimenta (negativo).** **Dado** una Gallina *Enferma*, **Cuando** termino el día, **Entonces** sigue *Enferma*, no consume Heno y su contador no cambia.
- **CA-4 · Marca diaria (borde).** **Dado** una Gallina alimentada ayer, **Cuando** comienza el nuevo día, **Entonces** `alimentadoHoy` es falso y, si no la alimento, cuenta como día sin alimento.

### HU-22 · Producción y recolección
**Épica:** Construcción y animales · **Prioridad:** Must · **Puntos:** 3 · **RF:** RF-22 · **RN:** RN-02, RN-15, RN-26 · **Depende de:** HU-21, HU-13

**Como** jugador **quiero** que los animales sanos y alimentados produzcan y poder recolectar sus productos **para** venderlos.

**Criterios de aceptación**
- **CA-1 · Producción y recolección.** **Dado** una Gallina *Sana* alimentada hoy, **Cuando** termino el día, **Entonces** el Gallinero tiene 1 Huevo pendiente; y **cuando** recolecto, mi inventario gana 1 Huevo y mi energía baja 1.
- **CA-2 · Animal enfermo no produce (negativo, prueba crítica).** **Dado** una Gallina *Enferma*, **Cuando** termino el día, **Entonces** el Gallinero no suma ningún producto pendiente.
- **CA-3 · Tope de pendientes (borde).** **Dado** una Gallina alimentada 3 días seguidos sin recolectar, **Cuando** pasan los 3 días, **Entonces** el Gallinero tiene 2 Huevos pendientes (máximo por animal) y no 3; una Vaca equivalente produce Leche con la misma regla.
- **CA-4 · Inventario lleno o nada que recoger (negativo).** **Dado** 2 Huevos pendientes y sin espacio en el inventario, **Cuando** recolecto, **Entonces** se rechaza con `INVENTARIO_LLENO` y los pendientes no se mueven; y con 0 pendientes se informa que no hay nada que recoger.

### HU-23 · Curar animales enfermos
**Épica:** Construcción y animales · **Prioridad:** Should · **Puntos:** 2 · **RF:** RF-23 · **RN:** RN-02, RN-16, RN-27 · **Depende de:** HU-14, HU-21

**Como** jugador **quiero** curar a un animal enfermo con medicina **para** recuperar su producción.

**Criterios de aceptación**
- **CA-1 · Curación exitosa.** **Dado** una Gallina *Enferma*, 1 Medicina y 100 de energía, **Cuando** la curo, **Entonces** pasa a *Sana* con `díasSinAlimento = 0`, me quedan 0 Medicinas y mi energía es 99.
- **CA-2 · Sin medicina (negativo).** **Dado** 0 Medicinas, **Cuando** intento curarla, **Entonces** se rechaza con `SIN_MEDICINA` y sigue *Enferma*.
- **CA-3 · Sin energía (negativo).** **Dado** 0 de energía y 1 Medicina, **Cuando** intento curarla, **Entonces** se rechaza con `SIN_ENERGIA` y conservo la Medicina.
- **CA-4 · Producción tras curar (borde).** **Dado** una Gallina recién curada que alimento ese mismo día, **Cuando** termino el día, **Entonces** produce 1 Huevo pendiente.

### HU-24 · Construir y usar el almacén
**Épica:** Construcción y animales · **Prioridad:** Should · **Puntos:** 2 · **RF:** RF-25 · **RN:** RN-15, RN-20, RN-29 · **Depende de:** HU-18, HU-27

**Como** jugador **quiero** construir un almacén **para** ampliar mi inventario.

**Criterios de aceptación**
- **CA-1 · Ampliación.** **Dado** el logro LG-01 y 300 m o más, **Cuando** construyo un Almacén (2 × 2), **Entonces** mi saldo baja 300 m y el inventario pasa de 24 a 36 casillas.
- **CA-2 · Límite (negativo).** **Dado** un Almacén ya construido, **Cuando** intento otro, **Entonces** se rechaza con `LIMITE_EDIFICIOS`.
- **CA-3 · Bloqueado (negativo).** **Dado** que no tengo LG-01, **Cuando** intento construirlo, **Entonces** se rechaza con `EDIFICIO_BLOQUEADO`.
- **CA-4 · Uso del espacio (borde).** **Dado** las 24 casillas llenas, **Cuando** construyo el Almacén e inserto un ítem nuevo, **Entonces** se acepta en la casilla 25.

### HU-25 · Construir y usar el invernadero
**Épica:** Construcción y animales · **Prioridad:** Could · **Puntos:** 5 · **RF:** RF-24 · **RN:** RN-06, RN-09, RN-12, RN-20, RN-22 · **Depende de:** HU-12, HU-27

**Como** jugador **quiero** un invernadero **para** cultivar en cualquier estación y proteger mis plantas.

**Criterios de aceptación**
- **CA-1 · Construcción.** **Dado** el logro LG-05 y 1.500 m, **Cuando** construyo un Invernadero (3 × 3), **Entonces** mi saldo baja 1.500 m y sus 9 parcelas quedan como cultivo protegido.
- **CA-2 · Siembra sin restricción de estación.** **Dado** que es Primavera, **Cuando** siembro Calabaza dentro del invernadero, **Entonces** se acepta (fuera no se permitiría).
- **CA-3 · Inmunidad (borde).** **Dado** clima del día Helada o Sequía, **Cuando** termino el día, **Entonces** los cultivos del invernadero siguen vivos y los de fuera de él se marchitan según RN-09; y al cambiar de estación no se marchitan.
- **CA-4 · Sin lluvia (negativo).** **Dado** clima del día Lluvioso, **Cuando** termino el día, **Entonces** los cultivos del invernadero **no** quedan regados y deben regarse a mano (1 de energía).
- **CA-5 · Bloqueado (negativo).** **Dado** que no tengo LG-05, **Cuando** intento construirlo, **Entonces** se rechaza con `EDIFICIO_BLOQUEADO`.

---

### Épica E · Progresión y persistencia

### HU-26 · Registrar logros
**Épica:** Progresión y persistencia · **Prioridad:** Must · **Puntos:** 5 · **RF:** RF-26 · **RN:** RN-28 · **Depende de:** HU-04, HU-10, HU-15, HU-18, HU-23

**Como** jugador **quiero** que mis hitos se registren como logros con recompensa **para** sentir progreso.

**Criterios de aceptación**
- **CA-1 · Primer logro.** **Dado** 0 cosechas, **Cuando** cosecho 1 cultivo, **Entonces** se desbloquea LG-01 «Primera cosecha», recibo 50 m y se publica el evento de logro.
- **CA-2 · Una sola vez (borde).** **Dado** LG-01 ya desbloqueado, **Cuando** cosecho de nuevo, **Entonces** no recibo otra recompensa.
- **CA-3 · Qué cuenta (negativo).** **Dado** 24 cosechas y 1 limpieza de parcela marchita, **Entonces** LG-02 no se concede; y **cuando** hago la cosecha número 25, **Entonces** se concede y recibo 150 m.
- **CA-4 · Condiciones por saldo y año.** **Dado** un saldo que alcanza 3.000 m, **Entonces** LG-05 se desbloquea y paga 200 m; y al llegar al Año 2, Primavera, día 1, se desbloquea LG-09 y paga 500 m.
- **CA-5 · Otros contadores.** **Dado** que construyo mi primer edificio, tengo 4 animales a la vez o curo un animal, **Entonces** se conceden LG-06 (100 m), LG-07 (150 m) o LG-08 (80 m) respectivamente.

### HU-27 · Desbloquear edificios por logros
**Épica:** Progresión y persistencia · **Prioridad:** Should · **Puntos:** 2 · **RF:** RF-27 · **RN:** RN-28, RN-29 · **Depende de:** HU-18, HU-26

**Como** jugador **quiero** que algunos edificios se habiliten al cumplir logros **para** tener metas con sentido.

**Criterios de aceptación**
- **CA-1 · Estado inicial.** **Dado** una partida nueva, **Entonces** solo el Gallinero está disponible; Almacén, Establo e Invernadero están bloqueados.
- **CA-2 · Desbloqueo.** **Dado** que consigo LG-01, LG-02 o LG-05, **Entonces** quedan disponibles respectivamente el Almacén, el Establo y el Invernadero.
- **CA-3 · Mensaje de bloqueo (negativo).** **Dado** un edificio bloqueado, **Cuando** intento construirlo, **Entonces** se rechaza con `EDIFICIO_BLOQUEADO` y el mensaje nombra el logro requerido.
- **CA-4 · Permanencia (borde).** **Dado** un edificio desbloqueado, **Cuando** guardo y cargo la partida, **Entonces** sigue desbloqueado.

### HU-28 · Iniciar nueva partida
**Épica:** Progresión y persistencia · **Prioridad:** Must · **Puntos:** 3 · **RF:** RF-28 · **RN:** RN-04, RN-13, RN-30 · **Depende de:** HU-09, HU-11, HU-13

**Como** jugador **quiero** empezar una partida nueva con un estado inicial claro **para** comenzar a jugar sin preparación.

**Criterios de aceptación**
- **CA-1 · Valores iniciales.** **Dado** que elijo Nueva partida, **Entonces** el estado es Año 1, Primavera, día 1, Soleado, 500 m, energía 100, inventario con 10 semillas de Zanahoria y 5 de Papa, sin edificios ni animales, demanda 100 en todos los productos y logros bloqueados con contadores en 0.
- **CA-2 · Mapa inicial.** **Dado** una partida nueva, **Entonces** el mapa tiene 16 × 10 = 160 parcelas: 40 de TIERRA (bloque 8 × 5), 102 de HIERBA, 6 de AGUA y 12 de OBSTÁCULO.
- **CA-3 · Partida en curso (negativo).** **Dado** una partida en curso, **Cuando** elijo Nueva partida, **Entonces** se pide confirmación; si cancelo, la partida actual queda intacta.
- **CA-4 · Determinismo (borde).** **Dado** la misma semilla del generador, **Cuando** creo dos partidas nuevas, **Entonces** ambas tienen el mismo pronóstico del día 2.

### HU-29 · Guardar partida
**Épica:** Progresión y persistencia · **Prioridad:** Must · **Puntos:** 5 · **RF:** RF-29 · **RN:** RN-31, RN-32 · **Depende de:** HU-28

**Como** jugador **quiero** guardar mi partida en una de 3 ranuras **para** continuarla más tarde.

**Criterios de aceptación**
- **CA-1 · Guardado correcto.** **Dado** una partida en curso, **Cuando** guardo en la ranura 2, **Entonces** se crea `slot2` con `schemaVersion = 1`, `savedAt`, `state` y un `checksum` SHA-256 del `state`.
- **CA-2 · Escritura atómica (negativo).** **Dado** un archivo previo en la ranura y una falla de escritura (disco lleno o sin permisos simulados), **Cuando** guardo, **Entonces** se informa `ERROR_ES`, el archivo previo **queda intacto** y la partida sigue en juego.
- **CA-3 · Sobrescritura.** **Dado** una ranura ocupada, **Cuando** elijo guardar en ella, **Entonces** se pide confirmación y, al aceptar, el archivo se reemplaza.
- **CA-4 · Tamaño y tiempo (borde).** **Dado** una granja a plena carga, **Cuando** guardo, **Entonces** el archivo pesa ≤ 200 KB y la operación toma ≤ 1 s.

### HU-30 · Cargar partida
**Épica:** Progresión y persistencia · **Prioridad:** Must · **Puntos:** 5 · **RF:** RF-30 · **RN:** RN-32, RN-33 · **Depende de:** HU-29

**Como** jugador **quiero** cargar una partida guardada y recuperarla exactamente **para** seguir donde la dejé.

**Criterios de aceptación**
- **CA-1 · Estado idéntico (prueba crítica).** **Dado** una partida guardada, **Cuando** la cargo, **Entonces** el estado cargado es estructuralmente **idéntico** al guardado (calendario, energía, dinero, inventario, cultivos, edificios, animales, mercado, clima, logros y contadores).
- **CA-2 · Continuidad aleatoria.** **Dado** una partida guardada y cargada, **Cuando** avanzo 5 días en la original y 5 en la cargada, **Entonces** ambas producen la misma secuencia de climas.
- **CA-3 · Reemplazo del estado.** **Dado** una partida en curso distinta, **Cuando** cargo una ranura válida, **Entonces** el estado actual se reemplaza y el HUD refleja la partida cargada.
- **CA-4 · Tiempo (borde).** **Dado** una partida a plena carga, **Cuando** cargo, **Entonces** la operación toma ≤ 1 s.

### HU-31 · Rechazar archivos inválidos sin alterar la partida
**Épica:** Progresión y persistencia · **Prioridad:** Must · **Puntos:** 3 · **RF:** RF-31 · **RN:** RN-33, RN-35 · **Depende de:** HU-30

**Como** jugador **quiero** que un archivo dañado o inexistente se rechace con un mensaje claro **para** no perder mi partida actual.

**Criterios de aceptación**
- **CA-1 · Archivo inexistente (prueba crítica).** **Dado** una ranura 3 sin archivo, **Cuando** intento cargarla, **Entonces** se rechaza con `ARCHIVO_INEXISTENTE` («No existe una partida en la ranura 3.») y mi partida actual no cambia.
- **CA-2 · Archivo corrupto (prueba crítica).** **Dado** un archivo con un carácter alterado o JSON truncado, **Cuando** intento cargarlo, **Entonces** se rechaza con `ARCHIVO_CORRUPTO` y mi partida actual tiene **0 diferencias** respecto a antes.
- **CA-3 · Versión no soportada.** **Dado** un archivo con `schemaVersion = 99`, **Cuando** intento cargarlo, **Entonces** se rechaza con `VERSION_NO_SOPORTADA`.
- **CA-4 · Invariante violada (borde).** **Dado** un archivo con dinero −5 y un checksum recalculado de forma válida, **Cuando** intento cargarlo, **Entonces** se rechaza con `ARCHIVO_CORRUPTO`; y la verificación se detiene en el primer fallo según el orden de RN-33 (un JSON mal formado se detecta antes que el checksum).
- **CA-5 · Error de lectura (negativo).** **Dado** un fallo de E/S al leer, **Cuando** intento cargar, **Entonces** se informa `ERROR_ES` y mi partida no cambia.

### HU-32 · Listar ranuras de guardado
**Épica:** Progresión y persistencia · **Prioridad:** Should · **Puntos:** 2 · **RF:** RF-32 · **RN:** RN-31 · **Depende de:** HU-29

**Como** jugador **quiero** ver qué hay en cada ranura **para** elegir cuál guardar o cargar.

**Criterios de aceptación**
- **CA-1 · Listado.** **Dado** las ranuras 1 y 3 ocupadas y la 2 vacía, **Cuando** abro el listado, **Entonces** veo «Ranura 1: Año 1 · Verano · día 5 · 1.250 m · 2026-11-22 10:30», «Ranura 2: Vacía» y la ranura 3 con sus datos.
- **CA-2 · Archivo dañado (negativo).** **Dado** una ranura cuyo archivo está dañado, **Cuando** abro el listado, **Entonces** se muestra «Dañada» y el resto del listado sigue funcionando.
- **CA-3 · Actualización (borde).** **Dado** una ranura vacía, **Cuando** guardo en ella, **Entonces** el listado muestra sus metadatos sin reiniciar la aplicación.

---

### Épica F · Interfaz

### HU-33 · Pantalla de inicio
**Épica:** Interfaz · **Prioridad:** Must · **Puntos:** 3 · **RF:** RF-33 · **RN:** RN-30, RN-31 · **Depende de:** HU-28

**Como** jugador **quiero** una pantalla de inicio **para** elegir si empiezo, cargo o salgo.

**Criterios de aceptación**
- **CA-1 · Arranque.** **Dado** que ejecuto el `.jar`, **Entonces** en ≤ 5 s veo la pantalla con los botones «Nueva partida», «Cargar» y «Salir».
- **CA-2 · Nueva partida.** **Cuando** pulso «Nueva partida», **Entonces** paso a la pantalla principal con una partida inicial.
- **CA-3 · Cargar sin ranuras (negativo).** **Dado** las 3 ranuras vacías, **Cuando** pulso «Cargar», **Entonces** se informa que no hay partidas guardadas y permanezco en el inicio.
- **CA-4 · Salir (borde).** **Cuando** pulso «Salir», **Entonces** la aplicación se cierra sin errores ni procesos residuales.

### HU-34 · HUD superior
**Épica:** Interfaz · **Prioridad:** Must · **Puntos:** 3 · **RF:** RF-34 · **RN:** RN-01, RN-02, RN-11, RN-14 · **Depende de:** HU-10, HU-28

**Como** jugador **quiero** ver siempre dinero, fecha, clima y energía **para** decidir con información a la vista.

**Criterios de aceptación**
- **CA-1 · Valores iniciales.** **Dado** una partida nueva, **Entonces** el HUD muestra «500 m», «Año 1 · Primavera · Día 1», clima Soleado con su pronóstico y energía «100/100».
- **CA-2 · Actualización por eventos.** **Dado** una siembra que baja la energía a 98, **Entonces** el HUD muestra «98/100» sin consultas periódicas (se actualiza al recibir el evento).
- **CA-3 · Energía agotada (borde).** **Dado** 0 de energía, **Entonces** la barra aparece vacía y el HUD muestra «0/100» (la información no depende solo del color).
- **CA-4 · Cambio de día.** **Cuando** termino el día, **Entonces** el HUD muestra el día siguiente y el nuevo pronóstico.

### HU-35 · Mapa de la granja
**Épica:** Interfaz · **Prioridad:** Must · **Puntos:** 5 · **RF:** RF-35 · **RN:** RN-04, RN-08, RN-20 · **Depende de:** HU-28

**Como** jugador **quiero** ver mi granja dibujada como un mapa **para** reconocer mis parcelas, cultivos, edificios y animales.

**Criterios de aceptación**
- **CA-1 · Terreno.** **Dado** una partida nueva, **Entonces** se dibujan 16 × 10 parcelas y se distinguen TIERRA, HIERBA, AGUA y OBSTÁCULO (con gráficos vectoriales si no hay sprites).
- **CA-2 · Etapas del cultivo.** **Dado** cultivos en Semilla, Brote, Crecimiento, Maduro y Marchito, **Entonces** cada etapa se dibuja con un aspecto **distinto**.
- **CA-3 · Edificios y animales.** **Dado** un Gallinero con 2 Gallinas, **Entonces** el edificio y los animales se dibujan en su posición.
- **CA-4 · Clic fuera del mapa (negativo).** **Cuando** hago clic fuera del área del mapa, **Entonces** no ocurre ninguna acción ni error.
- **CA-5 · Redibujo (borde).** **Dado** un evento del juego, **Entonces** el mapa se redibuja por el evento (no por un bucle de consulta) y es legible desde 1024 × 768 sin recortes.

### HU-36 · Herramientas y clic sobre parcelas
**Épica:** Interfaz · **Prioridad:** Must · **Puntos:** 5 · **RF:** RF-36 · **RN:** RN-02, RN-34 · **Depende de:** HU-01, HU-35

**Como** jugador **quiero** elegir una herramienta y actuar con clic sobre una parcela **para** jugar de forma directa.

**Criterios de aceptación**
- **CA-1 · Selección.** **Cuando** pulso `1` a `5` o un botón de la barra, **Entonces** se selecciona Sembrar, Regar, Cosechar, Construir o Alimentar respectivamente y la barra la marca; `Esc` la deselecciona.
- **CA-2 · Resaltado y acción.** **Dado** la herramienta Sembrar con semilla seleccionada, **Cuando** paso el cursor por una parcela, **Entonces** se resalta; y **cuando** hago clic, **Entonces** se ejecuta lo de HU-01.
- **CA-3 · Rechazo (negativo).** **Dado** una acción rechazada (por ejemplo, sin energía), **Cuando** hago clic, **Entonces** el estado no cambia y se muestra el mensaje en español del código devuelto; y sin herramienta seleccionada el clic no hace nada.
- **CA-4 · Cosechar polivalente (borde).** **Dado** la herramienta Cosechar, **Entonces** sobre un cultivo Maduro cosecha, sobre un Marchito limpia y sobre un edificio recolecta sus productos.
- **CA-5 · Atajos globales.** **Cuando** pulso `Enter`, `M`, `I`, `L`, `F5` o `F9`, **Entonces** termino el día, abro el mercado, el inventario, los logros, guardo rápido o cargo rápido, respectivamente.

### HU-37 · Inventario en grilla
**Épica:** Interfaz · **Prioridad:** Must · **Puntos:** 3 · **RF:** RF-37 · **RN:** RN-15 · **Depende de:** HU-13, HU-36

**Como** jugador **quiero** ver mi inventario como una grilla de casillas **para** reconocer qué tengo de un vistazo.

**Criterios de aceptación**
- **CA-1 · Grilla.** **Dado** una partida nueva, **Cuando** abro el inventario con `I`, **Entonces** veo 24 casillas, con icono y cantidad en las ocupadas (Zanahoria 10, Papa 5) y las demás vacías.
- **CA-2 · Almacén.** **Dado** un Almacén construido, **Entonces** la grilla muestra 36 casillas.
- **CA-3 · Selección de semilla.** **Cuando** selecciono la casilla de Papa, **Entonces** queda marcada y la herramienta Sembrar usa Papa; y si la semilla se agota, la selección se limpia.
- **CA-4 · Pila máxima (borde).** **Dado** una pila de 99 Zanahorias, **Entonces** se muestra «99» y la siguiente unidad ocupa otra casilla.

### HU-38 · Mercado modal
**Épica:** Interfaz · **Prioridad:** Must · **Puntos:** 5 · **RF:** RF-38 · **RN:** RN-16, RN-17, RN-19 · **Depende de:** HU-14, HU-15, HU-16

**Como** jugador **quiero** un mercado con pestañas Comprar y Vender **para** operar con precios claros.

**Criterios de aceptación**
- **CA-1 · Apertura modal.** **Cuando** pulso `M`, **Entonces** se abre un diálogo modal con las pestañas «Comprar» y «Vender» y el mapa no responde a clics mientras esté abierto; `Esc` lo cierra.
- **CA-2 · Venta con previsión.** **Dado** 3 Zanahorias en Primavera, **Entonces** la pestaña Vender muestra el precio actual 27 m con su tendencia (▲, ▼ o ▬ con texto), y al fijar la cantidad en 3 muestra un total previsto de 79 m.
- **CA-3 · Fondos insuficientes (negativo).** **Dado** 100 m, **Cuando** intento comprar una Vaca (800 m), **Entonces** se muestra el mensaje de `SIN_FONDOS`, el saldo no cambia y el botón de compra queda deshabilitado.
- **CA-4 · Rango de cantidad (borde).** **Entonces** el selector de cantidad solo admite de 1 a 99 en Comprar y de 1 a la existencia en Vender.

### HU-39 · Notificaciones (toasts)
**Épica:** Interfaz · **Prioridad:** Should · **Puntos:** 3 · **RF:** RF-39 · **RN:** RN-28, RN-35 · **Depende de:** HU-26, HU-35

**Como** jugador **quiero** avisos breves que aparecen y se desvanecen **para** enterarme de logros, errores y eventos sin interrumpir el juego.

**Criterios de aceptación**
- **CA-1 · Duración.** **Cuando** se publica un evento de éxito o información, **Entonces** el *toast* se muestra 3 s con un desvanecimiento de 0,4 s; y el de un logro dura 5 s.
- **CA-2 · Errores.** **Dado** un rechazo (por ejemplo, `FUERA_DE_ESTACION`), **Entonces** aparece un *toast* de tipo error con el mensaje en español de RN-35.
- **CA-3 · Máximo visible (borde).** **Dado** 5 eventos simultáneos, **Entonces** solo hay 4 *toasts* visibles y el quinto espera en cola hasta que uno desaparezca.
- **CA-4 · Evento de estación.** **Cuando** comienza el Verano, **Entonces** aparece un *toast* informativo con la nueva estación.

### HU-40 · Efectos visuales de estación y clima
**Épica:** Interfaz · **Prioridad:** Could · **Puntos:** 5 · **RF:** RF-40 · **RN:** RN-01, RN-11, RN-12 · **Depende de:** HU-11, HU-35

**Como** jugador **quiero** que la estación y el clima se vean en el mapa **para** sentir el paso del tiempo.

**Criterios de aceptación**
- **CA-1 · Paleta por estación.** **Dado** las 4 estaciones, **Entonces** el mapa usa una paleta distinta en cada una y el tinte de noche se aplica durante la transición de fin de día.
- **CA-2 · Partículas e indicadores.** **Dado** clima Lluvioso o Nevado, **Entonces** se muestran partículas de lluvia o de nieve; y con Sequía o Helada aparece un indicador visual propio.
- **CA-3 · Solo vista (negativo).** **Dado** los efectos activados o desactivados, **Cuando** juego la misma secuencia de acciones, **Entonces** el estado del juego es idéntico en ambos casos.
- **CA-4 · Rendimiento (borde).** **Dado** partículas activas, **Entonces** la interfaz mantiene ≥ 30 FPS y los efectos se pueden desactivar.

### HU-41 · Animaciones suaves
**Épica:** Interfaz · **Prioridad:** Could · **Puntos:** 3 · **RF:** RF-41 · **RN:** RN-08, RN-25 · **Depende de:** HU-35, HU-42

**Como** jugador **quiero** que cultivos y animales se muevan con suavidad **para** que la granja se sienta viva.

**Criterios de aceptación**
- **CA-1 · Animales.** **Dado** un Gallinero con Gallinas, **Entonces** cada animal se anima con 2 o 3 cuadros.
- **CA-2 · Crecimiento.** **Dado** un cultivo que cambia de etapa, **Entonces** la transición visual dura ≤ 500 ms.
- **CA-3 · Independencia del estado (negativo).** **Dado** que termino el día, **Entonces** el estado ya avanzó **antes** de la animación de transición, y cerrar la aplicación durante la animación no pierde ni altera el estado.
- **CA-4 · Reloj propio (borde).** **Entonces** las animaciones las mueve un `javax.swing.Timer` de la vista y desactivarlas no cambia el estado del juego.

### HU-42 · Carga de sprites con respaldo vectorial
**Épica:** Interfaz · **Prioridad:** Should · **Puntos:** 3 · **RF:** RF-42 · **RN:** RN-08, RN-25 · **Depende de:** HU-35

**Como** jugador **quiero** ver sprites en lugar de formas básicas **para** que el juego tenga aspecto de videojuego, sin que falle si faltan imágenes.

**Criterios de aceptación**
- **CA-1 · Caché.** **Dado** que se piden 2 veces los mismos sprites a través del `AssetManager`, **Entonces** el archivo se lee una sola vez y ambas veces se obtiene el mismo recurso.
- **CA-2 · Sprite faltante (negativo).** **Dado** un archivo de imagen inexistente, **Cuando** se dibuja el elemento, **Entonces** se usa el gráfico vectorial de respaldo, no se lanza ninguna excepción y se registra un aviso.
- **CA-3 · Sin imágenes (borde).** **Dado** una instalación sin ninguna imagen, **Entonces** el juego ejecuta y se juega completo con gráficos vectoriales.
- **CA-4 · Licencias.** **Entonces** el 100 % de los paquetes de imágenes aparece en `docs/ASSETS.md` con su fuente y licencia (CC0 o CC-BY).

---

## 6. Pruebas críticas obligatorias → historias

| # | Prueba crítica (prompt) | Historia y criterio |
|---|---|---|
| 1 | Sembrar fuera de estación | HU-01 · CA-2 |
| 2 | Cosechar con inventario lleno | HU-04 · CA-2 |
| 3 | Helada que marchita cultivos | HU-12 · CA-2 |
| 4 | Saturación de precios al vender mucho | HU-15 · CA-2 y CA-3 |
| 5 | Animal enfermo que no produce | HU-22 · CA-2 (y HU-21 · CA-3) |
| 6 | Guardar → cargar devuelve estado idéntico | HU-30 · CA-1 |
| 7 | Cargar archivo corrupto o inexistente sin alterar la partida | HU-31 · CA-1 y CA-2 |

## 7. Backlog priorizado por fase de implementación

> Cada fase termina con `mvn clean package` en verde y una demostración posible. La **Fase 5.6** reúne lo visual y lo *Could*: es la que se **recorta** si se acaba el tiempo, sin afectar el diseño base.

### Fase 5.1 · Núcleo del turno — 25 puntos (8 historias)

| Orden | Historia | Épica | Prioridad | Puntos | Depende de |
|---|---|---|---|---|---|
| 1 | HU-13 · Administrar el inventario | Economía | Must | 3 | — |
| 2 | HU-09 · Calendario y estaciones | Tiempo y clima | Must | 3 | — |
| 3 | HU-08 · Administrar la energía diaria | Tiempo y clima | Must | 2 | — |
| 4 | HU-11 · Sorteo de clima y pronóstico | Tiempo y clima | Must | 3 | — |
| 5 | HU-28 · Iniciar nueva partida | Progresión y persistencia | Must | 3 | HU-09, HU-11, HU-13 |
| 6 | HU-10 · Terminar el día | Tiempo y clima | Must | 5 | HU-08, HU-09, HU-11, HU-28 |
| 7 | HU-33 · Pantalla de inicio | Interfaz | Must | 3 | HU-28 |
| 8 | HU-34 · HUD superior | Interfaz | Must | 3 | HU-10, HU-28 |

### Fase 5.2 · Cultivo — 29 puntos (9 historias)

| Orden | Historia | Épica | Prioridad | Puntos | Depende de |
|---|---|---|---|---|---|
| 1 | HU-01 · Sembrar cultivo | Cultivo | Must | 3 | HU-08, HU-13, HU-28 |
| 2 | HU-02 · Regar cultivo | Cultivo | Must | 2 | HU-01, HU-11 |
| 3 | HU-03 · Crecimiento por etapas | Cultivo | Must | 3 | HU-02, HU-10 |
| 4 | HU-04 · Cosechar cultivo maduro | Cultivo | Must | 3 | HU-03, HU-13 |
| 5 | HU-05 · Marchitamiento por falta de riego y por estación | Cultivo | Must | 5 | HU-03, HU-09, HU-11 |
| 6 | HU-06 · Limpiar parcela marchita | Cultivo | Should | 1 | HU-05 |
| 7 | HU-07 · Consultar el estado de una parcela | Cultivo | Must | 2 | HU-01 |
| 8 | HU-35 · Mapa de la granja | Interfaz | Must | 5 | HU-28 |
| 9 | HU-36 · Herramientas y clic sobre parcelas | Interfaz | Must | 5 | HU-01, HU-35 |

### Fase 5.3 · Clima y economía — 23 puntos (6 historias)

| Orden | Historia | Épica | Prioridad | Puntos | Depende de |
|---|---|---|---|---|---|
| 1 | HU-12 · Efectos del clima en los cultivos | Tiempo y clima | Must | 5 | HU-05, HU-11 |
| 2 | HU-37 · Inventario en grilla | Interfaz | Must | 3 | HU-13, HU-36 |
| 3 | HU-14 · Comprar en el mercado | Economía | Must | 3 | HU-13 |
| 4 | HU-15 · Vender con precio dinámico | Economía | Must | 5 | HU-04, HU-13 |
| 5 | HU-16 · Tendencia de precio | Economía | Should | 2 | HU-10, HU-15 |
| 6 | HU-38 · Mercado modal | Interfaz | Must | 5 | HU-14, HU-15, HU-16 |

### Fase 5.4 · Construcción y animales — 21 puntos (7 historias)

| Orden | Historia | Épica | Prioridad | Puntos | Depende de |
|---|---|---|---|---|---|
| 1 | HU-18 · Construir edificios | Construcción y animales | Must | 5 | HU-28 |
| 2 | HU-19 · Comprar animales | Construcción y animales | Must | 3 | HU-14, HU-18 |
| 3 | HU-20 · Alimentar animales | Construcción y animales | Must | 3 | HU-19, HU-13 |
| 4 | HU-21 · Estados del animal | Construcción y animales | Must | 3 | HU-10, HU-20 |
| 5 | HU-22 · Producción y recolección | Construcción y animales | Must | 3 | HU-21, HU-13 |
| 6 | HU-23 · Curar animales enfermos | Construcción y animales | Should | 2 | HU-14, HU-21 |
| 7 | HU-17 · Resumen del día | Economía | Could | 2 | HU-10, HU-15 |

### Fase 5.5 · Progresión y persistencia — 24 puntos (7 historias)

| Orden | Historia | Épica | Prioridad | Puntos | Depende de |
|---|---|---|---|---|---|
| 1 | HU-26 · Registrar logros | Progresión y persistencia | Must | 5 | HU-04, HU-10, HU-15, HU-18, HU-23 |
| 2 | HU-27 · Desbloquear edificios por logros | Progresión y persistencia | Should | 2 | HU-18, HU-26 |
| 3 | HU-24 · Construir y usar el almacén | Construcción y animales | Should | 2 | HU-18, HU-27 |
| 4 | HU-29 · Guardar partida | Progresión y persistencia | Must | 5 | HU-28 |
| 5 | HU-30 · Cargar partida | Progresión y persistencia | Must | 5 | HU-29 |
| 6 | HU-31 · Rechazar archivos inválidos sin alterar la partida | Progresión y persistencia | Must | 3 | HU-30 |
| 7 | HU-32 · Listar ranuras de guardado | Progresión y persistencia | Should | 2 | HU-29 |

### Fase 5.6 · Interfaz visual y extras (recortable) — 19 puntos (5 historias)

| Orden | Historia | Épica | Prioridad | Puntos | Depende de |
|---|---|---|---|---|---|
| 1 | HU-39 · Notificaciones (toasts) | Interfaz | Should | 3 | HU-26, HU-35 |
| 2 | HU-42 · Carga de sprites con respaldo vectorial | Interfaz | Should | 3 | HU-35 |
| 3 | HU-25 · Construir y usar el invernadero | Construcción y animales | Could | 5 | HU-12, HU-27 |
| 4 | HU-40 · Efectos visuales de estación y clima | Interfaz | Could | 5 | HU-11, HU-35 |
| 5 | HU-41 · Animaciones suaves | Interfaz | Could | 3 | HU-35, HU-42 |

### Resumen de puntos

| Fase | Puntos | Acumulado | Must | Should | Could |
|---|---|---|---|---|---|
| 5.1 · Núcleo del turno | 25 | 25 | 25 | 0 | 0 |
| 5.2 · Cultivo | 29 | 54 | 28 | 1 | 0 |
| 5.3 · Clima y economía | 23 | 77 | 21 | 2 | 0 |
| 5.4 · Construcción y animales | 21 | 98 | 17 | 2 | 2 |
| 5.5 · Progresión y persistencia | 24 | 122 | 18 | 6 | 0 |
| 5.6 · Interfaz visual y extras (recortable) | 19 | 141 | 0 | 6 | 13 |
| **Total** | **141** | **141** | **109** | **17** | **15** |

> **MVP (todas las *Must*): 109 puntos** en 30 historias. Lo *Should* y *Could* (32 puntos) se puede recortar sin afectar el diseño base.

## 8. Mapa de historias (épica → historias)

| Épica | Historias | Puntos |
|---|---|---|
| Cultivo | HU-01 Sembrar cultivo, HU-02 Regar cultivo, HU-03 Crecimiento por etapas, HU-04 Cosechar cultivo maduro, HU-05 Marchitamiento por falta de riego y por estación, HU-06 Limpiar parcela marchita, HU-07 Consultar el estado de una parcela | 19 |
| Tiempo y clima | HU-08 Administrar la energía diaria, HU-09 Calendario y estaciones, HU-10 Terminar el día, HU-11 Sorteo de clima y pronóstico, HU-12 Efectos del clima en los cultivos | 18 |
| Economía | HU-13 Administrar el inventario, HU-14 Comprar en el mercado, HU-15 Vender con precio dinámico, HU-16 Tendencia de precio, HU-17 Resumen del día | 15 |
| Construcción y animales | HU-18 Construir edificios, HU-19 Comprar animales, HU-20 Alimentar animales, HU-21 Estados del animal, HU-22 Producción y recolección, HU-23 Curar animales enfermos, HU-24 Construir y usar el almacén, HU-25 Construir y usar el invernadero | 26 |
| Progresión y persistencia | HU-26 Registrar logros, HU-27 Desbloquear edificios por logros, HU-28 Iniciar nueva partida, HU-29 Guardar partida, HU-30 Cargar partida, HU-31 Rechazar archivos inválidos sin alterar la partida, HU-32 Listar ranuras de guardado | 25 |
| Interfaz | HU-33 Pantalla de inicio, HU-34 HUD superior, HU-35 Mapa de la granja, HU-36 Herramientas y clic sobre parcelas, HU-37 Inventario en grilla, HU-38 Mercado modal, HU-39 Notificaciones (toasts), HU-40 Efectos visuales de estación y clima, HU-41 Animaciones suaves, HU-42 Carga de sprites con respaldo vectorial | 38 |

## 9. Trazabilidad RF → HU → caso de uso

| RF | Prioridad | Historias de usuario | Casos de uso |
|---|---|---|---|
| RF-01 | Must | HU-01 | CU-02 |
| RF-02 | Must | HU-02 | CU-03 |
| RF-03 | Must | HU-03 | CU-06 |
| RF-04 | Must | HU-04 | CU-04 |
| RF-05 | Must | HU-05, HU-12 | CU-06 |
| RF-06 | Should | HU-06 | CU-04 |
| RF-07 | Must | HU-07 | CU-05 |
| RF-08 | Must | HU-10 | CU-06 |
| RF-09 | Must | HU-09 | CU-06 |
| RF-10 | Must | HU-11 | CU-06, CU-18 |
| RF-11 | Must | HU-12 | CU-06 |
| RF-12 | Must | HU-08 | CU-02, CU-06 |
| RF-13 | Must | HU-13 | CU-17 |
| RF-14 | Must | HU-14 | CU-07, CU-10 |
| RF-15 | Must | HU-15 | CU-08 |
| RF-16 | Should | HU-16 | CU-08 |
| RF-17 | Could | HU-17 | CU-06 |
| RF-18 | Must | HU-18 | CU-09 |
| RF-19 | Must | HU-19 | CU-10 |
| RF-20 | Must | HU-20 | CU-11 |
| RF-21 | Must | HU-21 | CU-06, CU-11 |
| RF-22 | Must | HU-22 | CU-06, CU-12 |
| RF-23 | Should | HU-23 | CU-13 |
| RF-24 | Could | HU-25 | CU-09 |
| RF-25 | Should | HU-24 | CU-09 |
| RF-26 | Must | HU-26 | CU-06, CU-16 |
| RF-27 | Should | HU-27 | CU-09, CU-16 |
| RF-28 | Must | HU-28 | CU-01 |
| RF-29 | Must | HU-29 | CU-14 |
| RF-30 | Must | HU-30 | CU-15 |
| RF-31 | Must | HU-31 | CU-15 |
| RF-32 | Should | HU-32 | CU-14, CU-15 |
| RF-33 | Must | HU-33 | CU-01 |
| RF-34 | Must | HU-34 | CU-18 |
| RF-35 | Must | HU-35 | CU-18 |
| RF-36 | Must | HU-36 | CU-18 |
| RF-37 | Must | HU-37 | CU-17 |
| RF-38 | Must | HU-38 | CU-07, CU-08 |
| RF-39 | Should | HU-39 | CU-18 |
| RF-40 | Could | HU-40 | CU-18 |
| RF-41 | Could | HU-41 | CU-18 |
| RF-42 | Should | HU-42 | CU-18 |
