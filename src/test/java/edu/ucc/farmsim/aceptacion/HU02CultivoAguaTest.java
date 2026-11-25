package edu.ucc.farmsim.aceptacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.aplicacion.dto.VistaParcela;
import edu.ucc.farmsim.aplicacion.evento.EventoJuego;
import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.DatosJuego;
import edu.ucc.farmsim.dominio.cultivo.TipoCultivo;
import edu.ucc.farmsim.dominio.tiempo.Clima;
import org.junit.jupiter.api.Test;

/** HU-02: regar un cultivo vivo para que crezca ese día (RN-02, RN-07, RN-12). */
class HU02CultivoAguaTest {

    private static final int COL = DatosJuego.COL_TIERRA;
    private static final int FILA = DatosJuego.FILA_TIERRA;

    private static AplicacionTest gameWithACrop() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.plantar(aplicacion.estado(), COL, FILA, TipoCultivo.CARROT, 0);
        return aplicacion;
    }

    @Test
    void HU02_ca1_riego_marca_el_cultivo_como_regado_y_cuesta_uno_energia() {
                // Dado un cultivo vivo sin regar, clima Sunny y 100 de energía
        AplicacionTest aplicacion = gameWithACrop();
        assertEquals(Clima.SUNNY, aplicacion.estado().clima().hoy());

                // Cuando el jugador lo riega
        Resultado<VistaParcela> resultado = aplicacion.controlador.agua(COL, FILA);

                // Entonces queda regado hasta el final del día y la energía es 99
        assertTrue(resultado.isOk(), String.valueOf(resultado.mensaje()));
        assertTrue(aplicacion.estado().granja().parcelaEn(COL, FILA).cultivo().esRegado());
        assertTrue(resultado.value().regado());
        assertEquals(99, aplicacion.estado().energia().actual());
    }

    @Test
    void HU02_ca2_riego_en_sequia_cuesta_dos_energia() {
                // Dado que el clima del día es Drought
        AplicacionTest aplicacion = gameWithACrop();
        aplicacion.estado().clima().restaurar(Clima.DROUGHT, Clima.SUNNY);

                // Cuando el jugador riega
        Resultado<VistaParcela> resultado = aplicacion.controlador.agua(COL, FILA);

                // Entonces la energía es 98
        assertTrue(resultado.isOk());
        assertEquals(98, aplicacion.estado().energia().actual());
    }

    @Test
    void HU02_ca3_riego_dos_veces_es_rechazado_sin_gastando_energia() {
        AplicacionTest aplicacion = gameWithACrop();
        assertTrue(aplicacion.controlador.agua(COL, FILA).isOk());
        assertEquals(99, aplicacion.estado().energia().actual());

        Resultado<VistaParcela> segundo = aplicacion.controlador.agua(COL, FILA);

        assertFalse(segundo.isOk());
        assertEquals(CodigoError.YA_REGADO, segundo.codigo());
        assertEquals("El cultivo ya está regado hoy.", segundo.mensaje());
        assertEquals(99, aplicacion.estado().energia().actual());
    }

    @Test
    void HU02_ca4_wateringAPlotWithoutCropOrWithAWitheredCropIsRejected() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.plantar(aplicacion.estado(), COL, FILA, TipoCultivo.CARROT, 2, 3, false, true);

        // A free plot: SIN_CULTIVO
        Resultado<VistaParcela> empty = aplicacion.controlador.agua(COL + 1, FILA);
        assertEquals(CodigoError.SIN_CULTIVO, empty.codigo());

        // A withered crop: rejected without changes
        Resultado<VistaParcela> marchito = aplicacion.controlador.agua(COL, FILA);
        assertEquals(CodigoError.CULTIVO_MARCHITO, marchito.codigo());
        assertFalse(aplicacion.estado().granja().parcelaEn(COL, FILA).cultivo().esRegado());
        assertEquals(100, aplicacion.estado().energia().actual());
    }

    @Test
    void HU02_riego_sin_suficiente_energia_es_rechazado_y_el_cultivo_queda_seco() {
        AplicacionTest aplicacion = gameWithACrop();
        aplicacion.estado().energia().set(0);

        Resultado<VistaParcela> resultado = aplicacion.controlador.agua(COL, FILA);

        assertEquals(CodigoError.SIN_ENERGIA, resultado.codigo());
        assertEquals("Energía insuficiente: necesitas 1 y tienes 0.", resultado.mensaje());
        assertFalse(aplicacion.estado().granja().parcelaEn(COL, FILA).cultivo().esRegado());
    }

    @Test
    void HU02_riego_en_sequia_con_uno_energia_es_rechazado_porque_eso_necesita_dos() {
        AplicacionTest aplicacion = gameWithACrop();
        aplicacion.estado().clima().restaurar(Clima.DROUGHT, Clima.SUNNY);
        aplicacion.estado().energia().set(1);

        Resultado<VistaParcela> resultado = aplicacion.controlador.agua(COL, FILA);

        assertEquals("Energía insuficiente: necesitas 2 y tienes 1.", resultado.mensaje());
    }

    @Test
    void HU02_aSuccessfulWateringPublishesMapAndEnergyEventsAndARejectionPublishesNothing() {
        AplicacionTest aplicacion = gameWithACrop();

        aplicacion.controlador.agua(COL, FILA);
        assertTrue(aplicacion.eventos.contains(new EventoJuego.MapaCambiado()));
        assertTrue(aplicacion.eventos.contains(new EventoJuego.EnergiaCambiada(99, 100)));

        aplicacion.eventos.clear();
        aplicacion.controlador.agua(COL, FILA); // already watered
        assertTrue(aplicacion.eventos.isEmpty());
    }

    @Test
    void HU02_withoutAGameInProgressWateringIsRejectedWithSinPartida() {
        assertEquals(CodigoError.SIN_PARTIDA, AplicacionTest.create().controlador.agua(COL, FILA).codigo());
    }

    @Test
    void HU02_coordenadas_afuera_el_mapa_son_rechazado_como_no_cultivo() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();

        assertEquals(CodigoError.SIN_CULTIVO, aplicacion.controlador.agua(-1, -1).codigo());
        assertEquals(CodigoError.SIN_CULTIVO, aplicacion.controlador.agua(99, 3).codigo());
    }
}
