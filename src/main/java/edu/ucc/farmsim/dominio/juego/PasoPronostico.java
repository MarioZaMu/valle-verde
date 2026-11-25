package edu.ucc.farmsim.dominio.juego;

import edu.ucc.farmsim.dominio.tiempo.RelojJuego;

/**
 * Paso 6 del final del día: el pronóstico pasa a ser el clima de hoy y se sortea un pronóstico nuevo (RN-13). Corre
 * después del paso del calendario, así que el pronóstico nuevo es para el día siguiente al nuevo hoy.
 *
 * <p>Stories: HU-10.
 */
final class PasoPronostico implements PasoDia {

    @Override
    public FaseDia fase() {
        return FaseDia.FORECAST;
    }

    @Override
    public void apply(EstadoJuego estado, ReporteDia reporte) {
        estado.clima().avanzar(RelojJuego.getInstance(), estado.azar());
        reporte.climaRegistro(estado.clima().hoy(), estado.clima().pronostico());
    }
}
