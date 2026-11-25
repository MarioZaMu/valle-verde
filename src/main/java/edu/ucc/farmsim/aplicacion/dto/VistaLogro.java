package edu.ucc.farmsim.aplicacion.dto;

/**
 * Un logro tal como lo muestra el diálogo de logros.
 *
 * <p>Stories: HU-26.
 *
 * @param codigo        identificador de RN-28, por ejemplo {@code LG-02}
 * @param name        nombre en español
 * @param descripcion texto de la condición en español
 * @param recompensa      recompensa en m
 * @param desbloqueado    si el jugador lo tiene
 * @param progreso    cuánto lleva el jugador (limitado a {@code objetivo})
 * @param objetivo      lo que pide la condición
 */
public record VistaLogro(String codigo, String name, String descripcion, int recompensa, boolean desbloqueado,
                              int progreso, int objetivo) { }
