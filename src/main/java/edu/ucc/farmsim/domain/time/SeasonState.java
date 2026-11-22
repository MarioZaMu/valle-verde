package edu.ucc.farmsim.domain.time;

/**
 * Pattern: State — Role: State interface (the behaviour that changes with the season).
 *
 * <p>Each season knows which season follows it. The state objects are stateless and shared.
 *
 * <p>Stories: HU-09, HU-11.
 */
public interface SeasonState {

    /** The season this state represents. */
    Season season();

    /** The state that follows this one in the calendar (Winter is followed by Spring). */
    SeasonState next();

    /** The strategy that draws the weather of days in this season (RN-11). */
    WeatherStrategy weatherStrategy();

    /** Returns the shared state object of a season. */
    static SeasonState of(Season season) {
        return switch (season) {
            case SPRING -> SpringState.INSTANCE;
            case SUMMER -> SummerState.INSTANCE;
            case AUTUMN -> AutumnState.INSTANCE;
            case WINTER -> WinterState.INSTANCE;
        };
    }
}
