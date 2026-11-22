package edu.ucc.farmsim.application.service;

import edu.ucc.farmsim.application.dto.PlotView;
import edu.ucc.farmsim.application.event.EventBus;
import edu.ucc.farmsim.application.event.GameEvent;
import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Names;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.domain.config.GameRules;
import edu.ucc.farmsim.domain.crop.CropFactory;
import edu.ucc.farmsim.domain.crop.CropType;
import edu.ucc.farmsim.domain.economy.ItemType;
import edu.ucc.farmsim.domain.farm.FarmMap;
import edu.ucc.farmsim.domain.farm.Plot;
import edu.ucc.farmsim.domain.farm.Terrain;
import edu.ucc.farmsim.domain.game.Energy;
import edu.ucc.farmsim.domain.game.GameState;
import edu.ucc.farmsim.domain.time.Season;
import java.util.stream.Collectors;

/**
 * The crop use cases: sow, water, harvest and clear (RN-06 to RN-10). Each operation checks every rule
 * first and only then changes the game, so a rejection never leaves anything half done.
 *
 * <p>Stories: HU-01.
 */
public final class FarmService {

    private static final String SEASON_SEPARATOR = " y ";

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
    public FarmService(GameSession session, ViewMapper mapper, EventBus events) {
        this.session = session;
        this.mapper = mapper;
        this.events = events;
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
        return Result.ok(mapper.plot(state, col, row));
    }

    private void publishChange(GameState state) {
        events.publish(new GameEvent.MapChanged());
        events.publish(new GameEvent.EnergyChanged(state.energy().current(), state.energy().max()));
    }
}
