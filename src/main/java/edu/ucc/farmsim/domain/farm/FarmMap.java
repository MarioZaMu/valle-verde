package edu.ucc.farmsim.domain.farm;

import edu.ucc.farmsim.domain.config.GameRules;
import edu.ucc.farmsim.domain.crop.Crop;
import edu.ucc.farmsim.domain.crop.CropSnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The farm grid: {@link GameRules#MAP_COLS} x {@link GameRules#MAP_ROWS} plots with coordinates
 * {@code (column, row)}, where {@code (0, 0)} is the top-left corner (RN-04).
 *
 * <p>Stories: HU-01, HU-03, HU-28.
 */
public final class FarmMap {

    private final Plot[][] plots;

    private FarmMap(Plot[][] plots) {
        this.plots = plots;
    }

    /** Builds the fixed initial map of a new game: a soil block, a pond, 12 obstacles and grass elsewhere. */
    public static FarmMap initial() {
        Plot[][] grid = new Plot[GameRules.MAP_ROWS][GameRules.MAP_COLS];
        for (int row = 0; row < GameRules.MAP_ROWS; row++) {
            for (int col = 0; col < GameRules.MAP_COLS; col++) {
                grid[row][col] = new Plot(initialTerrain(col, row));
            }
        }
        return new FarmMap(grid);
    }

    private static Terrain initialTerrain(int col, int row) {
        if (inside(col, row, GameRules.SOIL_COL, GameRules.SOIL_ROW, GameRules.SOIL_WIDTH, GameRules.SOIL_HEIGHT)) {
            return Terrain.SOIL;
        }
        if (inside(col, row, GameRules.POND_COL, GameRules.POND_ROW, GameRules.POND_WIDTH, GameRules.POND_HEIGHT)) {
            return Terrain.WATER;
        }
        for (int[] obstacle : GameRules.OBSTACLES) {
            if (obstacle[0] == col && obstacle[1] == row) {
                return Terrain.OBSTACLE;
            }
        }
        return Terrain.GRASS;
    }

    private static boolean inside(int col, int row, int left, int top, int width, int height) {
        return col >= left && col < left + width && row >= top && row < top + height;
    }

    /** Number of columns. */
    public int cols() {
        return GameRules.MAP_COLS;
    }

    /** Number of rows. */
    public int rows() {
        return GameRules.MAP_ROWS;
    }

    /** True if the coordinates are inside the map. */
    public boolean contains(int col, int row) {
        return col >= 0 && col < cols() && row >= 0 && row < rows();
    }

    /**
     * The plot at the given coordinates.
     *
     * @throws IllegalArgumentException if the coordinates are outside the map
     */
    public Plot plotAt(int col, int row) {
        if (!contains(col, row)) {
            throw new IllegalArgumentException("outside the map: (" + col + ", " + row + ")");
        }
        return plots[row][col];
    }

    /** Number of plots with the given terrain. */
    public int count(Terrain terrain) {
        int total = 0;
        for (Plot[] line : plots) {
            for (Plot plot : line) {
                if (plot.terrain() == terrain) {
                    total++;
                }
            }
        }
        return total;
    }

    /** Resolves the end of the day for every crop on the map (step 2 of RN-03). */
    public void growCrops() {
        for (Plot[] line : plots) {
            for (Plot plot : line) {
                if (plot.crop() != null) {
                    plot.crop().endOfDay();
                }
            }
        }
    }

    /** Copy of the crops of every plot in row-major order ({@code null} where there is none), for the memento. */
    public FarmMapSnapshot snapshot() {
        List<CropSnapshot> crops = new ArrayList<>();
        for (Plot[] line : plots) {
            for (Plot plot : line) {
                crops.add(plot.crop() == null ? null : plot.crop().snapshot());
            }
        }
        return new FarmMapSnapshot(Collections.unmodifiableList(crops));
    }

    /** Puts every plot's crop back as it was when the snapshot was taken. */
    public void restoreFrom(FarmMapSnapshot snapshot) {
        int index = 0;
        for (Plot[] line : plots) {
            for (Plot plot : line) {
                CropSnapshot saved = snapshot.crops().get(index++);
                plot.restoreCrop(saved == null ? null : Crop.fromSnapshot(saved));
            }
        }
    }
}

