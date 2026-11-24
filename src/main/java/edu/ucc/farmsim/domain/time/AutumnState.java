package edu.ucc.farmsim.domain.time;

/**
 * Pattern: State — Role: ConcreteState (autumn).
 *
 * <p>Stories: HU-09, HU-11.
 */
final class AutumnState implements SeasonState {

    private static final WeatherStrategy WEATHER = new SeasonalWeatherStrategy(Season.AUTUMN);

    static final AutumnState INSTANCE = new AutumnState();

    private AutumnState() {
                // instancia compartida
    }

    @Override
    public Season season() {
        return Season.AUTUMN;
    }

    @Override
    public WeatherStrategy weatherStrategy() {
        return WEATHER;
    }

    @Override
    public SeasonState next() {
        return WinterState.INSTANCE;
    }
}
