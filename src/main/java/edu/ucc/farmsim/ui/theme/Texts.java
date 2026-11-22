package edu.ucc.farmsim.ui.theme;

/**
 * Every text the player reads on screen, in Spanish. Keeping them here means the screens contain no
 * literals and the language is easy to review (RNF-10).
 *
 * <p>Stories: HU-33.
 */
public final class Texts {

    /** Start screen: begin a new game. */
    public static final String START_NEW_GAME = "Nueva partida";

    /** Start screen: load a saved game. */
    public static final String START_LOAD = "Cargar";

    /** Start screen: leave the application. */
    public static final String START_EXIT = "Salir";

    /** Shown when "Load" is chosen and no slot holds a game. */
    public static final String NO_SAVED_GAMES = "No hay partidas guardadas.";

    /** Shown when "Load" is chosen, a saved game exists, but loading is not implemented yet. */
    public static final String LOAD_NOT_AVAILABLE_YET = "La carga de partidas todavía no está disponible.";

    /** Asked before discarding a game in progress. */
    public static final String CONFIRM_DISCARD_GAME =
            "Ya hay una partida en curso. ¿Descartarla y empezar una nueva?";

    /** Game screen: go back to the start screen. */
    public static final String MENU = "Menú";

    private Texts() {
        // constants holder
    }
}
