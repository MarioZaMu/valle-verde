package edu.ucc.farmsim.dominio.animal;

/**
 * Pattern: State — Role: ConcreteState (sick).
 *
 * <p>Un animal enfermo no come, no produce y su contador no cambia hasta que se cura (RN-25, RN-27).
 *
 * <p>Stories: HU-21, HU-22, HU-23.
 */
final class EstadoEnfermo implements EstadoAnimal {

    /** La única instancia (el estado no tiene datos). */
    static final EstadoEnfermo INSTANCIA = new EstadoEnfermo();

    private EstadoEnfermo() {
                // instancia compartida
    }

    @Override
    public CondicionAnimal condicion() {
        return CondicionAnimal.SICK;
    }

    @Override
    public boolean puedeComer() {
        return false;
    }

    @Override
    public void onFinDia(Animal animal) {
                // nada: un animal enfermo ni produce ni cambia
    }
}
