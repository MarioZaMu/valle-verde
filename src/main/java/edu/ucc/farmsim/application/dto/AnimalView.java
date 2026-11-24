package edu.ucc.farmsim.application.dto;

/**
 * Un animal tal como lo muestra el panel del edificio.
 *
 * <p>Stories: HU-19, HU-21, HU-35.
 *
 * @param index           posición del animal dentro de su edificio
 * @param type            id del animal, por ejemplo {@code HEN}
 * @param name            nombre en español
 * @param condition       {@code HEALTHY}, {@code HUNGRY} or {@code SICK}
 * @param conditionName   nombre en español de la condición
 * @param daysWithoutFeed días consecutivos sin comer
 * @param fedToday        si comió hoy
 * @param pending         unidades de producto que esperan ser recogidas
 */
public record AnimalView(int index, String type, String name, String condition, String conditionName,
                         int daysWithoutFeed, boolean fedToday, int pending) { }
