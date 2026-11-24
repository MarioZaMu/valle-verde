package edu.ucc.farmsim.application.service;

import edu.ucc.farmsim.application.dto.MapView;
import edu.ucc.farmsim.application.dto.PlotView;
import edu.ucc.farmsim.application.event.EventBus;
import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Names;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.domain.config.GameRules;
import edu.ucc.farmsim.domain.crop.Crop;
import edu.ucc.farmsim.domain.crop.CropFactory;
import edu.ucc.farmsim.domain.crop.CropType;
import edu.ucc.farmsim.domain.economy.ItemType;
import edu.ucc.farmsim.domain.farm.FarmMap;
import edu.ucc.farmsim.domain.farm.Plot;
import edu.ucc.farmsim.domain.game.Energy;
import edu.ucc.farmsim.domain.game.GameState;
import edu.ucc.farmsim.domain.time.Season;
import edu.ucc.farmsim.domain.time.Weather;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Los casos de uso de cultivos: sembrar, regar, cosechar y limpiar (RN-06 a RN-10). Cada operación comprueba primero
 * todas las reglas y solo después cambia la partida, de modo que un rechazo nunca deja nada a medias.
 *
 * <p>Stories: HU-01, HU-02, HU-04, HU-06, HU-07, HU-25, HU-26, HU-35.
 */
@Service
public final class FarmService {

    private static final String SEASON_SEPARATOR = " y ";

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
     * @param achievements evalúa los logros después de cada cosecha
     */
    public FarmService(GameSession session, ViewMapper mapper, EventBus events, AchievementService achievements) {
        this.session = session;
        this.mapper = mapper;
        this.events = events;
        this.achievements = achievements;
    }

    /**
     * Siembra una semilla del inventario en una parcela de tierra libre de una estación válida (RN-06).
     *
     * @param col    columna de la parcela
     * @param row    fila de la parcela
     * @param seedId id del objeto semilla, por ejemplo {@code CARROT_SEED}
     */
    public Result<PlotView> plant(int col, int row, String seedId) {
        if (!session.hasGame()) {
            return Result.failWith(ErrorCode.SIN_PARTIDA);
        }
        GameState state = session.current();
        FarmMap farm = state.farm();
        if (!farm.contains(col, row) || !farm.plotAt(col, row).canBeSown()) {
            return Result.failWith(ErrorCode.PARCELA_NO_CULTIVABLE);
        }
        Plot plot = farm.plotAt(col, row);
        if (plot.hasCrop()) {
            return Result.failWith(ErrorCode.PARCELA_OCUPADA);
        }
        ItemType seed = ItemType.valueOf(seedId);
        if (seed.kind() != ItemType.Kind.SEED) {
            throw new IllegalArgumentException("Not a seed: " + seedId);
        }
        CropType type = seed.cropType();
        Season season = state.date().season();
        if (!plot.isSheltered() && !type.validSeasons().contains(season)) {
            String valid = type.validSeasons().stream().map(s -> Names.season(s.name()))
                    .collect(Collectors.joining(SEASON_SEPARATOR));
            return Result.failWith(ErrorCode.FUERA_DE_ESTACION,
                    Names.crop(type.name()), Names.season(season.name()), valid);
        }
        if (state.inventory().count(seed) < GameRules.SEEDS_PER_SOWING) {
            return Result.failWith(ErrorCode.SIN_SEMILLAS, Names.crop(type.name()));
        }
        Energy energy = state.energy();
        if (!energy.canSpend(GameRules.ENERGY_COST_SOW)) {
            return Result.failWith(ErrorCode.SIN_ENERGIA, GameRules.ENERGY_COST_SOW, energy.current());
        }
        state.inventory().remove(seed, GameRules.SEEDS_PER_SOWING);
        energy.trySpend(GameRules.ENERGY_COST_SOW);
        plot.plant(CropFactory.forType(type).createCrop());
        publishChange(state);
        events.publish(new GameEvent.InventoryChanged());
        return Result.ok(mapper.plot(state, col, row));
    }

