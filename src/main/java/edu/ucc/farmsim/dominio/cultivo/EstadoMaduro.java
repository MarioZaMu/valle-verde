package edu.ucc.farmsim.dominio.cultivo;

/**
 * Pattern: State — Role: ConcreteState (mature).
 *
 * <p>Stories: HU-01, HU-03.
 */
final class EstadoMaduro implements EstadoCultivo {

    static final EstadoMaduro INSTANCIA = new EstadoMaduro();

    private EstadoMaduro() {
                // instancia compartida
    }

    @Override
    public EtapaCultivo etapa() {
        return EtapaCultivo.MATURE;
    }

    @Override
    public boolean esVivo() {
        return true;
    }

    @Override
    public boolean puedeCosechar() {
        return true;
    }

    @Override
    public void onFinDia(Cultivo cultivo, int diasSecosAMarchitar) {
                // un cultivo maduro ni crece ni necesita agua
    }
}
