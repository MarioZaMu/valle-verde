package edu.ucc.farmsim.ui.screens;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

/**
 * Top-level application window. In phase 5.0 it is an empty frame; from phase 5.1 it hosts the
 * {@code ScreenRouter} that switches between the start screen and the game screen.
 *
 * <p>Stories: none (technical setup, phase 5.0).
 * The start screen that replaces this empty frame belongs to a later story.
 */
public final class MainWindow extends JFrame {

    /** Minimum window width in pixels (R-07: usable from 1024 x 768). */
    public static final int MIN_WIDTH = 1024;

    /** Minimum window height in pixels. */
    public static final int MIN_HEIGHT = 768;

    /**
     * Opening width before the window is maximized: room for the map at twice the size of its 32-pixel sprites.
     * The window opens maximized and never larger than the usable part of the screen.
     */
    public static final int DEFAULT_WIDTH = 1100;

    /** Opening height: the map at twice the size (640 px) plus the top bar, tool bar and buttons. */
    public static final int DEFAULT_HEIGHT = 860;

    private static final long serialVersionUID = 1L;

    /**
     * Creates the window; it is not shown until {@link #setVisible(boolean)} is called.
     *
     * @param title text for the title bar
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

    /** The part of the screen a window may cover (without the task bar); the default size if there is none. */
    private static Rectangle usableScreen() {
        if (GraphicsEnvironment.isHeadless()) {
            return new Rectangle(0, 0, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        }
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    }
}
