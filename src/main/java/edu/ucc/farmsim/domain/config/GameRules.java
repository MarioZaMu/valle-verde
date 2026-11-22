package edu.ucc.farmsim.domain.config;

/**
 * Single source of the numeric business rules (RN-01 to RN-35): no magic numbers elsewhere (RNF-12).
 *
 * <p>The values mirror docs/REGLAS_DE_NEGOCIO.md; if a rule changes there, it changes here only.
 * This class depends on nothing (it holds primitives), so every domain package may use it.
 *
 * <p>Stories: HU-01, HU-05, HU-08, HU-09, HU-11, HU-13, HU-28, HU-33.
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

    // ---- RN-16, RN-17, RN-18: market ----

    /** Price in m of one ration of hay. */
    public static final int PRICE_HAY = 5;

    /** Price in m of one medicine. */
    public static final int PRICE_MEDICINE = 40;

    /** Base selling price in m of an egg. */
    public static final int BASE_PRICE_EGG = 20;

    /** Base selling price in m of a bottle of milk. */
    public static final int BASE_PRICE_MILK = 60;

    /** Season factor, in percent, of a crop sold in one of its valid seasons. */
    public static final int SEASON_FACTOR_IN_SEASON = 90;

    /** Season factor, in percent, of a crop sold out of its valid seasons. */
    public static final int SEASON_FACTOR_OUT_OF_SEASON = 120;

    /** Season factor, in percent, of animal products. */
    public static final int SEASON_FACTOR_FLAT = 100;

    /** Smallest quantity of one transaction. */
    public static final int MIN_TRANSACTION = 1;

    /** Largest quantity of one purchase. */
    public static final int MAX_TRANSACTION = 99;

    /** Demand of every product when a game starts, and its ceiling, in percent. */
    public static final int DEMAND_MAX = 100;

    /** Lowest demand a product can fall to, in percent. */
    public static final int DEMAND_MIN = 50;

    /** Demand lost by each unit sold. */
    public static final int DEMAND_DROP_PER_UNIT = 2;

    /** Demand recovered when a day ends. */
    public static final int DEMAND_RECOVERY_PER_DAY = 5;

    // ---- RN-31: save slots ----

    /** Number of save slots. */
    public static final int SAVE_SLOTS = 3;

    // ---- RN-06: sowing ----

    /** Seeds consumed by one sowing. */
    public static final int SEEDS_PER_SOWING = 1;

    // ---- RN-09: withering ----

    /** Consecutive days without water after which a young crop withers. */
    public static final int DRY_DAYS_TO_WITHER = 3;

    /** Same, when the weather of the day that ends is Drought. */
    public static final int DRY_DAYS_TO_WITHER_IN_DROUGHT = 2;

    // ---- RN-15: inventory ----

    /** Base number of inventory slots. */
    public static final int INVENTORY_BASE_SLOTS = 24;

    /** Maximum number of units in one stack. */
    public static final int MAX_STACK = 99;

    // ---- RN-20, RN-21, RN-22: buildings ----

    /** Cost in m of a coop. */
    public static final int COOP_COST = 400;

    /** Largest number of coops. */
    public static final int COOP_MAX = 2;

    /** Cost in m of a barn. */
    public static final int BARN_COST = 1000;

    /** Largest number of barns. */
    public static final int BARN_MAX = 2;

    /** Cost in m of a warehouse. */
    public static final int WAREHOUSE_COST = 300;

    /** Largest number of warehouses. */
    public static final int WAREHOUSE_MAX = 1;

    /** Inventory slots a warehouse adds. */
    public static final int WAREHOUSE_EXTRA_SLOTS = 12;

    /** Cost in m of a greenhouse. */
    public static final int GREENHOUSE_COST = 1500;

    /** Largest number of greenhouses. */
    public static final int GREENHOUSE_MAX = 1;

    /** Plots a greenhouse protects (3 x 3). */
    public static final int GREENHOUSE_PROTECTED_PLOTS = 9;

    // ---- RN-23 to RN-27: animals ----

    /** Animals a coop or a barn can house. */
    public static final int ANIMALS_PER_BUILDING = 4;

    /** Price in m of a hen. */
    public static final int PRICE_HEN = 150;

    /** Price in m of a cow. */
    public static final int PRICE_COW = 800;

    /** Hay each animal eats per day. */
    public static final int HAY_PER_ANIMAL = 1;

    /** Consecutive days without eating after which an animal falls sick. */
    public static final int DAYS_WITHOUT_FEED_TO_SICK = 3;

    /** Units of product one animal can have waiting to be collected. */
    public static final int MAX_PENDING_PER_ANIMAL = 2;

    /** Medicines one cure uses. */
    public static final int MEDICINE_PER_CURE = 1;

    private GameRules() {
        // constants holder
    }
}
