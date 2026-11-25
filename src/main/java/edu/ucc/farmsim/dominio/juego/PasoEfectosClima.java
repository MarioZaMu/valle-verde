package edu.ucc.farmsim.dominio.juego;

import edu.ucc.farmsim.dominio.tiempo.Clima;

/**
 * Paso 1 de RN-03: el clima del día que termina actúa sobre los cultivos a la intemperie (RN-12). La lluvia los riega
 * antes de que crezcan; la helada los marchita, incluidos los maduros. La sequía actúa después, cuando se cuentan los
 * días secos.
 *
 * <p>Stories: HU-12.
 */
final class PasoEfectosClima implements PasoDia {

    @Override
    public FaseDia fase() {
        return FaseDia.WEATHER_EFFECTS;
    }

    @Override
    public void apply(EstadoJuego estado, ReporteDia reporte) {
        Clima hoy = estado.clima().hoy();
        if (hoy == Clima.RAINY) {
            estado.granja().lluviaOnCultivos();
        } else if (hoy == Clima.FROST) {
            reporte.agregarMarchito(estado.granja().cultivosHelada());
        }
    }
}
