package edu.ucc.farmsim.domain.game;

/**
 * Step 7 of the end of the day: energy goes back to the maximum (RN-02, RN-03).
 *
 * <p>Stories: HU-10.
 */
final class EnergyResetStep implements DayStep {

    @Override
    public DayPhase phase() {
        return DayPhase.ENERGY;
    }

    @Override
    public void apply(GameState state, DayReport report) {
        state.energy().restore();
    }
}
