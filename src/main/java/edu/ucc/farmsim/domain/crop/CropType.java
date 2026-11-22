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
    CARROT(4, Season.SPRING),
    POTATO(6, Season.SPRING),
    TOMATO(7, Season.SUMMER),
    CORN(8, Season.SUMMER, Season.AUTUMN),
    PUMPKIN(10, Season.AUTUMN);

    private final int daysToMature;
    private final Set<Season> validSeasons;

    CropType(int daysToMature, Season first, Season... more) {
        this.daysToMature = daysToMature;
        this.validSeasons = EnumSet.of(first, more);
    }

    /** Days a crop must be watered to become mature (T in RN-08). */
    public int daysToMature() {
        return daysToMature;
    }

    /** Seasons in which this crop can be sown and stays alive. */
    public Set<Season> validSeasons() {
        return EnumSet.copyOf(validSeasons);
    }

    /** Day of growth at which the crop leaves the Sprout stage: {@code ceil(T / 2)} (RN-08). */
    public int halfwayDays() {
        return (daysToMature + 1) / 2;
    }
}
