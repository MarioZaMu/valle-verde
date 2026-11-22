package edu.ucc.farmsim.domain.game;

import edu.ucc.farmsim.domain.economy.InventorySnapshot;
import edu.ucc.farmsim.domain.economy.MarketSnapshot;
import edu.ucc.farmsim.domain.farm.FarmMapSnapshot;
import edu.ucc.farmsim.domain.progress.Achievement;
import edu.ucc.farmsim.domain.time.GameDate;
import edu.ucc.farmsim.domain.time.Weather;
import java.util.Set;

/**
 * Pattern: Memento — Role: Memento (immutable snapshot of the game state).
 *
 * <p>It exposes plain data only, so the originator is the single place that knows how to rebuild a state
 * from it. It holds everything that changes during play: crops, buildings and animals, the market, the daily
 * ledger and the achievements.
 *
 * <p>Stories: HU-01, HU-10, HU-15, HU-16, HU-17, HU-18.
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
 * @param achievements the achievements unlocked
 */
public record GameMemento(GameDate date, int money, int energy, InventorySnapshot inventory,
                          FarmMapSnapshot farm, Weather today, Weather forecast, long randomState,
                          MarketSnapshot market, LedgerSnapshot ledger, Set<Achievement> achievements) { }
