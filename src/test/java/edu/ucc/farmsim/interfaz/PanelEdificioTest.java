package edu.ucc.farmsim.interfaz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.DatosJuego;
import edu.ucc.farmsim.dominio.animal.CondicionAnimal;
import edu.ucc.farmsim.dominio.animal.TipoAnimal;
import edu.ucc.farmsim.dominio.economia.TipoArticulo;
import edu.ucc.farmsim.dominio.edificio.Edificio;
import edu.ucc.farmsim.interfaz.componentes.DialogoEdificio;
import edu.ucc.farmsim.interfaz.componentes.PresentadorEdificio;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JTable;
import org.junit.jupiter.api.Test;

/** El panel del edificio: alimentar, recoger y curar desde la tabla de animales (HU-20, HU-22, HU-23). */
class PanelEdificioTest {

    private static Edificio gallineroCon(AplicacionTest aplicacion, CondicionAnimal... condiciones) {
        Edificio gallinero = DatosJuego.gallinero(aplicacion.estado());
        for (CondicionAnimal condicion : condiciones) {
            DatosJuego.casa(gallinero, TipoAnimal.HEN, condicion, condicion == CondicionAnimal.SICK ? 3 : 0, false, 0);
        }
        return gallinero;
    }

    @Test
    void HU20_el_panel_lista_los_animales_y_habilita_alimentacion_solo_cuando_alguien_puede_comer() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        gallineroCon(aplicacion, CondicionAnimal.HEALTHY, CondicionAnimal.SICK);
        PresentadorEdificio presentador = new PresentadorEdificio(aplicacion.controlador, 0);

        assertEquals("Gallinero 1 — 2/4 animales", presentador.titulo());
        assertEquals(2, presentador.animales().size());
        assertEquals("Sana", presentador.fila(presentador.animales().get(0))[1]);
        assertEquals("Enferma", presentador.fila(presentador.animales().get(1))[1]);
        assertTrue(presentador.puedeAlimentar());
        assertFalse(presentador.puedeRecolectar());
        assertFalse(presentador.puedeCurar(0));
        assertTrue(presentador.puedeCurar(1));
        assertFalse(presentador.puedeCurar(5));
    }

    @Test
    void HU20_alimentacion_desde_el_panel_respuestas_el_mensaje_y_deshabilita_el_boton() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        gallineroCon(aplicacion, CondicionAnimal.HEALTHY, CondicionAnimal.HEALTHY);
        aplicacion.estado().inventario().add(TipoArticulo.HAY, 5);
        PresentadorEdificio presentador = new PresentadorEdificio(aplicacion.controlador, 0);

        String mensaje = presentador.alimentar();

        assertEquals("Alimentaste a 2 animal(es).", mensaje);
        assertFalse(presentador.puedeAlimentar());
        assertEquals("No hay animales por alimentar.", presentador.alimentar());
    }

    @Test
    void HU20_aRejectedFeedingShowsTheRn35Message() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        gallineroCon(aplicacion, CondicionAnimal.HEALTHY);

        assertEquals("Heno insuficiente: necesitas 1.", new PresentadorEdificio(aplicacion.controlador, 0).alimentar());
    }

    @Test
    void HU22_collectingFromThePanelMovesTheProductsAndAnEmptyBuildingSaysSo() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        Edificio gallinero = DatosJuego.gallinero(aplicacion.estado());
        DatosJuego.casa(gallinero, TipoAnimal.HEN, CondicionAnimal.HEALTHY, 0, false, 2);
        PresentadorEdificio presentador = new PresentadorEdificio(aplicacion.controlador, 0);
        assertTrue(presentador.puedeRecolectar());

        assertEquals("Recogiste 2 × Huevo.", presentador.collect());

        assertFalse(presentador.puedeRecolectar());
        assertEquals(2, aplicacion.estado().inventario().count(TipoArticulo.EGG));
        assertEquals("No hay productos que recoger.", presentador.collect());
    }

    @Test
    void HU22_aFullInventoryShowsTheRn35Message() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        Edificio gallinero = DatosJuego.gallinero(aplicacion.estado());
        DatosJuego.casa(gallinero, TipoAnimal.HEN, CondicionAnimal.HEALTHY, 0, false, 1);
        DatosJuego.salvoInventarioRellenar(aplicacion.estado(), TipoArticulo.CARROT, 5);

        assertEquals("Inventario lleno: libera espacio o construye un Almacén.",
                new PresentadorEdificio(aplicacion.controlador, 0).collect());
    }

    @Test
    void HU23_curando_desde_el_panel_necesita_medicina_y_hojas_la_gallina_sano() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        gallineroCon(aplicacion, CondicionAnimal.SICK);
        PresentadorEdificio presentador = new PresentadorEdificio(aplicacion.controlador, 0);

        assertEquals("No tienes Medicina.", presentador.curar(0));
        aplicacion.estado().inventario().add(TipoArticulo.MEDICINE, 1);
        assertEquals("El animal se curó.", presentador.curar(0));

        assertFalse(presentador.puedeCurar(0));
        assertEquals("Sana", presentador.fila(presentador.animales().get(0))[1]);
        assertEquals("Este animal no está enfermo.", presentador.curar(0));
    }

    @Test
    void HU23_thePanelOfAMissingBuildingIsEmptyAndNeverThrows() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();

        PresentadorEdificio presentador = new PresentadorEdificio(aplicacion.controlador, 3);

        assertEquals("Edificio", presentador.titulo());
        assertTrue(presentador.animales().isEmpty());
        assertFalse(presentador.puedeAlimentar());
        assertFalse(presentador.puedeRecolectar());
    }

    @Test
    void HU20_el_dialogo_alimenta_recolecta_y_cura_con_su_botones() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        Edificio gallinero = gallineroCon(aplicacion, CondicionAnimal.HEALTHY, CondicionAnimal.SICK);
        aplicacion.estado().inventario().add(TipoArticulo.HAY, 3);
        aplicacion.estado().inventario().add(TipoArticulo.MEDICINE, 1);
        DialogoEdificio dialogo = new DialogoEdificio(null, new PresentadorEdificio(aplicacion.controlador, 0));
        JTable tabla = tablaLlamada(dialogo, "animal-table");
        assertEquals(2, tabla.getRowCount());
        assertTrue(dialogo.isModal());
        assertFalse(botonLlamado(dialogo, "cure-button").isEnabled());
        assertFalse(botonLlamado(dialogo, "collect-button").isEnabled());

        botonLlamado(dialogo, "feed-button").doClick();
        tabla.setRowSelectionInterval(1, 1);
        assertTrue(botonLlamado(dialogo, "cure-button").isEnabled());
        botonLlamado(dialogo, "cure-button").doClick();

        assertTrue(gallinero.animales().get(0).esHoyAlimentado());
        assertEquals(CondicionAnimal.HEALTHY, gallinero.animales().get(1).condicion());
        dialogo.dispose();
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

    private static <T extends Component> void collect(Container contenedor, Class<T> tipo, List<T> encontrado) {
        for (Component hijo : contenedor.getComponents()) {
            if (tipo.isInstance(hijo)) {
                encontrado.add(tipo.cast(hijo));
            }
            if (hijo instanceof Container interior) {
                collect(interior, tipo, encontrado);
            }
        }
    }
}
