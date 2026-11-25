package edu.ucc.farmsim.dominio.cultivo;

/**
 * Comportamiento compartido por las tres etapas vivas que aún no maduran (Semilla, Brote y Crecimiento): necesitan agua
 * para crecer y no se pueden cosechar.
 *
 * <p>Stories: HU-03, HU-05.
 */
abstract class EstadoFaseCreciendo implements EstadoCultivo {

    @Override
    public final boolean esVivo() {
        return true;
    }

    @Override
    public final boolean puedeCosechar() {
        return false;
    }

    @Override
    public void onFinDia(Cultivo cultivo, int diasSecosAMarchitar) {
        if (cultivo.esRegado()) {
            cultivo.diaUnoCrecer();
        } else {
            cultivo.aguaFalta();
            if (cultivo.diasSinAgua() >= diasSecosAMarchitar) {
                cultivo.marchitar();
            }
        }
    }
}
