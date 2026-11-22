package edu.ucc.farmsim.application.controller;

import edu.ucc.farmsim.application.dto.AchievementView;
import edu.ucc.farmsim.application.dto.BuildingActionView;
import edu.ucc.farmsim.application.dto.BuildingView;
import edu.ucc.farmsim.application.dto.BuildingsView;
import edu.ucc.farmsim.application.dto.HudView;
import edu.ucc.farmsim.application.dto.InventoryView;
import edu.ucc.farmsim.application.dto.MapView;
import edu.ucc.farmsim.application.dto.MarketView;
import edu.ucc.farmsim.application.dto.PlotView;
import edu.ucc.farmsim.application.dto.SaveSlotView;
import edu.ucc.farmsim.application.result.ErrorCode;
import edu.ucc.farmsim.application.result.Result;
import edu.ucc.farmsim.application.service.AchievementService;
import edu.ucc.farmsim.application.service.AnimalService;
import edu.ucc.farmsim.application.service.BuildingService;
import edu.ucc.farmsim.application.service.DayService;
import edu.ucc.farmsim.application.service.FarmService;
import edu.ucc.farmsim.application.service.GameSession;
import edu.ucc.farmsim.application.service.MarketService;
import edu.ucc.farmsim.application.service.ProgressService;
import edu.ucc.farmsim.application.service.SaveGameService;
import edu.ucc.farmsim.application.service.ViewMapper;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.stereotype.Component;

/**
 * Pattern: Controller — Role: GRASP controller (the single entry point the UI calls).
 *
 * <p>It only orchestrates: it asks the services to do the work and always answers with a {@link Result}.
 * No exception ever reaches the UI or the API: anything unexpected becomes {@code ERROR_INTERNO} (R-03,
 * RNF-07), and it never touches repositories (AR-4). It is not the REST controller: the {@code api} package
 * translates HTTP into calls to this class.
 *
 * <p>Stories: HU-01, HU-02, HU-04, HU-06, HU-07, HU-10, HU-14, HU-15, HU-16, HU-17, HU-18, HU-19, HU-20, HU-22, HU-23, HU-26, HU-28, HU-29, HU-30, HU-31, HU-32, HU-33, HU-35, HU-37, HU-38.
 */
@Component
public final class GameController implements GameCommands {

    private static final Logger LOG = Logger.getLogger(GameController.class.getName());

    private final GameSession session;
    private final ViewMapper mapper;
    private final ProgressService progress;
    private final DayService day;
    private final SaveGameService saves;
    private final FarmService farm;
    private final MarketService market;
    private final BuildingService buildings;
    private final AnimalService animals;
    private final AchievementService achievements;

    /**
     * Creates the controller.
     *
     * @param session  the game in progress
     * @param mapper   builds the views the UI draws
     * @param progress starts games
     * @param day      ends days
     * @param saves    knows about the save slots
     * @param farm     crop use cases
     * @param market   buying, selling and the market and inventory views
     * @param buildings construction
     * @param animals  animal care
     * @param achievements the achievements and their progress
     */
    public GameController(GameSession session, ViewMapper mapper, ProgressService progress, DayService day,
                          SaveGameService saves, FarmService farm, MarketService market,
                          BuildingService buildings, AnimalService animals,
                          AchievementService achievements) {
        this.session = session;
        this.mapper = mapper;
        this.progress = progress;
        this.day = day;
        this.saves = saves;
        this.farm = farm;
        this.market = market;
        this.buildings = buildings;
        this.animals = animals;
        this.achievements = achievements;
    }

