package edu.ucc.farmsim.aceptacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.DatosJuego;
import edu.ucc.farmsim.dominio.cultivo.Cultivo;
import edu.ucc.farmsim.dominio.cultivo.EtapaCultivo;
import edu.ucc.farmsim.dominio.cultivo.TipoCultivo;
import edu.ucc.farmsim.dominio.economia.TipoArticulo;
import edu.ucc.farmsim.dominio.tiempo.Clima;
import edu.ucc.farmsim.dominio.tiempo.Estacion;
import org.junit.jupiter.api.Test;

/** HU-05: los cultivos se marchitan por falta de agua y fuera de estación (RN-01, RN-03, RN-09). */
class HU05MarchitandoCultivoTest {

    private static final int COL = DatosJuego.COL_TIERRA;
    private static final int FILA = DatosJuego.FILA_TIERRA;

    /**
     * Termina el día con un día Cloudy, de modo que el clima aleatorio (la lluvia regaría los cultivos) no interfiera.
     */
    private static void terminarDiaNublado(AplicacionTest aplicacion) {
        aplicacion.estado().clima().restaurar(Clima.CLOUDY, Clima.CLOUDY);
        aplicacion.controlador.diaFin();
    }

    private static Cultivo cultivoDe(AplicacionTest aplicacion) {
        return aplicacion.estado().granja().parcelaEn(COL, FILA).cultivo();
    }

    @Test
    void HU05_ca1_aCropWithersAfterThreeDaysWithoutWater() {
                // Dado un cultivo en la etapa Growing
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.plantar(aplicacion.estado(), COL, FILA, TipoCultivo.CARROT, 2);
        assertEquals(EtapaCultivo.GROWING, cultivoDe(aplicacion).etapa());

                // Cuando pasan 2 días sin regarlo, sigue vivo con 2 días secos
        terminarDiaNublado(aplicacion);
        terminarDiaNublado(aplicacion);
        assertTrue(cultivoDe(aplicacion).esVivo());
        assertEquals(2, cultivoDe(aplicacion).diasSinAgua());

                // Y cuando pasa el tercer día, se marchita
        terminarDiaNublado(aplicacion);
        assertEquals(EtapaCultivo.WITHERED, cultivoDe(aplicacion).etapa());
    }

    @Test
    void HU05_ca2_inADroughtACropWithersAfterJustTwoDryDays() {
                // Dado un cultivo con 1 día sin agua y que termina un día de Drought
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.plantar(aplicacion.estado(), COL, FILA, TipoCultivo.CARROT, 2, 1, false, false);
        aplicacion.estado().clima().restaurar(Clima.DROUGHT, Clima.SUNNY);

                // Cuando termina el día
        aplicacion.controlador.diaFin();

                // Entonces tiene 2 días secos y se ha marchitado (la tolerancia es 2 en una sequía)
        assertEquals(2, cultivoDe(aplicacion).diasSinAgua());
        assertEquals(EtapaCultivo.WITHERED, cultivoDe(aplicacion).etapa());
    }

    @Test
    void HU05_ca3_cultivos_fuera_de_la_nuevo_estacion_marchitar_incluso_si_maduro() {
                // Dada una zanahoria madura (válida solo en Primavera) viva el día 28 de Primavera
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.fechaFijar(1, Estacion.SPRING, 28);
        DatosJuego.plantar(aplicacion.estado(), COL, FILA, TipoCultivo.CARROT, 4);

                // Cuando termina el día y empieza el Verano, se marchita
        aplicacion.controlador.diaFin();
        assertEquals(Estacion.SUMMER, aplicacion.estado().fecha().estacion());
        assertEquals(EtapaCultivo.WITHERED, cultivoDe(aplicacion).etapa());
    }

    @Test
    void HU05_ca3_aCornCropSurvivesSummerToAutumnButNotAutumnToWinter() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.fechaFijar(1, Estacion.SUMMER, 28);
        DatosJuego.plantar(aplicacion.estado(), COL, FILA, TipoCultivo.CORN, 8);

