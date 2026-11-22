package edu.ucc.farmsim.ui.theme;

import edu.ucc.farmsim.application.result.Names;
import edu.ucc.farmsim.ui.components.Tool;
import java.util.Locale;

/**
 * Every text the player reads on screen, in Spanish. Keeping them here means the screens contain no
 * literals and the language is easy to review (RNF-10).
 *
 * <p>Stories: HU-33, HU-34, HU-36.
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

    /** Game screen: end the day. */
    public static final String END_DAY = "Terminar día";

    /** Game screen: temporary text where the farm map will be drawn. */
    public static final String MAP_PLACEHOLDER = "Aquí aparecerá el mapa de la granja.";

    /** Shown when a tool whose feature does not exist yet is used. */
    public static final String TOOL_NOT_AVAILABLE_YET = "Esta herramienta estará disponible en una fase posterior.";

    /** Shown when a shortcut whose feature does not exist yet is pressed. */
    public static final String SHORTCUT_NOT_AVAILABLE_YET = "Esa función estará disponible en una fase posterior.";

    /** Label of the seed chooser next to the tools. */
    public static final String SEED_LABEL = "Semilla:";

    /** Shown in the top bar when there is no game yet. */
    public static final String NO_VALUE = "—";

    private static final Locale SPANISH = Locale.forLanguageTag("es");

    private Texts() {
        // constants holder
    }

    /** Spanish label of a tool. */
    public static String tool(Tool tool) {
        return switch (tool) {
            case SOW -> "Sembrar";
            case WATER -> "Regar";
            case HARVEST -> "Cosechar";
            case BUILD -> "Construir";
            case FEED -> "Alimentar";
        };
    }

    /** Spanish name of a season id (for example {@code SPRING}); unknown ids are returned unchanged. */
    public static String season(String id) {
        return Names.season(id);
    }

    /** Spanish name of a weather id (for example {@code RAINY}); unknown ids are returned unchanged. */
    public static String weather(String id) {
        return Names.weather(id);
    }

    /** Money with thousands separator and the coin symbol, for example {@code 1.250 m}. */
    public static String money(int amount) {
        return String.format(SPANISH, "%,d m", amount);
    }

    /** The date line, for example {@code Año 1 · Primavera · Día 1}. */
    public static String date(int year, String seasonId, int day) {
        return "Año " + year + " · " + season(seasonId) + " · Día " + day;
    }

    /** The forecast line, for example {@code Mañana: Lluvioso}. */
    public static String forecast(String weatherId) {
        return "Mañana: " + weather(weatherId);
    }

    /** Energy as {@code current/max}. */
    public static String energy(int current, int max) {
        return current + "/" + max;
    }
}
