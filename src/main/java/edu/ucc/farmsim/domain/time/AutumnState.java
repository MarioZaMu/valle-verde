package edu.ucc.farmsim.domain.time;

/**
 * Pattern: State — Role: ConcreteState (autumn).
 *
 * <p>Stories: HU-09.
 */
final class AutumnState implements SeasonState {

    static final AutumnState INSTANCE = new AutumnState();

    private AutumnState() {
        // shared instance
    }

    @Override
    public Season season() {
        return Season.AUTUMN;
    }

    @Override
    public SeasonState next() {
        return WinterState.INSTANCE;
    }
}
