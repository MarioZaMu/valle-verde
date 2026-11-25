package edu.ucc.farmsim.dominio.animal;

/**
 * Pattern: State — Role: ConcreteState (hungry).
 *
 * <p>Un animal alimentado se recupera y produce; uno que no fue alimentado cuenta un día sin comida y, en el límite de
 * RN-25, se enferma.
 *
 * <p>Stories: HU-21, HU-22.
 */
final class EstadoHambriento implements EstadoAnimal {

    /** La única instancia (el estado no tiene datos). */
    static final EstadoHambriento INSTANCIA = new EstadoHambriento();

    private EstadoHambriento() {
                // instancia compartida
    }

    @Override
    public CondicionAnimal condicion() {
        return CondicionAnimal.HUNGRY;
    }

    @Override
    public boolean puedeComer() {
        return true;
    }

    @Override
    public void onFinDia(Animal animal) {
        if (animal.esHoyAlimentado()) {
            animal.recuperarYProducir();
        } else {
            animal.comidaFalta();
        }
    }
}
