package edu.ucc.farmsim.domain.game;

/**
 * One step of the end of the day. Each story that adds a daily effect registers its own step in the
 * {@link DayPipeline}; the pipeline runs them in the order of their {@link DayPhase}.
 *
 * <p>Stories: HU-10.
 */
public interface DayStep {

    /** The phase of RN-03 this step belongs to. */
    DayPhase phase();

    /**
     * Applies the step to the game.
     *
     * @param state  the game being advanced
     * @param report facts about the day that the application turns into events
     */
    void apply(GameState state, DayReport report);
}
