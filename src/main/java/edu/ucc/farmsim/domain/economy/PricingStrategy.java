package edu.ucc.farmsim.domain.economy;

import edu.ucc.farmsim.domain.time.Season;

/**
 * Pattern: Strategy — Role: Strategy interface (how the price of one unit is worked out)
 *
 * <p>Crops depend on the season, animal products do not; the market picks the strategy of each product (RN-17).
 *
 * <p>Stories: HU-15.
 */
public interface PricingStrategy {

    /**
     * The price in m of one unit.
     *
     * @param item   the product
     * @param season the current season
     * @param demand the demand of the product, in percent
     */
    int unitPrice(ItemType item, Season season, int demand);

    /** {@code max(1, round_half_up(base × seasonFactor × demand / 10 000))}, with every factor in percent. */
    static int priceOf(int base, int seasonFactor, int demand) {
        long scaled = (long) base * seasonFactor * demand;
        return (int) Math.max(1, (scaled + 5_000) / 10_000);
    }
}
