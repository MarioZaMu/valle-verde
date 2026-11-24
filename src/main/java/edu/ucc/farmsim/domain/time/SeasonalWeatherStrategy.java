package edu.ucc.farmsim.domain.time;

import edu.ucc.farmsim.domain.config.GameRules;
import java.util.Random;

/**
 * Pattern: Strategy — Role: ConcreteStrategy (seasonal probability table).
 *
 * <p>Sortea el clima a partir de la tabla de porcentajes de una estación (RN-11); en otoño la tabla cambia el día
 * {@link GameRules#AUTUMN_FROST_FROM_DAY}, cuando la helada se vuelve posible.
 *
 * <p>Stories: HU-11.
 */
public final class SeasonalWeatherStrategy implements WeatherStrategy {

    private final Season season;

    /**
     * Crea la estrategia de una estación.
     *
     * @param season estación cuya tabla se usa
     */
    public SeasonalWeatherStrategy(Season season) {
        this.season = season;
    }

    @Override
    public Weather nextWeather(int day, Random rnd) {
        int[] weights = weights(day);
        int roll = rnd.nextInt(GameRules.WEATHER_TABLE_TOTAL);
        int accumulated = 0;
        for (int i = 0; i < weights.length; i++) {
            accumulated += weights[i];
            if (roll < accumulated) {
                return Weather.values()[i];
            }
        }
        throw new IllegalStateException("Weather table does not add up to " + GameRules.WEATHER_TABLE_TOTAL);
    }

    private int[] weights(int day) {
        return switch (season) {
            case SPRING -> GameRules.WEATHER_WEIGHTS_SPRING;
            case SUMMER -> GameRules.WEATHER_WEIGHTS_SUMMER;
            case AUTUMN -> day >= GameRules.AUTUMN_FROST_FROM_DAY
                    ? GameRules.WEATHER_WEIGHTS_AUTUMN_LATE
                    : GameRules.WEATHER_WEIGHTS_AUTUMN_EARLY;
            case WINTER -> GameRules.WEATHER_WEIGHTS_WINTER;
        };
    }
}
