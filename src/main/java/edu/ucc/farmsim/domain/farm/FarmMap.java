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
 * La grilla de la granja: {@link GameRules#MAP_COLS} x {@link GameRules#MAP_ROWS} parcelas con coordenadas {@code
 * (columna, fila)}, donde {@code (0, 0)} es la esquina superior izquierda (RN-04).
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
     * Construye el mapa inicial fijo de una partida nueva: el bloque de tierra, el estanque, los obstáculos, el camino
     * y las casas del valle, y pasto en el resto (RN-04).
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

    /** Número de columnas. */
    public int cols() {
        return GameRules.MAP_COLS;
    }

    /** Número de filas. */
    public int rows() {
        return GameRules.MAP_ROWS;
    }

    /** Verdadero si las coordenadas están dentro del mapa. */
    public boolean contains(int col, int row) {
        return col >= 0 && col < cols() && row >= 0 && row < rows();
    }

    /**
     * La parcela en las coordenadas dadas.
     *
     * @throws IllegalArgumentException si las coordenadas están fuera del mapa
     */
    public Plot plotAt(int col, int row) {
        if (!contains(col, row)) {
            throw new IllegalArgumentException("outside the map: (" + col + ", " + row + ")");
        }
        return plots[row][col];
    }

    /**
     * Verdadero si el avatar puede pararse en la parcela (RN-36): está dentro del mapa, es pasto, tierra (con o sin
     * cultivo) o camino, ningún edificio la cubre y ninguna persona del valle está sobre ella (RN-37). El agua, los
     * obstáculos y las casas bloquean el paso.
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
     * La parcela transitable más cercana a la dada, contando pasos por columnas y filas; los empates van a la fila de
     * arriba y luego a la columna de la izquierda. La propia parcela dada si es transitable.
     *
     * @return {columna, fila}
     * @throws IllegalStateException si ninguna parcela del mapa es transitable
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

    /** Los edificios en el orden en que se construyeron; el índice de un edificio es su id. */
    public List<Building> buildings() {
        return Collections.unmodifiableList(buildings);
    }

    /** Cuántos edificios del tipo se han construido. */
    public int countBuildings(BuildingType type) {
        return (int) buildings.stream().filter(b -> b.type() == type).count();
    }

    /** El edificio que cubre una parcela, o {@code null}. */
    public Building buildingAt(int col, int row) {
        return contains(col, row) ? plots[row][col].building() : null;
    }

    /**
     * Verdadero si cabe un edificio del tipo con su parcela superior izquierda en el lugar dado: todas las parcelas del
     * área están dentro del mapa, son pasto y están libres (RN-21 punto 1).
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
     * Construye un edificio y marca sus parcelas como ocupadas.
     *
     * @throws IllegalStateException si el área no es pasto libre dentro del mapa
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

    /** Número de parcelas con el terreno dado. */
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
     * Resuelve el final del día de cada cultivo del mapa (paso 2 de RN-03): crecimiento, día seco o marchitamiento. Una
     * sequía reduce la tolerancia a los días secos (RN-09).
     *
     * @param todayWeather clima del día que termina
     * @return número de cultivos que se marchitaron ahora
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
     * La lluvia riega todos los cultivos vivos a la intemperie (paso 1 de RN-03); los que están dentro de un
     * invernadero no reciben lluvia (RN-22).
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
     * Una helada marchita todos los cultivos vivos a la intemperie, sea cual sea su etapa (RN-09-b).
     *
     * @return cuántos cultivos se marchitaron ahora
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
     * Marchita todos los cultivos vivos que no son válidos en la estación dada (RN-09-c, paso 5 de RN-03).
     *
     * @return número de cultivos que se marchitaron ahora
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

    /** Copia de los cultivos de cada parcela en orden de fila ({@code null} donde no hay), para el memento. */
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

    /** Devuelve cada cultivo y cada edificio a como estaban cuando se tomó la instantánea. */
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

