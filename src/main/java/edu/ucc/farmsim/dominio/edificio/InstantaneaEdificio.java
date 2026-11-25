package edu.ucc.farmsim.dominio.edificio;

import edu.ucc.farmsim.dominio.animal.InstantaneaAnimal;
import java.util.List;

/**
 * Copia inmutable de un edificio y sus animales, para el memento.
 *
 * <p>Stories: HU-18.
 *
 * @param id      posición del edificio en el orden en que se construyeron
 * @param tipo    tipo de edificio
 * @param col     columna de su parcela superior izquierda
 * @param fila     fila de su parcela superior izquierda
 * @param animales los animales que hay dentro, en el orden en que llegaron
 */
public record InstantaneaEdificio(int id, TipoEdificio tipo, int col, int fila, List<InstantaneaAnimal> animales) {

    /** Hace de la lista una copia no modificable. */
    public InstantaneaEdificio {
        animales = List.copyOf(animales);
    }
}
