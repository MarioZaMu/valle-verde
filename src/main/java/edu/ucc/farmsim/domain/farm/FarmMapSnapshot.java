package edu.ucc.farmsim.domain.farm;

import edu.ucc.farmsim.domain.crop.CropSnapshot;
import java.util.List;

/**
 * Immutable copy of what grows on the map: one entry per plot in row-major order, {@code null} where the plot
 * has no crop. The terrain never changes, so it is not stored.
 *
 * <p>Stories: HU-01.
 *
 * @param crops crop snapshot of each plot, or {@code null}
 */
public record FarmMapSnapshot(List<CropSnapshot> crops) { }
