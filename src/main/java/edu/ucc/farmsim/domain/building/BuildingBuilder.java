package edu.ucc.farmsim.domain.building;

/**
 * Pattern: Builder — Role: Builder (declares the steps that assemble a building).
 *
 * <p>The {@link BuildingDirector} calls the steps in a fixed order; each concrete builder decides what the
 * housing and the effects of its building are. Rejections never happen here: the use case checks every rule
 * of RN-21 before asking for a building.
 *
 * <p>Stories: HU-18.
 */
public abstract class BuildingBuilder {

    private int id;
    private int col;
    private int row;

    /** The kind of building this builder assembles. */
    abstract BuildingType type();

    /** Step 1: where it is and which one it is. */
    final void placeAt(int buildingId, int topLeftCol, int topLeftRow) {
        this.id = buildingId;
        this.col = topLeftCol;
        this.row = topLeftRow;
    }

    /** Step 2: how many animals live inside and of which species ({@code null} species: none). */
    abstract void defineHousing();

    /** Step 3: what else the building does (more inventory slots, protected crops). */
    abstract void defineEffects();

    /** The housing capacity decided in step 2. */
    abstract int animalCapacity();

    /** The inventory slots decided in step 3. */
    abstract int extraInventorySlots();

    /** The protected plots decided in step 3. */
    abstract int protectedPlots();

    /** Final step: the finished building. */
    final Building result() {
        return new Building(id, type(), col, row, type().housedAnimal(), animalCapacity(),
                extraInventorySlots(), protectedPlots());
    }
}