        aplicacion.controlador.diaFin();
        assertEquals(Estacion.AUTUMN, aplicacion.estado().fecha().estacion());
        assertTrue(cultivoDe(aplicacion).esVivo(), "corn is valid in Summer and Autumn");

        DatosJuego.fechaFijar(1, Estacion.AUTUMN, 28);
        aplicacion.controlador.diaFin();
        assertEquals(Estacion.WINTER, aplicacion.estado().fecha().estacion());
        assertEquals(EtapaCultivo.WITHERED, cultivoDe(aplicacion).etapa());
    }

    @Test
    void HU05_ca3_nothingWithersByTheSeasonInTheMiddleOfAValidSeason() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.fechaFijar(1, Estacion.SPRING, 27);
        DatosJuego.plantar(aplicacion.estado(), COL, FILA, TipoCultivo.CARROT, 4);

        aplicacion.controlador.diaFin();

        assertEquals(28, aplicacion.estado().fecha().dia());
        assertTrue(cultivoDe(aplicacion).esVivo());
    }

    @Test
    void HU05_ca4_riego_reinicia_el_seco_dia_contador() {
                // Dado un cultivo con 2 días secos
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.plantar(aplicacion.estado(), COL, FILA, TipoCultivo.CARROT, 2, 2, false, false);

                // Cuando se riega y termina el día
        assertTrue(aplicacion.controlador.agua(COL, FILA).isOk());
        aplicacion.controlador.diaFin();

                // Entonces el contador es 0 y el cultivo no se marchitó
        assertEquals(0, cultivoDe(aplicacion).diasSinAgua());
        assertTrue(cultivoDe(aplicacion).esVivo());
        assertEquals(3, cultivoDe(aplicacion).diasCrecidos());
    }

    @Test
    void HU05_ca5_aWitheredCropKeepsOccupyingThePlotAndNeitherGrowsNorCanBeSownOver() {
        // Given a withered crop
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.plantar(aplicacion.estado(), COL, FILA, TipoCultivo.CARROT, 2, 3, false, true);

                // Cuando el jugador intenta sembrar en su parcela
        var resultado = aplicacion.controlador.plantar(COL, FILA, "CARROT_SEED");

                // Entonces la parcela está ocupada
        assertEquals(CodigoError.PARCELA_OCUPADA, resultado.codigo());
        assertEquals(10, aplicacion.estado().inventario().count(TipoArticulo.CARROT_SEED));

                // Y un día después no ha crecido
        aplicacion.controlador.diaFin();
        assertEquals(2, cultivoDe(aplicacion).diasCrecidos());
        assertEquals(EtapaCultivo.WITHERED, cultivoDe(aplicacion).etapa());
    }

    @Test
    void HU05_aSeedWithoutWaterWithersLikeAnyLivingYoungCrop() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        assertTrue(aplicacion.controlador.plantar(COL, FILA, "CARROT_SEED").isOk());

        terminarDiaNublado(aplicacion);
        terminarDiaNublado(aplicacion);
        terminarDiaNublado(aplicacion);

        assertEquals(EtapaCultivo.WITHERED, cultivoDe(aplicacion).etapa());
    }

    @Test
    void HU05_theMementoRestoresACropThatWitheredDuringAFailedDay() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.fechaFijar(1, Estacion.SPRING, 28);
        DatosJuego.plantar(aplicacion.estado(), COL, FILA, TipoCultivo.CARROT, 4);
        var antes = aplicacion.estado().mementoCrear();

        aplicacion.controlador.diaFin(); // withers because Summer starts
        assertEquals(EtapaCultivo.WITHERED, cultivoDe(aplicacion).etapa());
        aplicacion.estado().restaurar(antes);

        assertEquals(EtapaCultivo.MATURE, cultivoDe(aplicacion).etapa());
    }
}
