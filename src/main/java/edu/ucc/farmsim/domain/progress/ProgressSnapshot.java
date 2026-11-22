package edu.ucc.farmsim.domain.progress;

import java.util.Set;

/**
 * Immutable copy of the achievements unlocked and of their counters, for the memento.
 *
 * <p>Stories: HU-26, HU-30.
 *
 * @param unlocked       the achievements unlocked
 * @param harvests       crops harvested so far
 * @param unitsSold      units sold so far
 * @param buildingsBuilt buildings built so far
 * @param animalsCured   animals cured so far
 */
public record ProgressSnapshot(Set<Achievement> unlocked, int harvests, int unitsSold, int buildingsBuilt,
                               int animalsCured) {

    /** Makes the set an unmodifiable copy. */
    public ProgressSnapshot {
        unlocked = Set.copyOf(unlocked);
    }
}
