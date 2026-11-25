package edu.ucc.farmsim.aplicacion.dto;

/**
 * Una fila del diálogo de construcción: un edificio, su precio y si se puede construir.
 *
 * <p>Stories: HU-18.
 *
 * @param tipo        id del edificio, por ejemplo {@code COOP}
 * @param name        nombre en español
 * @param width       columnas que cubre
 * @param height      filas que cubre
 * @param costo        precio en m
 * @param construido       cuántos tiene ya el jugador
 * @param max         el máximo que puede tener el jugador
 * @param desbloqueado    si el jugador tiene el logro que se requiere
 * @param requisito nombre en español del logro que lo desbloquea, o {@code null} si no hace falta ninguno
 */
public record VistaOfertaEdificio(String tipo, String name, int width, int height, int costo, int construido, int max,
                                boolean desbloqueado, String requisito) { }
