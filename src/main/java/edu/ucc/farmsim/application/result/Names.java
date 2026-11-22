package edu.ucc.farmsim.application.result;

import java.util.Map;

/**
 * Spanish display names for the ids the application hands to the UI (seasons, weather, crops, growth stages),
 * so messages and screens always use the same words. Unknown ids are returned unchanged.
 *
 * <p>Stories: HU-01, HU-07.
 */
public final class Names {

    private static final Map<String, String> SEASONS = Map.of(
            "SPRING", "Primavera", "SUMMER", "Verano", "AUTUMN", "Otoño", "WINTER", "Invierno");
    private static final Map<String, String> WEATHERS = Map.of(
            "SUNNY", "Soleado", "CLOUDY", "Nublado", "RAINY", "Lluvioso",
            "DROUGHT", "Sequía", "FROST", "Helada", "SNOWY", "Nevado");
    private static final Map<String, String> CROPS = Map.of(
            "CARROT", "Zanahoria", "POTATO", "Papa", "TOMATO", "Tomate", "CORN", "Maíz", "PUMPKIN", "Calabaza");
    private static final Map<String, String> STAGES = Map.of(
            "SEED", "Semilla", "SPROUT", "Brote", "GROWING", "Crecimiento", "MATURE", "Maduro", "WITHERED", "Marchito");

    private static final Map<String, String> ITEMS = Map.ofEntries(
            Map.entry("CARROT_SEED", "Semilla de Zanahoria"), Map.entry("POTATO_SEED", "Semilla de Papa"),
            Map.entry("TOMATO_SEED", "Semilla de Tomate"), Map.entry("CORN_SEED", "Semilla de Maíz"),
            Map.entry("PUMPKIN_SEED", "Semilla de Calabaza"), Map.entry("CARROT", "Zanahoria"),
            Map.entry("POTATO", "Papa"), Map.entry("TOMATO", "Tomate"), Map.entry("CORN", "Maíz"),
            Map.entry("PUMPKIN", "Calabaza"), Map.entry("EGG", "Huevo"), Map.entry("MILK", "Leche"),
            Map.entry("HAY", "Heno"), Map.entry("MEDICINE", "Medicina"));

    private static final Map<String, String> TERRAINS = Map.of(
            "SOIL", "Tierra de cultivo", "GRASS", "Hierba", "WATER", "Agua", "OBSTACLE", "Obstáculo");

    private Names() {
        // static helpers only
    }

    /** Season name, for example {@code SPRING} to {@code Primavera}. */
    public static String season(String id) {
        return SEASONS.getOrDefault(id, id);
    }

    /** Weather name, for example {@code RAINY} to {@code Lluvioso}. */
    public static String weather(String id) {
        return WEATHERS.getOrDefault(id, id);
    }

    /** Crop name, for example {@code CARROT} to {@code Zanahoria}. */
    public static String crop(String id) {
        return CROPS.getOrDefault(id, id);
    }

    /** Growth stage name, for example {@code SPROUT} to {@code Brote}. */
    public static String stage(String id) {
        return STAGES.getOrDefault(id, id);
    }

    /** Terrain name, for example {@code SOIL} to {@code Tierra de cultivo}. */
    /** Spanish name of an item (seed, harvest, animal product or supply). */
    public static String item(String id) {
        return ITEMS.getOrDefault(id, id);
    }

    public static String terrain(String id) {
        return TERRAINS.getOrDefault(id, id);
    }
}

