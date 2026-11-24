package edu.ucc.farmsim.application.dto;

/**
 * El tutorial de inicio tal como lo muestra el cuadro del avatar (RN-39). Inmutable.
 *
 * <p>Stories: HU-49.
 *
 * @param step     {@code WALK}, {@code SOW}, {@code WATER}, {@code TALK}, {@code END_DAY}, o cómo terminó: {@code DONE}
 *                 o {@code SKIPPED}
 * @param number   posición del paso, de 1 a {@code total}
 * @param total    cuántos pasos hay
 * @param text     lo que dice el avatar, en español
 * @param finished si terminó (entonces el cuadro se oculta)
 */
public record TutorialView(String step, int number, int total, String text, boolean finished) { }
