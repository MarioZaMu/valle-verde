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
import edu.ucc.farmsim.domain.farm.Terrain;
import edu.ucc.farmsim.domain.game.Energy;
import edu.ucc.farmsim.domain.game.GameState;
import edu.ucc.farmsim.domain.time.Season;
import edu.ucc.farmsim.domain.time.Weather;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * The crop use cases: sow, water, harvest and clear (RN-06 to RN-10). Each operation checks every rule
 * first and only then changes the game, so a rejection never leaves anything half done.
 *
 * <p>Stories: HU-01, HU-02, HU-04, HU-06, HU-07, HU-26, HU-35.
 */
@Service
public final class FarmService {

    private static final String SEASON_SEPARATOR = " y ";

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
     * @param achievements evaluates the achievements after each harvest
     */
    public FarmService(GameSession session, ViewMapper mapper, EventBus events, AchievementService achievements) {
        this.session = session;
        this.mapper = mapper;
        this.events = events;
        this.achievements = achievements;
    }

    /**
     * Sows a seed from the inventory in a free soil plot of a valid season (RN-06).
     *
     * @param col    column of the plot
     * @param row    row of the plot
     * @param seedId item id of the seed, for example {@code CARROT_SEED}
     */
    public Result<PlotView> plant(int col, int row, String seedId) {
        if (!session.hasGame()) {
            return Result.failWith(ErrorCode.SIN_PARTIDA);
        }
        GameState state = session.current();
        FarmMap farm = state.farm();
        if (!farm.contains(col, row) || farm.plotAt(col, row).terrain() != Terrain.SOIL) {
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
        if (!type.validSeasons().contains(season)) {
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
     * Waters the crop of a plot (RN-07): 1 energy, or 2 when the weather of the day is Drought.
     *
     * @param col column of the plot
     * @param row row of the plot
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
        int cost = state.weather().today() == Weather.DROUGHT
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
     * The harvest tool (RN-10, RN-34): a mature crop is harvested into the inventory (2 energy and room for
     * one unit); a withered crop is cleared (1 energy, nothing collected). If any rule fails, nothing changes;
     * in particular a full inventory never costs the crop or the energy.
     *
     * @param col column of the plot
     * @param row row of the plot
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
     * Describes a plot without changing anything (HU-07): terrain, crop, stage, watered and days left.
     *
     * @param col column of the plot
     * @param row row of the plot
     */
    public Result<PlotView> inspect(int col, int row) {
        if (!session.hasGame()) {
            return Result.failWith(ErrorCode.SIN_PARTIDA);
        }
        return Result.ok(mapper.plot(session.current(), col, row));
    }

    /** The whole farm map, for drawing it. A pure query. */
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
