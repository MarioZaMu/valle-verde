package edu.ucc.farmsim.ui.components;

import java.awt.event.KeyEvent;

/**
 * The five tools of the tool bar, in the order of their keys 1 to 5 (RN-34).
 *
 * <p>Stories: HU-36.
 */
public enum Tool {
    /** Sow the selected seed. */
    SOW,
    /** Water a crop. */
    WATER,
    /** Harvest a mature crop, clear a withered one or collect a building's products. */
    HARVEST,
    /** Open the building catalog. */
    BUILD,
    /** Feed the animals of a building. */
    FEED;

    /** The character of this tool's key: {@code '1'} for the first tool, up to {@code '5'}. */
    public char key() {
        return (char) ('1' + ordinal());
    }

    /** The AWT key code of this tool's key. */
    public int keyCode() {
        return KeyEvent.VK_1 + ordinal();
    }
}
