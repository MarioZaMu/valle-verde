package edu.ucc.farmsim.domain.animal;

/**
 * Immutable copy of an animal, for the memento.
 *
 * <p>Stories: HU-19, HU-21.
 *
 * @param type             kind of animal
 * @param condition        healthy, hungry or sick
 * @param daysWithoutFeed  consecutive days without eating
 * @param fedToday         whether it ate today
 * @param pending          units of product waiting to be collected
 */
public record AnimalSnapshot(AnimalType type, AnimalCondition condition, int daysWithoutFeed,
                             boolean fedToday, int pending) { }
