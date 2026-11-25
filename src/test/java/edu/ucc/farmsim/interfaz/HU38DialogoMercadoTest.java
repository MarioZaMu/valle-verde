package edu.ucc.farmsim.interfaz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.DatosJuego;
import edu.ucc.farmsim.dominio.economia.TipoArticulo;
import edu.ucc.farmsim.interfaz.componentes.DialogoMercado;
import edu.ucc.farmsim.interfaz.componentes.PresentadorMercado;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import org.junit.jupiter.api.Test;

/** HU-38: el mercado con sus pestañas Comprar y Vender (RN-16, RN-17, RN-19). */
class HU38DialogoMercadoTest {

    private static PresentadorMercado presentadorDe(AplicacionTest aplicacion) {
        PresentadorMercado presentador = new PresentadorMercado(aplicacion.controlador);
        presentador.refrescar();
        return presentador;
    }

    @Test
    void HU38_ca2_el_vender_pestania_muestra_el_precio_con_su_tendencia_y_el_pronostico_total() {
                // Dadas 3 zanahorias en Primavera
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        aplicacion.estado().inventario().add(TipoArticulo.CARROT, 3);
        PresentadorMercado presentador = presentadorDe(aplicacion);

        var zanahoria = presentador.vende().stream().filter(o -> o.idArticulo().equals("CARROT")).findFirst().orElseThrow();

                // Entonces el precio es 27 m con una tendencia que tiene un símbolo y una palabra, y 3 unidades prevén 79 m
        assertEquals("27 m ▬ igual", presentador.textoPrecioVender(zanahoria));
        assertEquals("Total previsto: 79 m", presentador.ventaVistaPrevia("CARROT", 3));
    }

    @Test
    void HU38_ca2_theTrendIsAlwaysAWordAndASymbolNotJustAColour() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.demandaFijar(aplicacion.estado(), TipoArticulo.CARROT, 80);
        aplicacion.controlador.diaFin();
        PresentadorMercado presentador = presentadorDe(aplicacion);

        var zanahoria = presentador.vende().stream().filter(o -> o.idArticulo().equals("CARROT")).findFirst().orElseThrow();

