package edu.ucc.farmsim.domain.progress;

/**
 * Where the player is in the start tutorial (RN-39). It starts at {@link TutorialStep#WALK}; an action moves it
 * on only if it is the action of the current step, and once finished, done or skipped, it never starts again.
 * It changes nothing else in the game.
 *
 * <p>Stories: HU-49.
 */
public final class Tutorial {

    private TutorialStep step = TutorialStep.WALK;

    /** The current step, or how it ended. */
    public TutorialStep step() {
        return step;
    }

    /** True once the tutorial ended. */
    public boolean isFinished() {
        return step.isFinished();
    }

    /**
     * The player did an action.
     *
     * @param action the step the action belongs to
     * @return {@code true} if it was the current step and the tutorial moved on
     */
    public boolean complete(TutorialStep action) {
        if (isFinished() || action != step) {
            return false;
        }
        step = step.next();
        return true;
    }

    /**
     * Ends the tutorial at once.
     *
     * @return {@code true} if it was still running
     */
    public boolean skip() {
        if (isFinished()) {
            return false;
        }
        step = TutorialStep.SKIPPED;
        return true;
    }

    /** The step, for the memento. */
    public TutorialStep snapshot() {
        return step;
    }

    /** Puts the step back; a save written before the tutorial ({@code null}) loads it finished. */
    public void restoreFrom(TutorialStep saved) {
        step = saved == null ? TutorialStep.DONE : saved;
    }
}
