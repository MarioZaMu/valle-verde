package edu.ucc.farmsim.domain.building;

import edu.ucc.farmsim.domain.animal.Animal;
import edu.ucc.farmsim.domain.animal.AnimalSnapshot;
import edu.ucc.farmsim.domain.animal.AnimalType;
import edu.ucc.farmsim.domain.economy.ItemType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pattern: Builder — Role: Product (the building the builders assemble step by step).
 *
 * <p>A building placed on the map. A coop or a barn houses animals and keeps what they produce until the
 * player collects it (RN-26); a warehouse widens the inventory; a greenhouse protects crops.
 *
 * <p>Stories: HU-18, HU-19, HU-20, HU-22, HU-24.
 */
public final class Building {

    private final int id;
    private final BuildingType type;
    private final int col;
    private final int row;
    private final AnimalType species;
    private final int animalCapacity;
    private final int extraInventorySlots;
    private final int protectedPlots;
    private final List<Animal> animals = new ArrayList<>();

    Building(int id, BuildingType type, int col, int row, AnimalType species, int animalCapacity,
             int extraInventorySlots, int protectedPlots) {
        this.id = id;
        this.type = type;
        this.col = col;
        this.row = row;
        this.species = species;
        this.animalCapacity = animalCapacity;
        this.extraInventorySlots = extraInventorySlots;
        this.protectedPlots = protectedPlots;
    }

    /** Rebuilds a building and its animals from a snapshot. */
    public static Building fromSnapshot(BuildingSnapshot snapshot) {
        Building building = BuildingDirector.construct(snapshot.type(), snapshot.id(), snapshot.col(), snapshot.row());
        for (AnimalSnapshot animal : snapshot.animals()) {
            building.animals.add(Animal.fromSnapshot(animal));
        }
        return building;
    }

    /** An immutable copy of this building. */
    public BuildingSnapshot snapshot() {
        return new BuildingSnapshot(id, type, col, row, animals.stream().map(Animal::snapshot).toList());
    }

    /** Position in the order buildings were built; it identifies the building. */
    public int id() {
        return id;
    }

    /** Kind of building. */
    public BuildingType type() {
        return type;
    }

    /** Column of the top-left plot. */
    public int col() {
        return col;
    }

    /** Row of the top-left plot. */
    public int row() {
        return row;
    }

    /** Columns it covers. */
    public int width() {
        return type.width();
    }

    /** Rows it covers. */
    public int height() {
        return type.height();
    }

    /** True if the plot belongs to the building. */
    public boolean covers(int plotCol, int plotRow) {
        return plotCol >= col && plotCol < col + width() && plotRow >= row && plotRow < row + height();
    }

    /** The animal this building houses, or {@code null}. */
    public AnimalType species() {
        return species;
    }

    /** How many animals fit. */
    public int animalCapacity() {
        return animalCapacity;
    }

    /** Inventory slots this building adds. */
    public int extraInventorySlots() {
        return extraInventorySlots;
    }

    /** Plots of crop this building protects. */
    public int protectedPlots() {
        return protectedPlots;
    }

    /** The animals inside, in the order they arrived. */
    public List<Animal> animals() {
        return Collections.unmodifiableList(animals);
    }

    /** True if one more animal of the type fits (RN-23). */
    public boolean hasRoomFor(AnimalType animal) {
        return species == animal && animals.size() < animalCapacity;
    }

    /**
     * Houses an animal.
     *
     * @return {@code true} if it fit; {@code false} (and nothing changed) if the building houses another
     *         species or is full
     */
    public boolean addAnimal(Animal animal) {
        if (!hasRoomFor(animal.type())) {
            return false;
        }
        animals.add(animal);
        return true;
    }

    /** Units of product waiting to be collected, from every animal (RN-26). */
    public int pendingProducts() {
        return animals.stream().mapToInt(Animal::pending).sum();
    }

    /** What the animals of this building produce, or {@code null} if it houses none. */
    public ItemType product() {
        return species == null ? null : species.product();
    }

    /** Takes every unit waiting to be collected. */
    public int collectAll() {
        int total = 0;
        for (Animal animal : animals) {
            total += animal.collect();
        }
        return total;
    }
}
