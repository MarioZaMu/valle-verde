package edu.ucc.farmsim.aceptacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.aplicacion.resultado.CodigoError;
import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.DatosJuego;
import edu.ucc.farmsim.dominio.economia.TipoArticulo;
import org.junit.jupiter.api.Test;

/** HU-14: comprar semillas, heno y medicina a precios fijos (RN-14, RN-15, RN-16). */
class HU14ComprarTest {

    @Test
    void HU14_ca1_buyingFiveCornSeedsCosts150() {
        // Given 500 m
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();

                // Cuando el jugador compra 5 semillas de maíz (30 m cada una)
        Resultado<Integer> resultado = aplicacion.controlador.comprar("CORN_SEED", 5);

                // Entonces el saldo es 350 m y hay 5 semillas más
        assertTrue(resultado.isOk(), String.valueOf(resultado.mensaje()));
        assertEquals(150, resultado.value());
        assertEquals(350, aplicacion.estado().dinero());
        assertEquals(5, aplicacion.estado().inventario().count(TipoArticulo.CORN_SEED));
    }

    @Test
    void HU14_ca2_no_suficiente_dinero_es_rechazado_con_el_faltante_monto_y_nada_cambia() {
        // Given 100 m
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.dineroFijar(aplicacion.estado(), 100);

                // Cuando el jugador intenta comprar 3 medicinas (120 m)
        Resultado<Integer> resultado = aplicacion.controlador.comprar("MEDICINE", 3);

                // Entonces se rechaza: faltan 20 m
        assertFalse(resultado.isOk());
        assertEquals(CodigoError.SIN_FONDOS, resultado.codigo());
        assertEquals("Fondos insuficientes: faltan 20 m.", resultado.mensaje());
        assertEquals(100, aplicacion.estado().dinero());
        assertEquals(0, aplicacion.estado().inventario().count(TipoArticulo.MEDICINE));
        assertTrue(aplicacion.eventos.isEmpty());
    }

    @Test
    void HU14_ca3_sin_espacio_en_el_inventario_el_dinero_es_no_tomado() {
                // Dado un inventario sin lugar para 10 de heno
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.salvoInventarioRellenar(aplicacion.estado(), TipoArticulo.CARROT, 5);

                // Cuando el jugador intenta comprarlos
        Resultado<Integer> resultado = aplicacion.controlador.comprar("MEDICINE", 10);

                // Entonces se rechaza y el saldo es el mismo
        assertEquals(CodigoError.INVENTARIO_LLENO, resultado.codigo());
        assertEquals(500, aplicacion.estado().dinero());
    }

    @Test
    void HU14_ca4_theQuantityMustBeBetween1And99() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();

        Resultado<Integer> cero = aplicacion.controlador.comprar("HAY", 0);
        Resultado<Integer> cien = aplicacion.controlador.comprar("HAY", 100);

        assertEquals(CodigoError.CANTIDAD_INVALIDA, cero.codigo());
        assertEquals("La cantidad debe estar entre 1 y 99.", cero.mensaje());
        assertEquals(CodigoError.CANTIDAD_INVALIDA, cien.codigo());
        assertEquals(500, aplicacion.estado().dinero());
    }

    @Test
    void HU14_ca4_calabaza_semillas_puede_ser_compro_en_primavera_incluso_aunque_ellos_no_puede_ser_sembrado_luego() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();

        Resultado<Integer> resultado = aplicacion.controlador.comprar("PUMPKIN_SEED", 1);

        assertTrue(resultado.isOk());
        assertEquals(40, resultado.value());
        assertEquals(1, aplicacion.estado().inventario().count(TipoArticulo.PUMPKIN_SEED));
    }

    @Test
    void HU14_everyCatalogPriceMatchesRn16() {
        assertEquals(10, edu.ucc.farmsim.dominio.economia.Mercado.precioComprar(TipoArticulo.CARROT_SEED));
        assertEquals(20, edu.ucc.farmsim.dominio.economia.Mercado.precioComprar(TipoArticulo.POTATO_SEED));
        assertEquals(25, edu.ucc.farmsim.dominio.economia.Mercado.precioComprar(TipoArticulo.TOMATO_SEED));
        assertEquals(30, edu.ucc.farmsim.dominio.economia.Mercado.precioComprar(TipoArticulo.CORN_SEED));
        assertEquals(40, edu.ucc.farmsim.dominio.economia.Mercado.precioComprar(TipoArticulo.PUMPKIN_SEED));
        assertEquals(5, edu.ucc.farmsim.dominio.economia.Mercado.precioComprar(TipoArticulo.HAY));
        assertEquals(40, edu.ucc.farmsim.dominio.economia.Mercado.precioComprar(TipoArticulo.MEDICINE));
    }

    @Test
    void HU14_comprando_hace_no_cambiar_la_demanda_de_cualquier_producto() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();

        aplicacion.controlador.comprar("CARROT_SEED", 5);

        assertEquals(100, aplicacion.estado().mercado().demandaDe(TipoArticulo.CARROT));
    }
}
