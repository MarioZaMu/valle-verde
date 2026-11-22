package edu.ucc.farmsim.ui.screens;

/**
 * What the screen logic needs from the window: switch screens, talk to the player and close the
 * application. The real one uses Swing; tests use a fake, so the presenters need no display.
 *
 * <p>Stories: HU-33.
 */
public interface ScreenNavigator {

    /** Shows the start screen. */
    void showStart();

    /** Shows the game screen. */
    void showGame();

    /** Shows an informative message to the player. */
    void showMessage(String text);

    /** Asks a yes/no question; returns {@code true} if the player accepts. */
    boolean confirm(String text);

    /** Closes the application. */
    void exit();
}
