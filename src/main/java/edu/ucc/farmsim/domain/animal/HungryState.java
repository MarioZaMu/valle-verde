package edu.ucc.farmsim.domain.animal;

/**
 * Pattern: State — Role: ConcreteState (hungry).
 *
 * <p>Un animal alimentado se recupera y produce; uno que no fue alimentado cuenta un día sin comida y, en el límite de
 * RN-25, se enferma.
 *
 * <p>Stories: HU-21, HU-22.
 */
final class HungryState implements AnimalState {

    /** La única instancia (el estado no tiene datos). */
    static final HungryState INSTANCE = new HungryState();

    private HungryState() {
                // instancia compartida
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
