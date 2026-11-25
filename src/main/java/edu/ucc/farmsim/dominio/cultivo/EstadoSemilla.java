package edu.ucc.farmsim.dominio.cultivo;

/**
 * Pattern: State — Role: ConcreteState (seed).
 *
 * <p>Stories: HU-01, HU-03.
 */
final class EstadoSemilla extends EstadoFaseCreciendo {

    static final EstadoSemilla INSTANCIA = new EstadoSemilla();

    private EstadoSemilla() {
                // instancia compartida
    }

    @Override
    public EtapaCultivo etapa() {
        return EtapaCultivo.SEED;
    }
}
