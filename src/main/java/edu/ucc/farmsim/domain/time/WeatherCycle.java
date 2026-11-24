package edu.ucc.farmsim.domain.time;

import java.util.Random;

/**
 * El clima de hoy y el pronóstico de mañana (RN-13).
 *
 * <p>Una partida nueva empieza soleada con el pronóstico del día 2 ya sorteado. Cada final de día el pronóstico pasa a
 * ser el clima de hoy y se sortea uno nuevo para el día siguiente. El pronóstico siempre es fiable.
 *
 * <p>Stories: HU-11.
 */
public final class WeatherCycle {

    private Weather today;
    private Weather forecast;
    private final WeatherStrategy override;

    private WeatherCycle(Weather today, Weather forecast, WeatherStrategy override) {
        this.today = today;
        this.forecast = forecast;
        this.override = override;
    }

    /**
     * Empieza una partida nueva: el día 1 es soleado y el pronóstico del día 2 se sortea de la tabla de su estación.
     */
    public static WeatherCycle startNewGame(GameClock clock, Random rnd) {
        return startNewGame(clock, rnd, null);
    }

    /**
     * Empieza una partida nueva usando una estrategia dada para cada pronóstico (para las pruebas que necesitan un
     * clima conocido).
     *
     * @param override estrategia que se usa en lugar de las tablas por estación, o {@code null} para las tablas
     *                 normales
     */
    public static WeatherCycle startNewGame(GameClock clock, Random rnd, WeatherStrategy override) {
        WeatherCycle cycle = new WeatherCycle(Weather.SUNNY, Weather.SUNNY, override);
        cycle.forecast = cycle.drawForTomorrow(clock, rnd);
        return cycle;
    }

    /**
     * Paso 6 del final del día (RN-03): llámalo después de que el calendario avanzó. El clima de hoy pasa a ser el
     * antiguo pronóstico y se sortea un pronóstico nuevo para el día siguiente.
     */
    public void advance(GameClock clock, Random rnd) {
        this.today = forecast;
        this.forecast = drawForTomorrow(clock, rnd);
    }

    /** Clima del día actual. */
    public Weather today() {
        return today;
    }

    /** Pronóstico para el día siguiente. */
    public Weather forecast() {
        return forecast;
    }

    /** Fija ambos valores (lo usan las pruebas y el memento al restaurar una partida). */
    public void restore(Weather restoredToday, Weather restoredForecast) {
        this.today = restoredToday;
        this.forecast = restoredForecast;
    }

    private Weather drawForTomorrow(GameClock clock, Random rnd) {
        GameDate tomorrow = clock.tomorrow();
        WeatherStrategy strategy = override != null
                ? override
                : SeasonState.of(tomorrow.season()).weatherStrategy();
        return strategy.nextWeather(tomorrow.day(), rnd);
    }
}
