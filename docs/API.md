# API REST — Valle Verde

> **Versión 0.5 · 2026-11-22 · Fase 5.6**
> Describe las rutas que el servidor Spring Boot ofrece al cliente Swing (RNF-15, AR-12). El servidor escucha solo en `127.0.0.1`; con `java -jar farmsim.jar` el puerto es libre y lo conoce el cliente, y con `--server --port 8080` es el indicado. Hay **una partida por servidor** y no hay sesión de usuario.

## 1. Convenciones

| Aspecto | Regla |
|---|---|
| Formato | JSON en UTF-8. Los cuerpos de petición llevan `Content-Type: application/json` |
| Recursos | Sustantivos en minúscula: `juego`, `granja`, `guardados`. Los verbos son los de HTTP |
| Éxito | `200 OK`, o `201 Created` cuando la orden crea algo (partida, cultivo). Cuerpo: `{ "datos": …, "eventos": […] }` |
| Rechazo | Código HTTP de la tabla de §3. Cuerpo: `{ "codigo": "…", "mensaje": "…" }` con el texto en español de RN-35. **Sin eventos** y sin cambios de estado |
| Eventos | `eventos` es la lista, en orden, de lo que ocurrió mientras se ejecutaba la orden: `{ "tipo": "MapaCambiado", "datos": { … } }`. El cliente los republica en su bus local (DT-19) |
| Petición mal formada | `400` (o `404`/`405` si la ruta o el verbo no existen) con `{ "codigo": "SOLICITUD_INVALIDA", "mensaje": "…" }`. Este código es de transporte: no pertenece a RN-35 |

## 2. Rutas

Las columnas de tipo usan los *records* de `application.dto` (`VistaHud`, `VistaParcela`, `VistaMapa`).

