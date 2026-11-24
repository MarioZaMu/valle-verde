package edu.ucc.farmsim.domain.npc;

import edu.ucc.farmsim.domain.config.GameRules;
import edu.ucc.farmsim.domain.economy.ItemType;

/**
 * Las cuatro personas del valle (RN-37): cada una está quieta en una parcela de pasto de la configuración, bloquea el
 * paso del avatar y ofrece un encargo (RN-38): traer una cantidad de un objeto y recibir una recompensa en m.
 *
 * <p>Stories: HU-46, HU-47.
 */
public enum Npc {
    MARTA(GameRules.NPC_MARTA_COL, GameRules.NPC_MARTA_ROW, ItemType.CARROT,
            GameRules.ERRAND_MARTA_AMOUNT, GameRules.ERRAND_MARTA_REWARD),
    TOMAS(GameRules.NPC_TOMAS_COL, GameRules.NPC_TOMAS_ROW, ItemType.POTATO,
            GameRules.ERRAND_TOMAS_AMOUNT, GameRules.ERRAND_TOMAS_REWARD),
    LUCIA(GameRules.NPC_LUCIA_COL, GameRules.NPC_LUCIA_ROW, ItemType.HAY,
            GameRules.ERRAND_LUCIA_AMOUNT, GameRules.ERRAND_LUCIA_REWARD),
    INES(GameRules.NPC_INES_COL, GameRules.NPC_INES_ROW, ItemType.EGG,
            GameRules.ERRAND_INES_AMOUNT, GameRules.ERRAND_INES_REWARD);

    private final int col;
    private final int row;
    private final ItemType errandItem;
    private final int errandAmount;
    private final int errandReward;

    Npc(int col, int row, ItemType errandItem, int errandAmount, int errandReward) {
        this.col = col;
        this.row = row;
        this.errandItem = errandItem;
        this.errandAmount = errandAmount;
        this.errandReward = errandReward;
    }

    /** Columna de la parcela donde está. */
    public int col() {
        return col;
    }

    /** Fila de la parcela donde está. */
    public int row() {
        return row;
    }

    /** Lo que pide su encargo. */
    public ItemType errandItem() {
        return errandItem;
    }

    /** Cuántas unidades pide su encargo. */
    public int errandAmount() {
        return errandAmount;
    }

    /** Lo que paga, en m, cuando se entrega el encargo. */
    public int errandReward() {
        return errandReward;
    }

    /** Verdadero si la parcela es una de sus cuatro vecinas, los únicos lugares desde donde se le puede hablar. */
    public boolean isNextTo(int otherCol, int otherRow) {
        return Math.abs(otherCol - col) + Math.abs(otherRow - row) == 1;
    }

    /** La persona que está sobre la parcela, o {@code null}. */
    public static Npc at(int col, int row) {
        for (Npc npc : values()) {
            if (npc.col == col && npc.row == row) {
                return npc;
            }
        }
        return null;
    }

    /** Verdadero si alguien está sobre una parcela del área cuya parcela superior izquierda y tamaño se dan. */
    public static boolean anyInside(int left, int top, int width, int height) {
        for (Npc npc : values()) {
            if (npc.col >= left && npc.col < left + width && npc.row >= top && npc.row < top + height) {
                return true;
            }
        }
        return false;
    }
}
