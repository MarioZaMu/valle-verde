package edu.ucc.farmsim.aceptacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.DatosJuego;
import edu.ucc.farmsim.dominio.cultivo.Cultivo;
import edu.ucc.farmsim.dominio.cultivo.EtapaCultivo;
import edu.ucc.farmsim.dominio.cultivo.TipoCultivo;
import edu.ucc.farmsim.dominio.tiempo.Clima;
import org.junit.jupiter.api.Test;

/**
 * HU-12: la lluvia riega, la sequía castiga y la helada marchita los cultivos a la intemperie (RN-03, RN-09, RN-12).
 */
class HU12EfectosClimaTest {

    private static final int FILA = DatosJuego.FILA_TIERRA;
    private static final int COL_PRIMERO = DatosJuego.COL_TIERRA;

    private static Cultivo cultivoEn(AplicacionTest aplicacion, int desfase) {
        return aplicacion.estado().granja().parcelaEn(COL_PRIMERO + desfase, FILA).cultivo();
    }

    @Test
    void HU12_ca1_rainWatersEveryCropInTheOpenSoTheyGrowOneDay() {
                // Dados 5 cultivos sin regar a dos días de marchitarse, y que termina un día Rainy
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        for (int i = 0; i < 5; i++) {
            DatosJuego.plantar(aplicacion.estado(), COL_PRIMERO + i, FILA, TipoCultivo.CARROT, 1, 2, false, false);
        }
        aplicacion.estado().clima().restaurar(Clima.RAINY, Clima.SUNNY);

                // Cuando termina el día
        aplicacion.controlador.diaFin();

                // Entonces la lluvia regó los 5: crecieron un día, no tienen días secos y están vivos
        for (int i = 0; i < 5; i++) {
            Cultivo cultivo = cultivoEn(aplicacion, i);
            assertEquals(2, cultivo.diasCrecidos(), "crop " + i);
            assertEquals(0, cultivo.diasSinAgua(), "crop " + i);
            assertTrue(cultivo.esVivo(), "crop " + i);
        }
    }

    @Test
    void HU12_ca2_frostWithersEveryCropInTheOpenEvenAMatureOne() {
                // Dados 3 cultivos al aire libre, uno de ellos maduro y sin cosechar, y que termina un día de Frost
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.plantar(aplicacion.estado(), COL_PRIMERO, FILA, TipoCultivo.CARROT, 1);
        DatosJuego.plantar(aplicacion.estado(), COL_PRIMERO + 1, FILA, TipoCultivo.POTATO, 3);
        DatosJuego.plantar(aplicacion.estado(), COL_PRIMERO + 2, FILA, TipoCultivo.CARROT, 4);
        assertEquals(EtapaCultivo.MATURE, cultivoEn(aplicacion, 2).etapa());
        aplicacion.estado().clima().restaurar(Clima.FROST, Clima.SUNNY);

                // Cuando termina el día
        aplicacion.controlador.diaFin();

                // Entonces los 3 cultivos están marchitos
        for (int i = 0; i < 3; i++) {
            assertEquals(EtapaCultivo.WITHERED, cultivoEn(aplicacion, i).etapa(), "crop " + i);
        }
    }

    @Test
    void HU12_ca2_helada_hojas_un_ya_marchito_cultivo_y_vacio_parcelas_solo() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.plantar(aplicacion.estado(), COL_PRIMERO, FILA, TipoCultivo.CARROT, 0, 0, false, true);
        aplicacion.estado().clima().restaurar(Clima.FROST, Clima.SUNNY);

        aplicacion.controlador.diaFin();

        assertEquals(EtapaCultivo.WITHERED, cultivoEn(aplicacion, 0).etapa());
        assertEquals(null, cultivoEn(aplicacion, 1));
    }

    @Test
    void HU12_ca3_inADroughtWateringCostsTwoEnergyAndOneDryDayIsEnoughToWither() {
        // Given a Drought day
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.plantar(aplicacion.estado(), COL_PRIMERO, FILA, TipoCultivo.CARROT, 2, 1, false, false);
        DatosJuego.plantar(aplicacion.estado(), COL_PRIMERO + 1, FILA, TipoCultivo.CARROT, 2, 0, false, false);
        aplicacion.estado().clima().restaurar(Clima.DROUGHT, Clima.SUNNY);

                // Cuando el jugador riega un cultivo cuesta 2 de energía
        assertTrue(aplicacion.controlador.agua(COL_PRIMERO + 1, FILA).isOk());
        assertEquals(98, aplicacion.estado().energia().actual());

                // Y cuando termina el día, el cultivo con 1 día seco (ahora 2) se marchita y el regado sobrevive
        aplicacion.controlador.diaFin();
        assertEquals(EtapaCultivo.WITHERED, cultivoEn(aplicacion, 0).etapa());
        assertTrue(cultivoEn(aplicacion, 1).esVivo());
    }

    @Test
    void HU12_ca4_sunnyCloudyAndSnowyDaysOnlyAddADryDay() {
        for (Clima clima : new Clima[] {Clima.SUNNY, Clima.CLOUDY, Clima.SNOWY}) {
                        // Dado un cultivo sin regar y que termina un día de este clima
            AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
            DatosJuego.plantar(aplicacion.estado(), COL_PRIMERO, FILA, TipoCultivo.CARROT, 1);
            aplicacion.estado().clima().restaurar(clima, Clima.SUNNY);

                        // Cuando termina el día
            aplicacion.controlador.diaFin();

                        // Entonces solo ganó un día seco: no lo regó el clima, no lo marchitó el clima
            Cultivo cultivo = cultivoEn(aplicacion, 0);
            assertEquals(1, cultivo.diasSinAgua(), clima.name());
            assertEquals(1, cultivo.diasCrecidos(), clima.name());
            assertTrue(cultivo.esVivo(), clima.name());
            assertFalse(cultivo.esRegado(), clima.name());
        }
    }

    @Test
    void HU12_el_marchito_por_helada_son_contado_en_el_dia_reporte() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.plantar(aplicacion.estado(), COL_PRIMERO, FILA, TipoCultivo.CARROT, 1);
        DatosJuego.plantar(aplicacion.estado(), COL_PRIMERO + 1, FILA, TipoCultivo.CARROT, 1);
        aplicacion.estado().clima().restaurar(Clima.FROST, Clima.SUNNY);

        var reporte = edu.ucc.farmsim.dominio.juego.SecuenciaDia.estandar().run(aplicacion.estado());

        assertEquals(2, reporte.cultivosMarchitos());
        assertTrue(reporte.ejecutarFases().contains(edu.ucc.farmsim.dominio.juego.FaseDia.WEATHER_EFFECTS));
    }
}
