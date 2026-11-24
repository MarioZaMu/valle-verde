package edu.ucc.farmsim.ui.screens;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

/**
 * Ventana principal de la aplicación. En la fase 5.0 es un marco vacío; desde la fase 5.1 aloja el {@code ScreenRouter}
 * que alterna entre la pantalla de inicio y la pantalla de juego.
 *
 * <p>Stories: none (technical setup, phase 5.0).
 * La pantalla de inicio que reemplaza este marco vacío pertenece a una historia posterior.
 */
public final class MainWindow extends JFrame {

    /** Ancho mínimo de la ventana en píxeles (R-07: utilizable desde 1024 x 768). */
    public static final int MIN_WIDTH = 1024;

    /** Alto mínimo de la ventana en píxeles. */
    public static final int MIN_HEIGHT = 768;

    /**
     * Ancho de apertura antes de maximizar la ventana: espacio para el mapa al doble del tamaño de sus sprites de 32
     * píxeles. La ventana abre maximizada y nunca más grande que la parte utilizable de la pantalla.
     */
    public static final int DEFAULT_WIDTH = 1100;

    /**
     * Alto de apertura: el mapa al doble del tamaño (640 px) más la barra superior, la barra de herramientas y los
     * botones.
     */
    public static final int DEFAULT_HEIGHT = 860;

    private static final long serialVersionUID = 1L;

    /**
     * Crea la ventana; no se muestra hasta que se llama a {@link #setVisible(boolean)}.
     *
     * @param title texto para la barra de título
     */
    public MainWindow(String title) {
        super(title);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        Rectangle usable = usableScreen();
        setMinimumSize(new Dimension(Math.min(MIN_WIDTH, usable.width), Math.min(MIN_HEIGHT, usable.height)));
        setSize(Math.min(DEFAULT_WIDTH, usable.width), Math.min(DEFAULT_HEIGHT, usable.height));
        setLocationRelativeTo(null);
        setExtendedState(getExtendedState() | MAXIMIZED_BOTH);
    }

    /**
     * La parte de la pantalla que puede cubrir una ventana (sin la barra de tareas); el tamaño predeterminado si no hay
     * ninguna.
     */
    private static Rectangle usableScreen() {
        if (GraphicsEnvironment.isHeadless()) {
            return new Rectangle(0, 0, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        }
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    }
}
