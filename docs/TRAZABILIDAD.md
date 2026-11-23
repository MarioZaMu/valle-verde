# Matriz de trazabilidad — Valle Verde

> Generada por `tools/traceability.py` (no editar a mano). Cadena: **RF → HU → componente funcional → clases → pruebas**.
> Las clases se enlazan con la línea Javadoc `Stories: HU-xx` y las pruebas con el prefijo `HUxx_` del método.

## 1. Por requerimiento funcional

| RF | Prioridad | Historias | Componente funcional | Clases | Pruebas |
|---|---|---|---|---|---|
| RF-01 | Must | HU-01 | Cultivo | CarrotFactory, CornFactory, Crop, CropFactory, CropSnapshot, CropStage, CropState, CropType, Energy, FarmMap, FarmMapSnapshot, FarmService, GameCommands, GameController, GameEvent, GameMemento, GameRestController, GameRules, GameState, GrowingState, ItemType, MatureState, Names, Plot, PlotView, PotatoFactory, PumpkinFactory, RemoteGameClient, SeedState, SproutState, TomatoFactory, ViewMapper, WitheredState | HU-01: 10 |
| RF-02 | Must | HU-02 | Cultivo | Crop, FarmService, GameCommands, GameController, GameRestController, RemoteGameClient | HU-02: 9 |
| RF-03 | Must | HU-03 | Cultivo | Crop, CropState, DayPipeline, FarmMap, GrowingPhaseState, GrowingState, GrowthStep, MatureState, SeedState, SproutState, WitheredState | HU-03: 5 |
| RF-04 | Must | HU-04 | Cultivo | FarmService, GameCommands, GameController, GameRestController, RemoteGameClient | HU-04: 9 |
| RF-05 | Must | HU-05, HU-12 | Cultivo | CalendarStep, Crop, CropState, DayReport, FarmMap, GameRules, GrowingPhaseState, GrowthStep, WeatherEffectsStep | HU-05: 9, HU-12: 6 |
| RF-06 | Should | HU-06 | Cultivo | FarmService, GameCommands, GameController, GameRestController, RemoteGameClient | HU-06: 6 |
| RF-07 | Must | HU-07 | Cultivo | FarmService, GameCommands, GameController, GameRestController, MapPanel, Names, PlotText, PlotView, RemoteGameClient | HU-07: 10 |
| RF-08 | Must | HU-10 | Tiempo y clima | CalendarStep, DayPhase, DayPipeline, DayReport, DayService, DayStep, EnergyResetStep, EventBus, ForecastStep, GameCommands, GameController, GameEvent, GameListener, GameMemento, GameRestController, GameScreenPresenter, GameState, Inventory, InventorySnapshot, RemoteGameClient | HU-10: 13 |
| RF-09 | Must | HU-09 | Tiempo y clima | AutumnState, GameClock, GameDate, GameRules, Season, SeasonState, SpringState, SummerState, WinterState | HU-09: 8 |
| RF-10 | Must | HU-11 | Tiempo y clima | AutumnState, FixedWeatherStrategy, GameRules, SeasonState, SeasonalWeatherStrategy, SpringState, StateRandom, SummerState, Weather, WeatherCycle, WeatherStrategy, WinterState | HU-11: 8 |
| RF-11 | Must | HU-12 | Tiempo y clima | FarmMap, WeatherEffectsStep | HU-12: 6 |
| RF-12 | Must | HU-08 | Tiempo y clima | Energy, ErrorCode, GameRules, Messages | HU-08: 6 |
| RF-13 | Must | HU-13 | Economía | GameRules, Inventory, ItemType | HU-13: 5 |
| RF-14 | Must | HU-14 | Economía | BuyOfferView, GameCommands, GameController, GameEvent, GameRestController, GameState, Market, MarketDialog, MarketPresenter, MarketService, MarketView, RemoteGameClient | HU-14: 7 |
| RF-15 | Must | HU-15 | Economía | CropPricing, FlatPricing, GameCommands, GameController, GameEvent, GameMemento, GameRestController, GameState, Market, MarketDialog, MarketPresenter, MarketService, MarketSnapshot, MarketStep, MarketView, PricingStrategy, RemoteGameClient, SellOfferView | HU-15: 12 |
| RF-16 | Should | HU-16 | Economía | GameCommands, GameController, GameMemento, GameRestController, Market, MarketDialog, MarketPresenter, MarketService, MarketSnapshot, MarketStep, MarketView, RemoteGameClient, SellOfferView, Trend | HU-16: 6 |
| RF-17 | Could | HU-17 | Tiempo y clima | DayLedger, DayPipeline, DayReport, DayService, DaySummaryDialog, DaySummaryText, DaySummaryView, GameCommands, GameController, GameDialogs, GameEvent, GameMemento, GameRestController, GameScreen, GameScreenPresenter, GameState, LedgerSnapshot, RemoteGameClient, ViewMapper | HU-17: 10 |
| RF-18 | Must | HU-18 | Construcción y | Achievement, AchievementTracker, BarnBuilder, Building, BuildingBuilder, BuildingDirector, BuildingOfferView, BuildingService, BuildingSnapshot, BuildingType, BuildingView, BuildingsView, ConstructionDialog, ConstructionPresenter, CoopBuilder, FarmMap, FarmMapSnapshot, GameCommands, GameController, GameDialogs, GameMemento, GameRestController, GameScreen, GameScreenPresenter, GameState, GreenhouseBuilder, MapRenderer, Names, RemoteGameClient, ViewMapper, WarehouseBuilder | HU-18: 17 |
| RF-19 | Must | HU-19 | Construcción y | Animal, AnimalOfferView, AnimalService, AnimalSnapshot, AnimalType, AnimalView, Building, BuildingView, GameCommands, GameController, GameRestController, MapRenderer, MarketPresenter, MarketService, MarketView, RemoteGameClient | HU-19: 6 |
| RF-20 | Must | HU-20 | Construcción y | Animal, AnimalService, Building, BuildingActionView, BuildingDialog, BuildingPresenter, GameCommands, GameController, GameRestController, GameScreen, GameScreenPresenter, RemoteGameClient | HU-20: 10 |
| RF-21 | Must | HU-21 | Construcción y | Animal, AnimalCondition, AnimalSnapshot, AnimalState, AnimalView, AnimalsStep, DayReport, HealthyState, HungryState, SickState | HU-21: 5 |
| RF-22 | Must | HU-22 | Construcción y | Animal, AnimalService, AnimalsStep, Building, BuildingActionView, BuildingDialog, BuildingPresenter, DayReport, GameCommands, GameController, GameRestController, GameScreen, GameScreenPresenter, HealthyState, HungryState, RemoteGameClient, SickState | HU-22: 10 |
| RF-23 | Should | HU-23 | Construcción y | Animal, AnimalService, BuildingActionView, BuildingDialog, BuildingPresenter, GameCommands, GameController, GameRestController, GameScreen, RemoteGameClient, SickState | HU-23: 8 |
| RF-24 | Could | HU-25 | Construcción y | FarmMap, FarmService, MapPanel, MapRenderer, Plot, PlotText, PlotView | HU-25: 11 |
| RF-25 | Should | HU-24 | Construcción y | Building, BuildingService | HU-24: 5 |
| RF-26 | Must | HU-26 | Progreso | Achievement, AchievementService, AchievementTracker, AchievementView, AchievementsDialog, AchievementsPresenter, AchievementsStep, AnimalService, BuildingService, DayReport, DayService, FarmService, GameCommands, GameController, GameDialogs, GameEvent, GameMemento, GameRestController, GameScreen, GameScreenPresenter, GameState, MarketService, ProgressFacts, ProgressSnapshot, RemoteGameClient, ToastPresenter | HU-26: 10 |
| RF-27 | Should | HU-27 | Progreso | AchievementService, SaveGameService | HU-27: 5 |
| RF-28 | Must | HU-28 | Progreso | FarmMap, GameCommands, GameController, GameRestController, GameRules, GameSession, GameState, HudView, Plot, ProgressService, RemoteGameClient, Result, StartScreenPresenter, Terrain, ViewMapper | HU-28: 8 |
| RF-29 | Must | HU-29 | Guardado | Checksum, GameCommands, GameController, GameDialogs, GameRestController, GameScreen, GameScreenPresenter, MongoSaveRepository, RemoteGameClient, SaveCodec, SaveGameService, SaveRepository, SaveSlotView, SlotsDialog, SlotsPresenter | HU-29: 14 |
| RF-30 | Must | HU-30 | Guardado | GameCommands, GameController, GameDialogs, GameRestController, GameScreen, GameScreenPresenter, MongoSaveRepository, ProgressSnapshot, RemoteGameClient, SaveCodec, SaveGameService, SaveLoadException, SaveRepository, SlotsDialog, SlotsPresenter, StartScreenPresenter | HU-30: 7 |
| RF-31 | Must | HU-31 | Guardado | Checksum, GameCommands, GameController, GameRestController, GameStateValidator, MongoSaveRepository, RemoteGameClient, SaveCodec, SaveGameService, SaveLoadException, SaveRepository | HU-31: 12 |
| RF-32 | Should | HU-32 | Guardado | GameCommands, GameController, GameRestController, MongoSaveRepository, RemoteGameClient, SaveGameService, SaveRepository, SaveSlotView, SlotInfo, SlotsDialog, SlotsPresenter | HU-32: 5 |
| RF-33 | Must | HU-33 | Interfaz de usuario
(Swing + FlatLaf)
RF-33 a RF-41
RF-43, 48 | GameCommands, GameController, GameRestController, GameRules, GameScreen, Main, MongoSaveRepository, RemoteGameClient, SaveDocument, SaveGameService, SaveRepository, SaveStorageException, ScreenNavigator, ScreenRouter, StartScreen, StartScreenPresenter, Texts, TitleScene, WoodButton | HU-33: 11 |
| RF-34 | Must | HU-34 | Interfaz de usuario
(Swing + FlatLaf)
RF-33 a RF-41
RF-43, 48 | EnergyBar, GameEvent, GameScreen, GameScreenPresenter, HudModel, HudPanel, HudView, Texts, ViewMapper | HU-34: 9 |
| RF-35 | Must | HU-35 | Interfaz de usuario
(Swing + FlatLaf)
RF-33 a RF-41
RF-43, 48 | AnimalView, BuildingView, CellPos, FarmService, GameCommands, GameController, GameRestController, GameScreen, GroundLayer, MapGeometry, MapPanel, MapRenderer, MapView, Palette, PlotClickListener, PlotText, RemoteGameClient, ViewMapper | HU-35: 14 |
| RF-36 | Must | HU-36 | Interfaz de usuario
(Swing + FlatLaf)
RF-33 a RF-41
RF-43, 48 | GameScreen, GameScreenPresenter, Shortcut, Texts, Tool, ToolBar, ToolModel, WoodSlot | HU-36: 20 |
| RF-37 | Must | HU-37 | Interfaz de usuario
(Swing + FlatLaf)
RF-33 a RF-41
RF-43, 48 | GameCommands, GameController, GameDialogs, GameRestController, GameScreen, InventoryDialog, InventoryGrid, InventoryModel, InventoryView, MarketService, RemoteGameClient, SlotView | HU-37: 9 |
| RF-38 | Must | HU-38 | Interfaz de usuario
(Swing + FlatLaf)
RF-33 a RF-41
RF-43, 48 | AnimalOfferView, BuyOfferView, GameCommands, GameController, GameDialogs, GameRestController, GameScreen, MarketDialog, MarketPresenter, MarketService, MarketView, RemoteGameClient, SellOfferView | HU-38: 10 |
| RF-39 | Should | HU-39 | Interfaz de usuario
(Swing + FlatLaf)
RF-33 a RF-41
RF-43, 48 | GameScreen, GameScreenPresenter, OverlayPanel, Palette, ToastKind, ToastLayer, ToastPresenter, ToastQueue, ToastView | HU-39: 10 |
| RF-40 | Could | HU-40 | Interfaz de usuario
(Swing + FlatLaf)
RF-33 a RF-41
RF-43, 48 | GameScreen, MapPanel, MapRenderer, Particle, SeasonPalette, VisualEffects | HU-40: 8 |
| RF-41 | Could | HU-41 | Interfaz de usuario
(Swing + FlatLaf)
RF-33 a RF-41
RF-43, 48 | AmbientPainter, GameScreen, MapPanel, MapRenderer, VisualEffects | HU-41: 7 |
| RF-42 | Should | HU-42 | Gestor de assets
(AssetManager)
RF-42, 44 | AssetManager, MapRenderer | HU-42: 8 |
| RF-43 | Should | HU-43 | Interfaz de usuario
(Swing + FlatLaf)
RF-33 a RF-41
RF-43, 48 | Contrast, Main, Palette, UiTheme, WoodStyle | HU-43: 5 |
| RF-44 | Should | HU-44 | Gestor de assets
(AssetManager)
RF-42, 44 | AssetManager, GroundLayer, MapGeometry, MapRenderer, PixelScaler | HU-44: 7 |
| RF-45 | Should | HU-45 | Avatar y personajes
(NPC, encargos, tutorial)
RF-45 a RF-47
RF-49 a RF-50 | Avatar, AvatarService, AvatarSnapshot, AvatarView, BuildingService, Direction, ErrorCode, FarmMap, GameCommands, GameController, GameEvent, GameMemento, GameRestController, GameScreen, GameScreenPresenter, GameState, GameStateValidator, MapPanel, MapRenderer, MapView, RemoteGameClient, SaveCodec, Texts, ViewMapper, VisualEffects | HU-45: 15 |
| RF-46 | Should | HU-46 | Avatar y personajes
(NPC, encargos, tutorial)
RF-45 a RF-47
RF-49 a RF-50 | BuildingService, DialogueDialog, DialoguePresenter, DialogueService, DialogueView, ErrorCode, FarmMap, GameCommands, GameController, GameDialogs, GameRestController, GameScreen, GameScreenPresenter, MapPanel, MapRenderer, MapView, Names, Npc, NpcService, NpcView, RemoteGameClient, Texts, ViewMapper | HU-46: 10 |
| RF-47 | Could | HU-47 | Avatar y personajes
(NPC, encargos, tutorial)
RF-45 a RF-47
RF-49 a RF-50 | DialogueDialog, DialoguePresenter, DialogueService, DialogueView, ErrandBook, ErrorCode, GameCommands, GameController, GameEvent, GameMemento, GameRestController, GameState, GameStateValidator, MapRenderer, Names, Npc, NpcService, NpcView, RemoteGameClient, Texts, ToastPresenter | HU-47: 8 |
| RF-48 | Should | HU-48 | Interfaz de usuario
(Swing + FlatLaf)
RF-33 a RF-41
RF-43, 48 | AmbientPainter, AssetManager, EnergyBar, GameScreen, GroundLayer, HoverAnimator, HudModel, HudPanel, InventoryGrid, MapPanel, MapRenderer, PixelScaler, StartScreen, TitleScene, ToolBar, VisualEffects, WoodBox, WoodButton, WoodSlot, WoodStyle | HU-48: 13 |
| RF-49 | Should | HU-49 | Avatar y personajes
(NPC, encargos, tutorial)
RF-45 a RF-47
RF-49 a RF-50 | DialogueDialog, GameCommands, GameController, GameEvent, GameMemento, GameRestController, GameScreen, GameState, RemoteGameClient, Texts, ToastPresenter, Tutorial, TutorialBox, TutorialPresenter, TutorialService, TutorialStep, TutorialView | HU-49: 13 |
| RF-50 | Should | HU-50 | Avatar y personajes
(NPC, encargos, tutorial)
RF-45 a RF-47
RF-49 a RF-50 | FarmMap, FarmMapSnapshot, GameMemento, GameScreen, GroundLayer, House, HouseView, MapGeometry, MapPanel, MapRenderer, MapView, SaveCodec, Terrain, ViewMapper, VisualEffects, WalkKeys | HU-50: 12 |

