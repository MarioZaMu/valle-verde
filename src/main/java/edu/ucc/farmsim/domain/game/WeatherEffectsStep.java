package edu.ucc.farmsim.domain.game;

import edu.ucc.farmsim.domain.time.Weather;

/**
 * Step 1 of RN-03: the weather of the day that ends acts on the crops in the open (RN-12). Rain waters them
 * before they grow; frost withers them, mature ones included. Drought acts later, when the dry days are counted.
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
