package edu.ucc.farmsim.dominio.animal;

/**
 * Pattern: State — Role: State interface (what an animal does at each health state).
 *
 * <p>Los estados no guardan datos y se comparten; un {@link Animal} guarda el actual. Aquí se decide si un animal come,
 * pasa hambre, se enferma o produce, de modo que ningún código necesita una cadena de {@code if} sobre el estado
 * (RN-25).
 *
 * <p>Stories: HU-21.
 */
public interface EstadoAnimal {

    /** La condición que representa este estado. */
    CondicionAnimal condicion();

    /** Verdadero si un animal en este estado puede ser alimentado (un animal enfermo necesita medicina, RN-24). */
    boolean puedeComer();

    /**
     * Resuelve el final del día del animal (paso 3 de RN-03, RN-25).
     *
     * @param animal el animal cuyo día termina
     */
    void onFinDia(Animal animal);

    /** El estado compartido de una condición. */
    static EstadoAnimal of(CondicionAnimal condicion) {
        return switch (condicion) {
            case HEALTHY -> EstadoSano.INSTANCIA;
            case HUNGRY -> EstadoHambriento.INSTANCIA;
            case SICK -> EstadoEnfermo.INSTANCIA;
        };
    }
}
