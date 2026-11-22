package edu.ucc.farmsim.domain.building;

import edu.ucc.farmsim.domain.config.GameRules;

/**
 * Pattern: Builder — Role: ConcreteBuilder (barn: four cows).
 *
 * <p>Stories: HU-18.
 */
final class BarnBuilder extends BuildingBuilder {

    private int capacity;
    private int extraSlots;
    private int plots;

    @Override
    BuildingType type() {
        return BuildingType.BARN;
    }

    @Override
    void defineHousing() {
        capacity = GameRules.ANIMALS_PER_BUILDING;
    }

    @Override
    void defineEffects() {
        extraSlots = 0;
        plots = 0;
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
