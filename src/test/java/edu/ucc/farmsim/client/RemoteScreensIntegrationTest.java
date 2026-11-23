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
 * The real Swing screens, driven by the real {@link RemoteGameClient}, against the real server: what the
 * player sees after pressing the buttons is the same as with the in-process controller (DT-17, DT-19).
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

        // When the player presses "Nueva partida"
        button(router, "Nueva partida").doClick();
        flush();

        // Then the server holds the game and the client's top bar shows it, refreshed by the GameStarted event
        assertTrue(serverSession.hasGame());
        assertTrue(labels(router).contains("500 m"), labels(router).toString());

        // When the player picks the Sow tool and clicks a soil plot
        button(router, "1 Sembrar").doClick();
        MapPanel map = first(router, MapPanel.class);
        map.setSize(1024, 600);
        var cell = map.geometry().cellBounds(5, 3);
        map.handleClick(cell.x + 5, cell.y + 5);
        flush();

        // Then the crop exists on the server and the client's map and energy bar were refreshed by events
        assertTrue(serverSession.current().farm().plotAt(5, 3).hasCrop());
        assertNotNull(map.currentView().plotAt(5, 3).crop());
        assertEquals("98/100", first(router, JProgressBar.class).getString());

        // When the player ends the day
        button(router, "Terminar día").doClick();
        flush();

        // Then the top bar shows day 2 and the energy is back to full
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
