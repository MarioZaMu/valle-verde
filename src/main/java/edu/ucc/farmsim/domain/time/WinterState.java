package edu.ucc.farmsim.domain.time;

/**
 * Pattern: State — Role: ConcreteState (winter).
 *
 * <p>Stories: HU-09, HU-11.
 */
final class WinterState implements SeasonState {

    private static final WeatherStrategy WEATHER = new SeasonalWeatherStrategy(Season.WINTER);

    static final WinterState INSTANCE = new WinterState();

    private WinterState() {
        // shared instance
    }

    @Override
    public Season season() {
        return Season.WINTER;
    }

    @Override
    public WeatherStrategy weatherStrategy() {
        return WEATHER;
    }

    @Override
    public SeasonState next() {
        return SpringState.INSTANCE;
    }
}
