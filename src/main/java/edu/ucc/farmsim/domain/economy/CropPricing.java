package edu.ucc.farmsim.domain.economy;

import edu.ucc.farmsim.domain.config.GameRules;
import edu.ucc.farmsim.domain.time.Season;

/**
 * Pattern: Strategy — Role: ConcreteStrategy (crops: 90 % in season, 120 % out of season)
 *
 * <p>Stories: HU-15.
 */
public final class CropPricing implements PricingStrategy {

    @Override
    public int unitPrice(ItemType item, Season season, int demand) {
        int factor = item.cropType().validSeasons().contains(season)
                ? GameRules.SEASON_FACTOR_IN_SEASON
                : GameRules.SEASON_FACTOR_OUT_OF_SEASON;
        return PricingStrategy.priceOf(item.cropType().sellBasePrice(), factor, demand);
    }
}
