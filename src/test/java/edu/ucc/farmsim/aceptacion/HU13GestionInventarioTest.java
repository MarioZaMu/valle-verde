package edu.ucc.farmsim.aceptacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.dominio.configuracion.ReglasJuego;
import edu.ucc.farmsim.dominio.economia.Inventario;
import edu.ucc.farmsim.dominio.economia.TipoArticulo;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** HU-13: administrar el inventario (24 casillas, un tipo de objeto por casilla, pilas de hasta 99). */
class HU13GestionInventarioTest {

    @Test
    void HU13_ca1_stacksUpTo99PerSlot() {
                // Dado un inventario vacío de 24 casillas
        Inventario inventario = new Inventario(ReglasJuego.RANURAS_BASE_INVENTARIO);

                // Cuando se insertan 120 zanahorias
        boolean insertado = inventario.add(TipoArticulo.CARROT, 120);

                // Entonces ocupan 2 casillas: una de 99 y una de 21
        assertTrue(insertado);
        assertEquals(120, inventario.count(TipoArticulo.CARROT));
        assertEquals(List.of(99, 21), inventario.pilas().stream().map(s -> s.monto()).toList());
    }

    @Test
    void HU13_ca2_insercion_es_atomico_cuando_eso_hace_no_ajustar() {
                // Dada una pila de 98 zanahorias y las otras 23 casillas llenas de otros objetos
        Inventario inventario = new Inventario(ReglasJuego.RANURAS_BASE_INVENTARIO);
        assertTrue(inventario.add(TipoArticulo.MILK, ReglasJuego.PILA_MAXIMA * 10));
        assertTrue(inventario.add(TipoArticulo.HAY, ReglasJuego.PILA_MAXIMA * 13));
        assertTrue(inventario.add(TipoArticulo.CARROT, 98));
        assertEquals(ReglasJuego.RANURAS_BASE_INVENTARIO, inventario.ranurasUsados());

                // Cuando se insertan 3 zanahorias más (solo cabe 1)
        boolean insertado = inventario.add(TipoArticulo.CARROT, 3);

                // Entonces se rechaza y la pila queda en 98: ni siquiera se inserta una zanahoria
        assertFalse(insertado);
        assertEquals(98, inventario.count(TipoArticulo.CARROT));
    }

    @Test
    void HU13_ca3_quitar_toma_fuera_el_monto_y_rechaza_mas_que_existe() {
        // Given 120 carrots
        Inventario inventario = new Inventario(ReglasJuego.RANURAS_BASE_INVENTARIO);
        inventario.add(TipoArticulo.CARROT, 120);

                // Cuando se quitan 30, entonces quedan 90
        assertTrue(inventario.remove(TipoArticulo.CARROT, 30));
        assertEquals(90, inventario.count(TipoArticulo.CARROT));

                // Y quitar 130 se rechaza y deja las 90 intactas
        assertFalse(inventario.remove(TipoArticulo.CARROT, 130));
        assertEquals(90, inventario.count(TipoArticulo.CARROT));
    }

    @Test
    void HU13_ca4_tiene_veinte_cuatro_ranuras_uno_tipo_por_ranura_y_catorce_articulo_tipos() {
        // Given a new standard inventory
        Inventario inventario = Inventario.estandar();

                // Entonces tiene 24 casillas y una pila máxima de 99
        assertEquals(24, inventario.capacidad());
        assertEquals(99, ReglasJuego.PILA_MAXIMA);

                // Y hay 14 tipos de objeto: 5 semillas, 5 cultivos, huevo, leche, heno y medicina
        assertEquals(14, TipoArticulo.values().length);
        assertEquals(5, Arrays.stream(TipoArticulo.values()).filter(t -> t.categoria() == TipoArticulo.Categoria.SEED).count());
        assertEquals(5, Arrays.stream(TipoArticulo.values()).filter(t -> t.categoria() == TipoArticulo.Categoria.CROP).count());

                // Y cada casilla admite un solo tipo: dos tipos nunca comparten casilla
        inventario.add(TipoArticulo.CARROT, 1);
        inventario.add(TipoArticulo.EGG, 1);
        assertEquals(2, inventario.ranurasUsados());
    }

    @Test
    void HU13_no_positivo_montos_son_programacion_errores() {
        Inventario inventario = Inventario.estandar();
        try {
            inventario.add(TipoArticulo.CARROT, 0);
        } catch (IllegalArgumentException esperado) {
            return;
        }
        throw new AssertionError("A non-positive amount must be rejected with IllegalArgumentException");
    }
}
