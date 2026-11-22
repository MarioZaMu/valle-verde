package edu.ucc.farmsim;

import edu.ucc.farmsim.ui.screens.MainWindow;
import java.awt.GraphicsEnvironment;
import javax.swing.SwingUtilities;

/**
 * Composition root: the only place that will create the {@code StateRandom}, the repository, the
 * services, the event bus and the window (phase 5.1 onwards).
 *
 * <p>In phase 5.0 it only opens an empty window, or exits cleanly when there is no display.
 * Stories: none (technical setup, phase 5.0).
 */
public final class Main {

    private static final int EXIT_NO_DISPLAY = 1;

    private Main() {
        // entry point only
    }

    /**
     * Starts the application.
     *
     * @param args command line arguments (unused)
     */
    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println(GameInfo.NAME + " necesita una pantalla para ejecutarse.");
            System.exit(EXIT_NO_DISPLAY);
            return;
        }
        SwingUtilities.invokeLater(() -> new MainWindow(GameInfo.NAME).setVisible(true));
    }
}
