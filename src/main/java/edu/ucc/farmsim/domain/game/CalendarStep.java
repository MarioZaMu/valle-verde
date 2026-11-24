package edu.ucc.farmsim.domain.game;

import edu.ucc.farmsim.domain.time.GameClock;
import edu.ucc.farmsim.domain.time.GameDate;

/**
 * Paso 5 del final del día: el calendario pasa al día, estación o año siguiente (RN-01, RN-03).
 *
 * <p>Cuando cambia la estación, los cultivos que no son válidos en la nueva estación se marchitan (RN-09-c).
 *
 * <p>Stories: HU-05, HU-10.
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
        GameDate current = clock.date();
        report.recordCalendar(previous, current);
        if (current.season() != previous.season()) {
            report.addWithered(state.farm().witherOutOfSeason(current.season()));
        }
    }
}
