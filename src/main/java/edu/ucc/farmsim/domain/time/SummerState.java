package edu.ucc.farmsim.domain.time;

/**
 * Pattern: State — Role: ConcreteState (summer).
 *
 * <p>Stories: HU-09, HU-11.
 */
final class SummerState implements SeasonState {

    private static final WeatherStrategy WEATHER = new SeasonalWeatherStrategy(Season.SUMMER);

    static final SummerState INSTANCE = new SummerState();

    private SummerState() {
                // instancia compartida
    }

    @Override
    public Season season() {
        return Season.SUMMER;
    }

    @Override
    public WeatherStrategy weatherStrategy() {
        return WEATHER;
    }

    @Override
    public SeasonState next() {
        return AutumnState.INSTANCE;
    }
}
