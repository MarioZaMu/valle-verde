package edu.ucc.farmsim.domain.building;

import edu.ucc.farmsim.domain.animal.AnimalSnapshot;
import java.util.List;

/**
 * Copia inmutable de un edificio y sus animales, para el memento.
 *
 * <p>Stories: HU-18.
 *
 * @param id      posición del edificio en el orden en que se construyeron
 * @param type    tipo de edificio
 * @param col     columna de su parcela superior izquierda
 * @param row     fila de su parcela superior izquierda
 * @param animals los animales que hay dentro, en el orden en que llegaron
 */
public record BuildingSnapshot(int id, BuildingType type, int col, int row, List<AnimalSnapshot> animals) {

    /** Hace de la lista una copia no modificable. */
    public BuildingSnapshot {
        animals = List.copyOf(animals);
    }
}
