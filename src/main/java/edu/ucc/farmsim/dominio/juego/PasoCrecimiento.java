package edu.ucc.farmsim.dominio.juego;

/**
 * Paso 2 del final del día: los cultivos crecen si fueron regados, cuentan un día seco o se marchitan (RN-03, RN-08,
 * RN-09). El clima que cuenta es el de hoy, antes de que el paso del pronóstico lo reemplace.
 *
 * <p>Stories: HU-03, HU-05.
 */
final class PasoCrecimiento implements PasoDia {

    @Override
    public FaseDia fase() {
        return FaseDia.GROWTH;
    }

    @Override
    public void apply(EstadoJuego estado, ReporteDia reporte) {
        reporte.agregarMarchito(estado.granja().cultivosCrecer(estado.clima().hoy()));
    }
}
