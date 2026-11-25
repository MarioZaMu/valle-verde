package edu.ucc.farmsim.aplicacion.dto;

/**
 * Una casa del valle tal como la dibuja el mapa (RN-04). Inmutable.
 *
 * <p>Stories: HU-50.
 *
 * @param id     {@code HOME}, {@code SHOP}, {@code FARMHOUSE}, {@code CLINIC} or {@code COTTAGE}
 * @param col    columna de su parcela superior izquierda
 * @param fila    fila de su parcela superior izquierda
 * @param width  ancho en parcelas
 * @param height alto en parcelas
 */
public record VistaCasa(String id, int col, int fila, int width, int height) { }
