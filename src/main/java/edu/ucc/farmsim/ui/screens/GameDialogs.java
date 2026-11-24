package edu.ucc.farmsim.ui.screens;

import edu.ucc.farmsim.application.dto.DaySummaryView;
import edu.ucc.farmsim.application.dto.DialogueView;

/**
 * Lo que necesita la lógica de la pantalla de juego para abrir sus diálogos (mercado, inventario, construcción, un
 * edificio y el resumen del día). El real abre diálogos de Swing; las pruebas usan uno falso, así que no hace falta
 * pantalla.
 *
 * <p>Stories: HU-17, HU-18, HU-26, HU-29, HU-30, HU-37, HU-38, HU-46.
 */
public interface GameDialogs {

    /** Abre el diálogo del mercado (atajo M). */
    void showMarket();

    /** Abre el inventario (atajo I). */
    void showInventory();

    /**
     * Abre el diálogo de construcción para la parcela en la que se hizo clic con la herramienta Construir.
     *
     * @param col columna de la parcela, la esquina superior izquierda del edificio
     * @param row fila de la parcela
     */
    void showConstruction(int col, int row);

    /**
     * Abre el panel de un edificio: sus animales y los botones para alimentar, recoger y curar.
     *
     * @param buildingId id del edificio
     */
    void showBuilding(int buildingId);

    /** Abre el diálogo de logros (atajo L). */
    void showAchievements();

    /**
     * Abre el diálogo de ranuras de guardado.
     *
     * @param saving {@code true} cuando el jugador vino a guardar, {@code false} para cargar (solo cambia cuál botón es
     *               el natural; ambos están disponibles)
     */
    void showSlots(boolean saving);

    /** Muestra el resumen del día que acaba de terminar. */
    void showSummary(DaySummaryView summary);

    /** Abre el cuadro de diálogo de una persona del valle con lo que acaba de decir (HU-46). */
    void showDialogue(DialogueView dialogue);
}
