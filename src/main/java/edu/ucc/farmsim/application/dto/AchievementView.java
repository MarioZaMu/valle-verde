package edu.ucc.farmsim.application.dto;

/**
 * Un logro tal como lo muestra el diálogo de logros.
 *
 * <p>Stories: HU-26.
 *
 * @param code        identificador de RN-28, por ejemplo {@code LG-02}
 * @param name        nombre en español
 * @param description texto de la condición en español
 * @param reward      recompensa en m
 * @param unlocked    si el jugador lo tiene
 * @param progress    cuánto lleva el jugador (limitado a {@code target})
 * @param target      lo que pide la condición
 */
public record AchievementView(String code, String name, String description, int reward, boolean unlocked,
                              int progress, int target) { }
