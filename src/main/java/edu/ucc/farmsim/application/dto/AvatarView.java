package edu.ucc.farmsim.application.dto;

/**
 * Where the avatar stands and where it looks, as the screen draws it. Immutable.
 *
 * <p>Stories: HU-45.
 *
 * @param col    column of the plot it stands on
 * @param row    row of the plot it stands on
 * @param facing {@code NORTH}, {@code SOUTH}, {@code EAST} or {@code WEST}
 */
public record AvatarView(int col, int row, String facing) {

    /** Column of the plot in front of it (it may be outside the map). */
    public int frontCol() {
        return col + switch (facing) {
            case "EAST" -> 1;
            case "WEST" -> -1;
            default -> 0;
        };
    }

    /** Row of the plot in front of it (it may be outside the map). */
    public int frontRow() {
        return row + switch (facing) {
            case "SOUTH" -> 1;
            case "NORTH" -> -1;
            default -> 0;
        };
    }
}
