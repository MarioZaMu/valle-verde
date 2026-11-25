package edu.ucc.farmsim.dominio.granja;

/**
 * Copia inmutable de dónde está el avatar y hacia dónde mira, para el memento.
 *
 * <p>Stories: HU-45.
 *
 * @param col    columna de la parcela en que está
 * @param fila    fila de la parcela en que está
 * @param mirada la dirección hacia la que mira
 */
public record InstantaneaAvatar(int col, int fila, Direccion mirada) { }
