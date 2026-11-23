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
 * Walking with the avatar (RN-36). A step costs no energy and does not move the calendar. When the way is
 * blocked the step is rejected, but the avatar still turns to look there, as the rule asks.
 *
 * <p>Stories: HU-45.
 */
@Service
public final class AvatarService {

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
    public AvatarService(GameSession session, ViewMapper mapper, EventBus events) {
        this.session = session;
        this.mapper = mapper;
        this.events = events;
    }

    /**
     * One step of the avatar.
     *
     * @param directionId {@code NORTH}, {@code SOUTH}, {@code EAST} or {@code WEST}
     * @return where it stands now; {@code MOVIMIENTO_BLOQUEADO} if the plot there cannot be walked on
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
