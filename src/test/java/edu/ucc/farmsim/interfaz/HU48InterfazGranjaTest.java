package edu.ucc.farmsim.interfaz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.aplicacion.dto.VistaHud;
import edu.ucc.farmsim.aplicacion.evento.BusEvento;
import edu.ucc.farmsim.aplicacion.evento.EventoJuego;
import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.apoyo.DatosJuego;
import edu.ucc.farmsim.interfaz.componentes.BarraHerramienta;
import edu.ucc.farmsim.interfaz.componentes.GrillaInventario;
import edu.ucc.farmsim.interfaz.componentes.Herramienta;
import edu.ucc.farmsim.interfaz.componentes.HudPanel;
import edu.ucc.farmsim.interfaz.componentes.ModeloHerramienta;
import edu.ucc.farmsim.interfaz.componentes.ModeloInventario;
import edu.ucc.farmsim.interfaz.pantallas.NavegadorPantalla;
import edu.ucc.farmsim.interfaz.pantallas.PantallaJuego;
import edu.ucc.farmsim.interfaz.recursos.GestorRecurso;
import edu.ucc.farmsim.interfaz.tema.Textos;
import edu.ucc.farmsim.interfaz.tema.UiTheme;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

/**
 * HU-48: íconos en la barra de herramientas, la barra superior y el inventario, y ningún texto cortado a 1024 x 768.
 */
class HU48InterfazGranjaTest {

    private static <T extends Component> List<T> todo(Container raiz, Class<T> tipo) {
        List<T> encontrado = new ArrayList<>();
        for (Component hijo : raiz.getComponents()) {
            if (tipo.isInstance(hijo)) {
                encontrado.add(tipo.cast(hijo));
            }
            if (hijo instanceof Container contenedor) {
                encontrado.addAll(todo(contenedor, tipo));
            }
        }
        return encontrado;
    }

    private static String descrito(Icon icono) {
        return icono instanceof ImageIcon imagen ? imagen.getDescription() : null;
    }

    @Test
    void HU48_ca1_everyToolHasA32PixelIconItsNameAndItsKey() {
        BarraHerramienta barra = new BarraHerramienta(new ModeloHerramienta());

        List<javax.swing.JToggleButton> botones = todo(barra, javax.swing.JToggleButton.class);
        assertEquals(Herramienta.values().length, botones.size());
        for (Herramienta herramienta : Herramienta.values()) {
            javax.swing.JToggleButton boton = botones.get(herramienta.ordinal());
            assertEquals(32, boton.getIcon().getIconWidth());
            assertEquals(32, boton.getIcon().getIconHeight());
            assertEquals("icons/" + herramienta.name() + ".png", descrito(boton.getIcon()));
            assertTrue(boton.getText().contains(Textos.herramienta(herramienta)));
            assertTrue(boton.getText().contains(String.valueOf(herramienta.clave())));
        }
    }

    @Test
    void HU48_ca2_la_arriba_barra_muestra_iconos_para_dinero_estacion_clima_y_pronostico_y_un_energia_barra_con_texto() throws Exception {
        BusEvento bus = new BusEvento();
        HudPanel hud = new HudPanel(bus);
        bus.publish(new EventoJuego.JuegoIniciado(new VistaHud(500, 1, "SUMMER", 3, "RAINY", "FROST", 80, 100)));
        SwingUtilities.invokeAndWait(() -> { });

        List<String> iconos = todo(hud, JLabel.class).stream().map(l -> descrito(l.getIcon())).toList();
        assertTrue(iconos.contains("icons/COIN.png"), iconos.toString());
        assertTrue(iconos.contains("icons/SUMMER.png"), iconos.toString());
        assertTrue(iconos.contains("icons/RAINY.png"), iconos.toString());
        assertTrue(iconos.contains("icons/FROST.png"), iconos.toString());
        JProgressBar energia = todo(hud, JProgressBar.class).get(0);
        assertEquals("80/100", energia.getString());
        assertTrue(todo(hud, JLabel.class).stream().anyMatch(l -> l.getText().contains("Verano")));
    }

