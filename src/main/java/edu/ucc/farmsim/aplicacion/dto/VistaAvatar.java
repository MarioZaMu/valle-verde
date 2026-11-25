package edu.ucc.farmsim.aplicacion.dto;

/**
 * Dónde está el avatar y hacia dónde mira, tal como lo dibuja la pantalla. Inmutable.
 *
 * <p>Stories: HU-45.
 *
 * @param col    columna de la parcela en que está
 * @param fila    fila de la parcela en que está
 * @param mirada {@code NORTH}, {@code SOUTH}, {@code EAST} or {@code WEST}
 */
public record VistaAvatar(int col, int fila, String mirada) {

    /** Columna de la parcela que tiene enfrente (puede estar fuera del mapa). */
    public int colFrente() {
        return col + switch (mirada) {
            case "EAST" -> 1;
            case "WEST" -> -1;
            default -> 0;
        };
    }

    /** Fila de la parcela que tiene enfrente (puede estar fuera del mapa). */
    public int filaFrente() {
        return fila + switch (mirada) {
            case "SOUTH" -> 1;
            case "NORTH" -> -1;
            default -> 0;
        };
    }
}
