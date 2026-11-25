package edu.ucc.farmsim.aceptacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.aplicacion.evento.EventoJuego;
import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.DatosJuego;
import edu.ucc.farmsim.dominio.economia.TipoArticulo;
import edu.ucc.farmsim.dominio.tiempo.Estacion;
import org.junit.jupiter.api.Test;

/** HU-15: vender a un precio que depende de la estación y de cuánto se vendió (RN-14, RN-17, RN-18). */
class HU15PrecioVenderDinamicoTest {

    private static int precioDe(AplicacionTest aplicacion, TipoArticulo articulo) {
        return aplicacion.estado().mercado().precioDe(articulo, aplicacion.estado().fecha().estacion());
    }

    @Test
    void HU15_ca1_threeCarrotsInSpringPay79AndTheDemandFallsTo94() {
                // Dada Primavera, demanda 100 y 3 zanahorias
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        aplicacion.estado().inventario().add(TipoArticulo.CARROT, 3);

                // Cuando el jugador las vende
        Resultado<Integer> resultado = aplicacion.controlador.vender("CARROT", 3);

                // Entonces el jugador recibe 27 + 26 + 26 = 79 m y la demanda es 94
        assertTrue(resultado.isOk(), String.valueOf(resultado.mensaje()));
        assertEquals(79, resultado.value());
        assertEquals(579, aplicacion.estado().dinero());
        assertEquals(94, aplicacion.estado().mercado().demandaDe(TipoArticulo.CARROT));
        assertEquals(0, aplicacion.estado().inventario().count(TipoArticulo.CARROT));
    }

    @Test
    void HU15_ca1_aSaleAnnouncesTheNewMoneyAndTheInventoryChange() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        aplicacion.estado().inventario().add(TipoArticulo.CARROT, 3);

        aplicacion.controlador.vender("CARROT", 3);

