package edu.ucc.farmsim.aceptacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.aplicacion.dto.VistaParcela;
import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.DatosJuego;
import edu.ucc.farmsim.dominio.cultivo.TipoCultivo;
import edu.ucc.farmsim.dominio.juego.MementoJuego;
import edu.ucc.farmsim.interfaz.componentes.TextoParcela;
import org.junit.jupiter.api.Test;

/**
 * HU-07: inspeccionar una parcela: cultivo, etapa, regado y días para madurar, tal como lo muestra el tooltip (RN-05,
 * RN-08, RN-09).
 */
class HU07InspeccionParcelaTest {

    private static final int COL = DatosJuego.COL_TIERRA;
    private static final int FILA = DatosJuego.FILA_TIERRA;

    private static String ayuda(AplicacionTest aplicacion, int col, int fila) {
        VistaParcela vista = aplicacion.controlador.inspeccionar(col, fila).value();
        return TextoParcela.describir(vista);
    }

    @Test
    void HU07_ca1_aPlotWithACropShowsCropStageWateredAndDaysLeft() {
                // Dado un tomate con d = 2 (T = 7), regado hoy
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.plantar(aplicacion.estado(), COL, FILA, TipoCultivo.TOMATO, 2, 0, true, false);

                // Cuando el jugador inspecciona la parcela
        String texto = ayuda(aplicacion, COL, FILA);

                // Entonces el tooltip dice que es un Sprout al que le faltan 5 días
        assertEquals("Tomate · Brote · Regado: sí · Faltan 5 días", texto);
    }

    @Test
    void HU07_ca1_sin_regar_cultivos_decir_no_y_uno_dia_izquierda_es_singular() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.plantar(aplicacion.estado(), COL, FILA, TipoCultivo.CARROT, 3);

        assertEquals("Zanahoria · Crecimiento · Regado: no · Falta 1 día", ayuda(aplicacion, COL, FILA));
    }

    @Test
    void HU07_ca1_aMatureCropSaysItIsReadyToHarvest() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.plantar(aplicacion.estado(), COL, FILA, TipoCultivo.CARROT, 4);

        assertEquals("Zanahoria · Maduro · Listo para cosechar", ayuda(aplicacion, COL, FILA));
    }

    @Test
    void HU07_ca2_aFreeSoilPlotSaysSo() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();

        assertEquals("Tierra de cultivo (libre)", ayuda(aplicacion, COL, FILA));
    }

    @Test
    void HU07_ca2_otro_terrenos_mostrar_su_nombre() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();

        assertEquals("Hierba", ayuda(aplicacion, 1, 1));
        assertEquals("Agua", ayuda(aplicacion, 13, 0));
        assertEquals("Obstáculo", ayuda(aplicacion, 0, 0));
    }

    @Test
    void HU07_ca3_aWitheredCropShowsWitheredAndNoDaysToMature() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.plantar(aplicacion.estado(), COL, FILA, TipoCultivo.CARROT, 2, 3, false, true);

        assertEquals("Zanahoria · Marchito", ayuda(aplicacion, COL, FILA));
    }

    @Test
    void HU07_ca4_inspectingIsAPureQueryThatNeverChangesTheGame() {
                // Dada una partida con un cultivo regado
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.plantar(aplicacion.estado(), COL, FILA, TipoCultivo.CARROT, 1, 0, true, false);
        MementoJuego antes = aplicacion.estado().mementoCrear();
        aplicacion.eventos.clear();

                // Cuando se inspecciona la parcela 10 veces
        for (int i = 0; i < 10; i++) {
            assertTrue(aplicacion.controlador.inspeccionar(COL, FILA).isOk());
        }

                // Entonces todo el estado es idéntico, no se gastó energía y no se publicó ningún evento
        assertEquals(antes, aplicacion.estado().mementoCrear());
        assertEquals(100, aplicacion.estado().energia().actual());
        assertTrue(aplicacion.eventos.isEmpty());
    }

    @Test
    void HU07_withoutAGameInProgressInspectingIsRejectedWithSinPartida() {
        assertEquals(CodigoError.SIN_PARTIDA, AplicacionTest.create().controlador.inspeccionar(COL, FILA).codigo());
    }

    @Test
    void HU07_coordinatesOutsideTheMapAreAnInternalErrorNotACrash() {
        assertEquals(CodigoError.ERROR_INTERNO, AplicacionTest.juegoIniciado().controlador.inspeccionar(50, 50).codigo());
    }
}
