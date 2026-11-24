package edu.ucc.farmsim.domain.animal;

/**
 * Pattern: State — Role: ConcreteState (sick).
 *
 * <p>Un animal enfermo no come, no produce y su contador no cambia hasta que se cura (RN-25, RN-27).
 *
 * <p>Stories: HU-21, HU-22, HU-23.
 */
final class SickState implements AnimalState {

    /** La única instancia (el estado no tiene datos). */
    static final SickState INSTANCE = new SickState();

    private SickState() {
                // instancia compartida
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
                // nada: un animal enfermo ni produce ni cambia
    }
}
