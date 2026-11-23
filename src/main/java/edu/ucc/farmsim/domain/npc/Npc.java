package edu.ucc.farmsim.domain.npc;

import edu.ucc.farmsim.domain.config.GameRules;
import edu.ucc.farmsim.domain.economy.ItemType;

/**
 * The four people of the valley (RN-37): each one stands still on a grass plot of the configuration, blocks the
 * way of the avatar and offers one errand (RN-38): bring a quantity of an item and get a reward in m.
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

    /** Column of the plot where it stands. */
    public int col() {
        return col;
    }

    /** Row of the plot where it stands. */
    public int row() {
        return row;
    }

    /** What its errand asks for. */
    public ItemType errandItem() {
        return errandItem;
    }

    /** How many units its errand asks for. */
    public int errandAmount() {
        return errandAmount;
    }

    /** What it pays, in m, when the errand is delivered. */
    public int errandReward() {
        return errandReward;
    }

    /** True if the plot is one of its four neighbours, the only places from where it can be talked to. */
    public boolean isNextTo(int otherCol, int otherRow) {
        return Math.abs(otherCol - col) + Math.abs(otherRow - row) == 1;
    }

    /** The person standing on the plot, or {@code null}. */
    public static Npc at(int col, int row) {
        for (Npc npc : values()) {
            if (npc.col == col && npc.row == row) {
                return npc;
            }
        }
        return null;
    }

    /** True if someone stands on a plot of the area whose top-left plot and size are given. */
    public static boolean anyInside(int left, int top, int width, int height) {
        for (Npc npc : values()) {
            if (npc.col >= left && npc.col < left + width && npc.row >= top && npc.row < top + height) {
                return true;
            }
        }
        return false;
    }
}
