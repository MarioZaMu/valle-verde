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
 * Los casos de uso de construcción (RN-20, RN-21): construir un edificio sobre pasto libre, y listar lo construido y lo
 * que se puede construir. Las reglas se comprueban en el orden de RN-21 y un rechazo nunca cambia la partida.
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
     * Crea el servicio.
     *
     * @param session la partida en curso
     * @param mapper  construye las vistas que dibuja la UI
     * @param events  donde se publican los eventos
     * @param achievements evalúa los logros después de cada construcción
     */
    public BuildingService(GameSession session, ViewMapper mapper, EventBus events, AchievementService achievements) {
        this.session = session;
        this.mapper = mapper;
        this.events = events;
        this.achievements = achievements;
    }

    /**
     * Construye un edificio con su parcela superior izquierda en el lugar dado (RN-21): el área debe ser pasto libre
     * dentro del mapa (el avatar y las personas del valle no cuentan como libres: un edificio nunca cae sobre ellos,
     * RN-36, RN-37), el edificio debe estar desbloqueado, bajo su límite y al alcance del dinero. La construcción es
     * instantánea y no cuesta energía.
     *
     * @param typeId id del edificio, por ejemplo {@code COOP}
     * @param col    columna de la parcela superior izquierda
     * @param row    fila de la parcela superior izquierda
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

    /** Los edificios de la granja y el catálogo de lo que se puede construir. Una consulta pura. */
    public Result<BuildingsView> buildings() {
        if (!session.hasGame()) {
            return Result.failWith(ErrorCode.SIN_PARTIDA);
        }
        return Result.ok(mapper.buildings(session.current()));
    }
}
