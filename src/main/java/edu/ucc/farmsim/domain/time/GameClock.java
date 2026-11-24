package edu.ucc.farmsim.domain.time;

import edu.ucc.farmsim.domain.config.GameRules;

/**
 * Pattern: Singleton — Role: Singleton instance (the game's only calendar).
 *
 * <p>Guarda la fecha actual y la avanza de un día a la vez (RN-01). Hay un calendario por partida en marcha; los
 * servicios y el memento lo leen y restauran a través de esta instancia. El juego usa un solo hilo (S-01), así que no
 * hace falta sincronización.
 *
 * <p>Las pruebas deben llamar a {@link #resetForTests()} antes de cada caso para que ninguna fecha se filtre entre
 * ellos.
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

    /** Devuelve el único reloj. */
    public static GameClock getInstance() {
        return INSTANCE;
    }

    /** Fecha actual. */
    public GameDate date() {
        return new GameDate(year, season.season(), day);
    }

    /** Estado de la estación actual. */
    public SeasonState seasonState() {
        return season;
    }

    /** La fecha del día siguiente, sin avanzar el reloj. */
    public GameDate tomorrow() {
        if (day < GameRules.DAYS_PER_SEASON) {
            return new GameDate(year, season.season(), day + 1);
        }
        SeasonState next = season.next();
        int nextYear = season.season() == Season.WINTER ? year + 1 : year;
        return new GameDate(nextYear, next.season(), GameRules.START_DAY);
    }

    /** Mueve el calendario un día adelante: el día 28 de una estación pasa al día 1 de la siguiente. */
    public void advanceDay() {
        GameDate next = tomorrow();
        this.year = next.year();
        this.day = next.day();
        this.season = SeasonState.of(next.season());
    }

    /** Fija el calendario en una fecha dada (lo usa el memento al restaurar una partida). */
    public void restore(GameDate date) {
        this.year = date.year();
        this.day = date.day();
        this.season = SeasonState.of(date.season());
    }

    /** Devuelve el reloj al Año 1, Primavera, día 1. */
    public void resetForTests() {
        restore(GameDate.start());
    }
}
