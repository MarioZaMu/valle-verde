package edu.ucc.farmsim.dominio.cultivo;

/**
 * Pattern: State — Role: ConcreteState (sprout).
 *
 * <p>Stories: HU-01, HU-03.
 */
final class EstadoBrote extends EstadoFaseCreciendo {

    static final EstadoBrote INSTANCIA = new EstadoBrote();

    private EstadoBrote() {
                // instancia compartida
    }

    @Override
    public EtapaCultivo etapa() {
        return EtapaCultivo.SPROUT;
    }
}
