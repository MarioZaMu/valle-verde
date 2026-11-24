package edu.ucc.farmsim.domain.farm;

/**
 * Copia inmutable de dónde está el avatar y hacia dónde mira, para el memento.
 *
 * <p>Stories: HU-45.
 *
 * @param col    columna de la parcela en que está
 * @param row    fila de la parcela en que está
 * @param facing la dirección hacia la que mira
 */
public record AvatarSnapshot(int col, int row, Direction facing) { }
