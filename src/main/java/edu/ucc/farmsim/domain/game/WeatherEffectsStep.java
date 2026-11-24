package edu.ucc.farmsim.domain.game;

import edu.ucc.farmsim.domain.time.Weather;

/**
 * Paso 1 de RN-03: el clima del día que termina actúa sobre los cultivos a la intemperie (RN-12). La lluvia los riega
 * antes de que crezcan; la helada los marchita, incluidos los maduros. La sequía actúa después, cuando se cuentan los
 * días secos.
 *
 * <p>Stories: HU-12.
 */
final class WeatherEffectsStep implements DayStep {

    @Override
    public DayPhase phase() {
        return DayPhase.WEATHER_EFFECTS;
    }

    @Override
    public void apply(GameState state, DayReport report) {
        Weather today = state.weather().today();
        if (today == Weather.RAINY) {
            state.farm().rainOnCrops();
        } else if (today == Weather.FROST) {
            report.addWithered(state.farm().frostCrops());
        }
    }
}
