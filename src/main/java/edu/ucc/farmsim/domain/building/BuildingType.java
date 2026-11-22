package edu.ucc.farmsim.domain.building;

import edu.ucc.farmsim.domain.animal.AnimalType;
import edu.ucc.farmsim.domain.config.GameRules;
import edu.ucc.farmsim.domain.progress.Achievement;

/**
 * Catalog of buildings (RN-20): size, cost, the most that can be built, the achievement that unlocks it
 * (RN-29) and the animal it houses, if any.
 *
 * <p>Stories: HU-18.
 */
public enum BuildingType {
    COOP(2, 2, GameRules.COOP_COST, GameRules.COOP_MAX, null, AnimalType.HEN),
    BARN(3, 2, GameRules.BARN_COST, GameRules.BARN_MAX, Achievement.GREEN_THUMB, AnimalType.COW),
    WAREHOUSE(2, 2, GameRules.WAREHOUSE_COST, GameRules.WAREHOUSE_MAX, Achievement.FIRST_HARVEST, null),
    GREENHOUSE(3, 3, GameRules.GREENHOUSE_COST, GameRules.GREENHOUSE_MAX, Achievement.CAPITALIST, null);

    private final int width;
    private final int height;
    private final int cost;
    private final int maxCount;
    private final Achievement requirement;
    private final AnimalType housedAnimal;

    BuildingType(int width, int height, int cost, int maxCount, Achievement requirement, AnimalType housedAnimal) {
        this.width = width;
        this.height = height;
        this.cost = cost;
        this.maxCount = maxCount;
        this.requirement = requirement;
        this.housedAnimal = housedAnimal;
    }

    /** Columns the building covers. */
    public int width() {
        return width;
    }

    /** Rows the building covers. */
    public int height() {
        return height;
    }

    /** Cost in m. */
    public int cost() {
        return cost;
    }

    /** The most buildings of this type the player can have. */
    public int maxCount() {
        return maxCount;
    }

    /** The achievement that unlocks it, or {@code null} if it is available from the start. */
    public Achievement requirement() {
        return requirement;
    }

    /** The animal this building houses, or {@code null} if it houses none. */
    public AnimalType housedAnimal() {
        return housedAnimal;
    }

    /** The building where an animal lives. */
    public static BuildingType housing(AnimalType animal) {
        for (BuildingType type : values()) {
            if (type.housedAnimal == animal) {
                return type;
            }
        }
        throw new IllegalStateException("No building houses " + animal);
    }
}
