# API REST — Valle Verde

> **Versión 0.4 · 2026-11-22 · Fase 5.5**
> Describe las rutas que el servidor Spring Boot ofrece al cliente Swing (RNF-15, AR-12). El servidor escucha solo en `127.0.0.1`; con `java -jar farmsim.jar` el puerto es libre y lo conoce el cliente, y con `--server --port 8080` es el indicado. Hay **una partida por servidor** y no hay sesión de usuario.

## 1. Convenciones

| Aspecto | Regla |
|---|---|
| Formato | JSON en UTF-8. Los cuerpos de petición llevan `Content-Type: application/json` |
| Recursos | Sustantivos en minúscula: `game`, `farm`, `saves`. Los verbos son los de HTTP |
| Éxito | `200 OK`, o `201 Created` cuando la orden crea algo (partida, cultivo). Cuerpo: `{ "data": …, "events": […] }` |
| Rechazo | Código HTTP de la tabla de §3. Cuerpo: `{ "code": "…", "message": "…" }` con el texto en español de RN-35. **Sin eventos** y sin cambios de estado |
| Eventos | `events` es la lista, en orden, de lo que ocurrió mientras se ejecutaba la orden: `{ "type": "MapChanged", "data": { … } }`. El cliente los republica en su bus local (DT-19) |
| Petición mal formada | `400` (o `404`/`405` si la ruta o el verbo no existen) con `{ "code": "SOLICITUD_INVALIDA", "message": "…" }`. Este código es de transporte: no pertenece a RN-35 |

## 2. Rutas

Las columnas de tipo usan los *records* de `application.dto` (`HudView`, `PlotView`, `MapView`).

| Verbo y ruta | Orden (`GameCommands`) | Cuerpo | `data` | Éxito |
|---|---|---|---|---|
| `POST /api/game` | `newGame(seed)` | `{ "seed": 2026 }` | `HudView` | `201` |
| `GET /api/game` | `hud()` | — | `HudView` | `200` |
| `POST /api/game/days` | `endDay()` | — | `HudView` | `200` |
| `GET /api/farm/map` | `map()` | — | `MapView` | `200` |
| `GET /api/farm/plots/{col}/{row}` | `inspect(col, row)` | — | `PlotView` | `200` |
| `POST /api/farm/plots/{col}/{row}/crop` | `plant(col, row, seedId)` | `{ "seedId": "CARROT_SEED" }` | `PlotView` | `201` |
| `POST /api/farm/plots/{col}/{row}/watering` | `water(col, row)` | — | `PlotView` | `200` |
| `POST /api/farm/plots/{col}/{row}/harvest` | `harvest(col, row)` | — | `PlotView` | `200` |
| `GET /api/market` | `market()` | — | `MarketView` (qué se vende y con qué tendencia, y el catálogo de compra) | `200` |
| `POST /api/market/purchases` | `buy(itemId, quantity)` | `{ "itemId": "CORN_SEED", "quantity": 5 }` | total pagado (m) | `200` |
| `POST /api/market/sales` | `sell(itemId, quantity)` | `{ "itemId": "CARROT", "quantity": 3 }` | total recibido (m) | `200` |
| `GET /api/market/quotes?itemId=CARROT&quantity=3` | `quote(itemId, quantity)` | — | total previsto (m), sin vender | `200` |
| `GET /api/inventory` | `inventory()` | — | `InventoryView` (capacidad y casillas ocupadas) | `200` |
| `GET /api/buildings` | `buildings()` | — | `BuildingsView` (los edificios y el catálogo de construcción) | `200` |
| `POST /api/buildings` | `build(buildingId, col, row)` | `{ "buildingId": "COOP", "col": 2, "row": 7 }` | `BuildingView` | `201` |
| `POST /api/animals` | `buyAnimal(animalId)` | `{ "animalId": "HEN" }` | `BuildingView` donde vive el animal | `201` |
| `POST /api/buildings/{id}/feeding` | `feed(id)` | — | `BuildingActionView` (cantidad alimentada y aviso opcional) | `200` |
| `POST /api/buildings/{id}/collection` | `collect(id)` | — | `BuildingActionView` (unidades recogidas y aviso opcional) | `200` |
| `POST /api/buildings/{id}/animals/{n}/treatment` | `cure(id, n)` | — | `BuildingActionView` | `200` |
| `GET /api/achievements` | `achievements()` | — | lista de `AchievementView` (nombre, condición, recompensa, progreso) | `200` |
| `GET /api/saves/any` | `hasSavedGames()` | — | `true` / `false` | `200` |
| `GET /api/saves` | `saveSlots()` | — | las 3 ranuras (`SaveSlotView`: `EMPTY`, `OK` o `DAMAGED`; una dañada no impide listar las demás) | `200` |
| `PUT /api/saves/{slot}` | `save(slot)` | — | la ranura como queda (`SaveSlotView`) | `200` |
| `POST /api/saves/{slot}/load` | `load(slot)` | — | `HudView` de la partida cargada; evento `GameStarted` | `200` |

