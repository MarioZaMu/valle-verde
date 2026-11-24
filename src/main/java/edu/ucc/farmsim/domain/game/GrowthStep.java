package edu.ucc.farmsim.domain.game;

/**
 * Paso 2 del final del día: los cultivos crecen si fueron regados, cuentan un día seco o se marchitan (RN-03, RN-08,
 * RN-09). El clima que cuenta es el de hoy, antes de que el paso del pronóstico lo reemplace.
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
