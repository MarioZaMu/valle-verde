package edu.ucc.farmsim.domain.time;

import java.util.Random;

/**
 * Pattern: Strategy — Role: ConcreteStrategy (always the same weather).
 *
 * <p>Se usa para el primer día fijo de una partida (RN-13) y en las pruebas que necesitan un clima conocido.
 *
 * <p>Stories: HU-11.
 */
public final class FixedWeatherStrategy implements WeatherStrategy {

    private final Weather weather;

    /**
     * Crea una estrategia que siempre devuelve el mismo clima.
     *
     * @param weather el clima que se devuelve
     */
    public FixedWeatherStrategy(Weather weather) {
        this.weather = weather;
    }

    @Override
    public Weather nextWeather(int day, Random rnd) {
        return weather;
    }
}
