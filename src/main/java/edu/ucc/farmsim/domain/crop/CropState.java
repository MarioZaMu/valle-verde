package edu.ucc.farmsim.domain.crop;

/**
 * Pattern: State — Role: State interface (what a crop can do at each growth stage).
 *
 * <p>The states are stateless and shared; a {@link Crop} holds the current one and changes it as it grows,
 * so no code needs a chain of {@code if}s on the stage.
 *
 * <p>Stories: HU-01.
 */
public interface CropState {

    /** The stage this state represents. */
    CropStage stage();

    /** True unless the crop is withered. */
    boolean isAlive();

    /** True only when the crop is mature and can be harvested. */
    boolean canHarvest();

    /**
     * The state that corresponds to a crop's progress (RN-08): Seed at 0 days; Sprout until half of the days
     * to mature; Growing until all of them; Mature afterwards; Withered if it died.
     */
    static CropState forProgress(CropType type, int daysGrown, boolean withered) {
        if (withered) {
            return WitheredState.INSTANCE;
        }
        if (daysGrown >= type.daysToMature()) {
            return MatureState.INSTANCE;
        }
        if (daysGrown >= type.halfwayDays()) {
            return GrowingState.INSTANCE;
        }
        if (daysGrown >= 1) {
            return SproutState.INSTANCE;
        }
        return SeedState.INSTANCE;
    }
}
