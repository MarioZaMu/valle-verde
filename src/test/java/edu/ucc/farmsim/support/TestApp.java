package edu.ucc.farmsim.support;

import edu.ucc.farmsim.application.controller.GameController;
import edu.ucc.farmsim.application.event.EventBus;
import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.application.service.DayService;
import edu.ucc.farmsim.application.service.FarmService;
import edu.ucc.farmsim.application.service.GameSession;
import edu.ucc.farmsim.application.service.ProgressService;
import edu.ucc.farmsim.application.service.SaveGameService;
import edu.ucc.farmsim.application.service.ViewMapper;
import edu.ucc.farmsim.domain.game.DayPipeline;
import edu.ucc.farmsim.domain.game.GameState;
import edu.ucc.farmsim.domain.save.SaveRepository;
import edu.ucc.farmsim.domain.time.GameClock;
import java.util.ArrayList;
import java.util.List;

/** Builds the application graph for acceptance tests: a fresh clock, a session, an event bus and a controller. */
public final class TestApp {

    /** Fixed seed used by tests that do not care about it. */
    public static final long SEED = 2026L;

    public final GameSession session;
    public final EventBus bus;
    public final GameController controller;
    /** Every event published on the bus since the app was created. */
    public final List<GameEvent> events = new ArrayList<>();

    private TestApp(GameSession session, EventBus bus, GameController controller) {
        this.session = session;
        this.bus = bus;
        this.controller = controller;
        bus.subscribe(events::add);
    }

    /** Creates the graph and starts a new game (Year 1, Spring, day 1, 500 m, 10 carrot and 5 potato seeds). */
    public static TestApp startedGame() {
        TestApp app = create();
        app.controller.newGame(SEED);
        app.events.clear();
        return app;
    }

    /** The game in progress. */
    public GameState state() {
        return session.current();
    }

    /** Creates the graph with the standard end-of-day pipeline. */
    public static TestApp create() {
        return create(DayPipeline.standard());
    }

    /** Creates the graph with a given end-of-day pipeline (tests that need extra or failing steps). */
    public static TestApp create(DayPipeline pipeline) {
        GameClock.getInstance().resetForTests();
        GameSession session = new GameSession();
        return create(session, pipeline);
    }

    /** Creates the graph around a given session (tests that need a faulty session). */
    public static TestApp create(GameSession session, DayPipeline pipeline) {
        return create(session, pipeline, new FakeSaveRepository());
    }

    /** Creates the graph around a given session and save repository. */
    public static TestApp create(GameSession session, DayPipeline pipeline, SaveRepository repository) {
        GameClock.getInstance().resetForTests();
        ViewMapper mapper = new ViewMapper();
        EventBus bus = new EventBus();
        ProgressService progress = new ProgressService(session, mapper, bus);
        DayService day = new DayService(session, pipeline, mapper, bus);
        SaveGameService saves = new SaveGameService(repository);
        FarmService farm = new FarmService(session, mapper, bus);
        return new TestApp(session, bus, new GameController(session, mapper, progress, day, saves, farm));
    }
}
