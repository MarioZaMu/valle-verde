package edu.ucc.farmsim.interfaz.componentes;

import java.awt.event.KeyEvent;

/**
 * Los atajos de teclado de la pantalla de juego además de las teclas de herramientas (RN-34).
 *
 * <p>Stories: HU-36.
 */
public enum Atajo {
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

    private final int codigoClave;

    Atajo(int codigoClave) {
        this.codigoClave = codigoClave;
    }

    /** El código de tecla AWT de este atajo. */
    public int codigoClave() {
        return codigoClave;
    }
}
