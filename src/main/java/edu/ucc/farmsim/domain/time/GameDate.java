package edu.ucc.farmsim.domain.time;

import edu.ucc.farmsim.domain.config.GameRules;

/**
 * A calendar date: year (from 1), season and day of the season (1 to 28).
 *
 * <p>Stories: HU-09.
 *
 * @param year   year, at least {@link GameRules#START_YEAR}
 * @param season season of the year
 * @param day    day of the season, from 1 to {@link GameRules#DAYS_PER_SEASON}
 */
public record GameDate(int year, Season season, int day) {

    /** Validates the fields. */
    public GameDate {
        if (year < GameRules.START_YEAR) {
            throw new IllegalArgumentException("year must be at least " + GameRules.START_YEAR + ": " + year);
        }
        if (season == null) {
            throw new IllegalArgumentException("season is required");
        }
        if (day < 1 || day > GameRules.DAYS_PER_SEASON) {
            throw new IllegalArgumentException("day must be between 1 and " + GameRules.DAYS_PER_SEASON + ": " + day);
        }
    }

    /** The date on which a new game starts: Year 1, Spring, day 1 (RN-30). */
    public static GameDate start() {
        return new GameDate(GameRules.START_YEAR, Season.SPRING, GameRules.START_DAY);
    }
}
