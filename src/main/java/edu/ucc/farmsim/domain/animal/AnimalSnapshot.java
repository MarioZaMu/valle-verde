package edu.ucc.farmsim.domain.animal;

/**
 * Copia inmutable de un animal, para el memento.
 *
 * <p>Stories: HU-19, HU-21.
 *
 * @param type             tipo de animal
 * @param condition        sano, hambriento o enfermo
 * @param daysWithoutFeed  días consecutivos sin comer
 * @param fedToday         si comió hoy
 * @param pending          unidades de producto que esperan ser recogidas
 */
public record AnimalSnapshot(AnimalType type, AnimalCondition condition, int daysWithoutFeed,
                             boolean fedToday, int pending) { }
