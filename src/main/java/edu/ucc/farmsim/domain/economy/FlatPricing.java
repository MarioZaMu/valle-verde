package edu.ucc.farmsim.domain.economy;

import edu.ucc.farmsim.domain.config.GameRules;
import edu.ucc.farmsim.domain.time.Season;

/**
 * Pattern: Strategy — Role: ConcreteStrategy (animal products: the season does not matter)
 *
 * <p>Stories: HU-15.
 */
public final class FlatPricing implements PricingStrategy {

    @Override
    public int unitPrice(ItemType item, Season season, int demand) {
        int base = switch (item) {
            case EGG -> GameRules.BASE_PRICE_EGG;
            case MILK -> GameRules.BASE_PRICE_MILK;
            default -> throw new IllegalArgumentException("Not an animal product: " + item);
        };
        return PricingStrategy.priceOf(base, GameRules.SEASON_FACTOR_FLAT, demand);
    }
}
