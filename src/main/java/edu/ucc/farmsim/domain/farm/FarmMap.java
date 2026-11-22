package edu.ucc.farmsim.domain.farm;

import edu.ucc.farmsim.domain.config.GameRules;

/**
 * The farm grid: {@link GameRules#MAP_COLS} x {@link GameRules#MAP_ROWS} plots with coordinates
 * {@code (column, row)}, where {@code (0, 0)} is the top-left corner (RN-04).
 *
 * <p>Stories: HU-28.
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
}
