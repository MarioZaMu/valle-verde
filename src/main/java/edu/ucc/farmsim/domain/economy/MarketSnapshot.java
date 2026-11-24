package edu.ucc.farmsim.domain.economy;

import java.util.Map;

/**
 * Una copia inmutable del mercado para el memento: la demanda de cada producto y su precio de ayer (0 cuando aún no
 * hay).
 *
 * <p>Stories: HU-15, HU-16.
 *
 * @param demand        demanda de cada producto vendible, en porcentaje
 * @param previousPrice precio de la siguiente unidad antes de la última recuperación, o 0
 */
public record MarketSnapshot(Map<ItemType, Integer> demand, Map<ItemType, Integer> previousPrice) {

    /** Hace de los mapas copias no modificables. */
    public MarketSnapshot {
        demand = Map.copyOf(demand);
        previousPrice = Map.copyOf(previousPrice);
    }
}
