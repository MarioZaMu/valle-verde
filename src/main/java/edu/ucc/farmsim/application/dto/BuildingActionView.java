package edu.ucc.farmsim.application.dto;

/**
 * El resultado de una acción sobre un edificio (alimentar, recoger, curar): el edificio tal como está ahora, a cuántos
 * animales o unidades llegó la acción y un aviso opcional para el jugador cuando algo quedó sin hacer.
 *
 * <p>Stories: HU-20, HU-22, HU-23.
 *
 * @param building el edificio después de la acción
 * @param amount   animales alimentados, unidades recogidas o animales curados
 * @param notice   texto en español para mostrar al jugador, o {@code null}
 */
public record BuildingActionView(BuildingView building, int amount, String notice) { }
