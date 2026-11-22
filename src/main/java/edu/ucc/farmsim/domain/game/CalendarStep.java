package edu.ucc.farmsim.domain.game;

import edu.ucc.farmsim.domain.time.GameClock;
import edu.ucc.farmsim.domain.time.GameDate;

/**
 * Step 5 of the end of the day: the calendar moves to the next day, season or year (RN-01, RN-03).
 *
 * <p>Stories: HU-10.
 */
final class CalendarStep implements DayStep {

    @Override
    public DayPhase phase() {
        return DayPhase.CALENDAR;
    }

    @Override
    public void apply(GameState state, DayReport report) {
        GameClock clock = GameClock.getInstance();
        GameDate previous = clock.date();
        clock.advanceDay();
        report.recordCalendar(previous, clock.date());
    }
}
