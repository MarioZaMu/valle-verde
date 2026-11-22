package edu.ucc.farmsim.domain.game;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Runs the end of the day as an ordered list of {@link DayStep}s (RN-03).
 *
 * <p>Steps can be registered in any order: the pipeline always runs them sorted by {@link DayPhase}, so
 * the order of RN-03 cannot be broken by whoever adds a step. Each story adds its own step.
 *
 * <p>It also closes the daily ledger: the money of the day is recorded in the report and the counters start
 * again at zero for the next day (HU-17).
 *
 * <p>Stories: HU-03, HU-10, HU-17.
 */
public final class DayPipeline {

    private final List<DayStep> steps = new ArrayList<>();

    /** The pipeline with every step implemented so far. */
    public static DayPipeline standard() {
        return new DayPipeline()
                .add(new WeatherEffectsStep())
                .add(new GrowthStep())
                .add(new AnimalsStep())
                .add(new MarketStep())
                .add(new CalendarStep())
                .add(new ForecastStep())
                .add(new EnergyResetStep());
    }

    /**
     * Registers a step.
     *
     * @return this pipeline, for chaining
     * @throws IllegalArgumentException if the step is {@code null}
     */
    public DayPipeline add(DayStep step) {
        if (step == null) {
            throw new IllegalArgumentException("step is required");
        }
        steps.add(step);
        return this;
    }

    /**
     * Runs every step in phase order on the given game.
     *
     * @return the facts recorded by the steps
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
