package edu.ucc.farmsim.domain.game;

/**
 * Paso 8 del final del día: los logros se evalúan otra vez, ahora con la fecha nueva y las cifras del día nuevo, y se
 * pagan sus recompensas (RN-03, RN-28).
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
