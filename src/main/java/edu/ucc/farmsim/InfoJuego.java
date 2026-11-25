package edu.ucc.farmsim;

/**
 * Fuente única del nombre visible del juego y de la versión de la aplicación (RNF-12).
 *
 * <p>El nombre es provisional: renombrar el juego significa cambiar solo {@link #NOMBRE}.
 * Stories: none (technical setup, phase 5.0).
 */
public final class InfoJuego {

    /** Nombre visible del juego, mostrado en el título de la ventana y en la pantalla de inicio. */
    public static final String NOMBRE = "Valle Verde";

    /** Versión de la aplicación, sincronizada con la versión del proyecto Maven. */
    public static final String VERSION = "0.1.0";

    private InfoJuego() {
                // contenedor de constantes
    }
}
