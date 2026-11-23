package edu.ucc.farmsim.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.application.dto.AvatarView;
import edu.ucc.farmsim.application.dto.BuildingActionView;
import edu.ucc.farmsim.application.dto.BuildingView;
import edu.ucc.farmsim.application.dto.BuildingsView;
import edu.ucc.farmsim.application.dto.HudView;
import edu.ucc.farmsim.application.dto.InventoryView;
import edu.ucc.farmsim.application.dto.MapView;
import edu.ucc.farmsim.application.dto.MarketView;
import edu.ucc.farmsim.application.dto.PlotView;
import edu.ucc.farmsim.application.event.EventBus;
import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.application.service.GameSession;
import edu.ucc.farmsim.config.ServerApplication;
import edu.ucc.farmsim.domain.economy.ItemType;
import edu.ucc.farmsim.domain.time.GameClock;
import edu.ucc.farmsim.support.FakeSaveRepository;
import edu.ucc.farmsim.support.GameFixtures;
import edu.ucc.farmsim.support.NoMongoTestConfig;
import edu.ucc.farmsim.support.TestApp;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * The client against the real server on a free port: it must give the same results and the same events as
 * the in-process controller (DT-17, DT-19), and never throw (R-03).
 */
@SpringBootTest(classes = ServerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("no-mongo")
@Import(NoMongoTestConfig.class)
class RemoteGameClientTest {

    private static final int COL = GameFixtures.SOIL_COL;
    private static final int ROW = GameFixtures.SOIL_ROW;

    @LocalServerPort
    private int port;
    @Autowired
    private GameSession session;
    @Autowired
    private FakeSaveRepository saves;

    private final EventBus bus = new EventBus();
    private final List<GameEvent> events = new ArrayList<>();
    private RemoteGameClient client;

    @BeforeEach
    void connect() {
        GameClock.getInstance().resetForTests();
        session.replace(null);
        saves.reset();
        client = new RemoteGameClient(URI.create("http://127.0.0.1:" + port), bus);
        bus.subscribe(events::add);
    }

    @Test
    void newGameRebuildsTheHudAndRepublishesGameStartedOnTheLocalBus() {
        Result<HudView> result = client.newGame(TestApp.SEED);

        assertTrue(result.isOk(), String.valueOf(result.message()));
        assertEquals(500, result.value().money());
        assertEquals(1, events.size());
        assertEquals(new GameEvent.GameStarted(result.value()), events.get(0));
    }

    @Test
    void RNF02_theWindowCanOpenBeforeTheServerIsUpAndAnOrderWaitsForIt() throws Exception {
        java.util.concurrent.CompletableFuture<URI> server = new java.util.concurrent.CompletableFuture<>();
        RemoteGameClient early = new RemoteGameClient(server, bus, java.time.Duration.ofSeconds(5));
        new Thread(() -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            server.complete(URI.create("http://127.0.0.1:" + port));
        }).start();

        long start = System.nanoTime();
        Result<HudView> result = early.newGame(TestApp.SEED);

        assertTrue(result.isOk(), String.valueOf(result.message()));
        assertTrue((System.nanoTime() - start) / 1_000_000 >= 250, "it waited for the server");
        assertEquals(500, result.value().money());
    }

    @Test
    void RNF02_aServerThatFailsToStartOrNeverComesUpIsAnInternalErrorNotAHang() {
        java.util.concurrent.CompletableFuture<URI> failed = new java.util.concurrent.CompletableFuture<>();
        failed.completeExceptionally(new IllegalStateException("port in use"));
        RemoteGameClient broken = new RemoteGameClient(failed, bus, java.time.Duration.ofSeconds(5));
        RemoteGameClient never = new RemoteGameClient(new java.util.concurrent.CompletableFuture<>(), bus,
                java.time.Duration.ofMillis(200));

        assertEquals(ErrorCode.ERROR_INTERNO, broken.hud().code());
        assertEquals(ErrorCode.ERROR_INTERNO, never.newGame(TestApp.SEED).code());
        assertFalse(never.hasGameInProgress());
        assertTrue(events.isEmpty());
    }

    @Test
    void hasGameInProgressFollowsTheServer() {
        assertFalse(client.hasGameInProgress());
        client.newGame(TestApp.SEED);
        assertTrue(client.hasGameInProgress());
    }

    @Test
    void HU45_aStepThroughTheServerGivesTheSameAnswerAndEventAsTheLocalController() {
        client.newGame(TestApp.SEED);
        client.skipTutorial();
        TestApp local = TestApp.startedGame();
        events.clear();

        Result<AvatarView> remote = client.move("EAST");
        Result<AvatarView> here = local.controller.move("EAST");

        assertTrue(remote.isOk(), String.valueOf(remote.message()));
        assertEquals(here.value(), remote.value());
        assertEquals(local.events, events);
        assertEquals(here.value(), client.map().value().avatar());
        client.move("SOUTH");
        client.move("WEST");
        Result<AvatarView> blocked = client.move("SOUTH");
        assertEquals(ErrorCode.MOVIMIENTO_BLOQUEADO, blocked.code());
        assertEquals("No puedes pasar por ahí.", blocked.message());
    }

    @Test
    void HU49_theTutorialThroughTheServerMatchesTheLocalControllerStepByStep() {
        client.newGame(TestApp.SEED);
        TestApp local = TestApp.create();
        local.controller.newGame(TestApp.SEED);
        local.events.clear();
        events.clear();

        assertEquals(local.controller.tutorial().value(), client.tutorial().value());
        assertEquals(local.controller.move("EAST").value(), client.move("EAST").value());
        assertEquals(local.controller.plant(8, 6, "CARROT_SEED").value(), client.plant(8, 6, "CARROT_SEED").value());
        assertEquals(local.controller.skipTutorial().value(), client.skipTutorial().value());
        assertEquals(local.events, events, "the same TutorialAdvanced events, in the same places");
        assertTrue(client.tutorial().value().finished());
        session.replace(null);
        assertEquals(ErrorCode.SIN_PARTIDA, client.tutorial().code());
    }

    @Test
    void HU46_HU47_talkingAndDeliveringThroughTheServerMatchTheLocalController() {
        client.newGame(TestApp.SEED);
        TestApp local = TestApp.startedGame();
        for (var state : List.of(session.current(), local.state())) {
            state.avatar().restoreFrom(new edu.ucc.farmsim.domain.farm.AvatarSnapshot(28, 11,
                    edu.ucc.farmsim.domain.farm.Direction.EAST));
            state.inventory().add(ItemType.CARROT, 5);
        }
        events.clear();

        assertEquals(local.controller.npcs().value(), client.npcs().value());
        assertEquals(local.controller.talk("MARTA").value(), client.talk("MARTA").value());
        assertEquals(local.controller.deliver("MARTA").value(), client.deliver("MARTA").value());
        assertEquals(local.events, events);
        assertEquals(ErrorCode.NPC_LEJOS, client.talk("INES").code());
        assertEquals(local.controller.map().value(), client.map().value());
    }

    @Test
    void sowingRepublishesTheEventsOfTheOrderInTheOrderTheServerProducedThem() {
        client.newGame(TestApp.SEED);
        events.clear();

        Result<PlotView> result = client.plant(COL, ROW, "CARROT_SEED");

        assertTrue(result.isOk());
        assertEquals("CARROT", result.value().crop());
        assertEquals(List.of(new GameEvent.MapChanged(), new GameEvent.EnergyChanged(98, 100),
                new GameEvent.InventoryChanged()), events);
    }

    @Test
    void aRejectionKeepsItsCodeAndSpanishMessageAndPublishesNothing() {
        client.newGame(TestApp.SEED);
        events.clear();

        Result<PlotView> result = client.plant(COL, ROW, "PUMPKIN_SEED");

        assertFalse(result.isOk());
        assertEquals(ErrorCode.FUERA_DE_ESTACION, result.code());
        assertEquals("No se puede sembrar Calabaza en Primavera: solo en Otoño.", result.message());
        assertTrue(events.isEmpty());
    }

    @Test
    void withoutAGameTheServerAnswersSinPartida() {
        Result<HudView> result = client.hud();

        assertFalse(result.isOk());
        assertEquals(ErrorCode.SIN_PARTIDA, result.code());
        assertEquals("No hay una partida en curso.", result.message());
    }

    @Test
    void theMapAndThePlotsComeBackAsTheViewsTheScreensDraw() {
        client.newGame(TestApp.SEED);
        client.plant(COL, ROW, "CARROT_SEED");

        Result<MapView> map = client.map();
        Result<PlotView> plot = client.inspect(COL, ROW);

        assertTrue(map.isOk());
        assertEquals(48, map.value().cols());
        assertEquals(5, map.value().houses().size());
        assertEquals("CARROT", map.value().plotAt(COL, ROW).crop());
        assertEquals(map.value().plotAt(COL, ROW), plot.value());
    }

    @Test
    void endDayWateringAndHarvestTravelToo() {
        client.newGame(TestApp.SEED);
        client.plant(COL, ROW, "CARROT_SEED");

        assertTrue(client.water(COL, ROW).isOk());
        assertEquals(ErrorCode.YA_REGADO, client.water(COL, ROW).code());
        assertEquals(ErrorCode.NO_MADURO, client.harvest(COL, ROW).code());
        assertEquals(2, client.endDay().value().day());
    }

    @Test
    void savedGamesAreReportedAndAStorageOutageBecomesErrorEs() {
        assertEquals(false, client.hasSavedGames().value());
        saves.withSlot(3);
        assertEquals(true, client.hasSavedGames().value());

        saves.unreachable();
        Result<Boolean> down = client.hasSavedGames();

        assertEquals(ErrorCode.ERROR_ES, down.code());
        assertEquals("No se pudo acceder a las partidas guardadas: MongoDB no respondió.", down.message());
    }

    @Test
    void anUnexpectedServerErrorBecomesErrorInternoWithoutThrowing() {
        client.newGame(TestApp.SEED);

        Result<PlotView> result = client.plant(COL, ROW, "NOT_A_SEED");

        assertEquals(ErrorCode.ERROR_INTERNO, result.code());
    }

    @Test
    void anUnreachableServerBecomesErrorInternoWithoutThrowing() {
        RemoteGameClient lost = new RemoteGameClient(URI.create("http://127.0.0.1:1"), bus);

        assertEquals(ErrorCode.ERROR_INTERNO, lost.hud().code());
        assertEquals(ErrorCode.ERROR_INTERNO, lost.newGame(1L).code());
        assertFalse(lost.hasGameInProgress());
    }

    @Test
    void theRemoteClientGivesTheSameResultsAndEventsAsTheInProcessController() {
        TestApp local = TestApp.create();
        List<GameEvent> localEvents = local.events;
        GameClock.getInstance().resetForTests();

        List<Object> remote = script(client, events);
        GameClock.getInstance().resetForTests();
        List<Object> inProcess = script(local.controller, localEvents);

        assertEquals(inProcess, remote);
    }

    @Test
    void sellingBuyingAndTheMarketAndInventoryViewsTravelThroughTheClient() {
        client.newGame(TestApp.SEED);
        session.current().inventory().add(ItemType.CARROT, 3);
        events.clear();

        Result<Integer> quote = client.quote("CARROT", 3);
        Result<Integer> sale = client.sell("CARROT", 3);
        Result<Integer> purchase = client.buy("HAY", 4);
        Result<MarketView> market = client.market();
        Result<InventoryView> inventory = client.inventory();

        assertEquals(79, quote.value());
        assertEquals(79, sale.value());
        assertEquals(20, purchase.value());
        assertEquals(7, market.value().sells().size());
        assertEquals("Heno", market.value().buys().stream().filter(o -> o.itemId().equals("HAY")).findFirst().orElseThrow().name());
        assertEquals(24, inventory.value().capacity());
        assertEquals(List.of(new GameEvent.MoneyChanged(579), new GameEvent.InventoryChanged(),
                new GameEvent.MoneyChanged(559), new GameEvent.InventoryChanged()), events);
    }

    @Test
    void buildingsAnimalsAndTheDaySummaryTravelThroughTheClient() {
        client.newGame(TestApp.SEED);
        session.current().inventory().add(ItemType.HAY, 3);
        session.current().earn(500);
        events.clear();

        Result<BuildingView> built = client.build("COOP", 2, 7);
        Result<BuildingView> bought = client.buyAnimal("HEN");
        Result<BuildingActionView> fed = client.feed(0);
        Result<BuildingsView> all = client.buildings();
        Result<MapView> map = client.map();
        client.endDay();
        Result<BuildingActionView> collected = client.collect(0);

        assertTrue(built.isOk());
        assertEquals("Gallinero", built.value().name());
        assertEquals(1, bought.value().animals().size());
        assertEquals(1, fed.value().amount());
        assertEquals(1, all.value().buildings().size());
        assertEquals(1, map.value().buildings().size());
        assertEquals(0, map.value().buildingAt(3, 8).id());
        assertEquals(1, collected.value().amount());
        assertEquals(1, events.stream().filter(GameEvent.DaySummaryReady.class::isInstance).count());
        assertEquals(ErrorCode.UBICACION_INVALIDA, client.build("COOP", 3, 2).code());
    }

    @Test
    void achievementsSlotsSavingAndLoadingTravelThroughTheClient() {
        client.newGame(TestApp.SEED);
        events.clear();

        Result<java.util.List<edu.ucc.farmsim.application.dto.AchievementView>> achievements = client.achievements();
        Result<edu.ucc.farmsim.application.dto.SaveSlotView> saved = client.save(1);
        client.endDay();
        Result<java.util.List<edu.ucc.farmsim.application.dto.SaveSlotView>> slots = client.saveSlots();
        events.clear();
        Result<HudView> loaded = client.load(1);

        assertEquals(9, achievements.value().size());
        assertEquals("OK", saved.value().status());
        assertEquals(java.util.List.of("OK", "EMPTY", "EMPTY"),
                slots.value().stream().map(edu.ucc.farmsim.application.dto.SaveSlotView::status).toList());
        assertEquals(1, loaded.value().day());
        assertTrue(events.stream().anyMatch(GameEvent.GameStarted.class::isInstance));
        assertEquals(ErrorCode.ARCHIVO_INEXISTENTE, client.load(3).code());
        assertEquals("No existe una partida en la ranura 3.", client.load(3).message());
    }

    @Test
    void anAchievementUnlockedOnTheServerIsRepublishedOnTheLocalBus() {
        client.newGame(TestApp.SEED);
        session.current().inventory().add(ItemType.CARROT, 1);
        events.clear();
        session.current().progress().recordHarvest();
        GameFixtures.plant(session.current(), COL, ROW, edu.ucc.farmsim.domain.crop.CropType.CARROT, 4);

        client.harvest(COL, ROW);

        assertTrue(events.contains(new GameEvent.AchievementUnlocked("LG-01", "Primera cosecha", 50)));
    }

    /** The same sequence of orders, recording every result and then the events each one caused. */
    private static List<Object> script(edu.ucc.farmsim.application.controller.GameCommands game, List<GameEvent> seen) {
        List<Object> log = new ArrayList<>();
        seen.clear();
        log.add(describe(game.newGame(TestApp.SEED)));
        log.add(describe(game.plant(COL, ROW, "CARROT_SEED")));
        log.add(describe(game.plant(COL, ROW, "CARROT_SEED")));
        log.add(describe(game.plant(COL + 1, ROW, "PUMPKIN_SEED")));
        log.add(describe(game.water(COL, ROW)));
        log.add(describe(game.water(COL, ROW)));
        log.add(describe(game.harvest(COL, ROW)));
        log.add(describe(game.endDay()));
        log.add(describe(game.inspect(COL, ROW)));
        log.add(describe(game.map()));
        log.add(describe(game.buy("CORN_SEED", 5)));
        log.add(describe(game.buy("MEDICINE", 99)));
        log.add(describe(game.buy("HAY", 0)));
        log.add(describe(game.sell("CARROT", 1)));
        log.add(describe(game.quote("CARROT", 1)));
        log.add(describe(game.market()));
        log.add(describe(game.inventory()));
        log.add(describe(game.build("COOP", 2, 7)));
        log.add(describe(game.build("COOP", 3, 2)));
        log.add(describe(game.build("BARN", 8, 7)));
        log.add(describe(game.buyAnimal("HEN")));
        log.add(describe(game.buyAnimal("COW")));
        log.add(describe(game.feed(0)));
        log.add(describe(game.collect(0)));
        log.add(describe(game.cure(0, 0)));
        log.add(describe(game.buildings()));
        log.add(describe(game.endDay()));
        log.add(describe(game.achievements()));
        log.add(game.save(2).isOk()); // the time of the save differs between the two clocks
        log.add(game.saveSlots().value().stream().map(edu.ucc.farmsim.application.dto.SaveSlotView::status).toList());
        log.add(describe(game.load(2)));
        log.add(describe(game.load(3)));
        log.add(List.copyOf(seen));
        return log;
    }

    private static Object describe(Result<?> result) {
        return result.isOk() ? result.value() : result.code() + ": " + result.message();
    }
}
