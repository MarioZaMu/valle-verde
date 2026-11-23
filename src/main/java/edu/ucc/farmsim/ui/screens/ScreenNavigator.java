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

    /**
     * Makes sure the game screen exists before the first order of a game is sent, so it hears the events of that
     * order (RNF-02: it may still be waiting for the server to start). Nothing to do by default.
     */
    default void prepareGame() {
        // the game screen already exists
    }

    /** Shows the game screen. */
    void showGame();

    /** Shows an informative message to the player. */
    void showMessage(String text);

    /** Asks a yes/no question; returns {@code true} if the player accepts. */
    boolean confirm(String text);

    /**
     * Asks the player to pick one of the lines (the save slots).
     *
     * @param title   what the player is choosing
     * @param options the lines, one per slot, in order
     * @return the number of the line chosen, from 1, or 0 if the player cancelled
     */
    int choose(String title, java.util.List<String> options);

    /** Closes the application. */
    void exit();
}
