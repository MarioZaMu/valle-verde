package edu.ucc.farmsim.domain.time;

import java.util.Random;

/**
 * Pattern: Strategy — Role: Strategy interface (how the weather of a day is drawn).
 *
 * <p>Stories: HU-11.
 */
public interface WeatherStrategy {

    /**
     * Sortea el clima de un día.
     *
     * @param day día de la estación (1 a 28) para el que se sortea el clima
     * @param rnd generador aleatorio inyectado
     */
    Weather nextWeather(int day, Random rnd);
}
