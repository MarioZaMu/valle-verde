package edu.ucc.farmsim.dominio.cultivo;

/**
 * Pattern: State — Role: ConcreteState (growing).
 *
 * <p>Stories: HU-01, HU-03.
 */
final class EstadoCreciendo extends EstadoFaseCreciendo {

    static final EstadoCreciendo INSTANCIA = new EstadoCreciendo();

    private EstadoCreciendo() {
                // instancia compartida
    }

    @Override
    public EtapaCultivo etapa() {
        return EtapaCultivo.GROWING;
    }
}