| Verbo y ruta | Orden (`ComandosJuego`) | Cuerpo | `datos` | Éxito |
|---|---|---|---|---|
| `POST /api/game` | `juegoNuevo(semilla)` | `{ "semilla": 2026 }` | `VistaHud` | `201` |
| `GET /api/game` | `hud()` | — | `VistaHud` | `200` |
| `POST /api/game/days` | `diaFin()` | — | `VistaHud` | `200` |
| `GET /api/farm/map` | `map()` | — | `VistaMapa` (48 × 24 parcelas, edificios, avatar, `npcs` y `casas`: id, columna, fila, ancho y alto de cada casa del valle) | `200` |
| `POST /api/avatar/moves` | `mover(direccion)` | `{ "direccion": "EAST" }` (`NORTH`, `SOUTH`, `EAST` o `WEST`; otro valor es `400`) | `VistaAvatar` (`col`, `fila`, `mirada`); evento `AvatarMovido` | `200` |
| `GET /api/npcs` | `npcs()` | — | lista de `VistaNpc` (id, nombre, rol, parcela, encargo y si está cumplido) | `200` |
| `POST /api/npcs/{id}/talk` | `hablar(id)` | — | `VistaConversacion` (nombre, rol, texto, línea del encargo); sin eventos | `200` |
| `POST /api/npcs/{id}/errand` | `entregar(id)` | — | `VistaConversacion` tras la entrega; eventos `InventarioCambiado`, `DineroCambiado`, `EncargoCompletado` | `200` |
| `GET /api/tutorial` | `tutorial()` | — | `VistaTutorial` (paso, número, total, texto del avatar y si terminó) | `200` |
| `POST /api/tutorial/skip` | `tutorialSaltar()` | — | `VistaTutorial` terminado (`SKIPPED`); evento `TutorialAvanzado` si seguía en curso | `200` |
| `GET /api/farm/plots/{col}/{fila}` | `inspeccionar(col, fila)` | — | `VistaParcela` | `200` |
| `POST /api/farm/plots/{col}/{fila}/crop` | `plantar(col, fila, idSemilla)` | `{ "idSemilla": "CARROT_SEED" }` | `VistaParcela` | `201` |
| `POST /api/farm/plots/{col}/{fila}/watering` | `agua(col, fila)` | — | `VistaParcela` | `200` |
| `POST /api/farm/plots/{col}/{fila}/harvest` | `cosecha(col, fila)` | — | `VistaParcela` | `200` |
| `GET /api/market` | `mercado()` | — | `VistaMercado` (qué se vende y con qué tendencia, y el catálogo de compra) | `200` |
| `POST /api/market/purchases` | `comprar(idArticulo, cantidad)` | `{ "idArticulo": "CORN_SEED", "cantidad": 5 }` | total pagado (m) | `200` |
| `POST /api/market/sales` | `vender(idArticulo, cantidad)` | `{ "idArticulo": "CARROT", "cantidad": 3 }` | total recibido (m) | `200` |
| `GET /api/market/quotes?idArticulo=CARROT&cantidad=3` | `cotizar(idArticulo, cantidad)` | — | total previsto (m), sin vender | `200` |
| `GET /api/inventory` | `inventario()` | — | `VistaInventario` (capacidad y casillas ocupadas) | `200` |
| `GET /api/buildings` | `edificios()` | — | `VistaEdificios` (los edificios y el catálogo de construcción) | `200` |
| `POST /api/buildings` | `build(idEdificio, col, fila)` | `{ "idEdificio": "COOP", "col": 2, "fila": 7 }` | `VistaEdificio` | `201` |
| `POST /api/animals` | `animalComprar(animalId)` | `{ "animalId": "HEN" }` | `VistaEdificio` donde vive el animal | `201` |
| `POST /api/buildings/{id}/feeding` | `alimentar(id)` | — | `VistaAccionEdificio` (cantidad alimentada y aviso opcional) | `200` |
| `POST /api/buildings/{id}/collection` | `collect(id)` | — | `VistaAccionEdificio` (unidades recogidas y aviso opcional) | `200` |
| `POST /api/buildings/{id}/animals/{n}/treatment` | `curar(id, n)` | — | `VistaAccionEdificio` | `200` |
| `GET /api/achievements` | `logros()` | — | lista de `VistaLogro` (nombre, condición, recompensa, progreso) | `200` |
| `GET /api/saves/any` | `tieneJuegosArchivados()` | — | `true` / `false` | `200` |
| `GET /api/saves` | `guardarRanuras()` | — | las 3 ranuras (`VistaRanuraGuardado`: `EMPTY`, `OK` o `DAMAGED`; una dañada no impide listar las demás) | `200` |
| `PUT /api/saves/{ranura}` | `save(ranura)` | — | la ranura como queda (`VistaRanuraGuardado`) | `200` |
| `POST /api/saves/{ranura}/load` | `cargar(ranura)` | — | `VistaHud` de la partida cargada; evento `JuegoIniciado` | `200` |

`tieneJuegoEnProgreso()` no tiene ruta propia: el cliente lo deduce de `GET /api/game` (`404 SIN_PARTIDA` = no hay partida).

El **resumen del día** no tiene ruta: `POST /api/game/days` añade a sus eventos `ResumenDiaListo` con `VistaResumenDia` (ingresos, gastos, saldo anterior y nuevo y los hechos del día). `GET /api/market` incluye ahora los animales en venta y `GET /api/farm/map` los edificios con sus animales. Un edificio o un animal que no existe responde `500 ERROR_INTERNO` (nunca lo envía la interfaz).

`VistaParcela` incluye `resguardado` (`true` si un invernadero cubre la parcela, RN-22): en esas parcelas `POST …/crop` acepta sembrar fuera de estación y sobre hierba, y `…/watering` cuesta 1 de energía aunque haya Sequía.

Cargar una ranura rechazada **no cambia la partida en curso** (RN-33): `404 ARCHIVO_INEXISTENTE` si no hay documento, `422 ARCHIVO_CORRUPTO` si el texto, la suma de control o los datos no son válidos, `422 VERSION_NO_SOPORTADA` y `503 ERROR_ES` si MongoDB no responde en 2 s. Una ranura fuera de 1 a 3 responde `500 ERROR_INTERNO` (la interfaz nunca la envía). Los logros desbloqueados viajan como evento `LogroDesbloqueado` en la respuesta de la orden que los causó.

**Avatar (HU-45, RN-36):** `GET /api/farm/map` incluye `avatar` (`col`, `fila`, `mirada`). Un paso bloqueado responde `409 MOVIMIENTO_BLOQUEADO` sin eventos, aunque el avatar sí gira: el cliente vuelve a pedir el mapa para mostrar la nueva mirada.

