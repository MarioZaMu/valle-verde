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

    /** Asked before discarding a game in progress. */
    public static final String CONFIRM_DISCARD_GAME =
            "Ya hay una partida en curso. ¿Descartarla y empezar una nueva?";

    /** Game screen: go back to the start screen. */
    public static final String MENU = "Menú";

    /** Game screen: end the day. */
    public static final String END_DAY = "Terminar día";

    /** Game screen: temporary text where the farm map will be drawn. */
    public static final String MAP_PLACEHOLDER = "Aquí aparecerá el mapa de la granja.";

    /** Shown when the Sow tool is used and no seed is chosen. */
    public static final String NO_SEED_SELECTED = "Elige una semilla en la barra o en el inventario (I).";

    /** Title of the inventory dialog. */
    public static final String INVENTORY_TITLE = "Inventario";

    /** Title of the market dialog. */
    public static final String MARKET_TITLE = "Mercado";

    /** Tab to buy. */
    public static final String TAB_BUY = "Comprar";

    /** Tab to sell. */
    public static final String TAB_SELL = "Vender";

    /** Label of the quantity chooser. */
    public static final String QUANTITY = "Cantidad:";

    /** Button that buys. */
    public static final String BUY = "Comprar";

    /** Button that sells. */
    public static final String SELL = "Vender";

    /** Table column: product. */
    public static final String COL_PRODUCT = "Producto";

    /** Table column: price. */
    public static final String COL_PRICE = "Precio";

    /** Table column: units owned. */
    public static final String COL_OWNED = "En inventario";

    /** Table column: trend. */
    public static final String COL_TREND = "Tendencia";

    /** Label of the seed chooser next to the tools. */
    public static final String SEED_LABEL = "Semilla:";

    /** Shown when feeding or collecting on a plot without a building. */
    public static final String NO_BUILDING_HERE = "No hay ningún edificio en esta parcela.";

    /** Title of the construction dialog. */
    public static final String CONSTRUCTION_TITLE = "Construir";

    /** Table column: building. */
    public static final String COL_BUILDING = "Edificio";

    /** Table column: size. */
    public static final String COL_SIZE = "Tamaño";

    /** Table column: cost. */
    public static final String COL_COST = "Costo";

    /** Table column: state of a building offer. */
    public static final String COL_STATE = "Estado";

    /** Button that builds. */
    public static final String BUILD = "Construir aquí";

    /** Title of the building panel. */
    public static final String BUILDING_TITLE = "Edificio";

    /** Table column: animal. */
    public static final String COL_ANIMAL = "Animal";

    /** Table column: days without food. */
    public static final String COL_DAYS_WITHOUT_FEED = "Días sin comer";

    /** Table column: fed today. */
    public static final String COL_FED_TODAY = "Comió hoy";

    /** Table column: units waiting. */
    public static final String COL_PENDING = "Pendiente";

    /** Button that feeds a building. */
    public static final String FEED_BUILDING = "Alimentar";

    /** Button that collects the products of a building. */
    public static final String COLLECT = "Recolectar";

    /** Button that cures the selected animal. */
    public static final String CURE = "Curar";

    /** Title of the summary of the day. */
    public static final String SUMMARY_TITLE = "Resumen del día";

    /** Button that closes a dialog. */
    public static final String CLOSE = "Cerrar";

    /** Yes. */
    public static final String YES = "sí";

    /** No. */
    public static final String NO = "no";

    /** Title of the achievements dialog. */
    public static final String ACHIEVEMENTS_TITLE = "Logros";

    /** Table column: achievement. */
    public static final String COL_ACHIEVEMENT = "Logro";

    /** Table column: what it asks for. */
    public static final String COL_CONDITION = "Condición";

    /** Table column: reward. */
    public static final String COL_REWARD = "Recompensa";

    /** State of an unlocked achievement. */
    public static final String UNLOCKED = "Desbloqueado";

    /** Title of the save slots dialog. */
    public static final String SLOTS_TITLE = "Partidas guardadas";

    /** Button that saves. */
    public static final String SAVE = "Guardar";

    /** Button that loads. */
    public static final String LOAD = "Cargar";

    /** A slot with nothing. */
    public static final String SLOT_EMPTY = "Vacía";

    /** A slot whose document cannot be read. */
    public static final String SLOT_DAMAGED = "Dañada";

    /** Asked before replacing an occupied slot. */
    public static final String CONFIRM_OVERWRITE = "La ranura ya tiene una partida. ¿Reemplazarla?";

    /** Start of the message after saving; the slot number follows. */
    public static final String SAVED_IN_SLOT = "Partida guardada en la ranura ";

    /** Start of the message after loading; the slot number follows. */
    public static final String LOADED_FROM_SLOT = "Partida cargada de la ranura ";

    /** Title of the list shown to choose the slot to load. */
    public static final String CHOOSE_SLOT = "Elige la ranura que quieres cargar";

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

    /** Shown in the status bar when an achievement is unlocked. */
    public static String achievementUnlocked(String name, int reward) {
        return "¡Logro desbloqueado: " + name + "! +" + money(reward);
    }

    /** Label of the check box that switches the visual effects and animations on and off. */
    public static final String EFFECTS = "Efectos";

    /** Notice shown when a new season begins, for example {@code Comienza el Verano.} (HU-39 · CA-4). */
    public static String seasonStarted(String seasonId) {
        String article = "SPRING".equals(seasonId) ? "la" : "el";
        return "Comienza " + article + " " + Names.season(seasonId) + ".";
    }

    /** Size of a building, for example {@code 2 × 2}. */
    public static String size(int width, int height) {
        return width + " × " + height;
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
    /** The trend of a price as a symbol and a word, so colour is never the only cue (HU-16 · CA-4). */
    public static String trend(String trendId) {
        return switch (trendId) {
            case "UP" -> "▲ sube";
            case "DOWN" -> "▼ baja";
            default -> "▬ igual";
        };
    }

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
