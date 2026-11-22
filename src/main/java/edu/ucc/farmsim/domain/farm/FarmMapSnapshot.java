package edu.ucc.farmsim.domain.farm;

import edu.ucc.farmsim.domain.building.BuildingSnapshot;
import edu.ucc.farmsim.domain.crop.CropSnapshot;
import java.util.List;

/**
 * Immutable copy of what changes on the map: one crop per plot in row-major order ({@code null} where the plot
 * has none) and the buildings with their animals. The terrain never changes, so it is not stored.
 *
 * <p>Stories: HU-01, HU-18.
 *
 * @param crops     crop snapshot of each plot, or {@code null}
 * @param buildings the buildings in the order they were built
 */
public record FarmMapSnapshot(List<CropSnapshot> crops, List<BuildingSnapshot> buildings) {

    /** Makes the building list an unmodifiable copy. */
    public FarmMapSnapshot {
        buildings = List.copyOf(buildings);
    }
}