    /** True if a game is in progress (the UI asks for confirmation before discarding it). */
    @Override
    public boolean hasGameInProgress() {
        try {
            return session.hasGame();
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "hasGameInProgress failed", e);
            return false;
        }
    }

    /**
     * Starts a new game with the initial state of RN-30, replacing any game in progress.
     *
     * @param seed seed of the random generator
     */
    @Override
    public Result<HudView> newGame(long seed) {
        return guard(() -> progress.newGame(seed));
    }

    /** Ends the day: effects are resolved in the order of RN-03 and the new top bar is returned. */
    @Override
    public Result<HudView> endDay() {
        return guard(day::endDay);
    }

    /**
     * Sows a seed from the inventory in a plot (RN-06).
     *
     * @param col    column of the plot
     * @param row    row of the plot
     * @param seedId item id of the seed, for example {@code CARROT_SEED}
     */
    @Override
    public Result<PlotView> plant(int col, int row, String seedId) {
        return guard(() -> farm.plant(col, row, seedId));
    }

    /**
     * Waters the crop of a plot (RN-07).
     *
     * @param col column of the plot
     * @param row row of the plot
     */
    @Override
    public Result<PlotView> water(int col, int row) {
        return guard(() -> farm.water(col, row));
    }

    /**
     * The harvest tool (RN-10, RN-34): harvests a mature crop, or clears a withered one.
     *
     * @param col column of the plot
     * @param row row of the plot
     */
    @Override
    public Result<PlotView> harvest(int col, int row) {
        return guard(() -> farm.harvest(col, row));
    }

    /**
     * Describes a plot for the tooltip (HU-07). A pure query: it never changes the game.
     *
     * @param col column of the plot
     * @param row row of the plot
     */
    @Override
    public Result<PlotView> inspect(int col, int row) {
        return guard(() -> farm.inspect(col, row));
    }

    /** The whole farm map, for drawing it. A pure query. */
    @Override
    public Result<MapView> map() {
        return guard(farm::map);
    }

    /** True if at least one of the save slots holds a game. */
    @Override
    public Result<Boolean> hasSavedGames() {
        return guard(saves::hasAnySave);
    }

    /** The top bar of the game in progress. */
    @Override
    public Result<HudView> hud() {
        return guard(() -> {
            if (!session.hasGame()) {
                return Result.failWith(ErrorCode.SIN_PARTIDA);
            }
            return Result.ok(mapper.hud(session.current()));
        });
    }

    @Override
    public Result<Integer> buy(String itemId, int quantity) {
        return guard(() -> market.buy(itemId, quantity));
    }

    @Override
    public Result<Integer> sell(String itemId, int quantity) {
        return guard(() -> market.sell(itemId, quantity));
    }

    @Override
    public Result<Integer> quote(String itemId, int quantity) {
        return guard(() -> market.quote(itemId, quantity));
    }

    @Override
    public Result<MarketView> market() {
        return guard(market::market);
    }

    @Override
    public Result<InventoryView> inventory() {
        return guard(market::inventory);
    }

    @Override
    public Result<BuildingsView> buildings() {
        return guard(buildings::buildings);
    }

    @Override
    public Result<BuildingView> build(String buildingId, int col, int row) {
        return guard(() -> buildings.build(buildingId, col, row));
    }

    @Override
    public Result<BuildingView> buyAnimal(String animalId) {
        return guard(() -> animals.buyAnimal(animalId));
    }

    @Override
    public Result<BuildingActionView> feed(int buildingId) {
        return guard(() -> animals.feed(buildingId));
    }

    @Override
    public Result<BuildingActionView> collect(int buildingId) {
        return guard(() -> animals.collect(buildingId));
    }

    @Override
    public Result<BuildingActionView> cure(int buildingId, int animalIndex) {
        return guard(() -> animals.cure(buildingId, animalIndex));
    }

    @Override
    public Result<List<AchievementView>> achievements() {
        return guard(achievements::achievements);
    }

    @Override
    public Result<List<SaveSlotView>> saveSlots() {
        return guard(saves::slots);
    }

    @Override
    public Result<SaveSlotView> save(int slot) {
        return guard(() -> saves.save(slot));
    }

    @Override
    public Result<HudView> load(int slot) {
        return guard(() -> saves.load(slot));
    }

    private <T> Result<T> guard(Supplier<Result<T>> operation) {
        try {
            return operation.get();
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Unexpected error", e);
            return Result.failWith(ErrorCode.ERROR_INTERNO);
        }
    }
}