**NPC (HU-46, HU-47, RN-37, RN-38):** `GET /api/farm/map` incluye también `npcs`. Hablar o entregar exige el avatar en una de las cuatro parcelas vecinas (`409 NPC_LEJOS`); entregar sin la cantidad completa es `409 ENCARGO_INCOMPLETO`; un encargo ya cumplido responde `200` con el agradecimiento y no paga otra vez. Un `id` que no existe responde `500 ERROR_INTERNO` (la interfaz nunca lo envía).

**Tutorial (HU-49, RN-39):** caminar, sembrar, regar, hablar y terminar el día aceptados publican, después de sus propios eventos, `TutorialAvanzado` con el paso nuevo si eran la acción del paso actual (por ejemplo `POST /api/avatar/moves` en el paso 1 responde `AvatarMovido` y `TutorialAvanzado`). Una orden rechazada nunca lo hace avanzar. Sin partida, las dos rutas responden `404 SIN_PARTIDA`.

**Rutas planificadas:** ninguna por ahora; cada historia nueva agrega su ruta y su prueba (DoD 11).

### Ejemplo

```
POST /api/farm/plots/5/3/crop        {"idSemilla":"CARROT_SEED"}
→ 201  {"datos":{"col":5,"fila":3,"terreno":"SOIL","cultivo":"CARROT","etapa":"SEED","regado":false,"diasAMaduro":4},
        "eventos":[{"tipo":"MapaCambiado","datos":{}},{"tipo":"EnergiaCambiada","datos":{"energia":98,"energiaMaxima":100}}]}

POST /api/farm/plots/5/3/crop        {"idSemilla":"PUMPKIN_SEED"}      (en Primavera)
→ 409  {"codigo":"FUERA_DE_ESTACION","mensaje":"No se puede sembrar Calabaza en Primavera: solo en Otoño."}
```

## 3. Código HTTP de cada rechazo

`MapeadorEstadoError` decide el estado; el `switch` no tiene rama por defecto, así que un `CodigoError` nuevo no compila sin su estado.

| HTTP | Códigos de RN-35 | Significado |
|---|---|---|
| `400` | `CANTIDAD_INVALIDA` | la entrada del jugador no es válida |
| `404` | `SIN_PARTIDA`, `SIN_CULTIVO`, `ARCHIVO_INEXISTENTE` | lo que se pide no existe |
| `409` | `FUERA_DE_ESTACION`, `SIN_ENERGIA`, `SIN_FONDOS`, `INVENTARIO_LLENO`, `SIN_SEMILLAS`, `PARCELA_NO_CULTIVABLE`, `PARCELA_OCUPADA`, `YA_REGADO`, `NO_MADURO`, `CULTIVO_MARCHITO`, `EDIFICIO_BLOQUEADO`, `UBICACION_INVALIDA`, `LIMITE_EDIFICIOS`, `SIN_ESPACIO_ANIMALES`, `SIN_HENO`, `SIN_MEDICINA`, `ANIMAL_ENFERMO`, `MOVIMIENTO_BLOQUEADO`, `NPC_LEJOS`, `ENCARGO_INCOMPLETO` | una regla de negocio impide la orden |
| `422` | `ARCHIVO_CORRUPTO`, `VERSION_NO_SOPORTADA` | la partida guardada existe pero no se puede usar |
| `503` | `ERROR_ES` | MongoDB no responde (la partida en curso no cambia) |
| `500` | `ERROR_INTERNO` | error inesperado; el estado no cambió |

## 4. Cómo lo verifica el proyecto

- `ControladorRestJuegoTest`: una prueba por ruta con el estado de éxito y el de cada rechazo, sobre el contexto real de Spring (sin MongoDB).
- `ClienteJuegoRemotoTest`: el cliente contra el servidor real en un puerto libre; incluye la prueba de que **da los mismos resultados y eventos que el controlador en proceso**.
- `IntegracionPantallasRemotaTest`: las pantallas Swing reales manejadas por el cliente HTTP.
- `MapeadorEstadoErrorTest` y la regla ArchUnit AR-11 (los controladores REST no importan `domain`).
