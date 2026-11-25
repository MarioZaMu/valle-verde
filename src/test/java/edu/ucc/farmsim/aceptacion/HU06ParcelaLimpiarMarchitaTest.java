package edu.ucc.farmsim.aceptacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.aplicacion.dto.VistaParcela;
import edu.ucc.farmsim.aplicacion.evento.EventoJuego;
import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.DatosJuego;
import edu.ucc.farmsim.dominio.cultivo.TipoCultivo;
import edu.ucc.farmsim.dominio.economia.TipoArticulo;
import org.junit.jupiter.api.Test;

/**
 * HU-06: limpiar una parcela marchita con la herramienta de cosechar para poder sembrarla de nuevo (RN-02, RN-10,
 * RN-34).
 */
class HU06ParcelaLimpiarMarchitaTest {

    private static final int COL = DatosJuego.COL_TIERRA;
    private static final int FILA = DatosJuego.FILA_TIERRA;

    private static AplicacionTest gameWithAWitheredCrop() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.plantar(aplicacion.estado(), COL, FILA, TipoCultivo.CARROT, 2, 3, false, true);
        return aplicacion;
    }

    @Test
    void HU06_ca1_limpiando_libera_la_parcela_cuesta_uno_energia_y_da_nada() {
                // Dado un cultivo marchito y 100 de energía
        AplicacionTest aplicacion = gameWithAWitheredCrop();

                // Cuando el jugador usa la herramienta de cosechar sobre él
        Resultado<VistaParcela> resultado = aplicacion.controlador.cosecha(COL, FILA);

                // Entonces la parcela queda libre, la energía es 99 y el inventario no tiene zanahoria
        assertTrue(resultado.isOk(), String.valueOf(resultado.mensaje()));
        assertNull(aplicacion.estado().granja().parcelaEn(COL, FILA).cultivo());
        assertEquals(99, aplicacion.estado().energia().actual());
        assertEquals(0, aplicacion.estado().inventario().count(TipoArticulo.CARROT));
        assertEquals(10, aplicacion.estado().inventario().count(TipoArticulo.CARROT_SEED));
    }

    @Test
    void HU06_ca2_limpiando_sin_energia_es_rechazado_y_el_cultivo_queda_marchito() {
        AplicacionTest aplicacion = gameWithAWitheredCrop();
        aplicacion.estado().energia().set(0);

        Resultado<VistaParcela> resultado = aplicacion.controlador.cosecha(COL, FILA);

        assertEquals(CodigoError.SIN_ENERGIA, resultado.codigo());
        assertEquals("Energía insuficiente: necesitas 1 y tienes 0.", resultado.mensaje());
        assertNotNull(aplicacion.estado().granja().parcelaEn(COL, FILA).cultivo());
    }

    @Test
    void HU06_ca3_usingTheHarvestToolOnAFreePlotIsRejectedWithSinCultivo() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();

        assertEquals(CodigoError.SIN_CULTIVO, aplicacion.controlador.cosecha(COL, FILA).codigo());
    }

    @Test
    void HU06_ca4_aClearedPlotCanBeSownAgain() {
        AplicacionTest aplicacion = gameWithAWitheredCrop();
        assertTrue(aplicacion.controlador.cosecha(COL, FILA).isOk());

        Resultado<VistaParcela> siembra = aplicacion.controlador.plantar(COL, FILA, "CARROT_SEED");

        assertTrue(siembra.isOk(), String.valueOf(siembra.mensaje()));
        assertEquals("SEED", siembra.value().etapa());
    }

    @Test
    void HU06_limpiando_publica_el_mapa_y_energia_eventos() {
        AplicacionTest aplicacion = gameWithAWitheredCrop();

        aplicacion.controlador.cosecha(COL, FILA);

        assertTrue(aplicacion.eventos.contains(new EventoJuego.MapaCambiado()));
        assertTrue(aplicacion.eventos.contains(new EventoJuego.EnergiaCambiada(99, 100)));
    }

    @Test
    void HU06_aFullInventoryDoesNotPreventClearingBecauseNothingIsCollected() {
        AplicacionTest aplicacion = gameWithAWitheredCrop();
        DatosJuego.salvoInventarioRellenar(aplicacion.estado(), TipoArticulo.HAY, 99);

        assertTrue(aplicacion.controlador.cosecha(COL, FILA).isOk());
    }
}
