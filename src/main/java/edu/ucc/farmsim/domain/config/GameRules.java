package edu.ucc.farmsim.domain.config;

/**
 * Single source of the numeric business rules (RN-01 to RN-35): no magic numbers elsewhere (RNF-12).
 *
 * <p>The values mirror docs/REGLAS_DE_NEGOCIO.md; if a rule changes there, it changes here only.
 * This class depends on nothing (it holds primitives), so every domain package may use it.
 *
 * <p>Stories: HU-08, HU-09, HU-11, HU-13, HU-28.
 */
public final class GameRules {

    // ---- RN-01: calendar ----

    /** Days in each season; a year of four seasons lasts 112 days. */
    public static final int DAYS_PER_SEASON = 28;

    /** Year in which a new game starts. */
    public static final int START_YEAR = 1;

    /** Day of the season in which a new game starts. */
    public static final int START_DAY = 1;

    // ---- RN-11: weather probabilities (percent, in the order of the Weather enum) ----
    // Order: SUNNY, CLOUDY, RAINY, DROUGHT, FROST, SNOWY. Each table adds up to 100.

    /** Spring weather table. */
    public static final int[] WEATHER_WEIGHTS_SPRING = {55, 15, 30, 0, 0, 0};

    /** Summer weather table. */
    public static final int[] WEATHER_WEIGHTS_SUMMER = {60, 10, 15, 15, 0, 0};

    /** Autumn weather table for days 1 to 21. */
    public static final int[] WEATHER_WEIGHTS_AUTUMN_EARLY = {50, 20, 30, 0, 0, 0};

    /** Autumn weather table for days 22 to 28 (frost becomes possible). */
    public static final int[] WEATHER_WEIGHTS_AUTUMN_LATE = {35, 15, 25, 0, 25, 0};

    /** Winter weather table. */
    public static final int[] WEATHER_WEIGHTS_WINTER = {25, 20, 0, 0, 15, 40};

    /** First day of Autumn on which frost is possible. */
    public static final int AUTUMN_FROST_FROM_DAY = 22;

    /** Total of every weather table: the roll is made between 0 and this value. */
    public static final int WEATHER_TABLE_TOTAL = 100;

    // ---- RN-02: daily energy ----

    /** Maximum (and starting) energy. */
    public static final int MAX_ENERGY = 100;

    /** Energy cost of sowing. */
    public static final int ENERGY_COST_SOW = 2;

    /** Energy cost of watering a crop on a normal day. */
    public static final int ENERGY_COST_WATER = 1;

    /** Energy cost of watering a crop when the weather is drought. */
    public static final int ENERGY_COST_WATER_IN_DROUGHT = 2;

    /** Energy cost of harvesting a mature crop. */
    public static final int ENERGY_COST_HARVEST = 2;

    /** Energy cost of clearing a withered plot. */
    public static final int ENERGY_COST_CLEAN = 1;

    /** Energy cost of feeding each animal. */
    public static final int ENERGY_COST_FEED_PER_ANIMAL = 1;

    /** Energy cost of collecting the products of a building. */
    public static final int ENERGY_COST_COLLECT = 1;

    /** Energy cost of curing an animal. */
    public static final int ENERGY_COST_CURE = 1;

    // ---- RN-04: farm map (16 x 10 = 160 plots) ----

    /** Number of plot columns. */
    public static final int MAP_COLS = 16;

    /** Number of plot rows. */
    public static final int MAP_ROWS = 10;

    /** Column of the top-left plot of the soil block (8 x 5 = 40 soil plots). */
    public static final int SOIL_COL = 4;

    /** Row of the top-left plot of the soil block. */
    public static final int SOIL_ROW = 2;

    /** Width of the soil block. */
    public static final int SOIL_WIDTH = 8;

    /** Height of the soil block. */
    public static final int SOIL_HEIGHT = 5;

    /** Column of the top-left plot of the pond (3 x 2 = 6 water plots). */
    public static final int POND_COL = 13;

    /** Row of the top-left plot of the pond. */
    public static final int POND_ROW = 0;

    /** Width of the pond. */
    public static final int POND_WIDTH = 3;

    /** Height of the pond. */
    public static final int POND_HEIGHT = 2;

    /** Fixed positions {column, row} of the 12 obstacles (trees and rocks). */
    public static final int[][] OBSTACLES = {
        {0, 0}, {1, 0}, {0, 1}, {8, 0}, {0, 5}, {15, 5}, {0, 9}, {1, 9}, {7, 9}, {14, 9}, {15, 9}, {15, 8}
    };

    // ---- RN-30: new game ----

    /** Money at the start of a game. */
    public static final int START_MONEY = 500;

    /** Carrot seeds in the starting inventory. */
    public static final int START_CARROT_SEEDS = 10;

    /** Potato seeds in the starting inventory. */
    public static final int START_POTATO_SEEDS = 5;

    // ---- RN-15: inventory ----

    /** Base number of inventory slots. */
    public static final int INVENTORY_BASE_SLOTS = 24;

    /** Maximum number of units in one stack. */
    public static final int MAX_STACK = 99;

    private GameRules() {
        // constants holder
    }
}
