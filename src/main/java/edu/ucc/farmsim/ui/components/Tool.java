package edu.ucc.farmsim.ui.components;

import java.awt.event.KeyEvent;

/**
 * Las cinco herramientas de la barra de herramientas, en el orden de sus teclas 1 a 5 (RN-34).
 *
 * <p>Stories: HU-36.
 */
public enum Tool {
    /** Sembrar la semilla seleccionada. */
    SOW,
    /** Regar un cultivo. */
    WATER,
    /** Cosechar un cultivo maduro, limpiar uno marchito o recoger los productos de un edificio. */
    HARVEST,
    /** Abrir el catálogo de edificios. */
    BUILD,
    /** Alimentar a los animales de un edificio. */
    FEED;

    /** El carácter de la tecla de esta herramienta: {@code '1'} para la primera, hasta {@code '5'}. */
    public char key() {
        return (char) ('1' + ordinal());
    }

    /** El código de tecla AWT de la tecla de esta herramienta. */
    public int keyCode() {
        return KeyEvent.VK_1 + ordinal();
    }
}
