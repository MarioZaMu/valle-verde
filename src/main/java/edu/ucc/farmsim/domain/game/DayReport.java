package edu.ucc.farmsim.domain.game;

import edu.ucc.farmsim.domain.time.GameDate;
import edu.ucc.farmsim.domain.time.Weather;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Facts about one end of the day, filled in by the steps and read by the application layer.
 *
 * <p>Stories: HU-05, HU-10.
 */
public final class DayReport {

    private final List<DayPhase> phasesRun = new ArrayList<>();
    private GameDate previousDate;
    private GameDate newDate;
    private Weather todayWeather;
    private Weather forecast;
    private int witheredCrops;

    /** The phases that ran, in execution order. */
    public List<DayPhase> phasesRun() {
        return Collections.unmodifiableList(phasesRun);
    }

    /** Date before the calendar advanced, or {@code null} if no calendar step ran. */
    public GameDate previousDate() {
        return previousDate;
    }

    /** Date after the calendar advanced, or {@code null} if no calendar step ran. */
    public GameDate newDate() {
        return newDate;
    }

    /** Weather of the new day, or {@code null} if no forecast step ran. */
    public Weather todayWeather() {
        return todayWeather;
    }

    /** Forecast drawn for the following day, or {@code null} if no forecast step ran. */
    public Weather forecast() {
        return forecast;
    }

    /** Number of crops that withered during this end of the day. */
    public int witheredCrops() {
        return witheredCrops;
    }

    void addWithered(int count) {
        witheredCrops += count;
    }

    void markRun(DayPhase phase) {
        phasesRun.add(phase);
    }

    void recordCalendar(GameDate previous, GameDate current) {
        this.previousDate = previous;
        this.newDate = current;
    }

    void recordWeather(Weather today, Weather tomorrow) {
        this.todayWeather = today;
        this.forecast = tomorrow;
    }
}
