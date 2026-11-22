package edu.ucc.farmsim.domain.time;

import java.util.Random;

/**
 * Pattern: Strategy — Role: Strategy interface (how the weather of a day is drawn).
 *
 * <p>Stories: HU-11.
 */
public interface WeatherStrategy {

    /**
     * Draws the weather of a day.
     *
     * @param day day of the season (1 to 28) the weather is drawn for
     * @param rnd injected random generator
     */
    Weather nextWeather(int day, Random rnd);
}
