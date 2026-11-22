package edu.ucc.farmsim.ui.components;

import java.awt.event.KeyEvent;

/**
 * The keyboard shortcuts of the game screen besides the tool keys (RN-34).
 *
 * <p>Stories: HU-36.
 */
public enum Shortcut {
    /** End the day. */
    END_DAY(KeyEvent.VK_ENTER),
    /** Open the market. */
    MARKET(KeyEvent.VK_M),
    /** Open the inventory. */
    INVENTORY(KeyEvent.VK_I),
    /** Open the achievements. */
    ACHIEVEMENTS(KeyEvent.VK_L),
    /** Save quickly in the last used slot. */
    QUICK_SAVE(KeyEvent.VK_F5),
    /** Load quickly from the last used slot. */
    QUICK_LOAD(KeyEvent.VK_F9);

    private final int keyCode;

    Shortcut(int keyCode) {
        this.keyCode = keyCode;
    }

    /** The AWT key code of this shortcut. */
    public int keyCode() {
        return keyCode;
    }
}
