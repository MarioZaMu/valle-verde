package edu.ucc.farmsim.application.dto;

/**
 * One animal as the building panel shows it.
 *
 * <p>Stories: HU-19, HU-21, HU-35.
 *
 * @param index           position of the animal inside its building
 * @param type            animal id, for example {@code HEN}
 * @param name            Spanish name
 * @param condition       {@code HEALTHY}, {@code HUNGRY} or {@code SICK}
 * @param conditionName   Spanish name of the condition
 * @param daysWithoutFeed consecutive days without eating
 * @param fedToday        whether it ate today
 * @param pending         units of product waiting to be collected
 */
public record AnimalView(int index, String type, String name, String condition, String conditionName,
                         int daysWithoutFeed, boolean fedToday, int pending) { }