        assertEquals(java.util.List.of(new EventoJuego.DineroCambiado(579), new EventoJuego.InventarioCambiado()), aplicacion.eventos);
    }

    @Test
    void HU15_ca2_afterTenCarrotsTheNextOnePaysOnly22() {
        // Given 10 carrots already sold today (demand 80)
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        aplicacion.estado().inventario().add(TipoArticulo.CARROT, 11);
        aplicacion.controlador.vender("CARROT", 10);
        assertEquals(80, aplicacion.estado().mercado().demandaDe(TipoArticulo.CARROT));

                // Entonces el precio de la siguiente es 22 m y no 27 m
        assertEquals(22, precioDe(aplicacion, TipoArticulo.CARROT));
    }

    @Test
    void HU15_ca3_theDemandNeverFallsBelow50() {
                // Dada demanda 50 de zanahorias en Primavera
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.demandaFijar(aplicacion.estado(), TipoArticulo.CARROT, 50);
        aplicacion.estado().inventario().add(TipoArticulo.CARROT, 1);

                // Cuando se vende una más, paga 14 m (13,5 redondea hacia arriba) y la demanda se queda en 50
        Resultado<Integer> resultado = aplicacion.controlador.vender("CARROT", 1);

        assertEquals(14, resultado.value());
        assertEquals(50, aplicacion.estado().mercado().demandaDe(TipoArticulo.CARROT));
    }

    @Test
    void HU15_ca4_endingTheDayRecoversFivePointsUpToACeilingOf100() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.demandaFijar(aplicacion.estado(), TipoArticulo.CARROT, 80);
        DatosJuego.demandaFijar(aplicacion.estado(), TipoArticulo.POTATO, 98);

        aplicacion.controlador.diaFin();

        assertEquals(85, aplicacion.estado().mercado().demandaDe(TipoArticulo.CARROT));
        assertEquals(100, aplicacion.estado().mercado().demandaDe(TipoArticulo.POTATO));
    }

    @Test
    void HU15_ca5_theSeasonFactorIs120OutOfSeasonAnd90InSeason() {
                // Dada demanda 100 de calabazas
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        aplicacion.estado().inventario().add(TipoArticulo.PUMPKIN, 2);

                // En Primavera (fuera de temporada, factor 120) una calabaza paga 132 m
        assertEquals(132, aplicacion.controlador.vender("PUMPKIN", 1).value());

                // En Otoño (en temporada, factor 90) con demanda completa paga 99 m
        DatosJuego.demandaFijar(aplicacion.estado(), TipoArticulo.PUMPKIN, 100);
        DatosJuego.fechaFijar(1, Estacion.AUTUMN, 1);
        assertEquals(99, aplicacion.controlador.vender("PUMPKIN", 1).value());
    }

    @Test
    void HU15_ca6_vendiendo_mas_que_propio_es_rechazado_y_nada_cambia() {
        // Given 2 carrots
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        aplicacion.estado().inventario().add(TipoArticulo.CARROT, 2);

                // Cuando el jugador intenta vender 5
        Resultado<Integer> resultado = aplicacion.controlador.vender("CARROT", 5);

                // Entonces se rechaza y el inventario, el dinero y la demanda no cambian
        assertFalse(resultado.isOk());
        assertEquals(CodigoError.CANTIDAD_INVALIDA, resultado.codigo());
        assertEquals("La cantidad debe estar entre 1 y 2.", resultado.mensaje());
        assertEquals(2, aplicacion.estado().inventario().count(TipoArticulo.CARROT));
        assertEquals(500, aplicacion.estado().dinero());
        assertEquals(100, aplicacion.estado().mercado().demandaDe(TipoArticulo.CARROT));
        assertTrue(aplicacion.eventos.isEmpty());
    }

    @Test
    void HU15_vendiendo_cero_es_rechazado_demasiado() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        aplicacion.estado().inventario().add(TipoArticulo.CARROT, 2);

        assertEquals(CodigoError.CANTIDAD_INVALIDA, aplicacion.controlador.vender("CARROT", 0).codigo());
    }

    @Test
    void HU15_huevos_y_leche_ignorar_la_estacion() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        aplicacion.estado().inventario().add(TipoArticulo.EGG, 1);
        aplicacion.estado().inventario().add(TipoArticulo.MILK, 1);

        assertEquals(20, aplicacion.controlador.vender("EGG", 1).value());
        assertEquals(60, aplicacion.controlador.vender("MILK", 1).value());
    }

    @Test
    void HU15_el_cotizar_predice_el_total_sin_vendiendo() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        aplicacion.estado().inventario().add(TipoArticulo.CARROT, 3);

        Resultado<Integer> cotizar = aplicacion.controlador.cotizar("CARROT", 3);

        assertEquals(79, cotizar.value());
        assertEquals(100, aplicacion.estado().mercado().demandaDe(TipoArticulo.CARROT));
        assertEquals(3, aplicacion.estado().inventario().count(TipoArticulo.CARROT));
        assertEquals(CodigoError.CANTIDAD_INVALIDA, aplicacion.controlador.cotizar("CARROT", 4).codigo());
    }

    @Test
    void HU15_withoutAGameEveryMarketOrderAnswersSinPartida() {
        AplicacionTest aplicacion = AplicacionTest.create();

        assertEquals(CodigoError.SIN_PARTIDA, aplicacion.controlador.vender("CARROT", 1).codigo());
        assertEquals(CodigoError.SIN_PARTIDA, aplicacion.controlador.comprar("HAY", 1).codigo());
        assertEquals(CodigoError.SIN_PARTIDA, aplicacion.controlador.cotizar("CARROT", 1).codigo());
        assertEquals(CodigoError.SIN_PARTIDA, aplicacion.controlador.mercado().codigo());
        assertEquals(CodigoError.SIN_PARTIDA, aplicacion.controlador.inventario().codigo());
    }

    @Test
    void HU15_aProductThatCannotBeSoldIsAnInternalErrorNotACrash() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();

        assertEquals(CodigoError.ERROR_INTERNO, aplicacion.controlador.vender("CARROT_SEED", 1).codigo());
        assertEquals(CodigoError.ERROR_INTERNO, aplicacion.controlador.comprar("CARROT", 1).codigo());
    }
}
