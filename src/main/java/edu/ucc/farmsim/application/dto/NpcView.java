package edu.ucc.farmsim.application.dto;

/**
 * A person of the valley as the screen draws and describes it. Immutable.
 *
 * <p>Stories: HU-46, HU-47.
 *
 * @param id           {@code MARTA}, {@code TOMAS}, {@code LUCIA} or {@code INES}
 * @param name         name in Spanish, for example {@code Don Tomás}
 * @param role         what the person does, in Spanish
 * @param col          column of the plot where it stands
 * @param row          row of the plot where it stands
 * @param errandItem   item id its errand asks for, for example {@code CARROT}
 * @param errandAmount units its errand asks for
 * @param errandReward what it pays, in m
 * @param errandDone   whether the errand was already delivered
 */
public record NpcView(String id, String name, String role, int col, int row, String errandItem, int errandAmount,
                      int errandReward, boolean errandDone) {

    /** True if the plot is one of its four neighbours. */
    public boolean isNextTo(int otherCol, int otherRow) {
        return Math.abs(otherCol - col) + Math.abs(otherRow - row) == 1;
    }
}
