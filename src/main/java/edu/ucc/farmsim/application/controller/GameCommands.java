package edu.ucc.farmsim.application.controller;

import edu.ucc.farmsim.application.dto.HudView;
import edu.ucc.farmsim.application.dto.MapView;
import edu.ucc.farmsim.application.dto.PlotView;
import edu.ucc.farmsim.application.result.Result;

/**
 * Everything the player interface can ask the game to do. It is the only thing the UI knows about the game
 * (AR-3): {@link GameController} answers in the same process and {@code RemoteGameClient} sends each order to
 * the REST server, so the screens work the same on both and never learn whether there is a network.
 *
 * <p>Every operation answers with a {@link Result}; none of them throws (R-03, RNF-07).
 *
 * <p>Stories: HU-01, HU-02, HU-04, HU-06, HU-07, HU-10, HU-28, HU-33, HU-35.
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
}
