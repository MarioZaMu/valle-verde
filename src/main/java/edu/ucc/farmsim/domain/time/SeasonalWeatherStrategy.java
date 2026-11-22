package edu.ucc.farmsim.domain.time;

import edu.ucc.farmsim.domain.config.GameRules;
import java.util.Random;

/**
 * Pattern: Strategy — Role: ConcreteStrategy (seasonal probability table).
 *
 * <p>Draws the weather from the percentage table of a season (RN-11); in Autumn the table changes on
 * day {@link GameRules#AUTUMN_FROST_FROM_DAY}, when frost becomes possible.
 *
 * <p>Stories: HU-11.
 */
public final class SeasonalWeatherStrategy implements WeatherStrategy {

    private final Season season;

    /**
     * Creates the strategy of a season.
     *
     * @param season season whose table is used
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
