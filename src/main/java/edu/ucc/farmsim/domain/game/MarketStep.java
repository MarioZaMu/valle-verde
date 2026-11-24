package edu.ucc.farmsim.domain.game;

/**
 * Paso 4 de RN-03: se recuerdan los precios de ayer y la demanda se recupera (RN-18, RN-19).
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
