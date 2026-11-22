package edu.ucc.farmsim.application.controller;

import edu.ucc.farmsim.application.dto.BuildingActionView;
import edu.ucc.farmsim.application.dto.BuildingView;
import edu.ucc.farmsim.application.dto.BuildingsView;
import edu.ucc.farmsim.application.dto.HudView;
import edu.ucc.farmsim.application.dto.InventoryView;
import edu.ucc.farmsim.application.dto.MapView;
import edu.ucc.farmsim.application.dto.MarketView;
import edu.ucc.farmsim.application.dto.PlotView;
import edu.ucc.farmsim.application.result.Result;

/**
 * Everything the player interface can ask the game to do. It is the only thing the UI knows about the game
 * (AR-3): {@link GameController} answers in the same process and {@code RemoteGameClient} sends each order to
 * the REST server, so the screens work the same on both and never learn whether there is a network.
 *
 * <p>Every operation answers with a {@link Result}; none of them throws (R-03, RNF-07).
 *
 * <p>Stories: HU-01, HU-02, HU-04, HU-06, HU-07, HU-10, HU-14, HU-15, HU-16, HU-17, HU-18, HU-19, HU-20, HU-22, HU-23, HU-28, HU-33, HU-35, HU-37, HU-38.
 */
public interface GameCommands {

    /** True if a game is in progress (the UI asks for confirmation before discarding it). */
    boolean hasGameInProgress();

    /**
     * Starts a new game with the initial state of RN-30, replacing any game in progress.
     *
     * @param seed seed of the random generator
     */
    Result<HudView> newGame(long seed);

    /** The top bar of the game in progress. */
    Result<HudView> hud();

    /** Ends the day: effects are resolved in the order of RN-03 and the new top bar is returned. */
    Result<HudView> endDay();

    /**
     * Sows a seed from the inventory in a plot (RN-06).
     *
     * @param col    column of the plot
     * @param row    row of the plot
     * @param seedId item id of the seed, for example {@code CARROT_SEED}
     */
    Result<PlotView> plant(int col, int row, String seedId);

    /**
     * Waters the crop of a plot (RN-07).
     *
     * @param col column of the plot
     * @param row row of the plot
     */
    Result<PlotView> water(int col, int row);

    /**
     * The harvest tool (RN-10, RN-34): harvests a mature crop, or clears a withered one.
     *
     * @param col column of the plot
     * @param row row of the plot
     */
    Result<PlotView> harvest(int col, int row);

    /**
     * Describes a plot for the tooltip (HU-07). A pure query: it never changes the game.
     *
     * @param col column of the plot
     * @param row row of the plot
     */
    Result<PlotView> inspect(int col, int row);

    /** The whole farm map, for drawing it. A pure query. */
    Result<MapView> map();

    /** True if at least one of the save slots holds a game. */
    Result<Boolean> hasSavedGames();

    /**
     * Buys units of a seed, hay or medicine at the fixed price (RN-16).
     *
     * @param itemId   item id, for example {@code CORN_SEED}
     * @param quantity 1 to 99
     * @return the total paid in m
     */
    Result<Integer> buy(String itemId, int quantity);

    /**
     * Sells units of a crop, egg or milk at the dynamic price (RN-17, RN-18).
     *
     * @param itemId   item id, for example {@code CARROT}
     * @param quantity 1 up to the units owned
     * @return the total received in m
     */
    Result<Integer> sell(String itemId, int quantity);

    /** What selling the quantity would pay now. A pure query. */
    Result<Integer> quote(String itemId, int quantity);

    /** Prices, trends and the purchase catalogue. A pure query. */
    Result<MarketView> market();

    /** The inventory as a grid. A pure query. */
    Result<InventoryView> inventory();

    /** The buildings of the farm and the catalogue of what can be built. A pure query. */
    Result<BuildingsView> buildings();

    /**
     * Builds a building with its top-left plot at the given place (RN-20, RN-21).
     *
     * @param buildingId building id, for example {@code COOP}
     * @param col        column of the top-left plot
     * @param row        row of the top-left plot
     */
    Result<BuildingView> build(String buildingId, int col, int row);

    /**
     * Buys an animal and houses it in the first compatible building with room (RN-23).
     *
     * @param animalId animal id, for example {@code HEN}
     * @return the building where it lives now
     */
    Result<BuildingView> buyAnimal(String animalId);

    /**
     * Feeds the animals of a building that can eat and have not eaten today (RN-24).
     *
     * @param buildingId id of the building
     */
    Result<BuildingActionView> feed(int buildingId);

    /**
     * Collects the products waiting in a building (RN-26).
     *
     * @param buildingId id of the building
     */
    Result<BuildingActionView> collect(int buildingId);

    /**
     * Cures a sick animal with medicine (RN-27).
     *
     * @param buildingId  id of the building
     * @param animalIndex position of the animal inside the building
     */
    Result<BuildingActionView> cure(int buildingId, int animalIndex);
}
