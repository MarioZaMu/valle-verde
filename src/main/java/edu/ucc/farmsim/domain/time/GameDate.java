package edu.ucc.farmsim.domain.time;

import edu.ucc.farmsim.domain.config.GameRules;

/**
 * Una fecha del calendario: año (desde 1), estación y día de la estación (1 a 28).
 *
 * <p>Stories: HU-09.
 *
 * @param year   año, al menos {@link GameRules#START_YEAR}
 * @param season estación del año
 * @param day    día de la estación, de 1 a {@link GameRules#DAYS_PER_SEASON}
 */
public record GameDate(int year, Season season, int day) {

    /** Valida los campos. */
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

    /** La fecha en que empieza una partida nueva: Año 1, Primavera, día 1 (RN-30). */
    public static GameDate start() {
        return new GameDate(GameRules.START_YEAR, Season.SPRING, GameRules.START_DAY);
    }
}
