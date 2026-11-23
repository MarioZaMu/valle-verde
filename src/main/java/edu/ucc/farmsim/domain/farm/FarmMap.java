package edu.ucc.farmsim.domain.farm;

import edu.ucc.farmsim.domain.building.Building;
import edu.ucc.farmsim.domain.building.BuildingDirector;
import edu.ucc.farmsim.domain.building.BuildingSnapshot;
import edu.ucc.farmsim.domain.building.BuildingType;
import edu.ucc.farmsim.domain.config.GameRules;
import edu.ucc.farmsim.domain.crop.Crop;
import edu.ucc.farmsim.domain.crop.CropSnapshot;
import edu.ucc.farmsim.domain.npc.Npc;
import edu.ucc.farmsim.domain.time.Season;
import edu.ucc.farmsim.domain.time.Weather;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The farm grid: {@link GameRules#MAP_COLS} x {@link GameRules#MAP_ROWS} plots with coordinates
 * {@code (column, row)}, where {@code (0, 0)} is the top-left corner (RN-04).
 *
 * <p>Stories: HU-01, HU-03, HU-05, HU-12, HU-18, HU-25, HU-28, HU-45, HU-46, HU-50.
 */
public final class FarmMap {

    private final Plot[][] plots;
    private final List<Building> buildings = new ArrayList<>();

    private FarmMap(Plot[][] plots) {
        this.plots = plots;
    }

    /**
     * Builds the fixed initial map of a new game: the soil block, the pond, the obstacles, the path and the
     * houses of the valley, and grass elsewhere (RN-04).
     */
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
        for (House house : House.values()) {
            if (inside(col, row, house.col(), house.row(), house.width(), house.height())) {
                return Terrain.HOUSE;
            }
        }
        for (int[] path : GameRules.PATHS) {
            if (inside(col, row, path[0], path[1], path[2], path[3])) {
                return Terrain.PATH;
            }
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

    /**
     * True if the avatar can stand on the plot (RN-36): it is inside the map, it is grass, soil (with or without
     * a crop) or the path, no building covers it and no person of the valley stands on it (RN-37). Water,
     * obstacles and houses block the way.
     */
    public boolean isWalkable(int col, int row) {
        if (!contains(col, row)) {
            return false;
        }
        Plot plot = plots[row][col];
        Terrain ground = plot.terrain();
        return (ground == Terrain.GRASS || ground == Terrain.SOIL || ground == Terrain.PATH) && plot.building() == null
                && Npc.at(col, row) == null;
    }

    /**
     * The walkable plot nearest to the given one, counting steps along columns and rows; ties go to the upper
     * row, then to the left column. The given plot itself if it is walkable.
     *
     * @return {column, row}
     * @throws IllegalStateException if no plot of the map can be walked on
     */
    public int[] nearestWalkable(int col, int row) {
        for (int distance = 0; distance <= cols() + rows(); distance++) {
            for (int r = row - distance; r <= row + distance; r++) {
                int rest = distance - Math.abs(r - row);
                for (int c : rest == 0 ? new int[] {col} : new int[] {col - rest, col + rest}) {
                    if (isWalkable(c, r)) {
                        return new int[] {c, r};
                    }
                }
            }
        }
        throw new IllegalStateException("no plot of the map can be walked on");
    }

    /** The buildings in the order they were built; the index of a building is its id. */
    public List<Building> buildings() {
        return Collections.unmodifiableList(buildings);
    }

    /** How many buildings of the type have been built. */
    public int countBuildings(BuildingType type) {
        return (int) buildings.stream().filter(b -> b.type() == type).count();
    }

    /** The building that covers a plot, or {@code null}. */
    public Building buildingAt(int col, int row) {
        return contains(col, row) ? plots[row][col].building() : null;
    }

    /**
     * True if a building of the type fits with its top-left plot at the given place: every plot of the area is
     * inside the map, is grass and is free (RN-21 point 1).
     */
    public boolean canPlace(BuildingType type, int col, int row) {
        for (int r = row; r < row + type.height(); r++) {
            for (int c = col; c < col + type.width(); c++) {
                if (!contains(c, r) || plots[r][c].terrain() != Terrain.GRASS || plots[r][c].building() != null) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Builds a building and marks its plots as occupied.
     *
     * @throws IllegalStateException if the area is not free grass inside the map
     */
    public Building place(BuildingType type, int col, int row) {
        if (!canPlace(type, col, row)) {
            throw new IllegalStateException("Cannot build " + type + " at (" + col + ", " + row + ")");
        }
        return attach(BuildingDirector.construct(type, buildings.size(), col, row));
    }

    private Building attach(Building building) {
        for (int r = building.row(); r < building.row() + building.height(); r++) {
            for (int c = building.col(); c < building.col() + building.width(); c++) {
                plots[r][c].setBuilding(building);
            }
        }
        buildings.add(building);
        return building;
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

    /**
     * Resolves the end of the day for every crop on the map (step 2 of RN-03): growth, or a dry day, or
     * withering. A drought lowers the tolerance to dry days (RN-09).
     *
     * @param todayWeather weather of the day that ends
     * @return number of crops that withered now
     */
    public int growCrops(Weather todayWeather) {
        int openTolerance = todayWeather == Weather.DROUGHT
                ? GameRules.DRY_DAYS_TO_WITHER_IN_DROUGHT
                : GameRules.DRY_DAYS_TO_WITHER;
        int withered = 0;
        for (Plot[] line : plots) {
            for (Plot plot : line) {
                Crop crop = plot.crop();
                if (crop != null) {
                    boolean wasAlive = crop.isAlive();
                    crop.endOfDay(plot.isSheltered() ? GameRules.DRY_DAYS_TO_WITHER_IN_GREENHOUSE : openTolerance);
                    if (wasAlive && !crop.isAlive()) {
                        withered++;
                    }
                }
            }
        }
        return withered;
    }

    /**
     * Rain waters every living crop in the open (step 1 of RN-03); the ones inside a greenhouse get no rain
     * (RN-22).
     */
    public void rainOnCrops() {
        for (Plot[] line : plots) {
            for (Plot plot : line) {
                if (plot.crop() != null && !plot.isSheltered()) {
                    plot.crop().water();
                }
            }
        }
    }

    /**
     * A frost withers every living crop in the open, whatever its stage (RN-09-b).
     *
     * @return how many crops withered now
     */
    public int frostCrops() {
        int withered = 0;
        for (Plot[] line : plots) {
            for (Plot plot : line) {
                if (plot.crop() != null && !plot.isSheltered() && plot.crop().witherByFrost()) {
                    withered++;
                }
            }
        }
        return withered;
    }

    /**
     * Withers every living crop that is not valid in the given season (RN-09-c, step 5 of RN-03).
     *
     * @return number of crops that withered now
     */
    public int witherOutOfSeason(Season season) {
        int withered = 0;
        for (Plot[] line : plots) {
            for (Plot plot : line) {
                if (plot.crop() != null && !plot.isSheltered() && plot.crop().witherIfOutOfSeason(season)) {
                    withered++;
                }
            }
        }
        return withered;
    }

    /** Copy of the crops of every plot in row-major order ({@code null} where there is none), for the memento. */
    public FarmMapSnapshot snapshot() {
        List<CropSnapshot> crops = new ArrayList<>();
        for (Plot[] line : plots) {
            for (Plot plot : line) {
                crops.add(plot.crop() == null ? null : plot.crop().snapshot());
            }
        }
        return new FarmMapSnapshot(Collections.unmodifiableList(crops),
                buildings.stream().map(Building::snapshot).toList());
    }

    /** Puts every crop and every building back as it was when the snapshot was taken. */
    public void restoreFrom(FarmMapSnapshot snapshot) {
        int index = 0;
        for (Plot[] line : plots) {
            for (Plot plot : line) {
                CropSnapshot saved = snapshot.crops().get(index++);
                plot.restoreCrop(saved == null ? null : Crop.fromSnapshot(saved));
                plot.setBuilding(null);
            }
        }
        buildings.clear();
        for (BuildingSnapshot saved : snapshot.buildings()) {
            attach(Building.fromSnapshot(saved));
        }
    }
}