    /**
     * Riega el cultivo de una parcela (RN-07): 1 de energía, o 2 cuando el clima del día es sequía.
     *
     * @param col columna de la parcela
     * @param row fila de la parcela
     */
    public Result<PlotView> water(int col, int row) {
        if (!session.hasGame()) {
            return Result.failWith(ErrorCode.SIN_PARTIDA);
        }
        GameState state = session.current();
        Crop crop = cropAt(state, col, row);
        if (crop == null) {
            return Result.failWith(ErrorCode.SIN_CULTIVO);
        }
        if (!crop.isAlive()) {
            return Result.failWith(ErrorCode.CULTIVO_MARCHITO);
        }
        if (crop.isWatered()) {
            return Result.failWith(ErrorCode.YA_REGADO);
        }
        boolean dry = state.weather().today() == Weather.DROUGHT && !state.farm().plotAt(col, row).isSheltered();
        int cost = dry
                ? GameRules.ENERGY_COST_WATER_IN_DROUGHT
                : GameRules.ENERGY_COST_WATER;
        Energy energy = state.energy();
        if (!energy.canSpend(cost)) {
            return Result.failWith(ErrorCode.SIN_ENERGIA, cost, energy.current());
        }
        crop.water();
        energy.trySpend(cost);
        publishChange(state);
        return Result.ok(mapper.plot(state, col, row));
    }

    /**
     * La herramienta de cosechar (RN-10, RN-34): un cultivo maduro se cosecha hacia el inventario (2 de energía y lugar
     * para una unidad); un cultivo marchito se limpia (1 de energía, no se recoge nada). Si alguna regla falla, no
     * cambia nada; en particular un inventario lleno nunca cuesta el cultivo ni la energía.
     *
     * @param col columna de la parcela
     * @param row fila de la parcela
     */
    public Result<PlotView> harvest(int col, int row) {
        if (!session.hasGame()) {
            return Result.failWith(ErrorCode.SIN_PARTIDA);
        }
        GameState state = session.current();
        Crop crop = cropAt(state, col, row);
        if (crop == null) {
            return Result.failWith(ErrorCode.SIN_CULTIVO);
        }
        if (!crop.isAlive()) {
            return clear(state, col, row);
        }
        if (!crop.isMature()) {
            return Result.failWith(ErrorCode.NO_MADURO, crop.daysLeft());
        }
        Energy energy = state.energy();
        if (!energy.canSpend(GameRules.ENERGY_COST_HARVEST)) {
            return Result.failWith(ErrorCode.SIN_ENERGIA, GameRules.ENERGY_COST_HARVEST, energy.current());
        }
        if (!state.inventory().add(ItemType.produceOf(crop.type()), 1)) {
            return Result.failWith(ErrorCode.INVENTARIO_LLENO);
        }
        energy.trySpend(GameRules.ENERGY_COST_HARVEST);
        state.farm().plotAt(col, row).removeCrop();
        state.progress().recordHarvest();
        publishChange(state);
        events.publish(new GameEvent.InventoryChanged());
        achievements.check(state);
        return Result.ok(mapper.plot(state, col, row));
    }

    /**
     * Describe una parcela sin cambiar nada (HU-07): terreno, cultivo, etapa, regado y días restantes.
     *
     * @param col columna de la parcela
     * @param row fila de la parcela
     */
    public Result<PlotView> inspect(int col, int row) {
        if (!session.hasGame()) {
            return Result.failWith(ErrorCode.SIN_PARTIDA);
        }
        return Result.ok(mapper.plot(session.current(), col, row));
    }

    /** Todo el mapa de la granja, para dibujarlo. Una consulta pura. */
    public Result<MapView> map() {
        if (!session.hasGame()) {
            return Result.failWith(ErrorCode.SIN_PARTIDA);
        }
        return Result.ok(mapper.map(session.current()));
    }

    private Result<PlotView> clear(GameState state, int col, int row) {
        Energy energy = state.energy();
        if (!energy.canSpend(GameRules.ENERGY_COST_CLEAN)) {
            return Result.failWith(ErrorCode.SIN_ENERGIA, GameRules.ENERGY_COST_CLEAN, energy.current());
        }
        energy.trySpend(GameRules.ENERGY_COST_CLEAN);
        state.farm().plotAt(col, row).removeCrop();
        publishChange(state);
        return Result.ok(mapper.plot(state, col, row));
    }

    private static Crop cropAt(GameState state, int col, int row) {
        FarmMap farm = state.farm();
        return farm.contains(col, row) ? farm.plotAt(col, row).crop() : null;
    }

    private void publishChange(GameState state) {
        events.publish(new GameEvent.MapChanged());
        events.publish(new GameEvent.EnergyChanged(state.energy().current(), state.energy().max()));
    }
}
