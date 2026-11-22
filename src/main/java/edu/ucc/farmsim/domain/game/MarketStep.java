package edu.ucc.farmsim.domain.game;

/**
 * Step 4 of RN-03: yesterday's prices are remembered and the demand recovers (RN-18, RN-19).
 *
 * <p>Stories: HU-15, HU-16.
 */
final class MarketStep implements DayStep {

    @Override
    public DayPhase phase() {
        return DayPhase.MARKET;
    }

    @Override
    public void apply(GameState state, DayReport report) {
        state.market().endOfDay(state.date().season());
    }
}
