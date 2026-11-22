package edu.ucc.farmsim.domain.crop;

import edu.ucc.farmsim.domain.time.Season;
import java.util.EnumSet;
import java.util.Set;

/**
 * Catalog of crops (RN-05): days to mature and the seasons in which each can be sown. This enum is a
 * configuration table; the prices of seeds and harvests are added by the economy stories.
 *
 * <p>Stories: HU-01.
 */
public enum CropType {
    CARROT(4, 10, 30, Season.SPRING),
    POTATO(6, 20, 55, Season.SPRING),
    TOMATO(7, 25, 65, Season.SUMMER),
    CORN(8, 30, 75, Season.SUMMER, Season.AUTUMN),
    PUMPKIN(10, 40, 110, Season.AUTUMN);

    private final int daysToMature;
    private final int seedPrice;
    private final int sellBasePrice;
    private final Set<Season> validSeasons;

    CropType(int daysToMature, int seedPrice, int sellBasePrice, Season first, Season... more) {
        this.daysToMature = daysToMature;
        this.seedPrice = seedPrice;
        this.sellBasePrice = sellBasePrice;
        this.validSeasons = EnumSet.of(first, more);
    }

    /** Days a crop must be watered to become mature (T in RN-08). */
    public int daysToMature() {
        return daysToMature;
    }

    /** Seasons in which this crop can be sown and stays alive. */
    /** Price in m of one seed in the market (RN-05, RN-16). */
    public int seedPrice() {
        return seedPrice;
    }

    /** Base selling price in m of one harvested unit (RN-05, RN-17). */
    public int sellBasePrice() {
        return sellBasePrice;
    }

    public Set<Season> validSeasons() {
        return EnumSet.copyOf(validSeasons);
    }

    /** Day of growth at which the crop leaves the Sprout stage: {@code ceil(T / 2)} (RN-08). */
    public int halfwayDays() {
        return (daysToMature + 1) / 2;
    }
}
