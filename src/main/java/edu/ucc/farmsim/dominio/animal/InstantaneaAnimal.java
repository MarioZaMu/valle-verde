package edu.ucc.farmsim.dominio.animal;

/**
 * Copia inmutable de un animal, para el memento.
 *
 * <p>Stories: HU-19, HU-21.
 *
 * @param tipo             tipo de animal
 * @param condicion        sano, hambriento o enfermo
 * @param diasSinAlimentar  días consecutivos sin comer
 * @param hoyAlimentado         si comió hoy
 * @param pendiente          unidades de producto que esperan ser recogidas
 */
public record InstantaneaAnimal(TipoAnimal tipo, CondicionAnimal condicion, int diasSinAlimentar,
                             boolean hoyAlimentado, int pendiente) { }
