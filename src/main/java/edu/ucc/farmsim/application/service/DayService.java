package edu.ucc.farmsim.application.service;

import edu.ucc.farmsim.application.dto.HudView;
import edu.ucc.farmsim.application.event.EventBus;
import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.domain.game.DayPipeline;
import edu.ucc.farmsim.domain.game.DayReport;
import edu.ucc.farmsim.domain.game.GameMemento;
import edu.ucc.farmsim.domain.game.GameState;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.stereotype.Service;

/**
 * Ends the day: runs the pipeline of RN-03 and publishes the events (step 9).
 *
 * <p>Before running the steps it takes a snapshot; if any step fails the snapshot is restored, so a failed
 * end of the day leaves the game exactly as it was and publishes nothing (HU-10 · CA-4).
 *
 * <p>Stories: HU-10, HU-17.
 */
@Service
public final class DayService {

    private static final Logger LOG = Logger.getLogger(DayService.class.getName());

    private final GameSession session;
    private final DayPipeline pipeline;
    private final ViewMapper mapper;
    private final EventBus events;

    /**
     * Creates the service.
     *
     * @param session  the game in progress
     * @param pipeline the steps of the end of the day
     * @param mapper   builds the views the UI draws
     * @param events   where events are published
     */
    public DayService(GameSession session, DayPipeline pipeline, ViewMapper mapper, EventBus events) {
        this.session = session;
        this.pipeline = pipeline;
        this.mapper = mapper;
        this.events = events;
    }

    /** Ends the day of the game in progress and returns the new top bar. */
    public Result<HudView> endDay() {
        if (!session.hasGame()) {
            return Result.failWith(ErrorCode.SIN_PARTIDA);
        }
        GameState state = session.current();
        GameMemento before = state.createMemento();
        DayReport report;
        try {
            report = pipeline.run(state);
        } catch (RuntimeException e) {
            state.restore(before);
            LOG.log(Level.SEVERE, "The end of the day failed; the state was restored", e);
            return Result.failWith(ErrorCode.ERROR_INTERNO);
        }
        HudView hud = mapper.hud(state);
        events.publish(new GameEvent.DayEnded(hud));
        events.publish(new GameEvent.DaySummaryReady(mapper.summary(state, report)));
        return Result.ok(hud);
    }
}
