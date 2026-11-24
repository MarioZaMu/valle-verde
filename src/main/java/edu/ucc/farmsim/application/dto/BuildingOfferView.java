package edu.ucc.farmsim.application.dto;

/**
 * Una fila del diálogo de construcción: un edificio, su precio y si se puede construir.
 *
 * <p>Stories: HU-18.
 *
 * @param type        id del edificio, por ejemplo {@code COOP}
 * @param name        nombre en español
 * @param width       columnas que cubre
 * @param height      filas que cubre
 * @param cost        precio en m
 * @param built       cuántos tiene ya el jugador
 * @param max         el máximo que puede tener el jugador
 * @param unlocked    si el jugador tiene el logro que se requiere
 * @param requirement nombre en español del logro que lo desbloquea, o {@code null} si no hace falta ninguno
 */
public record BuildingOfferView(String type, String name, int width, int height, int cost, int built, int max,
                                boolean unlocked, String requirement) { }
