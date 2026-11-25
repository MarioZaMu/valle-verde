package edu.ucc.farmsim.aplicacion.dto;

import java.util.List;

/**
 * Un edificio en el mapa con los animales que hay dentro.
 *
 * <p>Stories: HU-18, HU-19, HU-35.
 *
 * @param id              posición en el orden en que se construyeron los edificios; identifica al edificio
 * @param tipo            id del edificio, por ejemplo {@code COOP}
 * @param name            nombre en español
 * @param col             columna de su parcela superior izquierda
 * @param fila             fila de su parcela superior izquierda
 * @param width           columnas que cubre
 * @param height          filas que cubre
 * @param capacidad        animales que puede alojar (0 si no aloja ninguno)
 * @param nombreProducto     nombre en español de lo que producen sus animales, o {@code null}
 * @param productosPendientes unidades que esperan ser recogidas
 * @param animales         los animales que hay dentro
 */
public record VistaEdificio(int id, String tipo, String name, int col, int fila, int width, int height,
                           int capacidad, String nombreProducto, int productosPendientes, List<VistaAnimal> animales) {

    /** Verdadero si la parcela pertenece al edificio. */
    public boolean cubre(int colParcela, int filaParcela) {
        return colParcela >= col && colParcela < col + width && filaParcela >= fila && filaParcela < fila + height;
    }
}
