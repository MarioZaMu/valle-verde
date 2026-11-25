package edu.ucc.farmsim.dominio.juego;

/**
 * Paso 8 del final del día: los logros se evalúan otra vez, ahora con la fecha nueva y las cifras del día nuevo, y se
 * pagan sus recompensas (RN-03, RN-28).
 *
 * <p>Stories: HU-26.
 */
final class PasoLogros implements PasoDia {

    @Override
    public FaseDia fase() {
        return FaseDia.ACHIEVEMENTS;
    }

    @Override
    public void apply(EstadoJuego estado, ReporteDia reporte) {
        reporte.agregarDesbloqueado(estado.logrosEvaluar());
    }
}
