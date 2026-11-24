package edu.ucc.farmsim.domain.building;

/**
 * Pattern: Builder — Role: Builder (declares the steps that assemble a building).
 *
 * <p>El {@link BuildingDirector} llama a los pasos en un orden fijo; cada constructor concreto decide cuál es el
 * alojamiento y los efectos de su edificio. Aquí nunca ocurren rechazos: el caso de uso comprueba todas las reglas de
 * RN-21 antes de pedir un edificio.
 *
 * <p>Stories: HU-18.
 */
public abstract class BuildingBuilder {

    private int id;
    private int col;
    private int row;

    /** El tipo de edificio que arma este constructor. */
    abstract BuildingType type();

    /** Paso 1: dónde está y cuál es. */
    final void placeAt(int buildingId, int topLeftCol, int topLeftRow) {
        this.id = buildingId;
        this.col = topLeftCol;
        this.row = topLeftRow;
    }

    /** Paso 2: cuántos animales viven dentro y de qué especie (especie {@code null}: ninguno). */
    abstract void defineHousing();

    /** Paso 3: qué más hace el edificio (más casillas de inventario, cultivos protegidos). */
    abstract void defineEffects();

    /** La capacidad de alojamiento decidida en el paso 2. */
    abstract int animalCapacity();

    /** Las casillas de inventario decididas en el paso 3. */
    abstract int extraInventorySlots();

    /** Las parcelas protegidas decididas en el paso 3. */
    abstract int protectedPlots();

    /** Paso final: el edificio terminado. */
    final Building result() {
        return new Building(id, type(), col, row, type().housedAnimal(), animalCapacity(),
                extraInventorySlots(), protectedPlots());
    }
}
