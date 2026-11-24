package edu.ucc.farmsim.domain.game;

/**
 * Paso 7 del final del día: la energía vuelve al máximo (RN-02, RN-03).
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
