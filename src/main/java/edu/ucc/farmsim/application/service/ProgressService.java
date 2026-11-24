package edu.ucc.farmsim.application.service;

import edu.ucc.farmsim.application.dto.HudView;
import edu.ucc.farmsim.application.event.EventBus;
import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.domain.game.GameState;
import org.springframework.stereotype.Service;

/**
 * Inicia partidas (y, en fases posteriores, sigue los logros y los desbloqueos).
 *
 * <p>Stories: HU-28.
 */
@Service
public final class ProgressService {

    private final GameSession session;
    private final ViewMapper mapper;
    private final EventBus events;

    /**
     * Crea el servicio.
     *
     * @param session la partida en curso
     * @param mapper  construye las vistas que dibuja la UI
     * @param events  donde se publican los eventos
     */
    public ProgressService(GameSession session, ViewMapper mapper, EventBus events) {
        this.session = session;
        this.mapper = mapper;
        this.events = events;
    }

    /** Crea el estado inicial de RN-30, lo convierte en la partida en curso y lo anuncia. */
    public Result<HudView> newGame(long seed) {
        GameState state = GameState.newGame(seed);
        session.replace(state);
        HudView hud = mapper.hud(state);
        events.publish(new GameEvent.GameStarted(hud));
        return Result.ok(hud);
    }
}
