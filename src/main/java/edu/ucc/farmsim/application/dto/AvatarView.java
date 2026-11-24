package edu.ucc.farmsim.application.dto;

/**
 * Dónde está el avatar y hacia dónde mira, tal como lo dibuja la pantalla. Inmutable.
 *
 * <p>Stories: HU-45.
 *
 * @param col    columna de la parcela en que está
 * @param row    fila de la parcela en que está
 * @param facing {@code NORTH}, {@code SOUTH}, {@code EAST} or {@code WEST}
 */
public record AvatarView(int col, int row, String facing) {

    /** Columna de la parcela que tiene enfrente (puede estar fuera del mapa). */
    public int frontCol() {
        return col + switch (facing) {
            case "EAST" -> 1;
            case "WEST" -> -1;
            default -> 0;
        };
    }

    /** Fila de la parcela que tiene enfrente (puede estar fuera del mapa). */
    public int frontRow() {
        return row + switch (facing) {
            case "SOUTH" -> 1;
            case "NORTH" -> -1;
            default -> 0;
        };
    }
}
