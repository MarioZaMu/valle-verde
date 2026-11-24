package edu.ucc.farmsim.ui.theme;

import edu.ucc.farmsim.application.result.Names;
import edu.ucc.farmsim.ui.components.Tool;
import java.util.Locale;

/**
 * Todo texto que el jugador lee en pantalla, en español. Tenerlos aquí significa que las pantallas no contienen
 * literales y que el idioma es fácil de revisar (RNF-10).
 *
 * <p>Stories: HU-33, HU-34, HU-36, HU-45, HU-46, HU-47, HU-49.
 */
public final class Texts {

    /** Pantalla de inicio: empezar una partida nueva. */
    public static final String START_NEW_GAME = "Nueva partida";

    /** Pantalla de inicio: cargar una partida guardada. */
    public static final String START_LOAD = "Cargar";

    /** Pantalla de inicio: salir de la aplicación. */
    public static final String START_EXIT = "Salir";

    /** Línea bajo el nombre del juego en la pantalla de inicio. */
    public static final String START_SUBTITLE = "Siembra, cuida tus animales y haz crecer tu granja";

    /** Se muestra cuando se elige «Cargar» y ninguna ranura contiene una partida. */
    public static final String NO_SAVED_GAMES = "No hay partidas guardadas.";

    /** Se pregunta antes de descartar una partida en curso. */
    public static final String CONFIRM_DISCARD_GAME =
            "Ya hay una partida en curso. ¿Descartarla y empezar una nueva?";

    /** Pantalla de juego: volver a la pantalla de inicio. */
    public static final String MENU = "Menú";

    /** Pantalla de juego: terminar el día. */
    public static final String END_DAY = "Terminar día";

    /** Pantalla de juego: texto temporal donde se dibujará el mapa de la granja. */
    public static final String MAP_PLACEHOLDER = "Aquí aparecerá el mapa de la granja.";

    /** Se muestra cuando se usa la herramienta Sembrar y no hay semilla elegida. */
    public static final String NO_SEED_SELECTED = "Elige una semilla en la barra o en el inventario (I).";

    /** Título del diálogo del inventario. */
    public static final String INVENTORY_TITLE = "Inventario";

    /** Título del diálogo del mercado. */
    public static final String MARKET_TITLE = "Mercado";

    /** Pestaña para comprar. */
    public static final String TAB_BUY = "Comprar";

    /** Pestaña para vender. */
    public static final String TAB_SELL = "Vender";

    /** Etiqueta del selector de cantidad. */
    public static final String QUANTITY = "Cantidad:";

    /** Botón que compra. */
    public static final String BUY = "Comprar";

    /** Botón que vende. */
    public static final String SELL = "Vender";

    /** Columna de la tabla: producto. */
    public static final String COL_PRODUCT = "Producto";

    /** Columna de la tabla: precio. */
    public static final String COL_PRICE = "Precio";

    /** Columna de la tabla: unidades que se tienen. */
    public static final String COL_OWNED = "En inventario";

    /** Columna de la tabla: tendencia. */
    public static final String COL_TREND = "Tendencia";

    /** Etiqueta del selector de semilla junto a las herramientas. */
    public static final String SEED_LABEL = "Semilla:";

    /** Se muestra al alimentar o recoger en una parcela sin edificio. */
    public static final String NO_BUILDING_HERE = "No hay ningún edificio en esta parcela.";

    /** Se muestra cuando se pulsa E y nadie está junto al avatar. */
    public static final String NOBODY_TO_TALK = "No hay nadie cerca para hablar: acércate a alguien.";

    /** Botón que entrega un encargo. */
    public static final String DELIVER = "Entregar";

    /** Se muestra en el diálogo después de entregar un encargo. */
    public static final String ERRAND_DELIVERED = "¡Encargo entregado!";

    /** Aviso de un encargo entregado. */
    public static String errandCompleted(String name, int reward) {
        return "Encargo de " + name + " cumplido: +" + money(reward);
    }

    /** Botón del cuadro del avatar que termina el tutorial de inicio. */
    public static final String SKIP_TUTORIAL = "Saltar tutorial";

    /** Dónde va el jugador en el tutorial de inicio, por ejemplo «Paso 2 de 5». */
    public static String tutorialStep(int number, int total) {
        return "Paso " + number + " de " + total;
    }

    /** Se muestra cuando se pulsa Espacio y el avatar mira fuera de la granja. */
    public static final String NOTHING_IN_FRONT = "No hay ninguna parcela frente a ti.";

    /** Título del diálogo de construcción. */
    public static final String CONSTRUCTION_TITLE = "Construir";

    /** Columna de la tabla: edificio. */
    public static final String COL_BUILDING = "Edificio";

    /** Columna de la tabla: tamaño. */
    public static final String COL_SIZE = "Tamaño";

    /** Columna de la tabla: costo. */
    public static final String COL_COST = "Costo";

    /** Columna de la tabla: estado de una oferta de edificio. */
    public static final String COL_STATE = "Estado";

    /** Botón que construye. */
    public static final String BUILD = "Construir aquí";

    /** Título del panel del edificio. */
    public static final String BUILDING_TITLE = "Edificio";

    /** Columna de la tabla: animal. */
    public static final String COL_ANIMAL = "Animal";

    /** Columna de la tabla: días sin comida. */
    public static final String COL_DAYS_WITHOUT_FEED = "Días sin comer";

