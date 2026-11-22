package edu.ucc.farmsim.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.application.dto.HudView;
import edu.ucc.farmsim.application.dto.MapView;
import edu.ucc.farmsim.application.dto.PlotView;
import edu.ucc.farmsim.application.event.EventBus;
import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.application.service.GameSession;
import edu.ucc.farmsim.config.ServerApplication;
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
    void hasGameInProgressFollowsTheServer() {
        assertFalse(client.hasGameInProgress());
        client.newGame(TestApp.SEED);
        assertTrue(client.hasGameInProgress());
    }

    @Test
    void sowingRepublishesTheEventsOfTheOrderInTheOrderTheServerProducedThem() {
        client.newGame(TestApp.SEED);
        events.clear();

        Result<PlotView> result = client.plant(COL, ROW, "CARROT_SEED");

        assertTrue(result.isOk());
        assertEquals("CARROT", result.value().crop());
        assertEquals(List.of(new GameEvent.MapChanged(), new GameEvent.EnergyChanged(98, 100)), events);
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
        assertEquals(16, map.value().cols());
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
        log.add(List.copyOf(seen));
        return log;
    }

    private static Object describe(Result<?> result) {
        return result.isOk() ? result.value() : result.code() + ": " + result.message();
    }
}
