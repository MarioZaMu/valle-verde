package edu.ucc.farmsim.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.ucc.farmsim.domain.config.GameRules;
import edu.ucc.farmsim.domain.time.GameClock;
import edu.ucc.farmsim.domain.time.GameDate;
import edu.ucc.farmsim.domain.time.Season;
import edu.ucc.farmsim.domain.time.SeasonState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** HU-09: calendario con 4 estaciones de 28 días y años de 112 días (RN-01). */
class HU09CalendarAndSeasonsTest {

    private GameClock clock;

    @BeforeEach
    void resetClock() {
        clock = GameClock.getInstance();
        clock.resetForTests();
    }

    @Test
    void HU09_ca1_advancesOneDayAtATime() {
        // Given Year 1, Spring, day 1
        assertEquals(new GameDate(1, Season.SPRING, 1), clock.date());

                // Cuando termina el día
        clock.advanceDay();

                // Entonces es Año 1, Primavera, día 2
        assertEquals(new GameDate(1, Season.SPRING, 2), clock.date());
    }

    @Test
    void HU09_ca2_changesSeasonAfterDay28InOrderSpringSummerAutumnWinter() {
                // Dado el día 28 de Primavera
        clock.restore(new GameDate(1, Season.SPRING, GameRules.DAYS_PER_SEASON));

                // Cuando termina el día, entonces es el día 1 de Verano del mismo año
        clock.advanceDay();
        assertEquals(new GameDate(1, Season.SUMMER, 1), clock.date());

                // Y el orden continúa Verano -> Otoño -> Invierno
        advanceDays(GameRules.DAYS_PER_SEASON);
        assertEquals(new GameDate(1, Season.AUTUMN, 1), clock.date());
        advanceDays(GameRules.DAYS_PER_SEASON);
        assertEquals(new GameDate(1, Season.WINTER, 1), clock.date());
    }

    @Test
    void HU09_ca3_changesYearAfterDay28OfWinter() {
                // Dado el día 28 de Invierno del Año 1
        clock.restore(new GameDate(1, Season.WINTER, GameRules.DAYS_PER_SEASON));

                // Cuando termina el día, entonces es Año 2, Primavera, día 1
        clock.advanceDay();

        assertEquals(new GameDate(2, Season.SPRING, 1), clock.date());
    }

    @Test
    void HU09_ca4_aFullCycleOf112DaysReturnsToSpringAndTheGameNeverEnds() {
        // Given a new game
        int daysPerYear = GameRules.DAYS_PER_SEASON * Season.values().length;
        assertEquals(112, daysPerYear);

                // Cuando pasan 112 días, entonces es Año 2, Primavera, día 1
        advanceDays(daysPerYear);
        assertEquals(new GameDate(2, Season.SPRING, 1), clock.date());

                // Y el juego simplemente sigue
        advanceDays(1);
        assertEquals(new GameDate(2, Season.SPRING, 2), clock.date());
    }

    @Test
    void HU09_tomorrowPeeksAtTheNextDateWithoutAdvancing() {
        clock.restore(new GameDate(1, Season.WINTER, GameRules.DAYS_PER_SEASON));

        assertEquals(new GameDate(2, Season.SPRING, 1), clock.tomorrow());
        assertEquals(new GameDate(1, Season.WINTER, GameRules.DAYS_PER_SEASON), clock.date());
    }

    @Test
    void HU09_clockIsASingleSharedInstance() {
        assertSame(GameClock.getInstance(), GameClock.getInstance());
    }

    @Test
    void HU09_eachSeasonStateKnowsItsSeasonAndTheNextOne() {
        for (Season season : Season.values()) {
            SeasonState state = SeasonState.of(season);
            assertEquals(season, state.season());
            assertEquals(Season.values()[(season.ordinal() + 1) % Season.values().length], state.next().season());
        }
    }

    @Test
    void HU09_invalidDatesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new GameDate(0, Season.SPRING, 1));
        assertThrows(IllegalArgumentException.class, () -> new GameDate(1, Season.SPRING, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new GameDate(1, Season.SPRING, GameRules.DAYS_PER_SEASON + 1));
    }

    private void advanceDays(int days) {
        for (int i = 0; i < days; i++) {
            clock.advanceDay();
        }
    }
}
