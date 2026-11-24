package edu.ucc.farmsim.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucc.farmsim.domain.config.GameRules;
import edu.ucc.farmsim.domain.time.FixedWeatherStrategy;
import edu.ucc.farmsim.domain.time.GameClock;
import edu.ucc.farmsim.domain.time.GameDate;
import edu.ucc.farmsim.domain.time.Season;
import edu.ucc.farmsim.domain.time.SeasonalWeatherStrategy;
import edu.ucc.farmsim.domain.time.StateRandom;
import edu.ucc.farmsim.domain.time.Weather;
import edu.ucc.farmsim.domain.time.WeatherCycle;
import edu.ucc.farmsim.domain.time.WeatherStrategy;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** HU-11: clima del día y pronóstico de mañana (RN-11, RN-13). */
class HU11WeatherAndForecastTest {

    private static final int DRAWS = 10_000;
    private static final Set<Weather> SPRING_WEATHER = Set.of(Weather.SUNNY, Weather.CLOUDY, Weather.RAINY);

    private GameClock clock;

    @BeforeEach
    void resetClock() {
        clock = GameClock.getInstance();
        clock.resetForTests();
    }

    @Test
    void HU11_ca1_firstDayIsSunnyAndTheForecastForDay2IsAlreadyDrawn() {
                // Cuando empieza una partida nueva
        WeatherCycle weather = WeatherCycle.startNewGame(clock, new StateRandom(1));

                // Entonces el día 1 es Sunny (fijo) y existe un pronóstico para el día 2
        assertEquals(Weather.SUNNY, weather.today());
        assertNotNull(weather.forecast());
        assertTrue(SPRING_WEATHER.contains(weather.forecast()), "a Spring forecast: " + weather.forecast());
    }

    @Test
    void HU11_ca2_theForecastBecomesTodaysWeatherAndANewForecastIsDrawn() {
                // Dado el pronóstico «Rainy»
        WeatherCycle weather = WeatherCycle.startNewGame(clock, new StateRandom(2));
        weather.restore(Weather.SUNNY, Weather.RAINY);

                // Cuando termina el día (primero avanza el calendario, RN-03 paso 5, luego el clima, paso 6)
        clock.advanceDay();
        weather.advance(clock, new StateRandom(3));

                // Entonces hoy es Rainy y hay un nuevo pronóstico visible
        assertEquals(Weather.RAINY, weather.today());
        assertNotNull(weather.forecast());
    }

    @Test
    void HU11_ca3_sameSeedGivesTheSameWeatherForTheWholeYear() {
                // Dadas dos partidas con la misma semilla
        List<Weather> first = weatherOfAYear(12345L);
        List<Weather> second = weatherOfAYear(12345L);

                // Entonces las secuencias de 112 días son idénticas día por día
        assertEquals(112, first.size());
        assertEquals(first, second);
        assertTrue(!first.equals(weatherOfAYear(54321L)), "a different seed should give a different year");
    }

    @Test
    void HU11_ca4_probabilitiesFollowTheSeasonalTables() {
                // La Primavera nunca trae Drought, Frost ni Snow
        Map<Weather, Integer> spring = draw(new SeasonalWeatherStrategy(Season.SPRING), 10);
        assertEquals(0, spring.get(Weather.DROUGHT) + spring.get(Weather.FROST) + spring.get(Weather.SNOWY));

        // Winter never brings Rain
        Map<Weather, Integer> winter = draw(new SeasonalWeatherStrategy(Season.WINTER), 10);
        assertEquals(0, winter.get(Weather.RAINY));
        assertTrue(winter.get(Weather.SNOWY) > 0);

                // El Otoño tiene Frost solo los días 22 a 28
        SeasonalWeatherStrategy autumn = new SeasonalWeatherStrategy(Season.AUTUMN);
        assertEquals(0, draw(autumn, 21).get(Weather.FROST));
        assertTrue(draw(autumn, 22).get(Weather.FROST) > 0);
        assertTrue(draw(autumn, 28).get(Weather.FROST) > 0);

                // El Verano es Sunny cerca del 60 % del tiempo (±3 %)
        double sunny = draw(new SeasonalWeatherStrategy(Season.SUMMER), 10).get(Weather.SUNNY) / (double) DRAWS;
        assertTrue(sunny >= 0.57 && sunny <= 0.63, "sunny ratio in Summer: " + sunny);
    }

