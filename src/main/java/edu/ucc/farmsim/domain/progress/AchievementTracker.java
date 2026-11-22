package edu.ucc.farmsim.domain.progress;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Keeps the achievements the player has unlocked and the counters they need (RN-28). An achievement is granted
 * once and never revoked; evaluating it only needs a {@link ProgressFacts}, never the game.
 *
 * <p>Stories: HU-18, HU-26.
 */
public final class AchievementTracker {

    private final Set<Achievement> unlocked = EnumSet.noneOf(Achievement.class);
    private int harvests;
    private int unitsSold;
    private int buildingsBuilt;
    private int animalsCured;

    /** True if the player has the achievement. */
    public boolean has(Achievement achievement) {
        return unlocked.contains(achievement);
    }

    /**
     * Grants an achievement.
     *
     * @return {@code true} if it was new; {@code false} if the player already had it
     */
    public boolean unlock(Achievement achievement) {
        return unlocked.add(achievement);
    }

    /** Counts a harvested crop. */
    public void recordHarvest() {
        harvests++;
    }

    /** Counts units sold. */
    public void recordSale(int units) {
        unitsSold += units;
    }

    /** Counts a building built. */
    public void recordBuild() {
        buildingsBuilt++;
    }

    /** Counts an animal cured. */
    public void recordCure() {
        animalsCured++;
    }

    /** Crops harvested so far. */
    public int harvests() {
        return harvests;
    }

    /** Units sold so far. */
    public int unitsSold() {
        return unitsSold;
    }

    /** Buildings built so far. */
    public int buildingsBuilt() {
        return buildingsBuilt;
    }

    /** Animals cured so far. */
    public int animalsCured() {
        return animalsCured;
    }

    /**
     * Unlocks every achievement whose condition the facts meet and that the player does not have yet.
     *
     * @return the ones unlocked now, in the order of RN-28
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

    /** An immutable copy of the achievements and the counters, for the memento. */
    public ProgressSnapshot snapshot() {
        return new ProgressSnapshot(unlocked, harvests, unitsSold, buildingsBuilt, animalsCured);
    }

    /** Replaces everything with a copy taken earlier. */
    public void restoreFrom(ProgressSnapshot saved) {
        unlocked.clear();
        unlocked.addAll(saved.unlocked());
        harvests = saved.harvests();
        unitsSold = saved.unitsSold();
        buildingsBuilt = saved.buildingsBuilt();
        animalsCured = saved.animalsCured();
    }
}
