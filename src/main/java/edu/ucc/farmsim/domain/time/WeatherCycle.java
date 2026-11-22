package edu.ucc.farmsim.domain.time;

import java.util.Random;

/**
 * The weather of today and the forecast of tomorrow (RN-13).
 *
 * <p>A new game starts Sunny with the forecast of day 2 already drawn. Each end of the day the forecast
 * becomes today's weather and a new forecast is drawn for the day after. The forecast is always reliable.
 *
 * <p>Stories: HU-11.
 */
public final class WeatherCycle {

    private Weather today;
    private Weather forecast;
    private final WeatherStrategy override;

    private WeatherCycle(Weather today, Weather forecast, WeatherStrategy override) {
        this.today = today;
        this.forecast = forecast;
        this.override = override;
    }

    /** Starts a new game: day 1 is Sunny and the forecast of day 2 is drawn from its season table. */
    public static WeatherCycle startNewGame(GameClock clock, Random rnd) {
        return startNewGame(clock, rnd, null);
    }

    /**
     * Starts a new game using a given strategy for every forecast (for tests that need a known weather).
     *
     * @param override strategy used instead of the seasonal tables, or {@code null} for the normal tables
     */
    public static WeatherCycle startNewGame(GameClock clock, Random rnd, WeatherStrategy override) {
        WeatherCycle cycle = new WeatherCycle(Weather.SUNNY, Weather.SUNNY, override);
        cycle.forecast = cycle.drawForTomorrow(clock, rnd);
        return cycle;
    }

    /**
     * Step 6 of the end of the day (RN-03): call it after the calendar has advanced.
     * Today's weather becomes the old forecast, and a new forecast is drawn for the day after.
     */
    public void advance(GameClock clock, Random rnd) {
        this.today = forecast;
        this.forecast = drawForTomorrow(clock, rnd);
    }

    /** Weather of the current day. */
    public Weather today() {
        return today;
    }

    /** Forecast for the next day. */
    public Weather forecast() {
        return forecast;
    }

    /** Sets both values (used by tests and by the memento when restoring a game). */
    public void restore(Weather restoredToday, Weather restoredForecast) {
        this.today = restoredToday;
        this.forecast = restoredForecast;
    }

    private Weather drawForTomorrow(GameClock clock, Random rnd) {
        GameDate tomorrow = clock.tomorrow();
        WeatherStrategy strategy = override != null
                ? override
                : SeasonState.of(tomorrow.season()).weatherStrategy();
        return strategy.nextWeather(tomorrow.day(), rnd);
    }
}
