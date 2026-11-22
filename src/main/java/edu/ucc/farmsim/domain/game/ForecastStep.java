package edu.ucc.farmsim.domain.game;

import edu.ucc.farmsim.domain.time.GameClock;

/**
 * Step 6 of the end of the day: the forecast becomes today's weather and a new forecast is drawn (RN-13).
 * It runs after the calendar step, so the new forecast is for the day after the new today.
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
