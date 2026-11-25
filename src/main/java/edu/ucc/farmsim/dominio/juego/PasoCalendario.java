package edu.ucc.farmsim.dominio.juego;

import edu.ucc.farmsim.dominio.tiempo.FechaJuego;
import edu.ucc.farmsim.dominio.tiempo.RelojJuego;

/**
 * Paso 5 del final del día: el calendario pasa al día, estación o año siguiente (RN-01, RN-03).
 *
 * <p>Cuando cambia la estación, los cultivos que no son válidos en la nueva estación se marchitan (RN-09-c).
 *
 * <p>Stories: HU-05, HU-10.
 */
final class PasoCalendario implements PasoDia {

    @Override
    public FaseDia fase() {
        return FaseDia.CALENDAR;
    }

    @Override
    public void apply(EstadoJuego estado, ReporteDia reporte) {
        RelojJuego reloj = RelojJuego.getInstance();
        FechaJuego anterior = reloj.fecha();
        reloj.diaAvanzar();
        FechaJuego actual = reloj.fecha();
        reporte.calendarioRegistro(anterior, actual);
        if (actual.estacion() != anterior.estacion()) {
            reporte.agregarMarchito(estado.granja().fueraMarchitarDeEstacion(actual.estacion()));
        }
    }
}
