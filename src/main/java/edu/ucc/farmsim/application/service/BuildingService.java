package edu.ucc.farmsim.application.service;

import edu.ucc.farmsim.application.dto.BuildingView;
import edu.ucc.farmsim.application.dto.BuildingsView;
import edu.ucc.farmsim.application.event.EventBus;
import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Names;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.domain.building.Building;
import edu.ucc.farmsim.domain.building.BuildingType;
import edu.ucc.farmsim.domain.game.GameState;
import edu.ucc.farmsim.domain.npc.Npc;
import edu.ucc.farmsim.domain.progress.Achievement;
import org.springframework.stereotype.Service;

/**
 * The construction use cases (RN-20, RN-21): build a building on free grass, and list what is built and what
 * can be built. The rules are checked in the order of RN-21 and a rejection never changes the game.
 *
 * <p>Stories: HU-18, HU-24, HU-26, HU-45, HU-46.
 */
@Service
public final class BuildingService {

    private final GameSession session;
    private final ViewMapper mapper;
    private final EventBus events;
    private final AchievementService achievements;

    /**
     * Creates the service.
     *
     * @param session the game in progress
     * @param mapper  builds the views the UI draws
     * @param events  where events are published
     * @param achievements evaluates the achievements after each construction
     */
    public BuildingService(GameSession session, ViewMapper mapper, EventBus events, AchievementService achievements) {
        this.session = session;
        this.mapper = mapper;
        this.events = events;
        this.achievements = achievements;
    }

    /**
     * Builds a building with its top-left plot at the given place (RN-21): the area must be free grass inside
     * (the avatar and the people of the valley do not count as free: a building never lands on them, RN-36, RN-37),
     * the map, the building unlocked, under its limit and affordable. Construction is instant and costs no energy.
     *
     * @param typeId building id, for example {@code COOP}
     * @param col    column of the top-left plot
     * @param row    row of the top-left plot
     */
    public Result<BuildingView> build(String typeId, int col, int row) {
        if (!session.hasGame()) {
            return Result.failWith(ErrorCode.SIN_PARTIDA);
        }
        BuildingType type = BuildingType.valueOf(typeId);
        GameState state = session.current();
        if (!state.farm().canPlace(type, col, row)
                || state.avatar().isInside(col, row, type.width(), type.height())
                || Npc.anyInside(col, row, type.width(), type.height())) {
            return Result.failWith(ErrorCode.UBICACION_INVALIDA);
        }
        Achievement required = type.requirement();
        if (required != null && !state.progress().has(required)) {
            return Result.failWith(ErrorCode.EDIFICIO_BLOQUEADO, Names.achievement(required.name()));
        }
        if (state.farm().countBuildings(type) >= type.maxCount()) {
            return Result.failWith(ErrorCode.LIMITE_EDIFICIOS, Names.buildingPlural(type.name()));
        }
        if (type.cost() > state.money()) {
            return Result.failWith(ErrorCode.SIN_FONDOS, type.cost() - state.money());
        }
        state.spend(type.cost());
        Building building = state.farm().place(type, col, row);
        if (building.extraInventorySlots() > 0) {
            state.inventory().addSlots(building.extraInventorySlots());
            events.publish(new GameEvent.InventoryChanged());
        }
        events.publish(new GameEvent.MoneyChanged(state.money()));
        events.publish(new GameEvent.MapChanged());
        state.progress().recordBuild();
        achievements.check(state);
        return Result.ok(mapper.building(building));
    }

    /** The buildings of the farm and the catalogue of what can be built. A pure query. */
    public Result<BuildingsView> buildings() {
        if (!session.hasGame()) {
            return Result.failWith(ErrorCode.SIN_PARTIDA);
        }
        return Result.ok(mapper.buildings(session.current()));
    }
}
