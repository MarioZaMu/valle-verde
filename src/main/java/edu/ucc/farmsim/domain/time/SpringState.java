package edu.ucc.farmsim.domain.time;

/**
 * Pattern: State — Role: ConcreteState (spring).
 *
 * <p>Stories: HU-09.
 */
final class SpringState implements SeasonState {

    static final SpringState INSTANCE = new SpringState();

    private SpringState() {
        // shared instance
    }

    @Override
    public Season season() {
        return Season.SPRING;
    }

    @Override
    public SeasonState next() {
        return SummerState.INSTANCE;
    }
}
