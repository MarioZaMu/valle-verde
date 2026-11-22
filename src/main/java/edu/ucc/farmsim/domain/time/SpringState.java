package edu.ucc.farmsim.domain.time;

/**
 * Pattern: State — Role: ConcreteState (spring).
 *
 * <p>Stories: HU-09, HU-11.
 */
final class SpringState implements SeasonState {

    private static final WeatherStrategy WEATHER = new SeasonalWeatherStrategy(Season.SPRING);

    static final SpringState INSTANCE = new SpringState();

    private SpringState() {
        // shared instance
    }

    @Override
    public Season season() {
        return Season.SPRING;
    }

    @Override
    public WeatherStrategy weatherStrategy() {
        return WEATHER;
    }

    @Override
    public SeasonState next() {
        return SummerState.INSTANCE;
    }
}
