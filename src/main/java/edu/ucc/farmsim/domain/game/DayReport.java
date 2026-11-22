package edu.ucc.farmsim.domain.game;

import edu.ucc.farmsim.domain.progress.Achievement;
import edu.ucc.farmsim.domain.time.GameDate;
import edu.ucc.farmsim.domain.time.Weather;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Facts about one end of the day, filled in by the steps and read by the application layer.
 *
 * <p>Stories: HU-05, HU-10, HU-17, HU-21, HU-22, HU-26.
 */
public final class DayReport {

    private final List<DayPhase> phasesRun = new ArrayList<>();
    private GameDate previousDate;
    private GameDate newDate;
    private Weather todayWeather;
    private Weather forecast;
    private int witheredCrops;
    private int sickAnimals;
    private int products;
    private final List<Achievement> unlocked = new ArrayList<>();
    private int balanceBefore;
    private int balanceAfter;
    private int income;
    private int expenses;

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

    /** Achievements unlocked during this end of the day (step 8). */
    public List<Achievement> unlocked() {
        return Collections.unmodifiableList(unlocked);
    }

    void addUnlocked(List<Achievement> achievements) {
        unlocked.addAll(achievements);
    }

    /** Animals that fell sick during this end of the day. */
    public int sickAnimals() {
        return sickAnimals;
    }

    /** Units of product the animals made during this end of the day. */
    public int products() {
        return products;
    }

    /** Money at the start of the day that ended. */
    public int balanceBefore() {
        return balanceBefore;
    }

    /** Money at the end of the day that ended. */
    public int balanceAfter() {
        return balanceAfter;
    }

    /** m earned during the day that ended. */
    public int income() {
        return income;
    }

    /** m spent during the day that ended. */
    public int expenses() {
        return expenses;
    }

    void addSickAnimal() {
        sickAnimals++;
    }

    void addProducts(int units) {
        products += units;
    }

    void recordMoney(int after, int earned, int spent) {
        this.balanceBefore = after - earned + spent;
        this.balanceAfter = after;
        this.income = earned;
        this.expenses = spent;
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
