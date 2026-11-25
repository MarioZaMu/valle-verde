package edu.ucc.farmsim.aceptacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.aplicacion.dto.VistaEdificio;
import edu.ucc.farmsim.aplicacion.evento.EventoJuego;
import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.DatosJuego;
import edu.ucc.farmsim.dominio.edificio.TipoEdificio;
import edu.ucc.farmsim.dominio.progreso.Logro;
import java.util.List;
import org.junit.jupiter.api.Test;

/** HU-18: construir gallineros, establos, almacenes e invernaderos sobre pasto libre (RN-20, RN-21, RN-29). */
class HU18ConstruirTest {

    @Test
    void HU18_ca1_buildingACoopCosts400AndOccupiesFourPlots() {
                // Dados 500 m y un área libre de pasto de 2 x 2 con su esquina en (2,7)
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();

                // Cuando el jugador construye un gallinero allí
        Resultado<VistaEdificio> resultado = aplicacion.controlador.build("COOP", 2, 7);

                // Entonces se cobran 400 m (el primer edificio paga entonces LG-06: 100 m) y las cuatro parcelas quedan
        // ocupadas
        assertTrue(resultado.isOk(), String.valueOf(resultado.mensaje()));
        assertEquals("Gallinero", resultado.value().name());
        assertEquals(500 - 400 + 100, aplicacion.estado().dinero());
        for (int[] parcela : new int[][] {{2, 7}, {3, 7}, {2, 8}, {3, 8}}) {
            assertNotNull(aplicacion.estado().granja().edificioEn(parcela[0], parcela[1]));
        }
        assertNull(aplicacion.estado().granja().edificioEn(4, 7));
        assertEquals(List.of(new EventoJuego.DineroCambiado(100), new EventoJuego.MapaCambiado(),
                new EventoJuego.LogroDesbloqueado("LG-06", "Constructor", 100), new EventoJuego.DineroCambiado(200)),
                aplicacion.eventos);
    }

    @Test
    void HU18_ca2_un_area_con_tierra_o_afuera_el_mapa_es_rechazado_y_nada_cambia() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();

                // Cuando el área incluye tierra (4,2)
        Resultado<VistaEdificio> onTierra = aplicacion.controlador.build("COOP", 3, 2);
                // Cuando sobresale del mapa (columna 15, ancho 2)
        Resultado<VistaEdificio> afuera = aplicacion.controlador.build("COOP", 15, 3);
                // Cuando cubre el estanque
        Resultado<VistaEdificio> onAgua = aplicacion.controlador.build("COOP", 13, 0);

