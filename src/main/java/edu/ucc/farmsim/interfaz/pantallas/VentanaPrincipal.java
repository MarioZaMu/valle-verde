package edu.ucc.farmsim.interfaz.pantallas;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

/**
 * Ventana principal de la aplicación. En la fase 5.0 es un marco vacío; desde la fase 5.1 aloja el {@code EnrutadorPantalla}
 * que alterna entre la pantalla de inicio y la pantalla de juego.
 *
 * <p>Stories: none (technical setup, phase 5.0).
 * La pantalla de inicio que reemplaza este marco vacío pertenece a una historia posterior.
 */
public final class VentanaPrincipal extends JFrame {

    /** Ancho mínimo de la ventana en píxeles (R-07: utilizable desde 1024 x 768). */
    public static final int ANCHO_MINIMO = 1024;

    /** Alto mínimo de la ventana en píxeles. */
    public static final int ALTO_MINIMO = 768;

    /**
     * Ancho de apertura antes de maximizar la ventana: espacio para el mapa al doble del tamaño de sus sprites de 32
     * píxeles. La ventana abre maximizada y nunca más grande que la parte utilizable de la pantalla.
     */
    public static final int ANCHO_PREDETERMINADO = 1100;

    /**
     * Alto de apertura: el mapa al doble del tamaño (640 px) más la barra superior, la barra de herramientas y los
     * botones.
     */
    public static final int ALTO_PREDETERMINADO = 860;

    private static final long serialVersionUID = 1L;

    /**
     * Crea la ventana; no se muestra hasta que se llama a {@link #setVisible(boolean)}.
     *
     * @param titulo texto para la barra de título
     */
    public VentanaPrincipal(String titulo) {
        super(titulo);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        Rectangle usable = pantallaUsable();
        setMinimumSize(new Dimension(Math.min(ANCHO_MINIMO, usable.width), Math.min(ALTO_MINIMO, usable.height)));
        setSize(Math.min(ANCHO_PREDETERMINADO, usable.width), Math.min(ALTO_PREDETERMINADO, usable.height));
        setLocationRelativeTo(null);
        setExtendedState(getExtendedState() | MAXIMIZED_BOTH);
    }

    /**
     * La parte de la pantalla que puede cubrir una ventana (sin la barra de tareas); el tamaño predeterminado si no hay
     * ninguna.
     */
    private static Rectangle pantallaUsable() {
        if (GraphicsEnvironment.isHeadless()) {
            return new Rectangle(0, 0, ANCHO_PREDETERMINADO, ALTO_PREDETERMINADO);
        }
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    }
}
