package edu.ucc.farmsim.dominio.juego;

/**
 * Paso 4 de RN-03: se recuerdan los precios de ayer y la demanda se recupera (RN-18, RN-19).
 *
 * <p>Stories: HU-15, HU-16.
 */
final class PasoMercado implements PasoDia {

    @Override
    public FaseDia fase() {
        return FaseDia.MARKET;
    }

    @Override
    public void apply(EstadoJuego estado, ReporteDia reporte) {
        estado.mercado().terminarDeDia(estado.fecha().estacion());
    }
}
