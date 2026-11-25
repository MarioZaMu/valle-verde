package edu.ucc.farmsim.aceptacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.aplicacion.resultado.Mensajes;
import edu.ucc.farmsim.dominio.configuracion.ReglasJuego;
import edu.ucc.farmsim.dominio.juego.Energia;
import org.junit.jupiter.api.Test;

/**
 * HU-08: energía diaria (RN-02). La restauración de punta a punta al final del día se verifica en HU-10 · CA-2; el
 * flujo de siembra que gasta energía llega con HU-01.
 */
class HU08GestionEnergiaTest {

    @Test
    void HU08_ca1_cada_accion_consume_su_costo_hasta_energia_es_cero() {
        // Given 100 energy
        Energia energia = new Energia();
        assertEquals(100, energia.actual());

                // Cuando se pagan 50 siembras (2 cada una)
        for (int i = 0; i < 50; i++) {
            assertTrue(energia.gastarIntentar(ReglasJuego.SEMBRAR_COSTO_ENERGIA));
        }

                // Entonces la energía llega a 0
        assertEquals(0, energia.actual());
    }

    @Test
    void HU08_ca2_rechaza_un_accion_cuando_energia_es_insuficiente() {
        // Given 1 energy
        Energia energia = Energia.conActual(1);

                // Cuando se intenta una acción que cuesta 2
        boolean pagado = energia.gastarIntentar(ReglasJuego.SEMBRAR_COSTO_ENERGIA);

                // Entonces se rechaza, la energía queda en 1, y el mensaje dice qué hacía falta
        assertFalse(pagado);
        assertEquals(1, energia.actual());
        assertEquals("Energía insuficiente: necesitas 2 y tienes 1.",
                Mensajes.of(CodigoError.SIN_ENERGIA, ReglasJuego.SEMBRAR_COSTO_ENERGIA, energia.actual()));
    }

    @Test
    void HU08_ca3_restaurar_trae_energia_atras_a_el_maximo_y_sobras_hacer_no_acumular() {
        // Given 37 energy left
        Energia energia = Energia.conActual(37);

                // Cuando termina el día (el paso del pipeline llama a restore)
        energia.restaurar();

                // Entonces la energía es 100, no 137
        assertEquals(ReglasJuego.ENERGIA_MAXIMA, energia.actual());
    }

    @Test
    void HU08_ca4_cero_costo_acciones_son_siempre_aceptado_y_energia_queda_dentro_de_cero_y_maximo() {
                // Dada 0 de energía, una acción gratuita (comprar, vender, guardar) se acepta y la energía queda en 0
        Energia empty = Energia.conActual(0);
        assertTrue(empty.gastarIntentar(0));
        assertEquals(0, empty.actual());
        assertFalse(empty.gastarIntentar(1));
        assertEquals(0, empty.actual());

                // Y la energía nunca pasa del máximo
        Energia lleno = new Energia();
        lleno.restaurar();
        assertEquals(100, lleno.actual());
        assertFalse(lleno.gastarIntentar(101));
        assertEquals(100, lleno.actual());
        assertThrows(IllegalArgumentException.class, () -> Energia.conActual(101));
        assertThrows(IllegalArgumentException.class, () -> Energia.conActual(-1));
    }

    @Test
    void HU08_actionCostsMatchBusinessRuleRn02() {
        assertEquals(2, ReglasJuego.SEMBRAR_COSTO_ENERGIA);
        assertEquals(1, ReglasJuego.AGUA_COSTO_ENERGIA);
        assertEquals(2, ReglasJuego.AGUA_COSTO_ENERGIA_EN_SEQUIA);
        assertEquals(2, ReglasJuego.COSECHAR_COSTO_ENERGIA);
        assertEquals(1, ReglasJuego.COSTO_ENERGIA_LIMPIO);
        assertEquals(1, ReglasJuego.ALIMENTAR_COSTO_ENERGIA_POR_ANIMAL);
        assertEquals(1, ReglasJuego.RECOLECTAR_COSTO_ENERGIA);
        assertEquals(1, ReglasJuego.CURAR_COSTO_ENERGIA);
    }
}
