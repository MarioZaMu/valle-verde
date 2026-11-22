package edu.ucc.farmsim.domain.time;

/**
 * Pattern: State — Role: ConcreteState (winter).
 *
 * <p>Stories: HU-09.
 */
final class WinterState implements SeasonState {

    static final WinterState INSTANCE = new WinterState();

    private WinterState() {
        // shared instance
    }

    @Override
    public Season season() {
        return Season.WINTER;
    }

    @Override
    public SeasonState next() {
        return SpringState.INSTANCE;
    }
}
