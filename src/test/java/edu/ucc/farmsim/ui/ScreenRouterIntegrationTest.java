package edu.ucc.farmsim.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import edu.ucc.farmsim.support.TestApp;
import edu.ucc.farmsim.ui.render.MapPanel;
import edu.ucc.farmsim.ui.screens.ScreenRouter;
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
class ScreenRouterIntegrationTest {

    @Test
    void HU33_clickingNewGameThenEndDayUpdatesTheTopBarThroughEvents() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        TestApp app = TestApp.create();
        ScreenRouter router = new ScreenRouter(app.controller, app.bus, () -> TestApp.SEED);

                // Cuando el jugador pulsa «Nueva partida» en la pantalla de inicio
        button(router, "Nueva partida").doClick();
        flush();

                // Entonces la pantalla de juego muestra la barra superior inicial
        assertTrue(labels(router).contains("500 m"), labels(router).toString());
        assertTrue(labels(router).contains("Año 1 · Primavera · Día 1"), labels(router).toString());

                // Cuando el jugador pulsa «Terminar día»
        button(router, "Terminar día").doClick();
        flush();

                // Entonces la barra superior muestra el día 2
        assertTrue(labels(router).contains("Año 1 · Primavera · Día 2"), labels(router).toString());
    }

    private static edu.ucc.farmsim.application.controller.GameCommands recording(TestApp app, List<String> calls) {
        return (edu.ucc.farmsim.application.controller.GameCommands) java.lang.reflect.Proxy.newProxyInstance(
                ScreenRouterIntegrationTest.class.getClassLoader(),
                new Class<?>[] {edu.ucc.farmsim.application.controller.GameCommands.class},
                (proxy, method, args) -> {
                    calls.add(method.getName());
                    try {
                        return method.invoke(app.controller, args);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }

    @Test
    void RNF02_theStartScreenAsksTheServerNothingAndTheGameScreenIsBuiltOnceTheServerIsUp() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        TestApp app = TestApp.create();
        List<String> calls = new ArrayList<>();
        java.util.concurrent.CompletableFuture<Void> ready = new java.util.concurrent.CompletableFuture<>();

        ScreenRouter router = new ScreenRouter(recording(app, calls), app.bus, () -> TestApp.SEED, ready);

        assertTrue(calls.isEmpty(), "the start screen opened without waiting for the server: " + calls);
        ready.complete(null);
        flush();
        assertFalse(calls.isEmpty(), "the game screen was built once the server was up");
        button(router, "Nueva partida").doClick();
        flush();
        assertTrue(labels(router).contains("500 m"), labels(router).toString());
    }

    @Test
    void RNF02_aClickBeforeTheServerIsUpBuildsTheGameScreenFirstSoTheTopBarSeesTheNewGame() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        TestApp app = TestApp.create();
        ScreenRouter router = new ScreenRouter(app.controller, app.bus, () -> TestApp.SEED,
                new java.util.concurrent.CompletableFuture<Void>());

        button(router, "Nueva partida").doClick();
        flush();

        assertTrue(labels(router).contains("500 m"), labels(router).toString());
        assertTrue(labels(router).contains("Año 1 · Primavera · Día 1"), labels(router).toString());
    }

    @Test
    void HU29_theGameScreenShowsASaveButtonNextToEndDayAndMenu() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        TestApp app = TestApp.create();
        ScreenRouter router = new ScreenRouter(app.controller, app.bus, () -> TestApp.SEED);
        button(router, "Nueva partida").doClick();
        flush();

        AbstractButton save = button(router, "Guardar");

        assertTrue(save.isVisible());
        assertNotNull(button(router, "Terminar día"));
        assertNotNull(button(router, "Menú"));
    }

    @Test
    void HU36_selectingTheSowToolAndClickingTheMapSowsAndTheHudShowsTheEnergy() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        TestApp app = TestApp.create();
        ScreenRouter router = new ScreenRouter(app.controller, app.bus, () -> TestApp.SEED);
        button(router, "Nueva partida").doClick();
        flush();

                // Cuando el jugador elige la herramienta Sembrar y hace clic en una parcela de tierra del mapa
        button(router, "1 Sembrar").doClick();
        MapPanel map = first(router, MapPanel.class);
        map.setSize(1024, 600);
        var cell = map.geometry().cellBounds(5, 3);
        map.handleClick(cell.x + 5, cell.y + 5);
        flush();

                // Entonces el cultivo se planta, la vista del mapa lo muestra y la barra de energía dice 98/100
        assertTrue(app.state().farm().plotAt(5, 3).hasCrop());
        assertNotNull(map.currentView().plotAt(5, 3).crop());
        assertEquals("98/100", first(router, JProgressBar.class).getString());

                // Y el atajo Enter termina el día
        var endDay = ((edu.ucc.farmsim.ui.screens.GameScreen) first(router, edu.ucc.farmsim.ui.screens.GameScreen.class))
                .getActionMap().get("shortcut-END_DAY");
        endDay.actionPerformed(new java.awt.event.ActionEvent(router, 0, "enter"));
        flush();
        assertTrue(labels(router).contains("Año 1 · Primavera · Día 2"), labels(router).toString());
        assertEquals("100/100", first(router, JProgressBar.class).getString());
    }

    private static <T extends Component> T first(Container root, Class<T> type) {
        List<T> found = new ArrayList<>();
        collect(root, type, found);
        assertTrue(!found.isEmpty(), "no " + type.getSimpleName());
        return found.get(0);
    }

    private static void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }

    private static AbstractButton button(Container root, String text) {
        List<AbstractButton> found = new ArrayList<>();
        collect(root, AbstractButton.class, found);
        AbstractButton match = found.stream().filter(b -> text.equals(b.getText())).findFirst().orElse(null);
        assertNotNull(match, "no button '" + text + "'");
        return match;
    }

    private static List<String> labels(Container root) {
        List<JLabel> found = new ArrayList<>();
        collect(root, JLabel.class, found);
        return found.stream().map(JLabel::getText).toList();
    }

    private static <T extends Component> void collect(Container container, Class<T> type, List<T> found) {
        for (Component child : container.getComponents()) {
            if (type.isInstance(child)) {
                found.add(type.cast(child));
            }
            if (child instanceof Container nested) {
                collect(nested, type, found);
            }
        }
    }
}
