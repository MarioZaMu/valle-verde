package edu.ucc.farmsim.support;

import edu.ucc.farmsim.application.controller.GameController;
import edu.ucc.farmsim.application.service.GameSession;
import edu.ucc.farmsim.application.service.ViewMapper;
import edu.ucc.farmsim.domain.time.GameClock;

/** Builds the application graph for acceptance tests: a fresh clock, a session and a controller. */
public final class TestApp {

    /** Fixed seed used by tests that do not care about it. */
    public static final long SEED = 2026L;

    public final GameSession session;
    public final GameController controller;

    private TestApp(GameSession session, GameController controller) {
        this.session = session;
        this.controller = controller;
    }

    /** Creates the graph with the clock reset to Year 1, Spring, day 1. */
    public static TestApp create() {
        GameClock.getInstance().resetForTests();
        GameSession session = new GameSession();
        return new TestApp(session, new GameController(session, new ViewMapper()));
    }
}
