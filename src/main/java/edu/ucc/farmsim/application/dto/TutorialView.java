package edu.ucc.farmsim.application.dto;

/**
 * The start tutorial as the avatar's box shows it (RN-39). Immutable.
 *
 * <p>Stories: HU-49.
 *
 * @param step     {@code WALK}, {@code SOW}, {@code WATER}, {@code TALK}, {@code END_DAY}, or how it ended:
 *                 {@code DONE} or {@code SKIPPED}
 * @param number   position of the step, from 1 to {@code total}
 * @param total    how many steps there are
 * @param text     what the avatar says, in Spanish
 * @param finished whether it ended (then the box is hidden)
 */
public record TutorialView(String step, int number, int total, String text, boolean finished) { }
