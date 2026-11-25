package edu.ucc.farmsim.aplicacion.dto;

/**
 * Un animal tal como lo muestra el panel del edificio.
 *
 * <p>Stories: HU-19, HU-21, HU-35.
 *
 * @param indice           posición del animal dentro de su edificio
 * @param tipo            id del animal, por ejemplo {@code HEN}
 * @param name            nombre en español
 * @param condicion       {@code HEALTHY}, {@code HUNGRY} or {@code SICK}
 * @param nombreCondicion   nombre en español de la condición
 * @param diasSinAlimentar días consecutivos sin comer
 * @param hoyAlimentado        si comió hoy
 * @param pendiente         unidades de producto que esperan ser recogidas
 */
public record VistaAnimal(int indice, String tipo, String name, String condicion, String nombreCondicion,
                         int diasSinAlimentar, boolean hoyAlimentado, int pendiente) { }
