# Matriz de trazabilidad — Valle Verde

> Generada por `tools/traceability.py` (no editar a mano). Cadena: **RF → HU → componente funcional → clases → pruebas**.
> Las clases se enlazan con la línea Javadoc `Stories: HU-xx` y las pruebas con el prefijo `HUxx_` del método.

## 1. Por requerimiento funcional

| RF | Prioridad | Historias | Componente funcional | Clases | Pruebas |
|---|---|---|---|---|---|
| RF-01 | Must | HU-01 | Cultivo | CarrotFactory, CornFactory, Crop, CropFactory, CropSnapshot, CropStage, CropState, CropType, Energy, FarmMap, FarmMapSnapshot, FarmService, GameController, GameEvent, GameMemento, GameRules, GameState, GrowingState, ItemType, MatureState, Names, Plot, PlotView, PotatoFactory, PumpkinFactory, SeedState, SproutState, TomatoFactory, ViewMapper, WitheredState | HU-01: 10 |
| RF-02 | Must | HU-02 | Cultivo | Crop, FarmService, GameController | HU-02: 9 |
| RF-03 | Must | HU-03 | Cultivo | Crop, CropState, DayPipeline, FarmMap, GrowingPhaseState, GrowingState, GrowthStep, MatureState, SeedState, SproutState, WitheredState | HU-03: 5 |
| RF-04 | Must | HU-04 | Cultivo | FarmService, GameController | HU-04: 9 |
| RF-05 | Must | HU-05, HU-12 | Cultivo | CalendarStep, Crop, CropState, DayReport, FarmMap, GameRules, GrowingPhaseState, GrowthStep | HU-05: 9, HU-12: 0 |
| RF-06 | Should | HU-06 | Cultivo | FarmService, GameController | HU-06: 6 |
| RF-07 | Must | HU-07 | Cultivo | FarmService, GameController, MapPanel, Names, PlotText, PlotView | HU-07: 10 |
| RF-08 | Must | HU-10 | Tiempo y clima | CalendarStep, DayPhase, DayPipeline, DayReport, DayService, DayStep, EnergyResetStep, EventBus, ForecastStep, GameController, GameEvent, GameListener, GameMemento, GameScreenPresenter, GameState, Inventory, InventorySnapshot | HU-10: 12 |
| RF-09 | Must | HU-09 | Tiempo y clima | AutumnState, GameClock, GameDate, GameRules, Season, SeasonState, SpringState, SummerState, WinterState | HU-09: 8 |
| RF-10 | Must | HU-11 | Tiempo y clima | AutumnState, FixedWeatherStrategy, GameRules, SeasonState, SeasonalWeatherStrategy, SpringState, StateRandom, SummerState, Weather, WeatherCycle, WeatherStrategy, WinterState | HU-11: 8 |
| RF-11 | Must | HU-12 | Tiempo y clima | pendiente | HU-12: 0 |
| RF-12 | Must | HU-08 | Tiempo y clima | Energy, ErrorCode, GameRules, Messages | HU-08: 6 |
| RF-13 | Must | HU-13 | Economía | GameRules, Inventory, ItemType | HU-13: 5 |
| RF-14 | Must | HU-14 | Economía | pendiente | HU-14: 0 |
| RF-15 | Must | HU-15 | Economía | pendiente | HU-15: 0 |
| RF-16 | Should | HU-16 | Economía | pendiente | HU-16: 0 |
| RF-17 | Could | HU-17 | Tiempo y clima | pendiente | HU-17: 0 |
| RF-18 | Must | HU-18 | Construcción y | pendiente | HU-18: 0 |
| RF-19 | Must | HU-19 | Construcción y | pendiente | HU-19: 0 |
| RF-20 | Must | HU-20 | Construcción y | pendiente | HU-20: 0 |
| RF-21 | Must | HU-21 | Construcción y | pendiente | HU-21: 0 |
| RF-22 | Must | HU-22 | Construcción y | pendiente | HU-22: 0 |
| RF-23 | Should | HU-23 | Construcción y | pendiente | HU-23: 0 |
| RF-24 | Could | HU-25 | Construcción y | pendiente | HU-25: 0 |
| RF-25 | Should | HU-24 | Construcción y | pendiente | HU-24: 0 |
| RF-26 | Must | HU-26 | Progreso | pendiente | HU-26: 0 |
| RF-27 | Should | HU-27 | Progreso | pendiente | HU-27: 0 |
| RF-28 | Must | HU-28 | Progreso | FarmMap, GameController, GameRules, GameSession, GameState, HudView, Plot, ProgressService, Result, StartScreenPresenter, Terrain, ViewMapper | HU-28: 7 |
| RF-29 | Must | HU-29 | Guardado | pendiente | HU-29: 0 |
| RF-30 | Must | HU-30 | Guardado | pendiente | HU-30: 0 |
| RF-31 | Must | HU-31 | Guardado | pendiente | HU-31: 0 |
| RF-32 | Should | HU-32 | Guardado | pendiente | HU-32: 0 |
| RF-33 | Must | HU-33 | Interfaz de usuario | GameController, GameRules, GameScreen, JsonSaveRepository, Main, SaveGameService, SaveRepository, ScreenNavigator, ScreenRouter, StartScreen, StartScreenPresenter, Texts | HU-33: 11 |
| RF-34 | Must | HU-34 | Interfaz de usuario | GameEvent, GameScreen, GameScreenPresenter, HudModel, HudPanel, HudView, Texts, ViewMapper | HU-34: 8 |
| RF-35 | Must | HU-35 | Interfaz de usuario | CellPos, FarmService, GameController, GameScreen, MapGeometry, MapPanel, MapRenderer, MapView, Palette, PlotClickListener, ViewMapper | HU-35: 10 |
| RF-36 | Must | HU-36 | Interfaz de usuario | GameScreen, GameScreenPresenter, Shortcut, Texts, Tool, ToolBar, ToolModel | HU-36: 13 |
| RF-37 | Must | HU-37 | Interfaz de usuario | pendiente | HU-37: 0 |
| RF-38 | Must | HU-38 | Interfaz de usuario | pendiente | HU-38: 0 |
| RF-39 | Should | HU-39 | Interfaz de usuario | pendiente | HU-39: 0 |
| RF-40 | Could | HU-40 | Interfaz de usuario | pendiente | HU-40: 0 |
| RF-41 | Could | HU-41 | Interfaz de usuario | pendiente | HU-41: 0 |
| RF-42 | Should | HU-42 | Gestor de assets | pendiente | HU-42: 0 |

