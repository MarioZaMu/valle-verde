package edu.ucc.farmsim.domain.game;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Ejecuta el final del día como una lista ordenada de {@link DayStep} (RN-03).
 *
 * <p>Los pasos se pueden registrar en cualquier orden: el pipeline siempre los ejecuta ordenados por {@link DayPhase},
 * de modo que quien agrega un paso no puede romper el orden de RN-03. Cada historia agrega su propio paso.
 *
 * <p>También cierra el libro diario: el dinero del día se registra en el informe y los contadores vuelven a cero para
 * el día siguiente (HU-17).
 *
 * <p>Stories: HU-03, HU-10, HU-17.
 */
public final class DayPipeline {

    private final List<DayStep> steps = new ArrayList<>();

    /** El pipeline con todos los pasos implementados hasta ahora. */
    public static DayPipeline standard() {
        return new DayPipeline()
                .add(new WeatherEffectsStep())
                .add(new GrowthStep())
                .add(new AnimalsStep())
                .add(new MarketStep())
                .add(new CalendarStep())
                .add(new ForecastStep())
                .add(new EnergyResetStep())
                .add(new AchievementsStep());
    }

    /**
     * Registra un paso.
     *
     * @return este pipeline, para encadenar llamadas
     * @throws IllegalArgumentException si el paso es {@code null}
     */
    public DayPipeline add(DayStep step) {
        if (step == null) {
            throw new IllegalArgumentException("step is required");
        }
        steps.add(step);
        return this;
    }

    /**
     * Ejecuta todos los pasos en orden de fase sobre la partida dada.
     *
     * @return los hechos registrados por los pasos
     */
    public DayReport run(GameState state) {
        List<DayStep> ordered = new ArrayList<>(steps);
        ordered.sort(Comparator.comparing(DayStep::phase));
        DayReport report = new DayReport();
        for (DayStep step : ordered) {
            step.apply(state, report);
            report.markRun(step.phase());
        }
        report.recordMoney(state.money(), state.ledger().income(), state.ledger().expenses());
        state.ledger().reset();
        return report;
    }
}
