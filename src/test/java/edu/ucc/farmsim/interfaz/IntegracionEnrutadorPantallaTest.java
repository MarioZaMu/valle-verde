package edu.ucc.farmsim.interfaz;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import edu.ucc.farmsim.apoyo.AplicacionTest;
import edu.ucc.farmsim.interfaz.dibujo.PanelMapa;
import edu.ucc.farmsim.interfaz.pantallas.EnrutadorPantalla;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Las pantallas reales de Swing conectadas al controlador real: una prueba de humo de HU-33, HU-34 y HU-10 juntas. */
class IntegracionEnrutadorPantallaTest {

    @Test
    void HU33_clicando_nuevo_juego_luego_fin_dia_actualiza_la_arriba_barra_por_eventos() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        AplicacionTest aplicacion = AplicacionTest.create();
        EnrutadorPantalla enrutador = new EnrutadorPantalla(aplicacion.controlador, aplicacion.bus, () -> AplicacionTest.SEED);

                // Cuando el jugador pulsa «Nueva partida» en la pantalla de inicio
        boton(enrutador, "Nueva partida").doClick();
        flush();

                // Entonces la pantalla de juego muestra la barra superior inicial
        assertTrue(etiquetas(enrutador).contains("500 m"), etiquetas(enrutador).toString());
        assertTrue(etiquetas(enrutador).contains("Año 1 · Primavera · Día 1"), etiquetas(enrutador).toString());

                // Cuando el jugador pulsa «Terminar día»
        boton(enrutador, "Terminar día").doClick();
        flush();

                // Entonces la barra superior muestra el día 2
        assertTrue(etiquetas(enrutador).contains("Año 1 · Primavera · Día 2"), etiquetas(enrutador).toString());
    }

    private static edu.ucc.farmsim.aplicacion.controlador.ComandosJuego grabacion(AplicacionTest aplicacion, List<String> llamadas) {
        return (edu.ucc.farmsim.aplicacion.controlador.ComandosJuego) java.lang.reflect.Proxy.newProxyInstance(
                IntegracionEnrutadorPantallaTest.class.getClassLoader(),
                new Class<?>[] {edu.ucc.farmsim.aplicacion.controlador.ComandosJuego.class},
                (proxy, method, args) -> {
                    llamadas.add(method.getName());
                    try {
                        return method.invoke(aplicacion.controlador, args);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }

    @Test
    void RNF02_el_inicio_pantalla_pide_el_servidor_nada_y_el_juego_pantalla_es_construido_una_vez_el_servidor_es_arriba() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        AplicacionTest aplicacion = AplicacionTest.create();
        List<String> llamadas = new ArrayList<>();
        java.util.concurrent.CompletableFuture<Void> listo = new java.util.concurrent.CompletableFuture<>();

        EnrutadorPantalla enrutador = new EnrutadorPantalla(grabacion(aplicacion, llamadas), aplicacion.bus, () -> AplicacionTest.SEED, listo);

        assertTrue(llamadas.isEmpty(), "the start screen opened without waiting for the server: " + llamadas);
        listo.complete(null);
        flush();
        assertFalse(llamadas.isEmpty(), "the game screen was built once the server was up");
        boton(enrutador, "Nueva partida").doClick();
        flush();
        assertTrue(etiquetas(enrutador).contains("500 m"), etiquetas(enrutador).toString());
    }

    @Test
    void RNF02_aClickBeforeTheServerIsUpBuildsTheGameScreenFirstSoTheTopBarSeesTheNewGame() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        AplicacionTest aplicacion = AplicacionTest.create();
        EnrutadorPantalla enrutador = new EnrutadorPantalla(aplicacion.controlador, aplicacion.bus, () -> AplicacionTest.SEED,
                new java.util.concurrent.CompletableFuture<Void>());

        boton(enrutador, "Nueva partida").doClick();
        flush();

        assertTrue(etiquetas(enrutador).contains("500 m"), etiquetas(enrutador).toString());
        assertTrue(etiquetas(enrutador).contains("Año 1 · Primavera · Día 1"), etiquetas(enrutador).toString());
    }

    @Test
    void HU29_theGameScreenShowsASaveButtonNextToEndDayAndMenu() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        AplicacionTest aplicacion = AplicacionTest.create();
        EnrutadorPantalla enrutador = new EnrutadorPantalla(aplicacion.controlador, aplicacion.bus, () -> AplicacionTest.SEED);
        boton(enrutador, "Nueva partida").doClick();
        flush();

        AbstractButton save = boton(enrutador, "Guardar");

        assertTrue(save.isVisible());
        assertNotNull(boton(enrutador, "Terminar día"));
        assertNotNull(boton(enrutador, "Menú"));
    }

    @Test
    void HU36_seleccionando_el_sembrar_herramienta_y_clicando_el_mapa_siembra_y_el_hud_muestra_la_energia() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        AplicacionTest aplicacion = AplicacionTest.create();
        EnrutadorPantalla enrutador = new EnrutadorPantalla(aplicacion.controlador, aplicacion.bus, () -> AplicacionTest.SEED);
        boton(enrutador, "Nueva partida").doClick();
        flush();

                // Cuando el jugador elige la herramienta Sembrar y hace clic en una parcela de tierra del mapa
        boton(enrutador, "1 Sembrar").doClick();
        PanelMapa map = first(enrutador, PanelMapa.class);
        map.setSize(1024, 600);
        var celda = map.geometria().limitesCelda(5, 3);
        map.clicManejar(celda.x + 5, celda.y + 5);
        flush();

                // Entonces el cultivo se planta, la vista del mapa lo muestra y la barra de energía dice 98/100
        assertTrue(aplicacion.estado().granja().parcelaEn(5, 3).tieneCultivo());
        assertNotNull(map.vistaActual().parcelaEn(5, 3).cultivo());
        assertEquals("98/100", first(enrutador, JProgressBar.class).getString());

                // Y el atajo Enter termina el día
        var diaFin = ((edu.ucc.farmsim.interfaz.pantallas.PantallaJuego) first(enrutador, edu.ucc.farmsim.interfaz.pantallas.PantallaJuego.class))
                .getActionMap().get("shortcut-END_DAY");
        diaFin.actionPerformed(new java.awt.event.ActionEvent(enrutador, 0, "enter"));
        flush();
        assertTrue(etiquetas(enrutador).contains("Año 1 · Primavera · Día 2"), etiquetas(enrutador).toString());
        assertEquals("100/100", first(enrutador, JProgressBar.class).getString());
    }

    private static <T extends Component> T first(Container raiz, Class<T> tipo) {
        List<T> encontrado = new ArrayList<>();
        collect(raiz, tipo, encontrado);
        assertTrue(!encontrado.isEmpty(), "no " + tipo.getSimpleName());
        return encontrado.get(0);
    }

    private static void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }

    private static AbstractButton boton(Container raiz, String texto) {
        List<AbstractButton> encontrado = new ArrayList<>();
        collect(raiz, AbstractButton.class, encontrado);
        AbstractButton coincidir = encontrado.stream().filter(b -> texto.equals(b.getText())).findFirst().orElse(null);
        assertNotNull(coincidir, "no button '" + texto + "'");
        return coincidir;
    }

    private static List<String> etiquetas(Container raiz) {
        List<JLabel> encontrado = new ArrayList<>();
        collect(raiz, JLabel.class, encontrado);
        return encontrado.stream().map(JLabel::getText).toList();
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
}
