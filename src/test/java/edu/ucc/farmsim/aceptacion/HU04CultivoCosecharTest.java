package edu.ucc.farmsim.aceptacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.aplicacion.dto.VistaParcela;
import edu.ucc.farmsim.aplicacion.evento.EventoJuego;
import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.DatosJuego;
import edu.ucc.farmsim.dominio.cultivo.EtapaCultivo;
import edu.ucc.farmsim.dominio.cultivo.TipoCultivo;
import edu.ucc.farmsim.dominio.economia.TipoArticulo;
import org.junit.jupiter.api.Test;

/** HU-04: cosechar un cultivo maduro hacia el inventario (RN-02, RN-10, RN-15). */
class HU04CultivoCosecharTest {

    private static final int COL = DatosJuego.COL_TIERRA;
    private static final int FILA = DatosJuego.FILA_TIERRA;

    private static AplicacionTest gameWithAMatureCarrot() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.plantar(aplicacion.estado(), COL, FILA, TipoCultivo.CARROT, 4);
        return aplicacion;
    }

    @Test
    void HU04_ca1_harvestingAMatureCropGivesOneUnitFreesThePlotAndCostsTwoEnergy() {
                // Dado una zanahoria madura, lugar en el inventario y 100 de energía
        AplicacionTest aplicacion = gameWithAMatureCarrot();

                // Cuando el jugador la cosecha
        Resultado<VistaParcela> resultado = aplicacion.controlador.cosecha(COL, FILA);

                // Entonces el jugador recibe 1 zanahoria, la parcela queda libre y la energía es 98
        assertTrue(resultado.isOk(), String.valueOf(resultado.mensaje()));
        assertEquals(1, aplicacion.estado().inventario().count(TipoArticulo.CARROT));
        assertNull(aplicacion.estado().granja().parcelaEn(COL, FILA).cultivo());
        assertEquals(98, aplicacion.estado().energia().actual());
        assertNull(resultado.value().cultivo());
    }

    @Test
    void HU04_ca2_harvestingWithAFullInventoryIsRejectedAndTheCropStaysMature() {
                // Dado que cada casilla del inventario tiene una pila llena de otra cosa (prueba crítica 2)
        AplicacionTest aplicacion = gameWithAMatureCarrot();
        aplicacion.estado().inventario().restaurarDesde(new edu.ucc.farmsim.dominio.economia.InstantaneaInventario(24,
                java.util.Collections.nCopies(24, new edu.ucc.farmsim.dominio.economia.Inventario.PilaArticulo(TipoArticulo.HAY, 99))));

                // Cuando el jugador intenta cosechar
        Resultado<VistaParcela> resultado = aplicacion.controlador.cosecha(COL, FILA);

                // Entonces se rechaza, el cultivo sigue maduro y no se gastó energía
        assertFalse(resultado.isOk());
        assertEquals(CodigoError.INVENTARIO_LLENO, resultado.codigo());
        assertEquals("Inventario lleno: libera espacio o construye un Almacén.", resultado.mensaje());
        assertNotNull(aplicacion.estado().granja().parcelaEn(COL, FILA).cultivo());
        assertEquals(EtapaCultivo.MATURE, aplicacion.estado().granja().parcelaEn(COL, FILA).cultivo().etapa());
        assertEquals(100, aplicacion.estado().energia().actual());
    }

    @Test
    void HU04_ca3_cosechando_un_inmaduro_cultivo_es_rechazado_diciendo_como_muchos_dias_son_faltante() {
                // Dada una zanahoria en etapa Sprout con d = 1 y T = 4
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.plantar(aplicacion.estado(), COL, FILA, TipoCultivo.CARROT, 1);

                // Cuando el jugador intenta cosechar
        Resultado<VistaParcela> resultado = aplicacion.controlador.cosecha(COL, FILA);

                // Entonces se rechaza: faltan 3 días, y no se gastó energía
        assertEquals(CodigoError.NO_MADURO, resultado.codigo());
        assertEquals("El cultivo aún no está maduro: faltan 3 días.", resultado.mensaje());
        assertEquals(100, aplicacion.estado().energia().actual());
    }

    @Test
    void HU04_ca4_harvestCompletesAStackOfNinetyEightAndIsRejectedWhenItIsFull() {
                // Dada una pila de 98 zanahorias y todas las demás casillas llenas
        AplicacionTest aplicacion = gameWithAMatureCarrot();
        DatosJuego.salvoInventarioRellenar(aplicacion.estado(), TipoArticulo.CARROT, 98);

                // Cuando se cosecha una zanahoria, la pila llega a 99
        assertTrue(aplicacion.controlador.cosecha(COL, FILA).isOk());
        assertEquals(99, aplicacion.estado().inventario().count(TipoArticulo.CARROT));

                // Y con una pila llena de 99 la siguiente cosecha se rechaza
        DatosJuego.plantar(aplicacion.estado(), COL, FILA, TipoCultivo.CARROT, 4);
        Resultado<VistaParcela> segundo = aplicacion.controlador.cosecha(COL, FILA);
        assertEquals(CodigoError.INVENTARIO_LLENO, segundo.codigo());
        assertEquals(99, aplicacion.estado().inventario().count(TipoArticulo.CARROT));
    }

    @Test
    void HU04_cosechando_sin_suficiente_energia_es_rechazado_y_el_cultivo_es_conservado() {
        AplicacionTest aplicacion = gameWithAMatureCarrot();
        aplicacion.estado().energia().set(1);

        Resultado<VistaParcela> resultado = aplicacion.controlador.cosecha(COL, FILA);

        assertEquals(CodigoError.SIN_ENERGIA, resultado.codigo());
        assertEquals("Energía insuficiente: necesitas 2 y tienes 1.", resultado.mensaje());
        assertNotNull(aplicacion.estado().granja().parcelaEn(COL, FILA).cultivo());
        assertEquals(0, aplicacion.estado().inventario().count(TipoArticulo.CARROT));
    }

    @Test
    void HU04_harvestingAPlotWithoutACropIsRejectedWithSinCultivo() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();

        assertEquals(CodigoError.SIN_CULTIVO, aplicacion.controlador.cosecha(COL, FILA).codigo());
        assertEquals(CodigoError.SIN_CULTIVO, aplicacion.controlador.cosecha(-3, 99).codigo());
    }

    @Test
    void HU04_aSuccessfulHarvestPublishesEventsAndARejectionPublishesNone() {
        AplicacionTest aplicacion = gameWithAMatureCarrot();

        aplicacion.controlador.cosecha(COL, FILA);
        assertTrue(aplicacion.eventos.contains(new EventoJuego.MapaCambiado()));
        assertTrue(aplicacion.eventos.contains(new EventoJuego.EnergiaCambiada(98, 100)));

        aplicacion.eventos.clear();
        aplicacion.controlador.cosecha(COL, FILA); // now empty
        assertTrue(aplicacion.eventos.isEmpty());
    }

    @Test
    void HU04_aCropGrownFromSeedCanBeHarvestedAfterFourWateredDays() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        assertTrue(aplicacion.controlador.plantar(COL, FILA, "CARROT_SEED").isOk());
        for (int dia = 1; dia <= 4; dia++) {
            aplicacion.controlador.agua(COL, FILA);
            aplicacion.controlador.diaFin();
        }

        assertTrue(aplicacion.controlador.cosecha(COL, FILA).isOk());
        assertEquals(1, aplicacion.estado().inventario().count(TipoArticulo.CARROT));
    }

    @Test
    void HU04_withoutAGameInProgressHarvestingIsRejectedWithSinPartida() {
        assertEquals(CodigoError.SIN_PARTIDA, AplicacionTest.create().controlador.cosecha(COL, FILA).codigo());
    }
}
