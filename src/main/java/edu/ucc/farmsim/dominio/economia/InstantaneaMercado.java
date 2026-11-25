package edu.ucc.farmsim.dominio.economia;

import java.util.Map;

/**
 * Una copia inmutable del mercado para el memento: la demanda de cada producto y su precio de ayer (0 cuando aún no
 * hay).
 *
 * <p>Stories: HU-15, HU-16.
 *
 * @param demanda        demanda de cada producto vendible, en porcentaje
 * @param precioAnterior precio de la siguiente unidad antes de la última recuperación, o 0
 */
public record InstantaneaMercado(Map<TipoArticulo, Integer> demanda, Map<TipoArticulo, Integer> precioAnterior) {

    /** Hace de los mapas copias no modificables. */
    public InstantaneaMercado {
        demanda = Map.copyOf(demanda);
        precioAnterior = Map.copyOf(precioAnterior);
    }
}
