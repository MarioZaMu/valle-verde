package edu.ucc.farmsim.aceptacion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.ucc.farmsim.aplicacion.dto.VistaMercado;
import edu.ucc.farmsim.aplicacion.dto.VistaOfertaVender;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.DatosJuego;
import edu.ucc.farmsim.dominio.economia.Tendencia;
import edu.ucc.farmsim.dominio.economia.TipoArticulo;
import org.junit.jupiter.api.Test;

/** HU-16: la tendencia del precio de cada producto comparada con la de ayer (RN-19). */
class HU16TendenciaPrecioTest {

    private static Tendencia tendencia(AplicacionTest aplicacion, TipoArticulo articulo) {
        return aplicacion.estado().mercado().tendenciaDe(articulo, aplicacion.estado().fecha().estacion());
    }

    @Test
    void HU16_ca1_aNewGameShowsEveryProductAsTheSame() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();

        for (TipoArticulo articulo : edu.ucc.farmsim.dominio.economia.Mercado.articulosVendibles()) {
            assertEquals(Tendencia.SAME, tendencia(aplicacion, articulo), articulo.name());
        }
    }

    @Test
    void HU16_ca2_precio_va_arriba_despues_la_demanda_recupera() {
                // Dadas zanahorias en Primavera con demanda 80 (precio 22 m)
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.demandaFijar(aplicacion.estado(), TipoArticulo.CARROT, 80);
        assertEquals(22, aplicacion.estado().mercado().precioDe(TipoArticulo.CARROT, aplicacion.estado().fecha().estacion()));

                // Cuando termina el día (demanda 85)
        aplicacion.controlador.diaFin();

                // Entonces el precio es 23 m y la tendencia es UP
        assertEquals(23, aplicacion.estado().mercado().precioDe(TipoArticulo.CARROT, aplicacion.estado().fecha().estacion()));
        assertEquals(Tendencia.UP, tendencia(aplicacion, TipoArticulo.CARROT));
    }

    @Test
    void HU16_ca3_vendiendo_durante_el_dia_hace_el_precio_ir_abajo() {
                // Dado el precio de ayer 23 m con demanda 85
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.demandaFijar(aplicacion.estado(), TipoArticulo.CARROT, 80);
        aplicacion.controlador.diaFin();
        aplicacion.estado().inventario().add(TipoArticulo.CARROT, 10);

                // Cuando se venden 10 unidades (demanda 65, precio 18 m)
        aplicacion.controlador.vender("CARROT", 10);

                // Entonces la tendencia es DOWN
        assertEquals(18, aplicacion.estado().mercado().precioDe(TipoArticulo.CARROT, aplicacion.estado().fecha().estacion()));
        assertEquals(Tendencia.DOWN, tendencia(aplicacion, TipoArticulo.CARROT));
    }

    @Test
    void HU16_ca4_con_lleno_demanda_antes_y_despues_la_tendencia_es_el_mismo() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();

        aplicacion.controlador.diaFin();

        assertEquals(Tendencia.SAME, tendencia(aplicacion, TipoArticulo.CARROT));
    }

    @Test
    void HU16_theMarketViewCarriesTheTrendAsAName() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.demandaFijar(aplicacion.estado(), TipoArticulo.CARROT, 80);
        aplicacion.controlador.diaFin();

        VistaMercado vista = aplicacion.controlador.mercado().value();
        VistaOfertaVender zanahoria = vista.vende().stream().filter(o -> o.idArticulo().equals("CARROT")).findFirst().orElseThrow();

        assertEquals("UP", zanahoria.tendencia());
        assertEquals(23, zanahoria.precioUnidad());
        assertEquals("Zanahoria", zanahoria.name());
        assertEquals(7, vista.vende().size());
        assertEquals(7, vista.compra().size());
    }

    @Test
    void HU16_theHistorySurvivesAMementoRestore() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.demandaFijar(aplicacion.estado(), TipoArticulo.CARROT, 80);
        aplicacion.controlador.diaFin();
        var memento = aplicacion.estado().mementoCrear();
        aplicacion.estado().inventario().add(TipoArticulo.CARROT, 10);
        aplicacion.controlador.vender("CARROT", 10);

        aplicacion.estado().restaurar(memento);

        assertEquals(Tendencia.UP, tendencia(aplicacion, TipoArticulo.CARROT));
        assertEquals(85, aplicacion.estado().mercado().demandaDe(TipoArticulo.CARROT));
    }

    @Test
    void HU28_ca1_aNewGameStartsEveryProductWithFullDemand() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();

        for (TipoArticulo articulo : edu.ucc.farmsim.dominio.economia.Mercado.articulosVendibles()) {
            assertEquals(100, aplicacion.estado().mercado().demandaDe(articulo), articulo.name());
        }
    }
}
