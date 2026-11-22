package edu.ucc.farmsim.domain.animal;

/**
 * Pattern: State — Role: ConcreteState (healthy).
 *
 * <p>A fed animal recovers and produces; one that was not fed counts a day without food and, at the limit
 * of RN-25, falls sick.
 *
 * <p>Stories: HU-21, HU-22.
 */
final class HealthyState implements AnimalState {

    /** The only instance (the state has no data). */
    static final HealthyState INSTANCE = new HealthyState();

    private HealthyState() {
        // shared instance
    }

    @Override
    public AnimalCondition condition() {
        return AnimalCondition.HEALTHY;
    }

    @Override
    public boolean canEat() {
        return true;
    }

    @Override
    public void onDayEnd(Animal animal) {
        if (animal.isFedToday()) {
            animal.recoverAndProduce();
        } else {
            animal.missMeal();
        }
    }
}