    /** Columna de la tabla: comió hoy. */
    public static final String COL_FED_TODAY = "Comió hoy";

    /** Columna de la tabla: unidades esperando. */
    public static final String COL_PENDING = "Pendiente";

    /** Botón que alimenta un edificio. */
    public static final String FEED_BUILDING = "Alimentar";

    /** Botón que recoge los productos de un edificio. */
    public static final String COLLECT = "Recolectar";

    /** Botón que cura al animal seleccionado. */
    public static final String CURE = "Curar";

    /** Título del resumen del día. */
    public static final String SUMMARY_TITLE = "Resumen del día";

    /** Botón que cierra un diálogo. */
    public static final String CLOSE = "Cerrar";

    /** Sí. */
    public static final String YES = "sí";

    /** No. */
    public static final String NO = "no";

    /** Título del diálogo de logros. */
    public static final String ACHIEVEMENTS_TITLE = "Logros";

    /** Columna de la tabla: logro. */
    public static final String COL_ACHIEVEMENT = "Logro";

    /** Columna de la tabla: lo que pide. */
    public static final String COL_CONDITION = "Condición";

    /** Columna de la tabla: recompensa. */
    public static final String COL_REWARD = "Recompensa";

    /** Estado de un logro desbloqueado. */
    public static final String UNLOCKED = "Desbloqueado";

    /** Título del diálogo de ranuras de guardado. */
    public static final String SLOTS_TITLE = "Partidas guardadas";

    /** Botón que guarda. */
    public static final String SAVE = "Guardar";

    /** Tooltip del botón Guardar de la pantalla de juego, con sus teclas rápidas. */
    public static final String SAVE_HINT = "Guardar la partida en una ranura (F5 guarda rápido, F9 carga)";

    /** Botón que carga. */
    public static final String LOAD = "Cargar";

    /** Una ranura sin nada. */
    public static final String SLOT_EMPTY = "Vacía";

    /** Una ranura cuyo documento no se puede leer. */
    public static final String SLOT_DAMAGED = "Dañada";

    /** Se pregunta antes de reemplazar una ranura ocupada. */
    public static final String CONFIRM_OVERWRITE = "La ranura ya tiene una partida. ¿Reemplazarla?";

    /** Inicio del mensaje después de guardar; sigue el número de ranura. */
    public static final String SAVED_IN_SLOT = "Partida guardada en la ranura ";

    /** Inicio del mensaje después de cargar; sigue el número de ranura. */
    public static final String LOADED_FROM_SLOT = "Partida cargada de la ranura ";

    /** Título de la lista que se muestra para elegir la ranura que se carga. */
    public static final String CHOOSE_SLOT = "Elige la ranura que quieres cargar";

    /** Se muestra en la barra superior cuando todavía no hay partida. */
    public static final String NO_VALUE = "—";

    private static final Locale SPANISH = Locale.forLanguageTag("es");

    private Texts() {
                // contenedor de constantes
    }

    /** Etiqueta en español de una herramienta. */
    public static String tool(Tool tool) {
        return switch (tool) {
            case SOW -> "Sembrar";
            case WATER -> "Regar";
            case HARVEST -> "Cosechar";
            case BUILD -> "Construir";
            case FEED -> "Alimentar";
        };
    }

    /** Se muestra en la barra de estado cuando se desbloquea un logro. */
    public static String achievementUnlocked(String name, int reward) {
        return "¡Logro desbloqueado: " + name + "! +" + money(reward);
    }

    /** Etiqueta de la casilla que activa y desactiva los efectos visuales y las animaciones. */
    public static final String EFFECTS = "Efectos";

    /**
     * Aviso que se muestra cuando empieza una estación nueva, por ejemplo {@code Comienza el Verano.} (HU-39 · CA-4).
     */
    public static String seasonStarted(String seasonId) {
        String article = "SPRING".equals(seasonId) ? "la" : "el";
        return "Comienza " + article + " " + Names.season(seasonId) + ".";
    }

    /** Tamaño de un edificio, por ejemplo {@code 2 × 2}. */
    public static String size(int width, int height) {
        return width + " × " + height;
    }

    /**
     * Nombre en español de un id de estación (por ejemplo {@code SPRING}); los ids desconocidos se devuelven sin
     * cambios.
     */
    public static String season(String id) {
        return Names.season(id);
    }

    /**
     * Nombre en español de un id de clima (por ejemplo {@code RAINY}); los ids desconocidos se devuelven sin cambios.
     */
    public static String weather(String id) {
        return Names.weather(id);
    }

    /** Dinero con separador de miles y el símbolo de moneda, por ejemplo {@code 1.250 m}. */
    /**
     * La tendencia de un precio como un símbolo y una palabra, de modo que el color nunca sea la única señal (HU-16 ·
     * CA-4).
     */
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

    /** La línea de la fecha, por ejemplo {@code Año 1 · Primavera · Día 1}. */
    public static String date(int year, String seasonId, int day) {
        return "Año " + year + " · " + season(seasonId) + " · Día " + day;
    }

    /** La línea del pronóstico, por ejemplo {@code Mañana: Lluvioso}. */
    public static String forecast(String weatherId) {
        return "Mañana: " + weather(weatherId);
    }

    /** Energía como {@code actual/máxima}. */
    public static String energy(int current, int max) {
        return current + "/" + max;
    }
}
