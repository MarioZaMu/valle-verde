package edu.ucc.farmsim.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import edu.ucc.farmsim.application.event.EventBus;
import edu.ucc.farmsim.application.service.GameSession;
import edu.ucc.farmsim.config.ServerApplication;
import edu.ucc.farmsim.domain.time.GameClock;
import edu.ucc.farmsim.support.NoMongoTestConfig;
import edu.ucc.farmsim.support.TestApp;
import edu.ucc.farmsim.ui.render.MapPanel;
import edu.ucc.farmsim.ui.screens.ScreenRouter;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Las pantallas reales de Swing, manejadas por el {@link RemoteGameClient} real, contra el servidor real: lo que ve el
 * jugador después de pulsar los botones es lo mismo que con el controlador en proceso (DT-17, DT-19).
 */
@SpringBootTest(classes = ServerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("no-mongo")
@Import(NoMongoTestConfig.class)
class RemoteScreensIntegrationTest {

    @LocalServerPort
    private int port;
    @Autowired
    private GameSession serverSession;

    @Test
    void HU33_newGameThenSowThenEndDayWorkThroughTheServerAndTheEventsRefreshTheScreens() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        GameClock.getInstance().resetForTests();
        serverSession.replace(null);
        EventBus clientBus = new EventBus();
        RemoteGameClient client = new RemoteGameClient(URI.create("http://127.0.0.1:" + port), clientBus);
        ScreenRouter router = new ScreenRouter(client, clientBus, () -> TestApp.SEED);

                // Cuando el jugador pulsa «Nueva partida»
        button(router, "Nueva partida").doClick();
        flush();

                // Entonces el servidor tiene la partida y la barra superior del cliente la muestra, actualizada por el evento
        // GameStarted
        assertTrue(serverSession.hasGame());
        assertTrue(labels(router).contains("500 m"), labels(router).toString());

                // Cuando el jugador elige la herramienta Sembrar y hace clic en una parcela de tierra
        button(router, "1 Sembrar").doClick();
        MapPanel map = first(router, MapPanel.class);
        map.setSize(1024, 600);
        var cell = map.geometry().cellBounds(5, 3);
        map.handleClick(cell.x + 5, cell.y + 5);
        flush();

                // Entonces el cultivo existe en el servidor y el mapa y la barra de energía del cliente se actualizaron por
        // eventos
        assertTrue(serverSession.current().farm().plotAt(5, 3).hasCrop());
        assertNotNull(map.currentView().plotAt(5, 3).crop());
        assertEquals("98/100", first(router, JProgressBar.class).getString());

                // Cuando el jugador termina el día
        button(router, "Terminar día").doClick();
        flush();

                // Entonces la barra superior muestra el día 2 y la energía vuelve a estar completa
        assertTrue(labels(router).contains("Año 1 · Primavera · Día 2"), labels(router).toString());
        assertEquals("100/100", first(router, JProgressBar.class).getString());
    }

    private static void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }

    private static <T extends Component> T first(Container root, Class<T> type) {
        List<T> found = new ArrayList<>();
        collect(root, type, found);
        assertTrue(!found.isEmpty(), "no " + type.getSimpleName());
        return found.get(0);
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