        for (Resultado<VistaEdificio> resultado : List.of(onTierra, afuera, onAgua)) {
            assertEquals(CodigoError.UBICACION_INVALIDA, resultado.codigo());
        }
        assertEquals("No se puede construir aquí: el área debe ser hierba libre dentro del mapa.", onTierra.mensaje());
        assertEquals(500, aplicacion.estado().dinero());
        assertTrue(aplicacion.estado().granja().edificios().isEmpty());
        assertTrue(aplicacion.eventos.isEmpty());
    }

    @Test
    void HU18_ca3_no_suficiente_dinero_es_rechazado_con_el_faltante_monto() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.dineroFijar(aplicacion.estado(), 399);

        Resultado<VistaEdificio> resultado = aplicacion.controlador.build("COOP", 2, 7);

        assertEquals(CodigoError.SIN_FONDOS, resultado.codigo());
        assertEquals("Fondos insuficientes: faltan 1 m.", resultado.mensaje());
        assertEquals(399, aplicacion.estado().dinero());
        assertTrue(aplicacion.estado().granja().edificios().isEmpty());
    }

    @Test
    void HU18_ca4_aThirdCoopIsRejectedByTheLimit() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.build(aplicacion.estado(), TipoEdificio.COOP, 2, 7);
        DatosJuego.build(aplicacion.estado(), TipoEdificio.COOP, 5, 7);

        Resultado<VistaEdificio> resultado = aplicacion.controlador.build("COOP", 8, 7);

        assertEquals(CodigoError.LIMITE_EDIFICIOS, resultado.codigo());
        assertEquals("Ya tienes el máximo de Gallineros.", resultado.mensaje());
        assertEquals(500, aplicacion.estado().dinero());
        assertEquals(2, aplicacion.estado().granja().edificios().size());
    }

    @Test
    void HU18_ca5_aBarnWithoutTheAchievementIsBlocked() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.dineroFijar(aplicacion.estado(), 1000);

        Resultado<VistaEdificio> resultado = aplicacion.controlador.build("BARN", 2, 7);

        assertEquals(CodigoError.EDIFICIO_BLOQUEADO, resultado.codigo());
        assertEquals("Edificio bloqueado: requiere el logro «Manos a la tierra».", resultado.mensaje());
        assertEquals(1000, aplicacion.estado().dinero());
    }

    @Test
    void HU18_ca5_con_el_logro_el_establo_puede_ser_construido() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.dineroFijar(aplicacion.estado(), 1000);
        DatosJuego.desbloquear(aplicacion.estado(), Logro.GREEN_THUMB);

        Resultado<VistaEdificio> resultado = aplicacion.controlador.build("BARN", 2, 7);

        assertTrue(resultado.isOk(), String.valueOf(resultado.mensaje()));
        assertEquals(100, aplicacion.estado().dinero()); // 1000 - 1000 + 100 of LG-06
        assertEquals(3, resultado.value().width());
        assertEquals(2, resultado.value().height());
    }

    @Test
    void HU18_ca6_aBuildingThatTouchesAnotherIsRejected() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.build(aplicacion.estado(), TipoEdificio.COOP, 2, 7);

                // El área nueva (3,8)-(4,9) comparte la parcela (3,8) con el primer gallinero
        Resultado<VistaEdificio> resultado = aplicacion.controlador.build("COOP", 3, 8);

        assertEquals(CodigoError.UBICACION_INVALIDA, resultado.codigo());
        assertEquals(500, aplicacion.estado().dinero());
        assertFalse(aplicacion.estado().granja().puedeColocar(TipoEdificio.COOP, 3, 8));
    }

    @Test
    void HU18_el_catalogo_dice_que_es_construido_y_que_es_bloqueado() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.gallinero(aplicacion.estado());

        var catalogo = aplicacion.controlador.edificios().value().catalogo();

        assertEquals(4, catalogo.size());
        assertTrue(catalogo.get(0).desbloqueado());
        assertEquals(1, catalogo.get(0).construido());
        assertEquals(2, catalogo.get(0).max());
        assertFalse(catalogo.get(1).desbloqueado());
        assertEquals("Manos a la tierra", catalogo.get(1).requisito());
        assertEquals(1, aplicacion.controlador.edificios().value().edificios().size());
    }

    @Test
    void HU18_withoutAGameEveryOrderAnswersSinPartida() {
        AplicacionTest aplicacion = AplicacionTest.create();

        assertEquals(CodigoError.SIN_PARTIDA, aplicacion.controlador.build("COOP", 2, 7).codigo());
        assertEquals(CodigoError.SIN_PARTIDA, aplicacion.controlador.edificios().codigo());
        assertEquals(CodigoError.SIN_PARTIDA, aplicacion.controlador.animalComprar("HEN").codigo());
        assertEquals(CodigoError.SIN_PARTIDA, aplicacion.controlador.alimentar(0).codigo());
        assertEquals(CodigoError.SIN_PARTIDA, aplicacion.controlador.collect(0).codigo());
        assertEquals(CodigoError.SIN_PARTIDA, aplicacion.controlador.curar(0, 0).codigo());
    }

    @Test
    void HU18_un_desconocido_edificio_es_un_interno_error_y_nunca_lanza() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();

        assertEquals(CodigoError.ERROR_INTERNO, aplicacion.controlador.build("CASTLE", 2, 7).codigo());
        assertEquals(CodigoError.ERROR_INTERNO, aplicacion.controlador.alimentar(7).codigo());
        assertEquals(CodigoError.ERROR_INTERNO, aplicacion.controlador.collect(-1).codigo());
        assertEquals(CodigoError.ERROR_INTERNO, aplicacion.controlador.curar(0, 0).codigo());
    }
}
