package edu.ucc.farmsim.ui.components;

import java.awt.event.KeyEvent;

/**
 * Los atajos de teclado de la pantalla de juego además de las teclas de herramientas (RN-34).
 *
 * <p>Stories: HU-36.
 */
public enum Shortcut {
    /** Terminar el día. */
    END_DAY(KeyEvent.VK_ENTER),
    /** Abrir el mercado. */
    MARKET(KeyEvent.VK_M),
    /** Abrir el inventario. */
    INVENTORY(KeyEvent.VK_I),
    /** Abrir los logros. */
    ACHIEVEMENTS(KeyEvent.VK_L),
    /** Guardar rápido en la última ranura usada. */
    QUICK_SAVE(KeyEvent.VK_F5),
    /** Cargar rápido desde la última ranura usada. */
    QUICK_LOAD(KeyEvent.VK_F9);

    private final int keyCode;

    Shortcut(int keyCode) {
        this.keyCode = keyCode;
    }

    /** El código de tecla AWT de este atajo. */
    public int keyCode() {
        return keyCode;
    }
}
