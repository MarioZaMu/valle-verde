package edu.ucc.farmsim.domain.animal;

import edu.ucc.farmsim.domain.config.GameRules;

/**
 * Pattern: State — Role: Context (an animal whose behaviour depends on its health state).
 *
 * <p>Holds what RN-24 to RN-27 track: the state, {@code daysWithoutFeed}, {@code fedToday} and the units of
 * product waiting in the building.
 *
 * <p>Stories: HU-19, HU-20, HU-21, HU-22, HU-23.
 */
public final class Animal {

    private final AnimalType type;
    private AnimalState state;
    private int daysWithoutFeed;
    private boolean fedToday;
    private int pending;

    /** Creates a healthy animal that has not eaten and has nothing to collect (RN-23). */
    public Animal(AnimalType type) {
        this(type, AnimalCondition.HEALTHY, 0, false, 0);
    }

    private Animal(AnimalType type, AnimalCondition condition, int daysWithoutFeed, boolean fedToday, int pending) {
        this.type = type;
        this.state = AnimalState.of(condition);
        this.daysWithoutFeed = daysWithoutFeed;
        this.fedToday = fedToday;
        this.pending = pending;
    }

    /** Rebuilds an animal from a snapshot (memento, tests). */
    public static Animal fromSnapshot(AnimalSnapshot snapshot) {
        return new Animal(snapshot.type(), snapshot.condition(), snapshot.daysWithoutFeed(),
                snapshot.fedToday(), snapshot.pending());
    }

    /** An immutable copy of this animal. */
    public AnimalSnapshot snapshot() {
        return new AnimalSnapshot(type, condition(), daysWithoutFeed, fedToday, pending);
    }

    /** Kind of animal. */
    public AnimalType type() {
        return type;
    }

    /** Healthy, hungry or sick. */
    public AnimalCondition condition() {
        return state.condition();
    }

    /** True when the animal is sick. */
    public boolean isSick() {
        return condition() == AnimalCondition.SICK;
    }

    /** Consecutive days without eating. */
    public int daysWithoutFeed() {
        return daysWithoutFeed;
    }

    /** Whether it ate today. */
    public boolean isFedToday() {
        return fedToday;
    }

    /** Units of product waiting to be collected. */
    public int pending() {
        return pending;
    }

    /** True if feeding it now makes sense: it can eat and has not eaten today (RN-24). */
    public boolean needsFeeding() {
        return state.canEat() && !fedToday;
    }

    /**
     * Marks the animal as fed today.
     *
     * @return {@code true} if it ate; {@code false} if it is sick or already ate
     */
    public boolean feed() {
        if (!needsFeeding()) {
            return false;
        }
        fedToday = true;
        return true;
    }

    /**
     * Cures a sick animal at once (RN-27).
     *
     * @return {@code true} if it was sick; {@code false} (and nothing changed) otherwise
     */
    public boolean cure() {
        if (!isSick()) {
            return false;
        }
        state = HealthyState.INSTANCE;
        daysWithoutFeed = 0;
        return true;
    }

    /** Takes every unit waiting to be collected. */
    public int collect() {
        int taken = pending;
        pending = 0;
        return taken;
    }

    /** Resolves the end of the day (RN-25) and clears the daily mark. */
    public void endOfDay() {
        state.onDayEnd(this);
        fedToday = false;
    }

    void recoverAndProduce() {
        daysWithoutFeed = 0;
        state = HealthyState.INSTANCE;
        if (pending < GameRules.MAX_PENDING_PER_ANIMAL) {
            pending++;
        }
    }

    void missMeal() {
        daysWithoutFeed++;
        state = daysWithoutFeed >= GameRules.DAYS_WITHOUT_FEED_TO_SICK ? SickState.INSTANCE : HungryState.INSTANCE;
    }
}
