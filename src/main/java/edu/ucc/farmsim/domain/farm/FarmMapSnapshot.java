package edu.ucc.farmsim.domain.farm;

import edu.ucc.farmsim.domain.building.BuildingSnapshot;
import edu.ucc.farmsim.domain.crop.CropSnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable copy of what changes on the map: one crop per plot in row-major order ({@code null} where the plot
 * has none) and the buildings with their animals. The terrain never changes, so it is not stored.
 *
 * <p>Stories: HU-01, HU-18, HU-50.
 *
 * @param crops     crop snapshot of each plot, or {@code null}
 * @param buildings the buildings in the order they were built
 */
public record FarmMapSnapshot(List<CropSnapshot> crops, List<BuildingSnapshot> buildings) {

    /** Makes the building list an unmodifiable copy. */
    public FarmMapSnapshot {
        buildings = List.copyOf(buildings);
    }

    /**
     * The same crops on a bigger map: a crop of a map {@code fromCols} wide keeps its column and row, and the
     * new plots have none (RN-40: a save of the 16 x 10 map becomes the north-west corner of the valley).
     *
     * @throws IllegalArgumentException if the crops do not fill whole rows of {@code fromCols}, or do not fit
     */
    public FarmMapSnapshot widened(int fromCols, int toCols, int toRows) {
        int fromRows = crops.size() / fromCols;
        if (crops.size() % fromCols != 0 || fromCols > toCols || fromRows > toRows) {
            throw new IllegalArgumentException(crops.size() + " crops are not a map of " + fromCols + " columns");
        }
        List<CropSnapshot> wide = new ArrayList<>(Collections.nCopies(toCols * toRows, (CropSnapshot) null));
        for (int i = 0; i < crops.size(); i++) {
            wide.set(i / fromCols * toCols + i % fromCols, crops.get(i));
        }
        return new FarmMapSnapshot(Collections.unmodifiableList(wide), buildings);
    }
}
