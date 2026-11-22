package edu.ucc.farmsim.domain.time;

/**
 * Pattern: State — Role: ConcreteState (summer).
 *
 * <p>Stories: HU-09.
 */
final class SummerState implements SeasonState {

    static final SummerState INSTANCE = new SummerState();

    private SummerState() {
        // shared instance
    }

    @Override
    public Season season() {
        return Season.SUMMER;
    }

    @Override
    public SeasonState next() {
        return AutumnState.INSTANCE;
    }
}
