package edu.ucc.farmsim.domain.building;

import edu.ucc.farmsim.domain.config.GameRules;

/**
 * Pattern: Builder — Role: ConcreteBuilder (warehouse: twelve more inventory slots).
 *
 * <p>Stories: HU-18.
 */
final class WarehouseBuilder extends BuildingBuilder {

    private int capacity;
    private int extraSlots;
    private int plots;

    @Override
    BuildingType type() {
        return BuildingType.WAREHOUSE;
    }

    @Override
    void defineHousing() {
        capacity = 0;
    }

    @Override
    void defineEffects() {
        extraSlots = GameRules.WAREHOUSE_EXTRA_SLOTS;
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
