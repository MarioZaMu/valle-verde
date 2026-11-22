package edu.ucc.farmsim.domain.building;

import edu.ucc.farmsim.domain.config.GameRules;

/**
 * Pattern: Builder — Role: ConcreteBuilder (greenhouse: nine protected plots).
 *
 * <p>Stories: HU-18.
 */
final class GreenhouseBuilder extends BuildingBuilder {

    private int capacity;
    private int extraSlots;
    private int plots;

    @Override
    BuildingType type() {
        return BuildingType.GREENHOUSE;
    }

    @Override
    void defineHousing() {
        capacity = 0;
    }

    @Override
    void defineEffects() {
        extraSlots = 0;
        plots = GameRules.GREENHOUSE_PROTECTED_PLOTS;
    }

    @Override
    int animalCapacity() {
        return capacity;
    }

    @Override
    int extraInventorySlots() {
        return extraSlots;
    }

    @Override
    int protectedPlots() {
        return plots;
    }
}
