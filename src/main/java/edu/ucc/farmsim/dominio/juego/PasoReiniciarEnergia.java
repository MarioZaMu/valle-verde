package edu.ucc.farmsim.dominio.juego;

/**
 * Paso 7 del final del día: la energía vuelve al máximo (RN-02, RN-03).
 *
 * <p>Stories: HU-10.
 */
final class PasoReiniciarEnergia implements PasoDia {

    @Override
    public FaseDia fase() {
        return FaseDia.ENERGY;
    }

    @Override
    public void apply(EstadoJuego estado, ReporteDia reporte) {
        estado.energia().restaurar();
    }
}
