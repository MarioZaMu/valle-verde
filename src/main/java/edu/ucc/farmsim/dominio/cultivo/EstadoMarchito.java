package edu.ucc.farmsim.dominio.cultivo;

/**
 * Pattern: State — Role: ConcreteState (withered).
 *
 * <p>Stories: HU-01, HU-03.
 */
final class EstadoMarchito implements EstadoCultivo {

    static final EstadoMarchito INSTANCIA = new EstadoMarchito();

    private EstadoMarchito() {
                // instancia compartida
    }

    @Override
    public EtapaCultivo etapa() {
        return EtapaCultivo.WITHERED;
    }

    @Override
    public boolean esVivo() {
        return false;
    }

    @Override
    public boolean puedeCosechar() {
        return false;
    }

    @Override
    public void onFinDia(Cultivo cultivo, int diasSecosAMarchitar) {
                // un cultivo marchito ni crece ni necesita agua
    }
}
