package edu.ucc.farmsim.application.dto;

/**
 * Una persona del valle tal como la dibuja y la describe la pantalla. Inmutable.
 *
 * <p>Stories: HU-46, HU-47.
 *
 * @param id           {@code MARTA}, {@code TOMAS}, {@code LUCIA} or {@code INES}
 * @param name         nombre en español, por ejemplo {@code Don Tomás}
 * @param role         lo que hace la persona, en español
 * @param col          columna de la parcela donde está
 * @param row          fila de la parcela donde está
 * @param errandItem   id del objeto que pide su encargo, por ejemplo {@code CARROT}
 * @param errandAmount unidades que pide su encargo
 * @param errandReward lo que paga, en m
 * @param errandDone   si el encargo ya fue entregado
 */
public record NpcView(String id, String name, String role, int col, int row, String errandItem, int errandAmount,
                      int errandReward, boolean errandDone) {

    /** Verdadero si la parcela es una de sus cuatro vecinas. */
    public boolean isNextTo(int otherCol, int otherRow) {
        return Math.abs(otherCol - col) + Math.abs(otherRow - row) == 1;
    }
}
