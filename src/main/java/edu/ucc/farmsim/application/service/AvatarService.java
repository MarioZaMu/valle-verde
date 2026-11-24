package edu.ucc.farmsim.application.service;

import edu.ucc.farmsim.application.dto.AvatarView;
import edu.ucc.farmsim.application.event.EventBus;
import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.domain.farm.Direction;
import edu.ucc.farmsim.domain.game.GameState;
import org.springframework.stereotype.Service;

/**
 * Caminar con el avatar (RN-36). Un paso no cuesta energía ni mueve el calendario. Cuando el camino está bloqueado el
 * paso se rechaza, pero el avatar igual gira para mirar hacia allí, como pide la regla.
 *
 * <p>Stories: HU-45.
 */
@Service
public final class AvatarService {

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
    public AvatarService(GameSession session, ViewMapper mapper, EventBus events) {
        this.session = session;
        this.mapper = mapper;
        this.events = events;
    }

    /**
     * Un paso del avatar.
     *
     * @param directionId {@code NORTH}, {@code SOUTH}, {@code EAST} or {@code WEST}
     * @return dónde está ahora; {@code MOVIMIENTO_BLOQUEADO} si no se puede caminar sobre la parcela de ese lado
     */
    public Result<AvatarView> move(String directionId) {
        if (!session.hasGame()) {
            return Result.failWith(ErrorCode.SIN_PARTIDA);
        }
        Direction direction = Direction.valueOf(directionId);
        GameState state = session.current();
        if (!state.stepAvatar(direction)) {
            return Result.failWith(ErrorCode.MOVIMIENTO_BLOQUEADO);
        }
        AvatarView avatar = mapper.avatar(state);
        events.publish(new GameEvent.AvatarMoved(avatar));
        return Result.ok(avatar);
    }
}
