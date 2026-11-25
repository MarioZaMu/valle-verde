package edu.ucc.farmsim.aplicacion.dto;

import java.util.List;

/**
 * El inventario como una grilla: cuántas casillas hay y las ocupadas en orden; el resto están vacías.
 *
 * <p>Stories: HU-37.
 *
 * @param capacidad número de casillas (24, o 36 con un almacén)
 * @param ranuras    las casillas ocupadas, en orden de casilla
 */
public record VistaInventario(int capacidad, List<VistaRanura> ranuras) { }
