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
 * Termina el día: ejecuta el pipeline de RN-03 y publica los eventos (paso 9).
 *
 * <p>Antes de ejecutar los pasos toma una instantánea; si algún paso falla se restaura la instantánea, de modo que un
 * final de día fallido deja la partida exactamente como estaba y no publica nada (HU-10 · CA-4).
 *
 * <p>Stories: HU-10, HU-17, HU-26.
 */
@Service
public final class DayService {

    private static final Logger LOG = Logger.getLogger(DayService.class.getName());

    private final GameSession session;
    private final DayPipeline pipeline;
    private final ViewMapper mapper;
    private final EventBus events;
    private final AchievementService achievements;

    /**
     * Crea el servicio.
     *
     * @param session  la partida en curso
     * @param pipeline los pasos del final del día
     * @param mapper   construye las vistas que dibuja la UI
     * @param events   donde se publican los eventos
     * @param achievements anuncia los logros desbloqueados al final del día
     */
    public DayService(GameSession session, DayPipeline pipeline, ViewMapper mapper, EventBus events,
                      AchievementService achievements) {
        this.session = session;
        this.pipeline = pipeline;
        this.mapper = mapper;
        this.events = events;
        this.achievements = achievements;
    }

    /** Termina el día de la partida en curso y devuelve la nueva barra superior. */
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
        achievements.announce(state, report.unlocked());
        events.publish(new GameEvent.DaySummaryReady(mapper.summary(state, report)));
        return Result.ok(hud);
    }
}
