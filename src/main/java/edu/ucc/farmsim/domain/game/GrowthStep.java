package edu.ucc.farmsim.domain.game;

/**
 * Step 2 of the end of the day: crops grow if they were watered (RN-03, RN-08).
 *
 * <p>Stories: HU-03.
 */
final class GrowthStep implements DayStep {

    @Override
    public DayPhase phase() {
        return DayPhase.GROWTH;
    }

    @Override
    public void apply(GameState state, DayReport report) {
        state.farm().growCrops();
    }
}
