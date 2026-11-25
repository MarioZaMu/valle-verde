package edu.ucc.farmsim.interfaz.dibujo;

/**
 * Se le avisa cuando el jugador hace clic en una parcela del mapa.
 *
 * <p>Stories: HU-35.
 */
@FunctionalInterface
public interface OyenteClicParcela {

    /** Se llama con las coordenadas de la parcela pulsada. */
    void parcelaClicada(int col, int fila);
}