        assertTrue(presentador.textoPrecioVender(zanahoria).endsWith("▲ sube"), presentador.textoPrecioVender(zanahoria));
    }

    @Test
    void HU38_ca3_sin_dinero_la_compra_es_bloqueado_con_el_sin_fondos_mensaje() {
        // Given 100 m
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.dineroFijar(aplicacion.estado(), 100);
        PresentadorMercado presentador = presentadorDe(aplicacion);

                // Entonces comprar 3 medicinas (120 m) no se puede hacer y dice por qué; el saldo no cambia
        assertFalse(presentador.puedeComprar("MEDICINE", 3));
        assertEquals("Fondos insuficientes: faltan 20 m.", presentador.bloquearCompra("MEDICINE", 3));
        assertEquals(100, aplicacion.estado().dinero());
        assertTrue(presentador.puedeComprar("MEDICINE", 2));
    }

    @Test
    void HU38_ca4_theQuantityRangeIs1To99WhenBuyingAndUpToTheUnitsOwnedWhenSelling() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        aplicacion.estado().inventario().add(TipoArticulo.CARROT, 3);
        PresentadorMercado presentador = presentadorDe(aplicacion);

        assertEquals(99, presentador.cantidadMaxima(true, "HAY"));
        assertEquals(3, presentador.cantidadMaxima(false, "CARROT"));
        assertEquals(0, presentador.cantidadMaxima(false, "POTATO"));
    }

    @Test
    void HU38_comprando_y_vendiendo_respuesta_los_mensajes_el_jugador_ve() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        aplicacion.estado().inventario().add(TipoArticulo.CARROT, 3);
        PresentadorMercado presentador = presentadorDe(aplicacion);

        assertEquals("Compraste 5 × Semilla de Maíz por 150 m.", presentador.unidadesComprar("CORN_SEED", 5));
        assertEquals("Vendiste 3 × Zanahoria por 79 m.", presentador.unidadesVender("CARROT", 3));
        assertEquals("429 m", presentador.textoDinero());
        assertEquals("La cantidad debe estar entre 1 y 0.", presentador.unidadesVender("CARROT", 1));
    }

    @Test
    void HU38_ca1_theMarketOpensAsAModalDialogWithTheBuyAndSellTabs() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        aplicacion.estado().inventario().add(TipoArticulo.CARROT, 3);

        DialogoMercado dialogo = new DialogoMercado(null, new PresentadorMercado(aplicacion.controlador));

        assertTrue(dialogo.isModal());
        JTabbedPane pestanias = first(dialogo, JTabbedPane.class);
        assertEquals("Comprar", pestanias.getTitleAt(0));
        assertEquals("Vender", pestanias.getTitleAt(1));
        JTable vender = tablaLlamada(dialogo, "sell-table");
        assertEquals(7, vender.getRowCount());
        dialogo.dispose();
    }

    @Test
    void HU38_ca2_selectingARowInTheSellTabForecastsAndSells() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        aplicacion.estado().inventario().add(TipoArticulo.CARROT, 3);
        DialogoMercado dialogo = new DialogoMercado(null, new PresentadorMercado(aplicacion.controlador));
        JTable vender = tablaLlamada(dialogo, "sell-table");
        int fila = indiceDeFila(vender, "Zanahoria");
        vender.setRowSelectionInterval(fila, fila);
        JSpinner cantidad = giradorDe(vender);

        cantidad.setValue(3);
        JButton boton = botonLlamado(dialogo, "sell-button");

        assertTrue(boton.isEnabled());
        boton.doClick();
        assertEquals(579, aplicacion.estado().dinero());
        assertEquals(0, aplicacion.estado().inventario().count(TipoArticulo.CARROT));
        dialogo.dispose();
    }

    @Test
    void HU38_ca3_el_comprar_boton_es_deshabilitado_cuando_el_jugador_no_puede_pagar() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.dineroFijar(aplicacion.estado(), 100);
        DialogoMercado dialogo = new DialogoMercado(null, new PresentadorMercado(aplicacion.controlador));
        JTable comprar = tablaLlamada(dialogo, "buy-table");
        int fila = indiceDeFila(comprar, "Medicina");
        comprar.setRowSelectionInterval(fila, fila);
        JSpinner cantidad = giradorDe(comprar);

        cantidad.setValue(3);

        assertFalse(botonLlamado(dialogo, "buy-button").isEnabled());
        cantidad.setValue(2);
        assertTrue(botonLlamado(dialogo, "buy-button").isEnabled());
        dialogo.dispose();
    }

    private static int indiceDeFila(JTable tabla, String name) {
        for (int i = 0; i < tabla.getRowCount(); i++) {
            if (name.equals(tabla.getValueAt(i, 0))) {
                return i;
            }
        }
        throw new AssertionError("no row " + name);
    }

    private static JSpinner giradorDe(JTable tabla) {
        Container pestania = tabla.getParent().getParent().getParent();
        return first(pestania, JSpinner.class);
    }

    private static JTable tablaLlamada(Container raiz, String name) {
        List<JTable> encontrado = new ArrayList<>();
        collect(raiz, JTable.class, encontrado);
        return encontrado.stream().filter(t -> name.equals(t.getName())).findFirst().orElseThrow();
    }

    private static JButton botonLlamado(Container raiz, String name) {
        List<JButton> encontrado = new ArrayList<>();
        collect(raiz, JButton.class, encontrado);
        return encontrado.stream().filter(b -> name.equals(b.getName())).findFirst().orElseThrow();
    }

    private static <T extends Component> T first(Container raiz, Class<T> tipo) {
        List<T> encontrado = new ArrayList<>();
        collect(raiz, tipo, encontrado);
        return encontrado.get(0);
    }

    private static <T extends Component> void collect(Container contenedor, Class<T> tipo, List<T> encontrado) {
        for (Component hijo : contenedor.getComponents()) {
            if (tipo.isInstance(hijo)) {
                encontrado.add(tipo.cast(hijo));
            }
            if (hijo instanceof Container anidado) {
                collect(anidado, tipo, encontrado);
            }
        }
    }

    @Test
    void HU38_ca3_aCowWithoutMoneyIsBlockedWithTheSinFondosMessage() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.dineroFijar(aplicacion.estado(), 100);
        PresentadorMercado presentador = presentadorDe(aplicacion);

        assertEquals(true, presentador.esAnimal("COW"));
        assertFalse(presentador.puedeComprar("COW", 1));
        assertEquals("Fondos insuficientes: faltan 700 m.", presentador.bloquearCompra("COW", 1));
        assertEquals(1, presentador.cantidadMaxima(true, "COW"));
        assertEquals(100, aplicacion.estado().dinero());
    }

    @Test
    void HU38_theHenCanBeBoughtWithACoopAndIsBlockedWithoutRoom() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        PresentadorMercado presentador = presentadorDe(aplicacion);
        assertFalse(presentador.puedeComprar("HEN", 1));
        assertEquals("No hay espacio para más Gallinas: construye o amplía el edificio.",
                presentador.bloquearCompra("HEN", 1));

        DatosJuego.gallinero(aplicacion.estado());
        presentador.refrescar();

        assertTrue(presentador.puedeComprar("HEN", 1));
        assertEquals("Compraste: Gallina. Vive en el Gallinero 1.", presentador.unidadesComprar("HEN", 1));
        assertEquals(350, aplicacion.estado().dinero());
        assertFalse(presentador.puedeComprar("HEN", 2));
    }
}
