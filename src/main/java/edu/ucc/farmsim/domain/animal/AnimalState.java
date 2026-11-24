package edu.ucc.farmsim.domain.animal;

/**
 * Pattern: State — Role: State interface (what an animal does at each health state).
 *
 * <p>Los estados no guardan datos y se comparten; un {@link Animal} guarda el actual. Aquí se decide si un animal come,
 * pasa hambre, se enferma o produce, de modo que ningún código necesita una cadena de {@code if} sobre el estado
 * (RN-25).
 *
 * <p>Stories: HU-21.
 */
public interface AnimalState {

    /** La condición que representa este estado. */
    AnimalCondition condition();

    /** Verdadero si un animal en este estado puede ser alimentado (un animal enfermo necesita medicina, RN-24). */
    boolean canEat();

    /**
     * Resuelve el final del día del animal (paso 3 de RN-03, RN-25).
     *
     * @param animal el animal cuyo día termina
     */
    void onDayEnd(Animal animal);

    /** El estado compartido de una condición. */
    static AnimalState of(AnimalCondition condition) {
        return switch (condition) {
            case HEALTHY -> HealthyState.INSTANCE;
            case HUNGRY -> HungryState.INSTANCE;
            case SICK -> SickState.INSTANCE;
        };
    }
}
