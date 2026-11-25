package edu.ucc.farmsim.interfaz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import edu.ucc.farmsim.aplicacion.resultado.Resultado;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.DatosJuego;
import edu.ucc.farmsim.apoyo.DialogosGrabacion;
import edu.ucc.farmsim.dominio.economia.TipoArticulo;
import edu.ucc.farmsim.interfaz.componentes.Atajo;
import edu.ucc.farmsim.interfaz.componentes.GrillaInventario;
import edu.ucc.farmsim.interfaz.componentes.ModeloHerramienta;
import edu.ucc.farmsim.interfaz.componentes.ModeloInventario;
import edu.ucc.farmsim.interfaz.pantallas.DialogosJuego;
import edu.ucc.farmsim.interfaz.pantallas.PresentadorPantallaJuego;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JToggleButton;
import org.junit.jupiter.api.Test;

/** HU-37: el inventario como una grilla de casillas, con la elección de semilla (RN-15, RN-34). */
class HU37GrillaInventarioTest {

    private static ModeloInventario modeloDe(AplicacionTest aplicacion, ModeloHerramienta herramientas) {
        ModeloInventario modelo = new ModeloInventario(aplicacion.controlador, herramientas);
        aplicacion.bus.suscribir(modelo);
        modelo.refrescar();
        return modelo;
    }

    @Test
    void HU37_ca1_aNewGameShows24SlotsWithCarrotAndPotatoSeedsAndTheRestEmpty() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();

        ModeloInventario modelo = modeloDe(aplicacion, new ModeloHerramienta());

        assertEquals(24, modelo.conteoRanura());
        assertEquals("Semilla de Zanahoria", modelo.celda(0).name());
        assertEquals(10, modelo.celda(0).monto());
        assertEquals(5, modelo.celda(1).monto());
        for (int i = 2; i < 24; i++) {
            assertTrue(modelo.celda(i).isEmpty(), "slot " + i);
        }
    }

    @Test
    void HU37_ca1_la_grilla_dibuja_uno_boton_por_ranura() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();

        GrillaInventario grilla = new GrillaInventario(modeloDe(aplicacion, new ModeloHerramienta()));

        assertEquals(24, grilla.getComponentCount());
        assertTrue(((JToggleButton) grilla.getComponent(0)).getText().contains("10"));
    }

    @Test
    void HU37_ca2_aWarehouseGivesAGridOf36Slots() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
                // (la vista del inventario informa la capacidad que tenga la partida)
        var inventario = new edu.ucc.farmsim.dominio.economia.Inventario(36);
        inventario.add(TipoArticulo.CARROT_SEED, 1);
        aplicacion.estado().inventario().restaurarDesde(inventario.instantanea());

        ModeloInventario modelo = modeloDe(aplicacion, new ModeloHerramienta());

        assertEquals(36, modelo.conteoRanura());
    }

    @Test
    void HU37_ca3_clickingASeedChoosesItForTheSowToolAndAnEmptyAmountClearsTheChoice() {
                // Dada la semilla de zanahoria predeterminada elegida
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        ModeloHerramienta herramientas = new ModeloHerramienta();
        ModeloInventario modelo = modeloDe(aplicacion, herramientas);
        assertTrue(modelo.isSelected(0));

                // Cuando el jugador hace clic en la casilla de la papa
        modelo.seleccionar(1);

                // Entonces la papa queda elegida y marcada
        assertEquals("POTATO_SEED", herramientas.semillaSeleccionada());
        assertTrue(modelo.isSelected(1));
        assertFalse(modelo.isSelected(0));

                // Y cuando se siembra la última semilla de papa, la elección se borra
        aplicacion.estado().inventario().remove(TipoArticulo.POTATO_SEED, 5);
        aplicacion.bus.publish(new edu.ucc.farmsim.aplicacion.evento.EventoJuego.InventarioCambiado());
        assertNull(herramientas.semillaSeleccionada());
    }

    @Test
    void HU37_ca3_siembra_la_ultimo_semilla_por_el_juego_limpia_la_eleccion_por_medio_eventos() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        ModeloHerramienta herramientas = new ModeloHerramienta();
        modeloDe(aplicacion, herramientas);
        aplicacion.estado().inventario().remove(TipoArticulo.CARROT_SEED, 9);

        Resultado<?> sembrado = aplicacion.controlador.plantar(DatosJuego.COL_TIERRA, DatosJuego.FILA_TIERRA, "CARROT_SEED");

        assertTrue(sembrado.isOk());
        assertNull(herramientas.semillaSeleccionada());
    }

    @Test
    void HU37_ca3_sowingWithoutAChosenSeedAsksForOne() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        ModeloHerramienta herramientas = new ModeloHerramienta();
        herramientas.semillaLimpiar();
        herramientas.seleccionar(edu.ucc.farmsim.interfaz.componentes.Herramienta.SOW);
        List<String> mensajes = new ArrayList<>();
        PresentadorPantallaJuego presentador = new PresentadorPantallaJuego(aplicacion.controlador, herramientas, mensajes::add, noDialogos());

        presentador.onParcelaClicada(DatosJuego.COL_TIERRA, DatosJuego.FILA_TIERRA);

        assertEquals(List.of(edu.ucc.farmsim.interfaz.tema.Textos.NO_SEMILLA_SELECCIONADA), mensajes);
        assertEquals(10, aplicacion.estado().inventario().count(TipoArticulo.CARROT_SEED));
    }

    @Test
    void HU37_ca4_aStackOf99ShowsNinetyNineAndTheNextUnitUsesAnotherSlot() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        aplicacion.estado().inventario().add(TipoArticulo.CARROT, 100);

        ModeloInventario modelo = modeloDe(aplicacion, new ModeloHerramienta());

        assertEquals(99, modelo.celda(2).monto());
        assertEquals(1, modelo.celda(3).monto());
        assertEquals(modelo.celda(2).idArticulo(), modelo.celda(3).idArticulo());
    }

    @Test
    void HU37_theInventoryShortcutOpensTheGridAndClickingAHarvestDoesNotChooseASeed() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        ModeloHerramienta herramientas = new ModeloHerramienta();
        DialogosGrabacion dialogos = new DialogosGrabacion();
        List<String> abierto = dialogos.abierto;
        PresentadorPantallaJuego presentador = new PresentadorPantallaJuego(aplicacion.controlador, herramientas, m -> { }, dialogos);
        aplicacion.estado().inventario().add(TipoArticulo.CARROT, 1);
        ModeloInventario modelo = modeloDe(aplicacion, herramientas);

        presentador.onAtajo(Atajo.INVENTORY);
        presentador.onAtajo(Atajo.MARKET);
        modelo.seleccionar(2);

        assertEquals(List.of("inventario", "mercado"), abierto);
        assertEquals("CARROT_SEED", herramientas.semillaSeleccionada());
    }

    private static DialogosJuego noDialogos() {
        return new DialogosGrabacion();
    }

    @Test
    void HU37_ca2_edificio_el_almacen_ensancha_la_grilla_a_treinta_seis_ranuras_por_el_evento() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        edu.ucc.farmsim.apoyo.DatosJuego.desbloquear(aplicacion.estado(), edu.ucc.farmsim.dominio.progreso.Logro.FIRST_HARVEST);
        ModeloInventario modelo = modeloDe(aplicacion, new ModeloHerramienta());
        assertEquals(24, modelo.conteoRanura());

        aplicacion.controlador.build("WAREHOUSE", 2, 7);

        assertEquals(36, modelo.conteoRanura());
    }
}
