package edu.ucc.farmsim.domain.crop;

import edu.ucc.farmsim.domain.time.Season;

/**
 * Pattern: State — Role: Context (a plant whose behaviour depends on its growth state).
 *
 * <p>Holds what RN-06 to RN-10 track for a sown plant: days grown, days without water and whether it was
 * watered today. Its stage is derived from the progress and kept as a state object.
 *
 * <p>Stories: HU-01, HU-02, HU-03, HU-05.
 */
public final class Crop {

    private final CropType type;
    private int daysGrown;
    private int daysWithoutWater;
    private boolean watered;
    private CropState state;

    /** Creates a freshly sown crop: Seed stage, no growth, not watered (RN-06). */
    public Crop(CropType type) {
        this(type, 0, 0, false, false);
    }

    private Crop(CropType type, int daysGrown, int daysWithoutWater, boolean watered, boolean withered) {
        this.type = type;
        this.daysGrown = daysGrown;
        this.daysWithoutWater = daysWithoutWater;
        this.watered = watered;
        this.state = CropState.forProgress(type, daysGrown, withered);
    }

    /** Rebuilds a crop from a snapshot (memento, tests). */
    public static Crop fromSnapshot(CropSnapshot snapshot) {
        return new Crop(snapshot.type(), snapshot.daysGrown(), snapshot.daysWithoutWater(),
                snapshot.watered(), snapshot.withered());
    }

    /** An immutable copy of this crop. */
    public CropSnapshot snapshot() {
        return new CropSnapshot(type, daysGrown, daysWithoutWater, watered, isWithered());
    }

    /** Kind of crop. */
    public CropType type() {
        return type;
    }

    /** Days the crop has grown. */
    public int daysGrown() {
        return daysGrown;
    }

    /** Consecutive days without water. */
    public int daysWithoutWater() {
        return daysWithoutWater;
    }

    /** Whether it was watered today. */
    public boolean isWatered() {
        return watered;
    }

    /** Current growth stage. */
    public CropStage stage() {
        return state.stage();
    }

    /** True unless the crop is withered. */
    public boolean isAlive() {
        return state.isAlive();
    }

    /** True when the crop can be harvested. */
    public boolean isMature() {
        return state.canHarvest();
    }

    /** True when the crop died. */
    public boolean isWithered() {
        return state.stage() == CropStage.WITHERED;
    }

    /**
     * Waters the crop for the rest of the day (RN-07).
     *
     * @return {@code true} if it was watered; {@code false} if it is withered or already watered today
     */
    public boolean water() {
        if (!isAlive() || watered) {
            return false;
        }
        watered = true;
        return true;
    }

    /**
     * Resolves the end of the day for this crop (step 2 of RN-03): the current state decides what happens, and
     * the watered mark is cleared for the next day.
     */
    public void endOfDay(int dryDaysToWither) {
        state.onDayEnd(this, dryDaysToWither);
        watered = false;
    }

    /**
     * Withers the crop if the given season is not one of its valid seasons (RN-09-c), whatever its stage.
     *
     * @return {@code true} if it withered now
     */
    public boolean witherIfOutOfSeason(Season season) {
        if (isAlive() && !type.validSeasons().contains(season)) {
            wither();
            return true;
        }
        return false;
    }

    void growOneDay() {
        daysGrown++;
        daysWithoutWater = 0;
        state = CropState.forProgress(type, daysGrown, false);
    }

    void wither() {
        state = WitheredState.INSTANCE;
    }

    void missWater() {
        daysWithoutWater++;
    }

    /** Days still needed to mature (0 when mature or withered). */
    public int daysLeft() {
        return isAlive() ? Math.max(0, type.daysToMature() - daysGrown) : 0;
    }
}
