package edu.ucc.farmsim.interfaz.pantallas;

import edu.ucc.farmsim.aplicacion.dto.VistaConversacion;
import edu.ucc.farmsim.aplicacion.dto.VistaResumenDia;

/**
 * Lo que necesita la lógica de la pantalla de juego para abrir sus diálogos (mercado, inventario, construcción, un
 * edificio y el resumen del día). El real abre diálogos de Swing; las pruebas usan uno falso, así que no hace falta
 * pantalla.
 *
 * <p>Stories: HU-17, HU-18, HU-26, HU-29, HU-30, HU-37, HU-38, HU-46.
 */
public interface DialogosJuego {

    /** Abre el diálogo del mercado (atajo M). */
    void mercadoMostrar();

    /** Abre el inventario (atajo I). */
    void inventarioMostrar();

    /**
     * Abre el diálogo de construcción para la parcela en la que se hizo clic con la herramienta Construir.
     *
     * @param col columna de la parcela, la esquina superior izquierda del edificio
     * @param fila fila de la parcela
     */
    void construccionMostrar(int col, int fila);

    /**
     * Abre el panel de un edificio: sus animales y los botones para alimentar, recoger y curar.
     *
     * @param idEdificio id del edificio
     */
    void edificioMostrar(int idEdificio);

    /** Abre el diálogo de logros (atajo L). */
    void logrosMostrar();

    /**
     * Abre el diálogo de ranuras de guardado.
     *
     * @param guardando {@code true} cuando el jugador vino a guardar, {@code false} para cargar (solo cambia cuál botón es
     *               el natural; ambos están disponibles)
     */
    void ranurasMostrar(boolean guardando);

    /** Muestra el resumen del día que acaba de terminar. */
    void resumenMostrar(VistaResumenDia resumen);

    /** Abre el cuadro de diálogo de una persona del valle con lo que acaba de decir (HU-46). */
    void conversacionMostrar(VistaConversacion conversacion);
}
