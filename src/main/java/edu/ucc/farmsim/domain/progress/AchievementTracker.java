package edu.ucc.farmsim.domain.progress;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Guarda los logros que el jugador ha desbloqueado y los contadores que necesitan (RN-28). Un logro se concede una vez
 * y nunca se revoca; evaluarlo solo necesita un {@link ProgressFacts}, nunca la partida.
 *
 * <p>Stories: HU-18, HU-26.
 */
public final class AchievementTracker {

    private final Set<Achievement> unlocked = EnumSet.noneOf(Achievement.class);
    private int harvests;
    private int unitsSold;
    private int buildingsBuilt;
    private int animalsCured;

    /** Verdadero si el jugador tiene el logro. */
    public boolean has(Achievement achievement) {
        return unlocked.contains(achievement);
    }

    /**
     * Concede un logro.
     *
     * @return {@code true} si era nuevo; {@code false} si el jugador ya lo tenía
     */
    public boolean unlock(Achievement achievement) {
        return unlocked.add(achievement);
    }

    /** Cuenta un cultivo cosechado. */
    public void recordHarvest() {
        harvests++;
    }

    /** Cuenta unidades vendidas. */
    public void recordSale(int units) {
        unitsSold += units;
    }

    /** Cuenta un edificio construido. */
    public void recordBuild() {
        buildingsBuilt++;
    }

    /** Cuenta un animal curado. */
    public void recordCure() {
        animalsCured++;
    }

    /** Cultivos cosechados hasta ahora. */
    public int harvests() {
        return harvests;
    }

    /** Unidades vendidas hasta ahora. */
    public int unitsSold() {
        return unitsSold;
    }

    /** Edificios construidos hasta ahora. */
    public int buildingsBuilt() {
        return buildingsBuilt;
    }

    /** Animales curados hasta ahora. */
    public int animalsCured() {
        return animalsCured;
    }

    /**
     * Desbloquea todos los logros cuya condición cumplen los hechos y que el jugador aún no tiene.
     *
     * @return los desbloqueados ahora, en el orden de RN-28
     */
    public List<Achievement> evaluate(ProgressFacts facts) {
        List<Achievement> fresh = new ArrayList<>();
        for (Achievement achievement : Achievement.values()) {
            if (!has(achievement) && achievement.isMetBy(facts)) {
                unlocked.add(achievement);
                fresh.add(achievement);
            }
        }
        return fresh;
    }

    /** Una copia inmutable de los logros y los contadores, para el memento. */
    public ProgressSnapshot snapshot() {
        return new ProgressSnapshot(unlocked, harvests, unitsSold, buildingsBuilt, animalsCured);
    }

    /** Reemplaza todo con una copia tomada antes. */
    public void restoreFrom(ProgressSnapshot saved) {
        unlocked.clear();
        unlocked.addAll(saved.unlocked());
        harvests = saved.harvests();
        unitsSold = saved.unitsSold();
        buildingsBuilt = saved.buildingsBuilt();
        animalsCured = saved.animalsCured();
    }
}