    @Test
    void HU48_ca3_cada_ocupado_ranura_muestra_el_icono_de_su_articulo_y_el_monto() {
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        ModeloInventario modelo = new ModeloInventario(aplicacion.controlador, new ModeloHerramienta());
        modelo.refrescar();
        GrillaInventario grilla = new GrillaInventario(modelo);

        List<AbstractButton> ranuras = todo(grilla, AbstractButton.class);
        AbstractButton first = ranuras.get(0);
        assertEquals("items/" + modelo.celda(0).idArticulo() + ".png", descrito(first.getIcon()));
        assertTrue(first.getText().contains(String.valueOf(modelo.celda(0).monto())));
        AbstractButton empty = ranuras.get(ranuras.size() - 1);
        assertNull(empty.getIcon());
    }

    @Test
    void HU48_ca4_en_el_minimo_ventana_tamanio_no_componente_de_el_juego_pantalla_es_cortar_apagado() {
        UiTheme.instalar();
        AplicacionTest conTutorial = AplicacionTest.create();
        conTutorial.controlador.juegoNuevo(AplicacionTest.SEED);
        for (AplicacionTest aplicacion : List.of(AplicacionTest.juegoIniciado(), conTutorial)) {
            PantallaJuego pantalla = new PantallaJuego(aplicacion.controlador, aplicacion.bus, new NavegadorSilencioso());
            pantalla.setSize(1024, 768);
            todoDisposicion(pantalla);
            cortarNadaAfirmar(pantalla);
        }
    }

    private static boolean mostrado(Component componente) {
        for (Component c = componente; c != null; c = c.getParent()) {
            if (!c.isVisible()) {
                return false;
            }
        }
        return true;
    }

    private static void cortarNadaAfirmar(PantallaJuego pantalla) {
        for (Component hijo : componentesTodo(pantalla)) {
            Container padre = hijo.getParent();
            if (padre == null || !mostrado(hijo) || hijo.getWidth() == 0) {
                continue;
            }
            Rectangle dentro = new Rectangle(0, 0, padre.getWidth(), padre.getHeight());
            assertTrue(dentro.contains(hijo.getBounds()),
                    hijo.getClass().getSimpleName() + " " + hijo.getBounds() + " sticks out of " + dentro);
            if (hijo instanceof AbstractButton || hijo instanceof JLabel) {
                assertTrue(hijo.getWidth() >= hijo.getPreferredSize().width,
                        hijo.getClass().getSimpleName() + " is narrower than its text");
            }
        }
    }

    @Test
    void HU48_ca5_sin_sprites_los_controles_mostrar_solo_texto() {
        BarraHerramienta barra = new BarraHerramienta(new ModeloHerramienta(), GestorRecurso.soloVector());
        for (javax.swing.JToggleButton boton : todo(barra, javax.swing.JToggleButton.class)) {
            assertNull(boton.getIcon());
            assertNotNull(boton.getText());
        }
        AplicacionTest aplicacion = AplicacionTest.juegoIniciado();
        DatosJuego.dineroFijar(aplicacion.estado(), 500);
        ModeloInventario sencillo = new ModeloInventario(aplicacion.controlador, new ModeloHerramienta());
        sencillo.refrescar();
        GrillaInventario grilla = new GrillaInventario(sencillo, GestorRecurso.soloVector());
        assertNull(todo(grilla, AbstractButton.class).get(0).getIcon());
        HudPanel hud = new HudPanel(new BusEvento(), GestorRecurso.soloVector());
        assertTrue(todo(hud, JLabel.class).stream().allMatch(l -> l.getIcon() == null));
    }

    private static void todoDisposicion(Container contenedor) {
        contenedor.doLayout();
        for (Component hijo : contenedor.getComponents()) {
            if (hijo instanceof Container interior) {
                todoDisposicion(interior);
            }
        }
    }

    private static List<Component> componentesTodo(Container raiz) {
        List<Component> encontrado = new ArrayList<>();
        for (Component hijo : raiz.getComponents()) {
            encontrado.add(hijo);
            if (hijo instanceof Container interior) {
                encontrado.addAll(componentesTodo(interior));
            }
        }
        return encontrado;
    }

    private static final class NavegadorSilencioso implements NavegadorPantalla {
        @Override
        public void inicioMostrar() { }

        @Override
        public void juegoMostrar() { }

        @Override
        public void mensajeMostrar(String texto) { }

        @Override
        public boolean confirmar(String texto) {
            return true;
        }

        @Override
        public int elegir(String titulo, List<String> opciones) {
            return 0;
        }

        @Override
        public void exit() { }
    }
}