## 2. Por historia de usuario

| Historia | Fase | Prioridad | Criterios | Pruebas | Estado | Clases |
|---|---|---|---|---|---|---|
| HU-01 · Sembrar cultivo | 5.2 | Must | 4 | 10 | completa | CarrotFactory, CornFactory, Crop, CropFactory, CropSnapshot, CropStage, CropState, CropType, Energy, FarmMap, FarmMapSnapshot, FarmService, GameCommands, GameController, GameEvent, GameMemento, GameRestController, GameRules, GameState, GrowingState, ItemType, MatureState, Names, Plot, PlotView, PotatoFactory, PumpkinFactory, RemoteGameClient, SeedState, SproutState, TomatoFactory, ViewMapper, WitheredState |
| HU-02 · Regar cultivo | 5.2 | Must | 4 | 9 | completa | Crop, FarmService, GameCommands, GameController, GameRestController, RemoteGameClient |
| HU-03 · Crecimiento por etapas | 5.2 | Must | 4 | 5 | completa | Crop, CropState, DayPipeline, FarmMap, GrowingPhaseState, GrowingState, GrowthStep, MatureState, SeedState, SproutState, WitheredState |
| HU-04 · Cosechar cultivo maduro | 5.2 | Must | 4 | 9 | completa | FarmService, GameCommands, GameController, GameRestController, RemoteGameClient |
| HU-05 · Marchitamiento por falta de riego y por estación | 5.2 | Must | 5 | 9 | completa | CalendarStep, Crop, CropState, DayReport, FarmMap, GameRules, GrowingPhaseState, GrowthStep |
| HU-06 · Limpiar parcela marchita | 5.2 | Should | 4 | 6 | completa | FarmService, GameCommands, GameController, GameRestController, RemoteGameClient |
| HU-07 · Consultar el estado de una parcela | 5.2 | Must | 4 | 10 | completa | FarmService, GameCommands, GameController, GameRestController, MapPanel, Names, PlotText, PlotView, RemoteGameClient |
| HU-08 · Administrar la energía diaria | 5.1 | Must | 4 | 6 | completa | Energy, ErrorCode, GameRules, Messages |
| HU-09 · Calendario y estaciones | 5.1 | Must | 4 | 8 | completa | AutumnState, GameClock, GameDate, GameRules, Season, SeasonState, SpringState, SummerState, WinterState |
| HU-10 · Terminar el día | 5.1 | Must | 4 | 13 | completa | CalendarStep, DayPhase, DayPipeline, DayReport, DayService, DayStep, EnergyResetStep, EventBus, ForecastStep, GameCommands, GameController, GameEvent, GameListener, GameMemento, GameRestController, GameScreenPresenter, GameState, Inventory, InventorySnapshot, RemoteGameClient |
| HU-11 · Sorteo de clima y pronóstico | 5.1 | Must | 4 | 8 | completa | AutumnState, FixedWeatherStrategy, GameRules, SeasonState, SeasonalWeatherStrategy, SpringState, StateRandom, SummerState, Weather, WeatherCycle, WeatherStrategy, WinterState |
| HU-12 · Efectos del clima en los cultivos | 5.3 | Must | 4 | 6 | completa | FarmMap, WeatherEffectsStep |
| HU-13 · Administrar el inventario | 5.1 | Must | 4 | 5 | completa | GameRules, Inventory, ItemType |
| HU-14 · Comprar en el mercado | 5.3 | Must | 4 | 7 | completa | BuyOfferView, GameCommands, GameController, GameEvent, GameRestController, GameState, Market, MarketDialog, MarketPresenter, MarketService, MarketView, RemoteGameClient |
| HU-15 · Vender con precio dinámico | 5.3 | Must | 6 | 12 | completa | CropPricing, FlatPricing, GameCommands, GameController, GameEvent, GameMemento, GameRestController, GameState, Market, MarketDialog, MarketPresenter, MarketService, MarketSnapshot, MarketStep, MarketView, PricingStrategy, RemoteGameClient, SellOfferView |
| HU-16 · Tendencia de precio | 5.3 | Should | 4 | 6 | completa | GameCommands, GameController, GameMemento, GameRestController, Market, MarketDialog, MarketPresenter, MarketService, MarketSnapshot, MarketStep, MarketView, RemoteGameClient, SellOfferView, Trend |
| HU-17 · Resumen del día | 5.4 | Could | 4 | 10 | completa | DayLedger, DayPipeline, DayReport, DayService, DaySummaryDialog, DaySummaryText, DaySummaryView, GameCommands, GameController, GameDialogs, GameEvent, GameMemento, GameRestController, GameScreen, GameScreenPresenter, GameState, LedgerSnapshot, RemoteGameClient, ViewMapper |
| HU-18 · Construir edificios | 5.4 | Must | 6 | 17 | completa | Achievement, AchievementTracker, BarnBuilder, Building, BuildingBuilder, BuildingDirector, BuildingOfferView, BuildingService, BuildingSnapshot, BuildingType, BuildingView, BuildingsView, ConstructionDialog, ConstructionPresenter, CoopBuilder, FarmMap, FarmMapSnapshot, GameCommands, GameController, GameDialogs, GameMemento, GameRestController, GameScreen, GameScreenPresenter, GameState, GreenhouseBuilder, MapRenderer, Names, RemoteGameClient, ViewMapper, WarehouseBuilder |
| HU-19 · Comprar animales | 5.4 | Must | 4 | 6 | completa | Animal, AnimalOfferView, AnimalService, AnimalSnapshot, AnimalType, AnimalView, Building, BuildingView, GameCommands, GameController, GameRestController, MapRenderer, MarketPresenter, MarketService, MarketView, RemoteGameClient |
| HU-20 · Alimentar animales | 5.4 | Must | 5 | 10 | completa | Animal, AnimalService, Building, BuildingActionView, BuildingDialog, BuildingPresenter, GameCommands, GameController, GameRestController, GameScreen, GameScreenPresenter, RemoteGameClient |
| HU-21 · Estados del animal | 5.4 | Must | 4 | 5 | completa | Animal, AnimalCondition, AnimalSnapshot, AnimalState, AnimalView, AnimalsStep, DayReport, HealthyState, HungryState, SickState |
| HU-22 · Producción y recolección | 5.4 | Must | 4 | 10 | completa | Animal, AnimalService, AnimalsStep, Building, BuildingActionView, BuildingDialog, BuildingPresenter, DayReport, GameCommands, GameController, GameRestController, GameScreen, GameScreenPresenter, HealthyState, HungryState, RemoteGameClient, SickState |
| HU-23 · Curar animales enfermos | 5.4 | Should | 4 | 8 | completa | Animal, AnimalService, BuildingActionView, BuildingDialog, BuildingPresenter, GameCommands, GameController, GameRestController, GameScreen, RemoteGameClient, SickState |
| HU-24 · Construir y usar el almacén | 5.5 | Should | 4 | 5 | completa | Building, BuildingService |
| HU-25 · Construir y usar el invernadero | 5.6 | Could | 5 | 11 | completa | FarmMap, FarmService, MapPanel, MapRenderer, Plot, PlotText, PlotView |
| HU-26 · Registrar logros | 5.5 | Must | 5 | 10 | completa | Achievement, AchievementService, AchievementTracker, AchievementView, AchievementsDialog, AchievementsPresenter, AchievementsStep, AnimalService, BuildingService, DayReport, DayService, FarmService, GameCommands, GameController, GameDialogs, GameEvent, GameMemento, GameRestController, GameScreen, GameScreenPresenter, GameState, MarketService, ProgressFacts, ProgressSnapshot, RemoteGameClient, ToastPresenter |
| HU-27 · Desbloquear edificios por logros | 5.5 | Should | 4 | 5 | completa | AchievementService, SaveGameService |
| HU-28 · Iniciar nueva partida | 5.1 | Must | 4 | 8 | completa | FarmMap, GameCommands, GameController, GameRestController, GameRules, GameSession, GameState, HudView, Plot, ProgressService, RemoteGameClient, Result, StartScreenPresenter, Terrain, ViewMapper |
| HU-29 · Guardar partida | 5.5 | Must | 4 | 14 | completa | Checksum, GameCommands, GameController, GameDialogs, GameRestController, GameScreen, GameScreenPresenter, MongoSaveRepository, RemoteGameClient, SaveCodec, SaveGameService, SaveRepository, SaveSlotView, SlotsDialog, SlotsPresenter |
| HU-30 · Cargar partida | 5.5 | Must | 4 | 7 | completa | GameCommands, GameController, GameDialogs, GameRestController, GameScreen, GameScreenPresenter, MongoSaveRepository, ProgressSnapshot, RemoteGameClient, SaveCodec, SaveGameService, SaveLoadException, SaveRepository, SlotsDialog, SlotsPresenter, StartScreenPresenter |
| HU-31 · Rechazar partidas inválidas sin alterar la partida | 5.5 | Must | 5 | 12 | completa | Checksum, GameCommands, GameController, GameRestController, GameStateValidator, MongoSaveRepository, RemoteGameClient, SaveCodec, SaveGameService, SaveLoadException, SaveRepository |
| HU-32 · Listar ranuras de guardado | 5.5 | Should | 3 | 5 | completa | GameCommands, GameController, GameRestController, MongoSaveRepository, RemoteGameClient, SaveGameService, SaveRepository, SaveSlotView, SlotInfo, SlotsDialog, SlotsPresenter |
| HU-33 · Pantalla de inicio | 5.1 | Must | 4 | 11 | completa | GameCommands, GameController, GameRestController, GameRules, GameScreen, Main, MongoSaveRepository, RemoteGameClient, SaveDocument, SaveGameService, SaveRepository, SaveStorageException, ScreenNavigator, ScreenRouter, StartScreen, StartScreenPresenter, Texts, TitleScene, WoodButton |
| HU-34 · HUD superior | 5.1 | Must | 4 | 9 | completa | EnergyBar, GameEvent, GameScreen, GameScreenPresenter, HudModel, HudPanel, HudView, Texts, ViewMapper |
| HU-35 · Mapa de la granja | 5.2 | Must | 5 | 14 | completa | AnimalView, BuildingView, CellPos, FarmService, GameCommands, GameController, GameRestController, GameScreen, GroundLayer, MapGeometry, MapPanel, MapRenderer, MapView, Palette, PlotClickListener, PlotText, RemoteGameClient, ViewMapper |
| HU-36 · Herramientas y clic sobre parcelas | 5.2 | Must | 5 | 20 | completa | GameScreen, GameScreenPresenter, Shortcut, Texts, Tool, ToolBar, ToolModel, WoodSlot |
| HU-37 · Inventario en grilla | 5.3 | Must | 4 | 9 | completa | GameCommands, GameController, GameDialogs, GameRestController, GameScreen, InventoryDialog, InventoryGrid, InventoryModel, InventoryView, MarketService, RemoteGameClient, SlotView |
| HU-38 · Mercado modal | 5.3 | Must | 4 | 10 | completa | AnimalOfferView, BuyOfferView, GameCommands, GameController, GameDialogs, GameRestController, GameScreen, MarketDialog, MarketPresenter, MarketService, MarketView, RemoteGameClient, SellOfferView |
| HU-39 · Notificaciones (toasts) | 5.6 | Should | 4 | 10 | completa | GameScreen, GameScreenPresenter, OverlayPanel, Palette, ToastKind, ToastLayer, ToastPresenter, ToastQueue, ToastView |
| HU-40 · Efectos visuales de estación y clima | 5.6 | Could | 4 | 8 | completa | GameScreen, MapPanel, MapRenderer, Particle, SeasonPalette, VisualEffects |
| HU-41 · Animaciones suaves | 5.6 | Could | 4 | 7 | completa | AmbientPainter, GameScreen, MapPanel, MapRenderer, VisualEffects |
| HU-42 · Carga de sprites con respaldo vectorial | 5.6 | Should | 4 | 8 | completa | AssetManager, MapRenderer |
| HU-43 · Tema visual profesional con FlatLaf | 5.7 | Should | 4 | 5 | completa | Contrast, Main, Palette, UiTheme, WoodStyle |
| HU-44 · Sprites pixel-art propios de 32 × 32 | 5.7 | Should | 5 | 7 | completa | AssetManager, GroundLayer, MapGeometry, MapRenderer, PixelScaler |
| HU-45 · Mover el avatar por la granja | 5.7 | Should | 5 | 15 | completa | Avatar, AvatarService, AvatarSnapshot, AvatarView, BuildingService, Direction, ErrorCode, FarmMap, GameCommands, GameController, GameEvent, GameMemento, GameRestController, GameScreen, GameScreenPresenter, GameState, GameStateValidator, MapPanel, MapRenderer, MapView, RemoteGameClient, SaveCodec, Texts, ViewMapper, VisualEffects |
| HU-46 · Hablar con los NPC | 5.7 | Should | 5 | 10 | completa | BuildingService, DialogueDialog, DialoguePresenter, DialogueService, DialogueView, ErrorCode, FarmMap, GameCommands, GameController, GameDialogs, GameRestController, GameScreen, GameScreenPresenter, MapPanel, MapRenderer, MapView, Names, Npc, NpcService, NpcView, RemoteGameClient, Texts, ViewMapper |
| HU-47 · Encargos de los NPC | 5.7 | Could | 4 | 8 | completa | DialogueDialog, DialoguePresenter, DialogueService, DialogueView, ErrandBook, ErrorCode, GameCommands, GameController, GameEvent, GameMemento, GameRestController, GameState, GameStateValidator, MapRenderer, Names, Npc, NpcService, NpcView, RemoteGameClient, Texts, ToastPresenter |
| HU-48 · Interfaz con aspecto de granja | 5.7 | Should | 8 | 13 | completa | AmbientPainter, AssetManager, EnergyBar, GameScreen, GroundLayer, HoverAnimator, HudModel, HudPanel, InventoryGrid, MapPanel, MapRenderer, PixelScaler, StartScreen, TitleScene, ToolBar, VisualEffects, WoodBox, WoodButton, WoodSlot, WoodStyle |
| HU-49 · Tutorial de inicio guiado por el avatar | 5.7 | Should | 5 | 13 | completa | DialogueDialog, GameCommands, GameController, GameEvent, GameMemento, GameRestController, GameScreen, GameState, RemoteGameClient, Texts, ToastPresenter, Tutorial, TutorialBox, TutorialPresenter, TutorialService, TutorialStep, TutorialView |
| HU-50 · Recorrer el valle: granja, aldea y cámara | 5.7 | Should | 6 | 12 | completa | FarmMap, FarmMapSnapshot, GameMemento, GameScreen, GroundLayer, House, HouseView, MapGeometry, MapPanel, MapRenderer, MapView, SaveCodec, Terrain, ViewMapper, VisualEffects, WalkKeys |

**Resumen:** 50 de 50 historias con todas sus pruebas; 461 pruebas `HUxx_` en total.
