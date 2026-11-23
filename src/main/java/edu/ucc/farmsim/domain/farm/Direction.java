package edu.ucc.farmsim.domain.farm;

/**
 * The four directions the avatar can face and walk to (RN-36). The map grows to the east (columns) and to
 * the south (rows), so north is one row up.
 *
 * <p>Stories: HU-45.
 */
public enum Direction {
    NORTH(0, -1),
    SOUTH(0, 1),
    EAST(1, 0),
    WEST(-1, 0);

    private final int dCol;
    private final int dRow;

    Direction(int dCol, int dRow) {
        this.dCol = dCol;
        this.dRow = dRow;
    }

    /** How many columns one step in this direction moves: -1, 0 or 1. */
    public int dCol() {
        return dCol;
    }

    /** How many rows one step in this direction moves: -1, 0 or 1. */
    public int dRow() {
        return dRow;
    }
}
