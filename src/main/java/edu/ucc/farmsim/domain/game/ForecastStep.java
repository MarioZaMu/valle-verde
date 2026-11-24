package edu.ucc.farmsim.domain.game;

import edu.ucc.farmsim.domain.time.GameClock;

/**
 * Paso 6 del final del día: el pronóstico pasa a ser el clima de hoy y se sortea un pronóstico nuevo (RN-13). Corre
 * después del paso del calendario, así que el pronóstico nuevo es para el día siguiente al nuevo hoy.
 *
 * <p>Stories: HU-10.
 */
final class ForecastStep implements DayStep {

    @Override
    public DayPhase phase() {
        return DayPhase.FORECAST;
    }

    @Override
    public void apply(GameState state, DayReport report) {
        state.weather().advance(GameClock.getInstance(), state.random());
        report.recordWeather(state.weather().today(), state.weather().forecast());
    }
}
