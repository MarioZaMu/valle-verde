package edu.ucc.farmsim.domain.building;

/**
 * Pattern: Builder — Role: Director (knows the order of the steps and which builder each type needs).
 *
 * <p>Stories: HU-18.
 */
public final class BuildingDirector {

    private BuildingDirector() {
                // solo métodos de utilidad estáticos
    }

    /**
     * Arma un edificio ejecutando en orden los pasos del constructor correspondiente.
     *
     * @param type tipo de edificio
     * @param id   posición del edificio en el orden en que se construyen
     * @param col  columna de su parcela superior izquierda
     * @param row  fila de su parcela superior izquierda
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
