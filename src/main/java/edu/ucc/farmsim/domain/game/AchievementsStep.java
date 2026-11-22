package edu.ucc.farmsim.domain.game;

/**
 * Step 8 of the end of the day: the achievements are evaluated again, now with the new date and the new day's
 * figures, and their rewards are paid (RN-03, RN-28).
 *
 * <p>Stories: HU-26.
 */
final class AchievementsStep implements DayStep {

    @Override
    public DayPhase phase() {
        return DayPhase.ACHIEVEMENTS;
    }

    @Override
    public void apply(GameState state, DayReport report) {
        report.addUnlocked(state.evaluateAchievements());
    }
}
