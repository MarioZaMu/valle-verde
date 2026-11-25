package edu.ucc.farmsim.aplicacion.dto;

/**
 * Una persona del valle tal como la dibuja y la describe la pantalla. Inmutable.
 *
 * <p>Stories: HU-46, HU-47.
 *
 * @param id           {@code MARTA}, {@code TOMAS}, {@code LUCIA} or {@code INES}
 * @param name         nombre en español, por ejemplo {@code Don Tomás}
 * @param rol         lo que hace la persona, en español
 * @param col          columna de la parcela donde está
 * @param fila          fila de la parcela donde está
 * @param articuloEncargo   id del objeto que pide su encargo, por ejemplo {@code CARROT}
 * @param montoEncargo unidades que pide su encargo
 * @param recompensaEncargo lo que paga, en m
 * @param encargoHecho   si el encargo ya fue entregado
 */
public record VistaNpc(String id, String name, String rol, int col, int fila, String articuloEncargo, int montoEncargo,
                      int recompensaEncargo, boolean encargoHecho) {

    /** Verdadero si la parcela es una de sus cuatro vecinas. */
    public boolean esSiguienteA(int colOtro, int filaOtra) {
        return Math.abs(colOtro - col) + Math.abs(filaOtra - fila) == 1;
    }
}
