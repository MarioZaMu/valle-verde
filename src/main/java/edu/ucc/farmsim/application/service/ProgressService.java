package edu.ucc.farmsim.application.service;

import edu.ucc.farmsim.application.dto.HudView;
import edu.ucc.farmsim.application.event.EventBus;
import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.domain.game.GameState;

/**
 * Starts games (and, in later phases, tracks achievements and unlocks).
 *
 * <p>Stories: HU-28.
 */
public final class ProgressService {

    private final GameSession session;
    private final ViewMapper mapper;
    private final EventBus events;

    /**
     * Creates the service.
     *
     * @param session the game in progress
     * @param mapper  builds the views the UI draws
     * @param events  where events are published
     */
    public ProgressService(GameSession session, ViewMapper mapper, EventBus events) {
        this.session = session;
        this.mapper = mapper;
        this.events = events;
    }

    /** Creates the initial state of RN-30, makes it the game in progress and announces it. */
    public Result<HudView> newGame(long seed) {
        GameState state = GameState.newGame(seed);
        session.replace(state);
        HudView hud = mapper.hud(state);
        events.publish(new GameEvent.GameStarted(hud));
        return Result.ok(hud);
    }
}
