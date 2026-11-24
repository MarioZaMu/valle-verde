package edu.ucc.farmsim.application.dto;

import java.util.List;

/**
 * Un edificio en el mapa con los animales que hay dentro.
 *
 * <p>Stories: HU-18, HU-19, HU-35.
 *
 * @param id              posición en el orden en que se construyeron los edificios; identifica al edificio
 * @param type            id del edificio, por ejemplo {@code COOP}
 * @param name            nombre en español
 * @param col             columna de su parcela superior izquierda
 * @param row             fila de su parcela superior izquierda
 * @param width           columnas que cubre
 * @param height          filas que cubre
 * @param capacity        animales que puede alojar (0 si no aloja ninguno)
 * @param productName     nombre en español de lo que producen sus animales, o {@code null}
 * @param pendingProducts unidades que esperan ser recogidas
 * @param animals         los animales que hay dentro
 */
public record BuildingView(int id, String type, String name, int col, int row, int width, int height,
                           int capacity, String productName, int pendingProducts, List<AnimalView> animals) {

    /** Verdadero si la parcela pertenece al edificio. */
    public boolean covers(int plotCol, int plotRow) {
        return plotCol >= col && plotCol < col + width && plotRow >= row && plotRow < row + height;
    }
}
