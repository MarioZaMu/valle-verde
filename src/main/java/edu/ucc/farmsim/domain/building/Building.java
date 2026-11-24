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
 * <p>Un edificio colocado en el mapa. Un gallinero o un establo aloja animales y guarda lo que producen hasta que el
 * jugador lo recoge (RN-26); un almacén amplía el inventario; un invernadero protege cultivos.
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

    /** Reconstruye un edificio y sus animales desde una instantánea. */
    public static Building fromSnapshot(BuildingSnapshot snapshot) {
        Building building = BuildingDirector.construct(snapshot.type(), snapshot.id(), snapshot.col(), snapshot.row());
        for (AnimalSnapshot animal : snapshot.animals()) {
            building.animals.add(Animal.fromSnapshot(animal));
        }
        return building;
    }

    /** Una copia inmutable de este edificio. */
    public BuildingSnapshot snapshot() {
        return new BuildingSnapshot(id, type, col, row, animals.stream().map(Animal::snapshot).toList());
    }

    /** Posición en el orden en que se construyeron los edificios; lo identifica. */
    public int id() {
        return id;
    }

    /** Tipo de edificio. */
    public BuildingType type() {
        return type;
    }

    /** Columna de la parcela superior izquierda. */
    public int col() {
        return col;
    }

    /** Fila de la parcela superior izquierda. */
    public int row() {
        return row;
    }

    /** Columnas que cubre. */
    public int width() {
        return type.width();
    }

    /** Filas que cubre. */
    public int height() {
        return type.height();
    }

    /** Verdadero si la parcela pertenece al edificio. */
    public boolean covers(int plotCol, int plotRow) {
        return plotCol >= col && plotCol < col + width() && plotRow >= row && plotRow < row + height();
    }

    /** El animal que aloja este edificio, o {@code null}. */
    public AnimalType species() {
        return species;
    }

    /** Cuántos animales caben. */
    public int animalCapacity() {
        return animalCapacity;
    }

    /** Casillas de inventario que agrega este edificio. */
    public int extraInventorySlots() {
        return extraInventorySlots;
    }

    /** Parcelas de cultivo que protege este edificio. */
    public int protectedPlots() {
        return protectedPlots;
    }

    /** Los animales que hay dentro, en el orden en que llegaron. */
    public List<Animal> animals() {
        return Collections.unmodifiableList(animals);
    }

    /** Verdadero si cabe un animal más del tipo (RN-23). */
    public boolean hasRoomFor(AnimalType animal) {
        return species == animal && animals.size() < animalCapacity;
    }

    /**
     * Aloja a un animal.
     *
     * @return {@code true} si cupo; {@code false} (y nada cambia) si el edificio aloja otra especie o está lleno
     */
    public boolean addAnimal(Animal animal) {
        if (!hasRoomFor(animal.type())) {
            return false;
        }
        animals.add(animal);
        return true;
    }

    /** Unidades de producto que esperan ser recogidas, de todos los animales (RN-26). */
    public int pendingProducts() {
        return animals.stream().mapToInt(Animal::pending).sum();
    }

    /** Lo que producen los animales de este edificio, o {@code null} si no aloja ninguno. */
    public ItemType product() {
        return species == null ? null : species.product();
    }

    /** Toma todas las unidades que esperan ser recogidas. */
    public int collectAll() {
        int total = 0;
        for (Animal animal : animals) {
            total += animal.collect();
        }
        return total;
    }
}
