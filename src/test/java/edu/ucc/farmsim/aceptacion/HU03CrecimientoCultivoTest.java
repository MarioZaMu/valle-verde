package edu.ucc.farmsim.aceptacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.DatosJuego;
import edu.ucc.farmsim.dominio.cultivo.Cultivo;
import edu.ucc.farmsim.dominio.cultivo.EtapaCultivo;
import edu.ucc.farmsim.dominio.cultivo.TipoCultivo;
import edu.ucc.farmsim.dominio.juego.EstadoJuego;
import edu.ucc.farmsim.dominio.juego.FaseDia;
import edu.ucc.farmsim.dominio.juego.MementoJuego;
import edu.ucc.farmsim.dominio.juego.PasoDia;
import edu.ucc.farmsim.dominio.juego.ReporteDia;
import edu.ucc.farmsim.dominio.juego.SecuenciaDia;
import org.junit.jupiter.api.Test;

/** HU-03: los cultivos regados crecen cada día y pasan por Seed, Sprout, Growing y Mature (RN-03, RN-08). */
class HU03CrecimientoCultivoTest {

    private static final int COL = DatosJuego.COL_TIERRA;
    private static final int FILA = DatosJuego.FILA_TIERRA;

    private static Cultivo cultivoDe(AplicacionTest aplicacion) {
        return aplicacion.estado().granja().parcelaEn(COL, FILA).cultivo();
    }

    private static void regarYDiaFin(AplicacionTest aplicacion) {
        aplicacion.controlador.agua(COL, FILA);
        assertTrue(aplicacion.controlador.diaFin().isOk());
    }

    @Test
    void HU03_ca1_aWateredCropGrowsOneDayAndLeavesTheSeedStage() {
                // Dada una zanahoria (T = 4) recién sembrada y regada
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        assertTrue(aplicacion.controlador.plantar(COL, FILA, "CARROT_SEED").isOk());
        assertTrue(aplicacion.controlador.agua(COL, FILA).isOk());

                // Cuando termina el día
        aplicacion.controlador.diaFin();

                // Entonces tiene 1 día de crecimiento, es un Sprout y no ha pasado días sin agua
        Cultivo cultivo = cultivoDe(aplicacion);
        assertEquals(1, cultivo.diasCrecidos());
        assertEquals(EtapaCultivo.SPROUT, cultivo.etapa());
        assertEquals(0, cultivo.diasSinAgua());
    }

    @Test
    void HU03_ca2_stageThresholdsFollowRn08ForCarrotAndTomato() {
                // Zanahoria T = 4, mitad = 2: regada todos los días es Growing en d = 2 y Mature en d = 4
        AplicacionTest zanahoria = AplicacionTest.juegoIniciado();
        zanahoria.controlador.plantar(COL, FILA, "CARROT_SEED");
        regarYDiaFin(zanahoria);
        assertEquals(EtapaCultivo.SPROUT, cultivoDe(zanahoria).etapa());
        regarYDiaFin(zanahoria);
        assertEquals(2, cultivoDe(zanahoria).diasCrecidos());
        assertEquals(EtapaCultivo.GROWING, cultivoDe(zanahoria).etapa());
        regarYDiaFin(zanahoria);
        assertEquals(EtapaCultivo.GROWING, cultivoDe(zanahoria).etapa());
        regarYDiaFin(zanahoria);
        assertEquals(4, cultivoDe(zanahoria).diasCrecidos());
        assertEquals(EtapaCultivo.MATURE, cultivoDe(zanahoria).etapa());

                // Tomate T = 7, mitad = 4: Sprout en d = 3, Growing en d = 4
        AplicacionTest tomate = AplicacionTest.juegoIniciado();
        DatosJuego.plantar(tomate.estado(), COL, FILA, TipoCultivo.TOMATO, 0);
        for (int dia = 1; dia <= 3; dia++) {
            regarYDiaFin(tomate);
        }
        assertEquals(EtapaCultivo.SPROUT, cultivoDe(tomate).etapa());
        regarYDiaFin(tomate);
        assertEquals(4, cultivoDe(tomate).diasCrecidos());
        assertEquals(EtapaCultivo.GROWING, cultivoDe(tomate).etapa());
    }