    @Test
    void HU11_everyWeatherTableAddsUpTo100Percent() {
        for (int[] table : new int[][] {GameRules.WEATHER_WEIGHTS_SPRING, GameRules.WEATHER_WEIGHTS_SUMMER,
                GameRules.WEATHER_WEIGHTS_AUTUMN_EARLY, GameRules.WEATHER_WEIGHTS_AUTUMN_LATE,
                GameRules.WEATHER_WEIGHTS_WINTER}) {
            assertEquals(Weather.values().length, table.length);
            int sum = 0;
            for (int weight : table) {
                sum += weight;
            }
            assertEquals(100, sum);
        }
    }

    @Test
    void HU11_aFixedStrategyAlwaysReturnsTheSameWeather() {
        WeatherStrategy fixed = new FixedWeatherStrategy(Weather.DROUGHT);
        for (int day = 1; day <= GameRules.DAYS_PER_SEASON; day++) {
            assertEquals(Weather.DROUGHT, fixed.nextWeather(day, new StateRandom(day)));
        }
    }

    @Test
    void HU11_aTestStrategyOverridesTheSeasonalOne() {
        WeatherCycle weather = WeatherCycle.startNewGame(clock, new StateRandom(1), new FixedWeatherStrategy(Weather.FROST));
        assertEquals(Weather.FROST, weather.forecast());
    }

    @Test
    void RNF08_stateRandomRestoresExactlyTheSameSequence() {
        StateRandom original = new StateRandom(99);
        original.nextInt(100);
        long saved = original.state();
        int expectedNext = original.nextInt(100);

        StateRandom restored = new StateRandom(0);
        restored.restore(saved);

        assertEquals(expectedNext, restored.nextInt(100));
        assertEquals(original.nextInt(1000), restored.nextInt(1000));
    }

    @Test
    void RNF08_sameSeedGivesTheSameSequence() {
        StateRandom a = new StateRandom(7);
        StateRandom b = new StateRandom(7);
        for (int i = 0; i < 50; i++) {
            assertEquals(a.nextInt(1000), b.nextInt(1000));
        }
    }

    private List<Weather> weatherOfAYear(long seed) {
        clock.resetForTests();
        StateRandom random = new StateRandom(seed);
        WeatherCycle cycle = WeatherCycle.startNewGame(clock, random);
        List<Weather> year = new ArrayList<>();
        year.add(cycle.today());
        for (int day = 2; day <= 112; day++) {
            clock.advanceDay();
            cycle.advance(clock, random);
            year.add(cycle.today());
        }
        return year;
    }

    private static Map<Weather, Integer> draw(WeatherStrategy strategy, int day) {
        StateRandom random = new StateRandom(42);
        Map<Weather, Integer> counts = new EnumMap<>(Weather.class);
        for (Weather weather : Weather.values()) {
            counts.put(weather, 0);
        }
        for (int i = 0; i < DRAWS; i++) {
            counts.merge(strategy.nextWeather(day, random), 1, Integer::sum);
        }
        return counts;
    }

    @Test
    void HU11_dateOfTheForecastIsTheDayAfterTheCurrentOne() {
        clock.restore(new GameDate(1, Season.WINTER, GameRules.DAYS_PER_SEASON));
                // El pronóstico del último día de Invierno se sortea con la tabla de Primavera del año siguiente
        WeatherCycle cycle = WeatherCycle.startNewGame(clock, new StateRandom(5));
        assertTrue(SPRING_WEATHER.contains(cycle.forecast()));
    }
}
