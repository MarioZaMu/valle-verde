package edu.ucc.farmsim.domain.game;

import edu.ucc.farmsim.domain.economy.InventorySnapshot;
import edu.ucc.farmsim.domain.time.GameDate;
import edu.ucc.farmsim.domain.time.Weather;

/**
 * Pattern: Memento — Role: Memento (immutable snapshot of the game state).
 *
 * <p>It exposes plain data only, so the originator is the single place that knows how to rebuild a state
 * from it. Later phases add the parts they bring (crops, buildings, market, achievements).
 *
 * <p>Stories: HU-10.
 *
 * @param date        calendar date
 * @param money       coins
 * @param energy      energy available
 * @param inventory   inventory content
 * @param today       weather of today
 * @param forecast    forecast for tomorrow
 * @param randomState internal state of the random generator
 */
public record GameMemento(GameDate date, int money, int energy, InventorySnapshot inventory,
                          Weather today, Weather forecast, long randomState) { }
