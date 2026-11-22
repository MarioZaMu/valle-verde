package edu.ucc.farmsim.domain.time;

import edu.ucc.farmsim.domain.config.GameRules;

/**
 * Pattern: Singleton — Role: Singleton instance (the game's only calendar).
 *
 * <p>Holds the current date and moves it forward one day at a time (RN-01). There is one calendar per
 * running game; the services and the memento read and restore it through this instance. The game is
 * single-threaded (S-01), so no synchronization is needed.
 *
 * <p>Tests must call {@link #resetForTests()} before each case so no date leaks between them.
 *
 * <p>Stories: HU-09.
 */
public final class GameClock {

    private static final GameClock INSTANCE = new GameClock();

    private int year;
    private int day;
    private SeasonState season;

    private GameClock() {
        resetForTests();
    }

    /** Returns the one and only clock. */
    public static GameClock getInstance() {
        return INSTANCE;
    }

    /** Current date. */
    public GameDate date() {
        return new GameDate(year, season.season(), day);
    }

    /** State of the current season. */
    public SeasonState seasonState() {
        return season;
    }

    /** The date of the next day, without advancing the clock. */
    public GameDate tomorrow() {
        if (day < GameRules.DAYS_PER_SEASON) {
            return new GameDate(year, season.season(), day + 1);
        }
        SeasonState next = season.next();
        int nextYear = season.season() == Season.WINTER ? year + 1 : year;
        return new GameDate(nextYear, next.season(), GameRules.START_DAY);
    }

    /** Moves the calendar one day forward: day 28 of a season rolls into day 1 of the next one. */
    public void advanceDay() {
        GameDate next = tomorrow();
        this.year = next.year();
        this.day = next.day();
        this.season = SeasonState.of(next.season());
    }

    /** Sets the calendar to a given date (used by the memento when restoring a game). */
    public void restore(GameDate date) {
        this.year = date.year();
        this.day = date.day();
        this.season = SeasonState.of(date.season());
    }

    /** Puts the clock back at Year 1, Spring, day 1. */
    public void resetForTests() {
        restore(GameDate.start());
    }
}
