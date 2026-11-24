package edu.ucc.farmsim.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import edu.ucc.farmsim.GameInfo;
import edu.ucc.farmsim.ui.screens.MainWindow;
import java.awt.GraphicsEnvironment;
import org.junit.jupiter.api.Test;

/** Prueba de humo de la ventana principal vacía (R-07: utilizable desde 1024 x 768). Se omite sin pantalla. */
class MainWindowTest {

    @Test
    void R07_windowHasTheGameTitleAndTheMinimumSize() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        MainWindow window = new MainWindow(GameInfo.NAME);
        try {
            assertEquals(GameInfo.NAME, window.getTitle());
            assertEquals(MainWindow.MIN_WIDTH, window.getMinimumSize().width);
            assertEquals(MainWindow.MIN_HEIGHT, window.getMinimumSize().height);
        } finally {
            window.dispose();
        }
    }
}
