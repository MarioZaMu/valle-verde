package edu.ucc.farmsim.domain.building;

import edu.ucc.farmsim.domain.animal.AnimalType;
import edu.ucc.farmsim.domain.config.GameRules;
import edu.ucc.farmsim.domain.progress.Achievement;

/**
 * Catálogo de edificios (RN-20): tamaño, costo, el máximo que se puede construir, el logro que lo desbloquea (RN-29) y
 * el animal que aloja, si lo hay.
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

    /** Columnas que cubre el edificio. */
    public int width() {
        return width;
    }

    /** Filas que cubre el edificio. */
    public int height() {
        return height;
    }

    /** Costo en m. */
    public int cost() {
        return cost;
    }

    /** El máximo de edificios de este tipo que puede tener el jugador. */
    public int maxCount() {
        return maxCount;
    }

    /** El logro que lo desbloquea, o {@code null} si está disponible desde el inicio. */
    public Achievement requirement() {
        return requirement;
    }

    /** El animal que aloja este edificio, o {@code null} si no aloja ninguno. */
    public AnimalType housedAnimal() {
        return housedAnimal;
    }

    /** El edificio donde vive un animal. */
    public static BuildingType housing(AnimalType animal) {
        for (BuildingType type : values()) {
            if (type.housedAnimal == animal) {
                return type;
            }
        }
        throw new IllegalStateException("No building houses " + animal);
    }
}
