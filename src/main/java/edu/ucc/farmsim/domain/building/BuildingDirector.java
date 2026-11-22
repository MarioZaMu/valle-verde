package edu.ucc.farmsim.domain.building;

/**
 * Pattern: Builder — Role: Director (knows the order of the steps and which builder each type needs).
 *
 * <p>Stories: HU-18.
 */
public final class BuildingDirector {

    private BuildingDirector() {
        // static helpers only
    }

    /**
     * Assembles a building by running the steps of the matching builder in order.
     *
     * @param type kind of building
     * @param id   position of the building in the order they are built
     * @param col  column of its top-left plot
     * @param row  row of its top-left plot
     */
    public static Building construct(BuildingType type, int id, int col, int row) {
        BuildingBuilder builder = builderFor(type);
        builder.placeAt(id, col, row);
        builder.defineHousing();
        builder.defineEffects();
        return builder.result();
    }

    private static BuildingBuilder builderFor(BuildingType type) {
        return switch (type) {
            case COOP -> new CoopBuilder();
            case BARN -> new BarnBuilder();
            case WAREHOUSE -> new WarehouseBuilder();
            case GREENHOUSE -> new GreenhouseBuilder();
        };
    }
}
