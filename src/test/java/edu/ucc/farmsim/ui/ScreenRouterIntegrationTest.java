package edu.ucc.farmsim.ui;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import edu.ucc.farmsim.support.TestApp;
import edu.ucc.farmsim.ui.screens.ScreenRouter;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

/** The real Swing screens wired to the real controller: a smoke test of HU-33, HU-34 and HU-10 together. */
class ScreenRouterIntegrationTest {

    @Test
    void HU33_clickingNewGameThenEndDayUpdatesTheTopBarThroughEvents() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        TestApp app = TestApp.create();
        ScreenRouter router = new ScreenRouter(app.controller, app.bus, () -> TestApp.SEED);

        // When the player presses "Nueva partida" on the start screen
        button(router, "Nueva partida").doClick();
        flush();

        // Then the game screen shows the initial top bar
        assertTrue(labels(router).contains("500 m"), labels(router).toString());
        assertTrue(labels(router).contains("Año 1 · Primavera · Día 1"), labels(router).toString());

        // When the player presses "Terminar día"
        button(router, "Terminar día").doClick();
        flush();

        // Then the top bar shows day 2
        assertTrue(labels(router).contains("Año 1 · Primavera · Día 2"), labels(router).toString());
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
