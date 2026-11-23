package edu.ucc.farmsim.domain.game;

import edu.ucc.farmsim.domain.economy.InventorySnapshot;
import edu.ucc.farmsim.domain.economy.MarketSnapshot;
import edu.ucc.farmsim.domain.farm.AvatarSnapshot;
import edu.ucc.farmsim.domain.farm.FarmMapSnapshot;
import edu.ucc.farmsim.domain.npc.Npc;
import edu.ucc.farmsim.domain.progress.ProgressSnapshot;
import edu.ucc.farmsim.domain.progress.TutorialStep;
import edu.ucc.farmsim.domain.time.GameDate;
import edu.ucc.farmsim.domain.time.Weather;
import java.util.List;

/**
 * Pattern: Memento — Role: Memento (immutable snapshot of the game state).
 *
 * <p>It exposes plain data only, so the originator is the single place that knows how to rebuild a state
 * from it. It holds everything that changes during play: crops, buildings and animals, the market, the daily
 * ledger, the achievements, the avatar, the errands done and the step of the start tutorial.
 *
 * <p>Stories: HU-01, HU-10, HU-15, HU-16, HU-17, HU-18, HU-26, HU-45, HU-47, HU-49.
 *
 * @param date        calendar date
 * @param money       coins
 * @param energy      energy available
 * @param inventory   inventory content
 * @param farm        crops on the map
 * @param today       weather of today
 * @param forecast    forecast for tomorrow
 * @param randomState internal state of the random generator
 * @param market       demand and previous prices
 * @param ledger       what was earned and spent today
 * @param progress     the achievements unlocked and their counters
 * @param avatar       where the avatar stands and looks; {@code null} in a save of version 1, which had no avatar
 * @param errandsDone  the people whose errand is done; {@code null} in a save written before the errands
 * @param tutorial     the step of the start tutorial; {@code null} in a save written before it (it loads finished)
 */
public record GameMemento(GameDate date, int money, int energy, InventorySnapshot inventory,
                          FarmMapSnapshot farm, Weather today, Weather forecast, long randomState,
                          MarketSnapshot market, LedgerSnapshot ledger, ProgressSnapshot progress,
                          AvatarSnapshot avatar, List<Npc> errandsDone, TutorialStep tutorial) {

    /** A snapshot with the avatar and the errands but without tutorial. */
    public GameMemento(GameDate date, int money, int energy, InventorySnapshot inventory, FarmMapSnapshot farm,
                       Weather today, Weather forecast, long randomState, MarketSnapshot market,
                       LedgerSnapshot ledger, ProgressSnapshot progress, AvatarSnapshot avatar, List<Npc> errandsDone) {
        this(date, money, energy, inventory, farm, today, forecast, randomState, market, ledger, progress, avatar,
                errandsDone, null);
    }

    /** A snapshot with the avatar but without errands done. */
    public GameMemento(GameDate date, int money, int energy, InventorySnapshot inventory, FarmMapSnapshot farm,
                       Weather today, Weather forecast, long randomState, MarketSnapshot market,
                       LedgerSnapshot ledger, ProgressSnapshot progress, AvatarSnapshot avatar) {
        this(date, money, energy, inventory, farm, today, forecast, randomState, market, ledger, progress, avatar,
                (List<Npc>) null);
    }

    /** A snapshot without avatar, as a save of version 1 holds it (the avatar goes back to its start). */
    public GameMemento(GameDate date, int money, int energy, InventorySnapshot inventory, FarmMapSnapshot farm,
                       Weather today, Weather forecast, long randomState, MarketSnapshot market,
                       LedgerSnapshot ledger, ProgressSnapshot progress) {
        this(date, money, energy, inventory, farm, today, forecast, randomState, market, ledger, progress, null);
    }
}
