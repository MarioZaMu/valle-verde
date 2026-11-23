package edu.ucc.farmsim.domain.progress;

/**
 * The steps of the start tutorial, in the order the player goes through them (RN-39), and the two ways it ends:
 * every step done, or skipped.
 *
 * <p>Stories: HU-49.
 */
public enum TutorialStep {
    /** Take a step with the avatar. */
    WALK,
    /** Sow a seed. */
    SOW,
    /** Water a crop. */
    WATER,
    /** Talk to a person of the valley. */
    TALK,
    /** End the day. */
    END_DAY,
    /** Every step was done. */
    DONE,
    /** The player skipped it. */
    SKIPPED;

    /** How many steps the player goes through. */
    public static final int COUNT = 5;

    /** True once the tutorial ended, done or skipped. */
    public boolean isFinished() {
        return this == DONE || this == SKIPPED;
    }

    /** Position of the step, from 1 to {@link #COUNT}; a finished tutorial counts as the last one. */
    public int number() {
        return isFinished() ? COUNT : ordinal() + 1;
    }

    /** The step that comes after this one; {@link #DONE} after the last. */
    TutorialStep next() {
        return isFinished() ? this : values()[ordinal() + 1];
    }
}
