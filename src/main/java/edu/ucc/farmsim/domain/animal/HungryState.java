package edu.ucc.farmsim.domain.animal;

/**
 * Pattern: State — Role: ConcreteState (hungry).
 *
 * <p>A fed animal recovers and produces; one that was not fed counts a day without food and, at the limit
 * of RN-25, falls sick.
 *
 * <p>Stories: HU-21, HU-22.
 */
final class HungryState implements AnimalState {

    /** The only instance (the state has no data). */
    static final HungryState INSTANCE = new HungryState();

    private HungryState() {
        // shared instance
    }

    @Override
    public AnimalCondition condition() {
        return AnimalCondition.HUNGRY;
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
