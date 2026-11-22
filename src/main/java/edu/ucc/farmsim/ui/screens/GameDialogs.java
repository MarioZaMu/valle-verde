package edu.ucc.farmsim.ui.screens;

/**
 * What the game screen logic needs to open the dialogs of the shortcuts (M and I). The real one opens Swing
 * dialogs; tests use a fake, so no display is needed.
 *
 * <p>Stories: HU-37, HU-38.
 */
public interface GameDialogs {

    /** Opens the market dialog (shortcut M). */
    void showMarket();

    /** Opens the inventory (shortcut I). */
    void showInventory();
}
