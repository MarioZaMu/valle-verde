package edu.ucc.farmsim.ui.render;

/**
 * Se le avisa cuando el jugador hace clic en una parcela del mapa.
 *
 * <p>Stories: HU-35.
 */
@FunctionalInterface
public interface PlotClickListener {

    /** Se llama con las coordenadas de la parcela pulsada. */
    void plotClicked(int col, int row);
}
