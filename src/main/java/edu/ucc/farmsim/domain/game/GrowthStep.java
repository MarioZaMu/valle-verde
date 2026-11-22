package edu.ucc.farmsim.domain.game;

/**
 * Step 2 of the end of the day: crops grow if they were watered, count a dry day, or wither (RN-03, RN-08,
 * RN-09). The weather that counts is today's, before the forecast step replaces it.
 *
 * <p>Stories: HU-03, HU-05.
 */
final class GrowthStep implements DayStep {

    @Override
    public DayPhase phase() {
        return DayPhase.GROWTH;
    }

    @Override
    public void apply(GameState state, DayReport report) {
        report.addWithered(state.farm().growCrops(state.weather().today()));
    }
}
