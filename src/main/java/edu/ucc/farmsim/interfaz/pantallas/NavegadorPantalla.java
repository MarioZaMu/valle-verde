package edu.ucc.farmsim.interfaz.pantallas;

/**
 * Lo que necesita la lógica de las pantallas de la ventana: cambiar de pantalla, hablar con el jugador y cerrar la
 * aplicación. El real usa Swing; las pruebas usan uno falso, así que los presentadores no necesitan pantalla.
 *
 * <p>Stories: HU-33.
 */
public interface NavegadorPantalla {

    /** Muestra la pantalla de inicio. */
    void inicioMostrar();

    /**
     * Se asegura de que la pantalla de juego exista antes de enviar la primera orden de una partida, para que escuche
     * los eventos de esa orden (RNF-02: aún puede estar esperando a que arranque el servidor). Por defecto no hay nada
     * que hacer.
     */
    default void juegoPreparar() {
                // la pantalla de juego ya existe
    }

    /** Muestra la pantalla de juego. */
    void juegoMostrar();

    /** Muestra un mensaje informativo al jugador. */
    void mensajeMostrar(String texto);

    /** Hace una pregunta de sí o no; devuelve {@code true} si el jugador acepta. */
    boolean confirmar(String texto);

    /**
     * Le pide al jugador que elija una de las líneas (las ranuras de guardado).
     *
     * @param titulo   lo que el jugador está eligiendo
     * @param opciones las líneas, una por ranura, en orden
     * @return el número de la línea elegida, desde 1, o 0 si el jugador canceló
     */
    int elegir(String titulo, java.util.List<String> opciones);

    /** Cierra la aplicación. */
    void exit();
}
