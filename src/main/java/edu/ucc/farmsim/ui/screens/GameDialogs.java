package edu.ucc.farmsim.ui.screens;

import edu.ucc.farmsim.application.dto.DaySummaryView;
import edu.ucc.farmsim.application.dto.DialogueView;

/**
 * What the game screen logic needs to open its dialogs (market, inventory, construction, a building and the
 * summary of the day). The real one opens Swing dialogs; tests use a fake, so no display is needed.
 *
 * <p>Stories: HU-17, HU-18, HU-26, HU-29, HU-30, HU-37, HU-38, HU-46.
 */
public interface GameDialogs {

    /** Opens the market dialog (shortcut M). */
    void showMarket();

    /** Opens the inventory (shortcut I). */
    void showInventory();

    /**
     * Opens the construction dialog for the plot that was clicked with the Build tool.
     *
     * @param col column of the plot, the top-left corner of the building
     * @param row row of the plot
     */
    void showConstruction(int col, int row);

    /**
     * Opens the panel of a building: its animals and the buttons to feed, collect and cure.
     *
     * @param buildingId id of the building
     */
    void showBuilding(int buildingId);

    /** Opens the achievements dialog (shortcut L). */
    void showAchievements();

    /**
     * Opens the save slots dialog.
     *
     * @param saving {@code true} when the player came to save, {@code false} to load (it only changes which
     *               button is the natural one; both are available)
     */
    void showSlots(boolean saving);

    /** Shows the summary of the day that just ended. */
    void showSummary(DaySummaryView summary);

    /** Opens the dialogue box of a person of the valley with what it just said (HU-46). */
    void showDialogue(DialogueView dialogue);
}
