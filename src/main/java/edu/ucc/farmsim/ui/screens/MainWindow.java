package edu.ucc.farmsim.ui.screens;

import java.awt.Dimension;
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

    private static final long serialVersionUID = 1L;

    /**
     * Creates the window; it is not shown until {@link #setVisible(boolean)} is called.
     *
     * @param title text for the title bar
     */
    public MainWindow(String title) {
        super(title);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        Dimension size = new Dimension(MIN_WIDTH, MIN_HEIGHT);
        setMinimumSize(size);
        setSize(size);
        setLocationRelativeTo(null);
    }
}
