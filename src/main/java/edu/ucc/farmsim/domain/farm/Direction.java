package edu.ucc.farmsim.domain.farm;

/**
 * Las cuatro direcciones hacia las que puede mirar y caminar el avatar (RN-36). El mapa crece hacia el este (columnas)
 * y hacia el sur (filas), así que el norte es una fila hacia arriba.
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

    /** Cuántas columnas mueve un paso en esta dirección: -1, 0 o 1. */
    public int dCol() {
        return dCol;
    }

    /** Cuántas filas mueve un paso en esta dirección: -1, 0 o 1. */
    public int dRow() {
        return dRow;
    }
}