## 2. Por historia de usuario

| Historia | Fase | Prioridad | Criterios | Pruebas | Estado | Clases |
|---|---|---|---|---|---|---|
| HU-01 · Sembrar cultivo | 5.2 | Must | 4 | 10 | completa | CarrotFactory, CornFactory, Crop, CropFactory, CropSnapshot, CropStage, CropState, CropType, Energy, FarmMap, FarmMapSnapshot, FarmService, GameController, GameEvent, GameMemento, GameRules, GameState, GrowingState, ItemType, MatureState, Names, Plot, PlotView, PotatoFactory, PumpkinFactory, SeedState, SproutState, TomatoFactory, ViewMapper, WitheredState |
| HU-02 · Regar cultivo | 5.2 | Must | 4 | 9 | completa | Crop, FarmService, GameController |
| HU-03 · Crecimiento por etapas | 5.2 | Must | 4 | 5 | completa | Crop, CropState, DayPipeline, FarmMap, GrowingPhaseState, GrowingState, GrowthStep, MatureState, SeedState, SproutState, WitheredState |
| HU-04 · Cosechar cultivo maduro | 5.2 | Must | 4 | 9 | completa | FarmService, GameController |
| HU-05 · Marchitamiento por falta de riego y por estación | 5.2 | Must | 5 | 9 | completa | CalendarStep, Crop, CropState, DayReport, FarmMap, GameRules, GrowingPhaseState, GrowthStep |
| HU-06 · Limpiar parcela marchita | 5.2 | Should | 4 | 6 | completa | FarmService, GameController |
| HU-07 · Consultar el estado de una parcela | 5.2 | Must | 4 | 10 | completa | FarmService, GameController, MapPanel, Names, PlotText, PlotView |
| HU-08 · Administrar la energía diaria | 5.1 | Must | 4 | 6 | completa | Energy, ErrorCode, GameRules, Messages |
| HU-09 · Calendario y estaciones | 5.1 | Must | 4 | 8 | completa | AutumnState, GameClock, GameDate, GameRules, Season, SeasonState, SpringState, SummerState, WinterState |
| HU-10 · Terminar el día | 5.1 | Must | 4 | 12 | completa | CalendarStep, DayPhase, DayPipeline, DayReport, DayService, DayStep, EnergyResetStep, EventBus, ForecastStep, GameController, GameEvent, GameListener, GameMemento, GameScreenPresenter, GameState, Inventory, InventorySnapshot |
| HU-11 · Sorteo de clima y pronóstico | 5.1 | Must | 4 | 8 | completa | AutumnState, FixedWeatherStrategy, GameRules, SeasonState, SeasonalWeatherStrategy, SpringState, StateRandom, SummerState, Weather, WeatherCycle, WeatherStrategy, WinterState |
| HU-12 · Efectos del clima en los cultivos | 5.3 | Must | 4 | 0 | sin pruebas | pendiente |
| HU-13 · Administrar el inventario | 5.1 | Must | 4 | 5 | completa | GameRules, Inventory, ItemType |
| HU-14 · Comprar en el mercado | 5.3 | Must | 4 | 0 | sin pruebas | pendiente |
| HU-15 · Vender con precio dinámico | 5.3 | Must | 6 | 0 | sin pruebas | pendiente |
| HU-16 · Tendencia de precio | 5.3 | Should | 4 | 0 | sin pruebas | pendiente |
| HU-17 · Resumen del día | 5.4 | Could | 4 | 0 | sin pruebas | pendiente |
| HU-18 · Construir edificios | 5.4 | Must | 6 | 0 | sin pruebas | pendiente |
| HU-19 · Comprar animales | 5.4 | Must | 4 | 0 | sin pruebas | pendiente |
| HU-20 · Alimentar animales | 5.4 | Must | 5 | 0 | sin pruebas | pendiente |
| HU-21 · Estados del animal | 5.4 | Must | 4 | 0 | sin pruebas | pendiente |
| HU-22 · Producción y recolección | 5.4 | Must | 4 | 0 | sin pruebas | pendiente |
| HU-23 · Curar animales enfermos | 5.4 | Should | 4 | 0 | sin pruebas | pendiente |
| HU-24 · Construir y usar el almacén | 5.5 | Should | 4 | 0 | sin pruebas | pendiente |
| HU-25 · Construir y usar el invernadero | 5.6 | Could | 5 | 0 | sin pruebas | pendiente |
| HU-26 · Registrar logros | 5.5 | Must | 5 | 0 | sin pruebas | pendiente |
| HU-27 · Desbloquear edificios por logros | 5.5 | Should | 4 | 0 | sin pruebas | pendiente |
| HU-28 · Iniciar nueva partida | 5.1 | Must | 4 | 7 | completa | FarmMap, GameController, GameRules, GameSession, GameState, HudView, Plot, ProgressService, Result, StartScreenPresenter, Terrain, ViewMapper |
| HU-29 · Guardar partida | 5.5 | Must | 4 | 0 | sin pruebas | pendiente |
| HU-30 · Cargar partida | 5.5 | Must | 4 | 0 | sin pruebas | pendiente |
| HU-31 · Rechazar partidas inválidas sin alterar la partida | 5.5 | Must | 5 | 0 | sin pruebas | pendiente |
| HU-32 · Listar ranuras de guardado | 5.5 | Should | 3 | 0 | sin pruebas | pendiente |
| HU-33 · Pantalla de inicio | 5.1 | Must | 4 | 11 | completa | GameController, GameRules, GameScreen, JsonSaveRepository, Main, SaveGameService, SaveRepository, ScreenNavigator, ScreenRouter, StartScreen, StartScreenPresenter, Texts |
| HU-34 · HUD superior | 5.1 | Must | 4 | 8 | completa | GameEvent, GameScreen, GameScreenPresenter, HudModel, HudPanel, HudView, Texts, ViewMapper |
| HU-35 · Mapa de la granja | 5.2 | Must | 5 | 10 | completa | CellPos, FarmService, GameController, GameScreen, MapGeometry, MapPanel, MapRenderer, MapView, Palette, PlotClickListener, ViewMapper |
| HU-36 · Herramientas y clic sobre parcelas | 5.2 | Must | 5 | 13 | completa | GameScreen, GameScreenPresenter, Shortcut, Texts, Tool, ToolBar, ToolModel |
| HU-37 · Inventario en grilla | 5.3 | Must | 4 | 0 | sin pruebas | pendiente |
| HU-38 · Mercado modal | 5.3 | Must | 4 | 0 | sin pruebas | pendiente |
| HU-39 · Notificaciones (toasts) | 5.6 | Should | 4 | 0 | sin pruebas | pendiente |
| HU-40 · Efectos visuales de estación y clima | 5.6 | Could | 4 | 0 | sin pruebas | pendiente |
| HU-41 · Animaciones suaves | 5.6 | Could | 4 | 0 | sin pruebas | pendiente |
| HU-42 · Carga de sprites con respaldo vectorial | 5.6 | Should | 4 | 0 | sin pruebas | pendiente |

**Resumen:** 17 de 42 historias con todas sus pruebas; 146 pruebas `HUxx_` en total.
