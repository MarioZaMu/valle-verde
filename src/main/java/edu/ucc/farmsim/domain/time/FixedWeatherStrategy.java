package edu.ucc.farmsim.domain.time;

import java.util.Random;

/**
 * Pattern: Strategy — Role: ConcreteStrategy (always the same weather).
 *
 * <p>Used for the fixed first day of a game (RN-13) and by tests that need a known weather.
 *
 * <p>Stories: HU-11.
 */
public final class FixedWeatherStrategy implements WeatherStrategy {

    private final Weather weather;

    /**
     * Creates a strategy that always returns the same weather.
     *
     * @param weather the weather to return
     */
    public FixedWeatherStrategy(Weather weather) {
        this.weather = weather;
    }

    @Override
    public Weather nextWeather(int day, Random rnd) {
        return weather;
    }
}
