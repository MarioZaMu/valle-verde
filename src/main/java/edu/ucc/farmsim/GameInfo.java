package edu.ucc.farmsim;

/**
 * Single source of the visible game name and the application version (RNF-12).
 *
 * <p>The name is provisional: renaming the game means changing {@link #NAME} only.
 * Stories: none (technical setup, phase 5.0).
 */
public final class GameInfo {

    /** Visible name of the game, shown in the window title and the start screen. */
    public static final String NAME = "Valle Verde";

    /** Application version, kept in sync with the Maven project version. */
    public static final String VERSION = "0.1.0";

    private GameInfo() {
        // constants holder
    }
}
