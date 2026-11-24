package edu.ucc.farmsim.ui.screens;

/**
 * Lo que necesita la lógica de las pantallas de la ventana: cambiar de pantalla, hablar con el jugador y cerrar la
 * aplicación. El real usa Swing; las pruebas usan uno falso, así que los presentadores no necesitan pantalla.
 *
 * <p>Stories: HU-33.
 */
public interface ScreenNavigator {

    /** Muestra la pantalla de inicio. */
    void showStart();

    /**
     * Se asegura de que la pantalla de juego exista antes de enviar la primera orden de una partida, para que escuche
     * los eventos de esa orden (RNF-02: aún puede estar esperando a que arranque el servidor). Por defecto no hay nada
     * que hacer.
     */
    default void prepareGame() {
                // la pantalla de juego ya existe
    }

    /** Muestra la pantalla de juego. */
    void showGame();

    /** Muestra un mensaje informativo al jugador. */
    void showMessage(String text);

    /** Hace una pregunta de sí o no; devuelve {@code true} si el jugador acepta. */
    boolean confirm(String text);

    /**
     * Le pide al jugador que elija una de las líneas (las ranuras de guardado).
     *
     * @param title   lo que el jugador está eligiendo
     * @param options las líneas, una por ranura, en orden
     * @return el número de la línea elegida, desde 1, o 0 si el jugador canceló
     */
    int choose(String title, java.util.List<String> options);

    /** Cierra la aplicación. */
    void exit();
}
