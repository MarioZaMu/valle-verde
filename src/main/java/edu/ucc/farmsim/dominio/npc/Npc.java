package edu.ucc.farmsim.dominio.npc;

import edu.ucc.farmsim.dominio.configuracion.ReglasJuego;
import edu.ucc.farmsim.dominio.economia.TipoArticulo;

/**
 * Las cuatro personas del valle (RN-37): cada una está quieta en una parcela de pasto de la configuración, bloquea el
 * paso del avatar y ofrece un encargo (RN-38): traer una cantidad de un objeto y recibir una recompensa en m.
 *
 * <p>Stories: HU-46, HU-47.
 */
public enum Npc {
    MARTA(ReglasJuego.NPC_MARTA_COL, ReglasJuego.FILA_MARTA_NPC, TipoArticulo.CARROT,
            ReglasJuego.MONTO_MARTA_ENCARGO, ReglasJuego.RECOMPENSA_MARTA_ENCARGO),
    TOMAS(ReglasJuego.NPC_TOMAS_COL, ReglasJuego.FILA_TOMAS_NPC, TipoArticulo.POTATO,
            ReglasJuego.MONTO_TOMAS_ENCARGO, ReglasJuego.RECOMPENSA_TOMAS_ENCARGO),
    LUCIA(ReglasJuego.NPC_LUCIA_COL, ReglasJuego.FILA_LUCIA_NPC, TipoArticulo.HAY,
            ReglasJuego.MONTO_LUCIA_ENCARGO, ReglasJuego.RECOMPENSA_LUCIA_ENCARGO),
    INES(ReglasJuego.NPC_INES_COL, ReglasJuego.FILA_INES_NPC, TipoArticulo.EGG,
            ReglasJuego.MONTO_INES_ENCARGO, ReglasJuego.RECOMPENSA_INES_ENCARGO);

    private final int col;
    private final int fila;
    private final TipoArticulo articuloEncargo;
    private final int montoEncargo;
    private final int recompensaEncargo;

    Npc(int col, int fila, TipoArticulo articuloEncargo, int montoEncargo, int recompensaEncargo) {
        this.col = col;
        this.fila = fila;
        this.articuloEncargo = articuloEncargo;
        this.montoEncargo = montoEncargo;
        this.recompensaEncargo = recompensaEncargo;
    }

    /** Columna de la parcela donde está. */
    public int col() {
        return col;
    }

    /** Fila de la parcela donde está. */
    public int fila() {
        return fila;
    }

    /** Lo que pide su encargo. */
    public TipoArticulo articuloEncargo() {
        return articuloEncargo;
    }

    /** Cuántas unidades pide su encargo. */
    public int montoEncargo() {
        return montoEncargo;
    }

    /** Lo que paga, en m, cuando se entrega el encargo. */
    public int recompensaEncargo() {
        return recompensaEncargo;
    }

    /** Verdadero si la parcela es una de sus cuatro vecinas, los únicos lugares desde donde se le puede hablar. */
    public boolean esSiguienteA(int colOtro, int filaOtra) {
        return Math.abs(colOtro - col) + Math.abs(filaOtra - fila) == 1;
    }

    /** La persona que está sobre la parcela, o {@code null}. */
    public static Npc at(int col, int fila) {
        for (Npc npc : values()) {
            if (npc.col == col && npc.fila == fila) {
                return npc;
            }
        }
        return null;
    }

    /** Verdadero si alguien está sobre una parcela del área cuya parcela superior izquierda y tamaño se dan. */
    public static boolean dentroCualquier(int izquierda, int arriba, int width, int height) {
        for (Npc npc : values()) {
            if (npc.col >= izquierda && npc.col < izquierda + width && npc.fila >= arriba && npc.fila < arriba + height) {
                return true;
            }
        }
        return false;
    }
}
