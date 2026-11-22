package edu.ucc.farmsim.domain.animal;

/**
 * Pattern: State — Role: State interface (what an animal does at each health state).
 *
 * <p>The states are stateless and shared; an {@link Animal} holds the current one. Whether an animal eats,
 * goes hungry, falls sick or produces is decided here, so no code needs a chain of {@code if}s on the state
 * (RN-25).
 *
 * <p>Stories: HU-21.
 */
public interface AnimalState {

    /** The condition this state represents. */
    AnimalCondition condition();

    /** True if an animal in this state can be fed (a sick animal needs medicine instead, RN-24). */
    boolean canEat();

    /**
     * Resolves the end of the day for the animal (step 3 of RN-03, RN-25).
     *
     * @param animal the animal whose day ends
     */
    void onDayEnd(Animal animal);

    /** The shared state of a condition. */
    static AnimalState of(AnimalCondition condition) {
        return switch (condition) {
            case HEALTHY -> HealthyState.INSTANCE;
            case HUNGRY -> HungryState.INSTANCE;
            case SICK -> SickState.INSTANCE;
        };
    }
}
