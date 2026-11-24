package edu.ucc.farmsim.domain.game;

import edu.ucc.farmsim.domain.progress.Achievement;
import edu.ucc.farmsim.domain.time.GameDate;
import edu.ucc.farmsim.domain.time.Weather;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Hechos de un final de día, llenados por los pasos y leídos por la capa de aplicación.
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

    /** Las fases que corrieron, en orden de ejecución. */
    public List<DayPhase> phasesRun() {
        return Collections.unmodifiableList(phasesRun);
    }

    /** Fecha antes de que avanzara el calendario, o {@code null} si no corrió el paso del calendario. */
    public GameDate previousDate() {
        return previousDate;
    }

    /** Fecha después de que avanzó el calendario, o {@code null} si no corrió el paso del calendario. */
    public GameDate newDate() {
        return newDate;
    }

    /** Clima del día nuevo, o {@code null} si no corrió el paso del pronóstico. */
    public Weather todayWeather() {
        return todayWeather;
    }

    /** Pronóstico sorteado para el día siguiente, o {@code null} si no corrió el paso del pronóstico. */
    public Weather forecast() {
        return forecast;
    }

    /** Número de cultivos que se marchitaron durante este final de día. */
    public int witheredCrops() {
        return witheredCrops;
    }

    /** Logros desbloqueados durante este final de día (paso 8). */
    public List<Achievement> unlocked() {
        return Collections.unmodifiableList(unlocked);
    }

    void addUnlocked(List<Achievement> achievements) {
        unlocked.addAll(achievements);
    }

    /** Animales que se enfermaron durante este final de día. */
    public int sickAnimals() {
        return sickAnimals;
    }

    /** Unidades de producto que hicieron los animales durante este final de día. */
    public int products() {
        return products;
    }

    /** Dinero al inicio del día que terminó. */
    public int balanceBefore() {
        return balanceBefore;
    }

    /** Dinero al final del día que terminó. */
    public int balanceAfter() {
        return balanceAfter;
    }

    /** m ganados durante el día que terminó. */
    public int income() {
        return income;
    }

    /** m gastados durante el día que terminó. */
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
