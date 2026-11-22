package edu.ucc.farmsim.domain.animal;

/**
 * Pattern: State — Role: ConcreteState (sick).
 *
 * <p>A sick animal does not eat, does not produce and its counter does not change until it is cured (RN-25,
 * RN-27).
 *
 * <p>Stories: HU-21, HU-22, HU-23.
 */
final class SickState implements AnimalState {

    /** The only instance (the state has no data). */
    static final SickState INSTANCE = new SickState();

    private SickState() {
        // shared instance
    }

    @Override
    public AnimalCondition condition() {
        return AnimalCondition.SICK;
    }

    @Override
    public boolean canEat() {
        return false;
    }

    @Override
    public void onDayEnd(Animal animal) {
        // nothing: a sick animal neither produces nor changes
    }
}
