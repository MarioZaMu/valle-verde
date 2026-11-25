package edu.ucc.farmsim.aplicacion.dto;

/**
 * El tutorial de inicio tal como lo muestra el cuadro del avatar (RN-39). Inmutable.
 *
 * <p>Stories: HU-49.
 *
 * @param paso     {@code WALK}, {@code SOW}, {@code WATER}, {@code TALK}, {@code END_DAY}, o cómo terminó: {@code DONE}
 *                 o {@code SKIPPED}
 * @param numero   posición del paso, de 1 a {@code total}
 * @param total    cuántos pasos hay
 * @param texto     lo que dice el avatar, en español
 * @param concluido si terminó (entonces el cuadro se oculta)
 */
public record VistaTutorial(String paso, int numero, int total, String texto, boolean concluido) { }
