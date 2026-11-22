package edu.ucc.farmsim.application.result;

import java.util.Map;

/**
 * Spanish display names for the ids the application hands to the UI (seasons, weather, crops, growth stages, buildings, animals, achievements),
 * so messages and screens always use the same words. Unknown ids are returned unchanged.
 *
 * <p>Stories: HU-01, HU-07, HU-18.
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

    private static final Map<String, String> BUILDINGS = Map.of(
            "COOP", "Gallinero", "BARN", "Establo", "WAREHOUSE", "Almacén", "GREENHOUSE", "Invernadero");
    private static final Map<String, String> BUILDINGS_PLURAL = Map.of(
            "COOP", "Gallineros", "BARN", "Establos", "WAREHOUSE", "Almacenes", "GREENHOUSE", "Invernaderos");
    private static final Map<String, String> ANIMALS = Map.of("HEN", "Gallina", "COW", "Vaca");
    private static final Map<String, String> ANIMALS_PLURAL = Map.of("HEN", "Gallinas", "COW", "Vacas");
    private static final Map<String, String> CONDITIONS = Map.of(
            "HEALTHY", "Sana", "HUNGRY", "Hambrienta", "SICK", "Enferma");
    private static final Map<String, String> ACHIEVEMENTS = Map.of(
            "FIRST_HARVEST", "Primera cosecha", "GREEN_THUMB", "Manos a la tierra",
            "BIG_HARVESTER", "Gran cosechador", "TRADER", "Comerciante", "CAPITALIST", "Capitalista",
            "BUILDER", "Constructor", "BARNYARD_FARMER", "Granjero de corral", "VETERINARIAN", "Veterinario",
            "ONE_YEAR_IN_THE_VALLEY", "Un año en el valle");

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

    /** Spanish name of an item (seed, harvest, animal product or supply). */
    public static String item(String id) {
        return ITEMS.getOrDefault(id, id);
    }

    /** Building name, for example {@code COOP} to {@code Gallinero}. */
    public static String building(String id) {
        return BUILDINGS.getOrDefault(id, id);
    }

    /** Plural of a building name, for example {@code COOP} to {@code Gallineros}. */
    public static String buildingPlural(String id) {
        return BUILDINGS_PLURAL.getOrDefault(id, id);
    }

    /** Animal name, for example {@code HEN} to {@code Gallina}. */
    public static String animal(String id) {
        return ANIMALS.getOrDefault(id, id);
    }

    /** Plural of an animal name, for example {@code COW} to {@code Vacas}. */
    public static String animalPlural(String id) {
        return ANIMALS_PLURAL.getOrDefault(id, id);
    }

    /** Name of an animal's condition, for example {@code SICK} to {@code Enferma}. */
    public static String condition(String id) {
        return CONDITIONS.getOrDefault(id, id);
    }

    /** Name of an achievement, for example {@code GREEN_THUMB} to {@code Manos a la tierra}. */
    public static String achievement(String id) {
        return ACHIEVEMENTS.getOrDefault(id, id);
    }

    /** Terrain name, for example {@code SOIL} to {@code Tierra de cultivo}. */
    public static String terrain(String id) {
        return TERRAINS.getOrDefault(id, id);
    }
}