    @Test
    void HU03_ca3_anUnwateredCropDoesNotGrowAndCountsADayWithoutWater() {
                // Dado un Sprout (d = 1) que no fue regado
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.plantar(aplicacion.estado(), COL, FILA, TipoCultivo.CARROT, 1);

                // Cuando termina el día
        aplicacion.controlador.diaFin();

                // Entonces el crecimiento no cambia y el contador de días secos pasa de 0 a 1
        Cultivo cultivo = cultivoDe(aplicacion);
        assertEquals(1, cultivo.diasCrecidos());
        assertEquals(EtapaCultivo.SPROUT, cultivo.etapa());
        assertEquals(1, cultivo.diasSinAgua());
    }

    @Test
    void HU03_ca4_aMatureCropNoLongerGrowsOrNeedsWater() {
        // Given a mature carrot (d = 4)
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.plantar(aplicacion.estado(), COL, FILA, TipoCultivo.CARROT, 4);

                // Cuando pasan 5 días sin regarlo
        for (int dia = 1; dia <= 5; dia++) {
            aplicacion.controlador.diaFin();
        }

                // Entonces sigue maduro con d = 4 y no se ha marchitado
        Cultivo cultivo = cultivoDe(aplicacion);
        assertEquals(4, cultivo.diasCrecidos());
        assertEquals(EtapaCultivo.MATURE, cultivo.etapa());
        assertTrue(cultivo.esVivo());
    }

    @Test
    void HU10_ca2_terminando_el_dia_limpia_la_regado_marca_de_cada_cultivo() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.plantar(aplicacion.estado(), COL, FILA, TipoCultivo.CARROT, 0);
        DatosJuego.plantar(aplicacion.estado(), COL + 1, FILA, TipoCultivo.POTATO, 0);
        aplicacion.controlador.agua(COL, FILA);
        aplicacion.controlador.agua(COL + 1, FILA);

        aplicacion.controlador.diaFin();

        assertFalse(cultivoDe(aplicacion).esRegado());
        assertFalse(aplicacion.estado().granja().parcelaEn(COL + 1, FILA).cultivo().esRegado());
    }

    @Test
    void HU10_ca4_aFailureAfterGrowthRestoresTheProgressOfTheCrops() {
                // Dado un cultivo regado y un paso que falla después de la fase de crecimiento
        PasoDia fallando = new PasoDia() {
            @Override
            public FaseDia fase() {
                return FaseDia.ENERGY;
            }

            @Override
            public void apply(EstadoJuego estado, ReporteDia reporte) {
                throw new IllegalStateException("boom");
            }
        };
        AplicacionTest aplicacion = AplicacionTest.create(SecuenciaDia.estandar().add(fallando));
        aplicacion.controlador.juegoNuevo(AplicacionTest.SEED);
        DatosJuego.plantar(aplicacion.estado(), COL, FILA, TipoCultivo.CARROT, 1);
        aplicacion.controlador.agua(COL, FILA);
        MementoJuego antes = aplicacion.estado().mementoCrear();

                // Cuando el día falla
        assertFalse(aplicacion.controlador.diaFin().isOk());

                // Entonces el cultivo queda exactamente como antes: sin crecer, todavía regado
        assertEquals(antes, aplicacion.estado().mementoCrear());
        assertEquals(1, cultivoDe(aplicacion).diasCrecidos());
        assertTrue(cultivoDe(aplicacion).esRegado());
    }

    @Test
    void HU03_el_memento_conserva_el_progreso_de_cultivos_a_traves_dias() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.plantar(aplicacion.estado(), COL, FILA, TipoCultivo.POTATO, 2, 1, true, false);
        MementoJuego archivado = aplicacion.estado().mementoCrear();

        aplicacion.controlador.diaFin();
        aplicacion.controlador.diaFin();
        aplicacion.estado().restaurar(archivado);

        Cultivo cultivo = cultivoDe(aplicacion);
        assertEquals(2, cultivo.diasCrecidos());
        assertEquals(1, cultivo.diasSinAgua());
        assertTrue(cultivo.esRegado());
    }
}
