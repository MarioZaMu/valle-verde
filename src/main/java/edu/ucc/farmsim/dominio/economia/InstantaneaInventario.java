package edu.ucc.farmsim.dominio.economia;

import java.util.List;

/**
 * Copia inmutable de un inventario: su número de casillas y el contenido de cada casilla (una casilla vacía es una pila
 * con tipo {@code null} y cantidad 0).
 *
 * <p>Stories: HU-10.
 *
 * @param capacidad número de casillas
 * @param ranuras    contenido de cada casilla, en orden de casilla
 */
public record InstantaneaInventario(int capacidad, List<Inventario.PilaArticulo> ranuras) { }
