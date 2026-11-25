package edu.ucc.farmsim.aplicacion.dto;

/**
 * El resultado de una acción sobre un edificio (alimentar, recoger, curar): el edificio tal como está ahora, a cuántos
 * animales o unidades llegó la acción y un aviso opcional para el jugador cuando algo quedó sin hacer.
 *
 * <p>Stories: HU-20, HU-22, HU-23.
 *
 * @param edificio el edificio después de la acción
 * @param monto   animales alimentados, unidades recogidas o animales curados
 * @param notificacion   texto en español para mostrar al jugador, o {@code null}
 */
public record VistaAccionEdificio(VistaEdificio edificio, int monto, String notificacion) { }
