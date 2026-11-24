package edu.ucc.farmsim.domain.economy;

import edu.ucc.farmsim.domain.time.Season;

/**
 * Pattern: Strategy — Role: Strategy interface (how the price of one unit is worked out)
 *
 * <p>Los cultivos dependen de la estación, los productos animales no; el mercado elige la estrategia de cada producto
 * (RN-17).
 *
 * <p>Stories: HU-15.
 */
public interface PricingStrategy {

    /**
     * El precio en m de una unidad.
     *
     * @param item   el producto
     * @param season la estación actual
     * @param demand la demanda del producto, en porcentaje
     */
    int unitPrice(ItemType item, Season season, int demand);

    /** {@code max(1, round_half_up(base × seasonFactor × demand / 10 000))}, con cada factor en porcentaje. */
    static int priceOf(int base, int seasonFactor, int demand) {
        long scaled = (long) base * seasonFactor * demand;
        return (int) Math.max(1, (scaled + 5_000) / 10_000);
    }
}
