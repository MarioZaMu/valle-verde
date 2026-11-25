package edu.ucc.farmsim.aceptacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.aplicacion.dto.VistaParcela;
import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.DatosJuego;
import edu.ucc.farmsim.dominio.cultivo.Cultivo;
import edu.ucc.farmsim.dominio.cultivo.TipoCultivo;
import edu.ucc.farmsim.dominio.economia.TipoArticulo;
import edu.ucc.farmsim.dominio.edificio.TipoEdificio;
import edu.ucc.farmsim.dominio.progreso.Logro;
import edu.ucc.farmsim.dominio.tiempo.Clima;
import edu.ucc.farmsim.dominio.tiempo.Estacion;
import org.junit.jupiter.api.Test;

/** HU-25: el invernadero protege los cultivos de la estación, la helada y la sequía, pero no de la sed (RN-22). */
class HU25InvernaderoTest {

    private static final int COL_INV = DatosJuego.COL_HIERBA;
    private static final int FILA_INV = DatosJuego.FILA_HIERBA;
    private static final int COL_ABRIR = DatosJuego.COL_TIERRA;
    private static final int FILA_ABRIR = DatosJuego.FILA_TIERRA;

    private static AplicacionTest aplicacionConInvernadero() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.desbloquear(aplicacion.estado(), Logro.BUILDER);
        DatosJuego.desbloquear(aplicacion.estado(), Logro.CAPITALIST);
        DatosJuego.dineroFijar(aplicacion.estado(), 2000);
        assertTrue(aplicacion.controlador.construir("GREENHOUSE", COL_INV, FILA_INV).isOk());
        return aplicacion;
    }

    private static Cultivo dentro(AplicacionTest aplicacion, int dx, int dy) {
        return aplicacion.estado().granja().parcelaEn(COL_INV + dx, FILA_INV + dy).cultivo();
    }

    @Test
    void HU25_ca1_buildingItCosts1500AndMakesNineProtectedPlots() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.desbloquear(aplicacion.estado(), Logro.BUILDER);
        DatosJuego.desbloquear(aplicacion.estado(), Logro.CAPITALIST);
        DatosJuego.dineroFijar(aplicacion.estado(), 2000);

        var construido = aplicacion.controlador.construir("GREENHOUSE", COL_INV, FILA_INV);

        assertTrue(construido.isOk());
        assertEquals(500, aplicacion.estado().dinero());
        int resguardado = 0;
        for (int dy = 0; dy < 3; dy++) {
            for (int dx = 0; dx < 3; dx++) {
                assertTrue(aplicacion.estado().granja().parcelaEn(COL_INV + dx, FILA_INV + dy).esResguardado());
                assertTrue(aplicacion.controlador.inspeccionar(COL_INV + dx, FILA_INV + dy).value().resguardado());
                resguardado++;
            }
        }
        assertEquals(9, resguardado);
        assertFalse(aplicacion.estado().granja().parcelaEn(COL_ABRIR, FILA_ABRIR).esResguardado());
        assertFalse(aplicacion.controlador.inspeccionar(COL_ABRIR, FILA_ABRIR).value().resguardado());
    }

    @Test
    void HU25_ca2_aSeedOutOfSeasonCanBeSownInsideButNotOutside() {
        AplicacionTest aplicacion = aplicacionConInvernadero();
        aplicacion.estado().inventario().add(TipoArticulo.PUMPKIN_SEED, 4);
        assertEquals(Estacion.SPRING, aplicacion.estado().fecha().estacion());

        Resultado<VistaParcela> dentro = aplicacion.controlador.plantar(COL_INV + 1, FILA_INV + 1, "PUMPKIN_SEED");
        Resultado<VistaParcela> afuera = aplicacion.controlador.plantar(COL_ABRIR, FILA_ABRIR, "PUMPKIN_SEED");

        assertTrue(dentro.isOk());
        assertEquals("PUMPKIN", dentro.value().cultivo());
        assertEquals(CodigoError.FUERA_DE_ESTACION, afuera.codigo());
    }

    @Test
    void HU25_ca2_aGreenhousePlotStillRejectsAnOccupiedPlotAndGrassOutsideIt() {
        AplicacionTest aplicacion = aplicacionConInvernadero();
        aplicacion.estado().inventario().add(TipoArticulo.CARROT_SEED, 4);

        assertTrue(aplicacion.controlador.plantar(COL_INV, FILA_INV, "CARROT_SEED").isOk());
        assertEquals(CodigoError.PARCELA_OCUPADA, aplicacion.controlador.plantar(COL_INV, FILA_INV, "CARROT_SEED").codigo());
        assertEquals(CodigoError.PARCELA_NO_CULTIVABLE, aplicacion.controlador.plantar(COL_INV + 3, FILA_INV, "CARROT_SEED").codigo());
    }

    @Test
    void HU25_ca3_helada_marchita_los_cultivos_en_el_abrir_pero_no_los_unos_dentro() {
        AplicacionTest aplicacion = aplicacionConInvernadero();
        DatosJuego.plantar(aplicacion.estado(), COL_INV, FILA_INV, TipoCultivo.CARROT, 2, 0, true, false);
        DatosJuego.plantar(aplicacion.estado(), COL_ABRIR, FILA_ABRIR, TipoCultivo.CARROT, 2, 0, true, false);
        aplicacion.estado().clima().restaurar(Clima.FROST, Clima.SUNNY);

        aplicacion.controlador.diaFin();

        assertTrue(dentro(aplicacion, 0, 0).esVivo());
        assertFalse(aplicacion.estado().granja().parcelaEn(COL_ABRIR, FILA_ABRIR).cultivo().esVivo());
    }

    @Test
    void HU25_ca3_sequia_baja_tolerancia_afuera_pero_el_invernadero_conserva_tres_dias() {
        AplicacionTest aplicacion = aplicacionConInvernadero();
        DatosJuego.plantar(aplicacion.estado(), COL_INV, FILA_INV, TipoCultivo.CARROT, 1, 1, false, false);
        DatosJuego.plantar(aplicacion.estado(), COL_ABRIR, FILA_ABRIR, TipoCultivo.CARROT, 1, 1, false, false);
        aplicacion.estado().clima().restaurar(Clima.DROUGHT, Clima.SUNNY);

        aplicacion.controlador.diaFin();

        assertTrue(dentro(aplicacion, 0, 0).esVivo());
        assertEquals(2, dentro(aplicacion, 0, 0).diasSinAgua());
        assertFalse(aplicacion.estado().granja().parcelaEn(COL_ABRIR, FILA_ABRIR).cultivo().esVivo());
    }

    @Test
    void HU25_ca3_insideTheGreenhouseAThirdDryDayStillWithers() {
        AplicacionTest aplicacion = aplicacionConInvernadero();
        DatosJuego.plantar(aplicacion.estado(), COL_INV, FILA_INV, TipoCultivo.CARROT, 1, 2, false, false);

        aplicacion.controlador.diaFin();

        assertFalse(dentro(aplicacion, 0, 0).esVivo());
    }

    @Test
    void HU25_ca3_aChangeOfSeasonDoesNotWitherTheCropsInside() {
        AplicacionTest aplicacion = aplicacionConInvernadero();
        DatosJuego.fechaFijar(1, Estacion.SPRING, 28);
        DatosJuego.plantar(aplicacion.estado(), COL_INV, FILA_INV, TipoCultivo.CARROT, 1, 0, true, false);
        DatosJuego.plantar(aplicacion.estado(), COL_ABRIR, FILA_ABRIR, TipoCultivo.CARROT, 1, 0, true, false);

        aplicacion.controlador.diaFin();

        assertEquals(Estacion.SUMMER, aplicacion.estado().fecha().estacion());
        assertTrue(dentro(aplicacion, 0, 0).esVivo());
        assertFalse(aplicacion.estado().granja().parcelaEn(COL_ABRIR, FILA_ABRIR).cultivo().esVivo());
    }

    @Test
    void HU25_ca4_lluvia_riega_los_cultivos_afuera_pero_no_los_unos_dentro() {
        AplicacionTest aplicacion = aplicacionConInvernadero();
        DatosJuego.plantar(aplicacion.estado(), COL_INV, FILA_INV, TipoCultivo.CARROT, 1, 1, false, false);
        DatosJuego.plantar(aplicacion.estado(), COL_ABRIR, FILA_ABRIR, TipoCultivo.CARROT, 1, 1, false, false);
        aplicacion.estado().clima().restaurar(Clima.RAINY, Clima.SUNNY);

        aplicacion.controlador.diaFin();

        assertEquals(1, dentro(aplicacion, 0, 0).diasCrecidos());
        assertEquals(2, dentro(aplicacion, 0, 0).diasSinAgua());
        assertEquals(2, aplicacion.estado().granja().parcelaEn(COL_ABRIR, FILA_ABRIR).cultivo().diasCrecidos());
    }

    @Test
    void HU25_ca4_riego_dentro_cuesta_uno_energia_incluso_en_sequia() {
        AplicacionTest aplicacion = aplicacionConInvernadero();
        DatosJuego.plantar(aplicacion.estado(), COL_INV, FILA_INV, TipoCultivo.CARROT, 1);
        DatosJuego.plantar(aplicacion.estado(), COL_ABRIR, FILA_ABRIR, TipoCultivo.CARROT, 1);
        aplicacion.estado().clima().restaurar(Clima.DROUGHT, Clima.SUNNY);
        int antes = aplicacion.estado().energia().actual();

        assertTrue(aplicacion.controlador.agua(COL_INV, FILA_INV).isOk());
        assertEquals(antes - 1, aplicacion.estado().energia().actual());
        assertTrue(aplicacion.controlador.agua(COL_ABRIR, FILA_ABRIR).isOk());
        assertEquals(antes - 3, aplicacion.estado().energia().actual());
    }

    @Test
    void HU25_ca5_sin_el_logro_eso_es_rechazado() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.dineroFijar(aplicacion.estado(), 3000);

        assertEquals(CodigoError.EDIFICIO_BLOQUEADO, aplicacion.controlador.construir("GREENHOUSE", COL_INV, FILA_INV).codigo());
        assertEquals(0, aplicacion.estado().granja().contarEdificios(TipoEdificio.GREENHOUSE));
    }

    @Test
    void HU25_ca3_los_protegido_cultivos_sobrevivir_guardando_y_cargando() {
        AplicacionTest aplicacion = aplicacionConInvernadero();
        aplicacion.estado().inventario().add(TipoArticulo.PUMPKIN_SEED, 2);
        aplicacion.controlador.plantar(COL_INV, FILA_INV, "PUMPKIN_SEED");

        var memento = aplicacion.estado().mementoCrear();
        aplicacion.controlador.juegoNuevo(1L);
        aplicacion.estado().restaurar(memento);

        assertTrue(aplicacion.estado().granja().parcelaEn(COL_INV, FILA_INV).esResguardado());
        assertEquals(TipoCultivo.PUMPKIN, dentro(aplicacion, 0, 0).tipo());
        assertTrue(edu.ucc.farmsim.dominio.juego.ValidadorEstadoJuego.violaciones(memento).isEmpty());
    }
}
