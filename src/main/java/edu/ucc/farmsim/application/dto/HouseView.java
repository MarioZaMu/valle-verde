package edu.ucc.farmsim.application.dto;

/**
 * Una casa del valle tal como la dibuja el mapa (RN-04). Inmutable.
 *
 * <p>Stories: HU-50.
 *
 * @param id     {@code HOME}, {@code SHOP}, {@code FARMHOUSE}, {@code CLINIC} or {@code COTTAGE}
 * @param col    columna de su parcela superior izquierda
 * @param row    fila de su parcela superior izquierda
 * @param width  ancho en parcelas
 * @param height alto en parcelas
 */
public record HouseView(String id, int col, int row, int width, int height) { }
