package edu.ucc.farmsim.domain.config;

/**
 * Single source of the numeric business rules (RN-01 to RN-35): no magic numbers elsewhere (RNF-12).
 *
 * <p>The values mirror docs/REGLAS_DE_NEGOCIO.md; if a rule changes there, it changes here only.
 * This class depends on nothing (it holds primitives), so every domain package may use it.
 *
 * <p>Stories: HU-08, HU-09, HU-13.
 */
public final class GameRules {

    // ---- RN-01: calendar ----

    /** Days in each season; a year of four seasons lasts 112 days. */
    public static final int DAYS_PER_SEASON = 28;

    /** Year in which a new game starts. */
    public static final int START_YEAR = 1;

    /** Day of the season in which a new game starts. */
    public static final int START_DAY = 1;

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

    // ---- RN-15: inventory ----

    /** Base number of inventory slots. */
    public static final int INVENTORY_BASE_SLOTS = 24;

    /** Maximum number of units in one stack. */
    public static final int MAX_STACK = 99;

    private GameRules() {
        // constants holder
    }
}
