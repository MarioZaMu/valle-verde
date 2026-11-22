package edu.ucc.farmsim.domain.building;

import edu.ucc.farmsim.domain.animal.AnimalSnapshot;
import java.util.List;

/**
 * Immutable copy of a building and its animals, for the memento.
 *
 * <p>Stories: HU-18.
 *
 * @param id      position of the building in the order they were built
 * @param type    kind of building
 * @param col     column of its top-left plot
 * @param row     row of its top-left plot
 * @param animals the animals inside, in the order they arrived
 */
public record BuildingSnapshot(int id, BuildingType type, int col, int row, List<AnimalSnapshot> animals) {

    /** Makes the list an unmodifiable copy. */
    public BuildingSnapshot {
        animals = List.copyOf(animals);
    }
}
