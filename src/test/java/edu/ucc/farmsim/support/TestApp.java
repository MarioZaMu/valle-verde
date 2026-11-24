package edu.ucc.farmsim.support;

import edu.ucc.farmsim.application.controller.GameController;
import edu.ucc.farmsim.application.event.EventBus;
import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.application.service.AchievementService;
import edu.ucc.farmsim.application.service.AnimalService;
import edu.ucc.farmsim.application.service.AvatarService;
import edu.ucc.farmsim.application.service.BuildingService;
import edu.ucc.farmsim.application.service.DayService;
import edu.ucc.farmsim.application.service.FarmService;
import edu.ucc.farmsim.application.service.GameSession;
import edu.ucc.farmsim.application.service.MarketService;
import edu.ucc.farmsim.application.service.NpcService;
import edu.ucc.farmsim.application.service.DialogueService;
import edu.ucc.farmsim.application.service.ProgressService;
import edu.ucc.farmsim.application.service.SaveGameService;
import edu.ucc.farmsim.application.service.TutorialService;
import edu.ucc.farmsim.application.service.ViewMapper;
import edu.ucc.farmsim.domain.game.DayPipeline;
import edu.ucc.farmsim.domain.game.GameState;
import edu.ucc.farmsim.domain.save.SaveRepository;
import edu.ucc.farmsim.domain.time.GameClock;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/**
 * Arma el grafo de la aplicación para las pruebas de aceptación: un reloj nuevo, una sesión, un bus de eventos y un
 * controlador.
 */
public final class TestApp {

    /** Semilla fija que usan las pruebas a las que no les importa. */
    public static final long SEED = 2026L;

    /** El momento con el que se sella todo guardado hecho a través de un grafo de pruebas: 2026-11-22 10:30 UTC. */
    public static final Clock FIXED_CLOCK = Clock.fixed(java.time.Instant.parse("2026-11-22T10:30:00Z"),
            java.time.ZoneOffset.UTC);

    public final GameSession session;
    public final EventBus bus;
    public final GameController controller;
    /** Todo evento publicado en el bus desde que se creó la aplicación. */
    public final List<GameEvent> events = new ArrayList<>();

    private TestApp(GameSession session, EventBus bus, GameController controller) {
        this.session = session;
        this.bus = bus;
        this.controller = controller;
        bus.subscribe(events::add);
    }

    /**
     * Crea el grafo e inicia una partida nueva (Año 1, Primavera, día 1, 500 m, 10 semillas de zanahoria y 5 de papa)
     * con su tutorial de inicio ya saltado, de modo que los eventos de las pruebas sean solo los de la partida misma.
     */
    public static TestApp startedGame() {
        TestApp app = create();
        app.controller.newGame(SEED);
        app.controller.skipTutorial();
        app.events.clear();
        return app;
    }

    /** La partida en curso. */
    public GameState state() {
        return session.current();
    }

    /** Crea el grafo con el pipeline estándar de fin de día. */
    public static TestApp create() {
        return create(DayPipeline.standard());
    }

    /** Crea el grafo con un pipeline de fin de día dado (pruebas que necesitan pasos extra o fallidos). */
    public static TestApp create(DayPipeline pipeline) {
        GameClock.getInstance().resetForTests();
        GameSession session = new GameSession();
        return create(session, pipeline);
    }

    /** Crea el grafo alrededor de una sesión dada (pruebas que necesitan una sesión defectuosa). */
    public static TestApp create(GameSession session, DayPipeline pipeline) {
        return create(session, pipeline, new FakeSaveRepository());
    }

    /** Crea el grafo alrededor de una sesión y un repositorio de partidas dados. */
    public static TestApp create(GameSession session, DayPipeline pipeline, SaveRepository repository) {
        return create(session, pipeline, repository, FIXED_CLOCK);
    }

    /**
     * Crea el grafo alrededor de una sesión, un repositorio de partidas y un reloj dados (el reloj sella los
     * guardados).
     */
    public static TestApp create(GameSession session, DayPipeline pipeline, SaveRepository repository, Clock clock) {
        GameClock.getInstance().resetForTests();
        ViewMapper mapper = new ViewMapper();
        EventBus bus = new EventBus();
        ProgressService progress = new ProgressService(session, mapper, bus);
        AchievementService achievements = new AchievementService(session, bus);
        DayService day = new DayService(session, pipeline, mapper, bus, achievements);
        SaveGameService saves = new SaveGameService(repository, session, mapper, bus, clock);
        FarmService farm = new FarmService(session, mapper, bus, achievements);
        MarketService market = new MarketService(session, bus, achievements);
        BuildingService buildings = new BuildingService(session, mapper, bus, achievements);
        AnimalService animals = new AnimalService(session, mapper, bus, achievements);
        return new TestApp(session, bus,
                new GameController(session, mapper, progress, day, saves, farm, market, buildings, animals,
                        achievements, new AvatarService(session, mapper, bus),
                        new NpcService(session, mapper, new DialogueService(), bus, achievements),
                        new TutorialService(session, bus)));
    }
}
