package edu.ucc.farmsim.domain.economy;

import java.util.Map;

/**
 * An immutable copy of the market for the memento: the demand of each product and its price of yesterday
 * (0 when there is none yet).
 *
 * <p>Stories: HU-15, HU-16.
 *
 * @param demand        demand of each sellable product, in percent
 * @param previousPrice price of the next unit before the last recovery, or 0
 */
public record MarketSnapshot(Map<ItemType, Integer> demand, Map<ItemType, Integer> previousPrice) {

    /** Makes the maps unmodifiable copies. */
    public MarketSnapshot {
        demand = Map.copyOf(demand);
        previousPrice = Map.copyOf(previousPrice);
    }
}