`hasGameInProgress()` no tiene ruta propia: el cliente lo deduce de `GET /api/game` (`404 SIN_PARTIDA` = no hay partida).

El **resumen del día** no tiene ruta: `POST /api/game/days` añade a sus eventos `DaySummaryReady` con `DaySummaryView` (ingresos, gastos, saldo anterior y nuevo y los hechos del día). `GET /api/market` incluye ahora los animales en venta y `GET /api/farm/map` los edificios con sus animales. Un edificio o un animal que no existe responde `500 ERROR_INTERNO` (nunca lo envía la interfaz).

Cargar una ranura rechazada **no cambia la partida en curso** (RN-33): `404 ARCHIVO_INEXISTENTE` si no hay documento, `422 ARCHIVO_CORRUPTO` si el texto, la suma de control o los datos no son válidos, `422 VERSION_NO_SOPORTADA` y `503 ERROR_ES` si MongoDB no responde en 2 s. Una ranura fuera de 1 a 3 responde `500 ERROR_INTERNO` (la interfaz nunca la envía). Los logros desbloqueados viajan como evento `AchievementUnlocked` en la respuesta de la orden que los causó.

**Rutas planificadas:** ninguna por ahora; cada historia nueva agrega su ruta y su prueba (DoD 11).

### Ejemplo

```
POST /api/farm/plots/5/3/crop        {"seedId":"CARROT_SEED"}
→ 201  {"data":{"col":5,"row":3,"terrain":"SOIL","crop":"CARROT","stage":"SEED","watered":false,"daysToMature":4},
        "events":[{"type":"MapChanged","data":{}},{"type":"EnergyChanged","data":{"energy":98,"maxEnergy":100}}]}

POST /api/farm/plots/5/3/crop        {"seedId":"PUMPKIN_SEED"}      (en Primavera)
→ 409  {"code":"FUERA_DE_ESTACION","message":"No se puede sembrar Calabaza en Primavera: solo en Otoño."}
```

## 3. Código HTTP de cada rechazo

`ErrorStatusMapper` decide el estado; el `switch` no tiene rama por defecto, así que un `ErrorCode` nuevo no compila sin su estado.

| HTTP | Códigos de RN-35 | Significado |
|---|---|---|
| `400` | `CANTIDAD_INVALIDA` | la entrada del jugador no es válida |
| `404` | `SIN_PARTIDA`, `SIN_CULTIVO`, `ARCHIVO_INEXISTENTE` | lo que se pide no existe |
| `409` | `FUERA_DE_ESTACION`, `SIN_ENERGIA`, `SIN_FONDOS`, `INVENTARIO_LLENO`, `SIN_SEMILLAS`, `PARCELA_NO_CULTIVABLE`, `PARCELA_OCUPADA`, `YA_REGADO`, `NO_MADURO`, `CULTIVO_MARCHITO`, `EDIFICIO_BLOQUEADO`, `UBICACION_INVALIDA`, `LIMITE_EDIFICIOS`, `SIN_ESPACIO_ANIMALES`, `SIN_HENO`, `SIN_MEDICINA`, `ANIMAL_ENFERMO` | una regla de negocio impide la orden |
| `422` | `ARCHIVO_CORRUPTO`, `VERSION_NO_SOPORTADA` | la partida guardada existe pero no se puede usar |
| `503` | `ERROR_ES` | MongoDB no responde (la partida en curso no cambia) |
| `500` | `ERROR_INTERNO` | error inesperado; el estado no cambió |

## 4. Cómo lo verifica el proyecto

- `GameRestControllerTest`: una prueba por ruta con el estado de éxito y el de cada rechazo, sobre el contexto real de Spring (sin MongoDB).
- `RemoteGameClientTest`: el cliente contra el servidor real en un puerto libre; incluye la prueba de que **da los mismos resultados y eventos que el controlador en proceso**.
- `RemoteScreensIntegrationTest`: las pantallas Swing reales manejadas por el cliente HTTP.
- `ErrorStatusMapperTest` y la regla ArchUnit AR-11 (los controladores REST no importan `domain`).
