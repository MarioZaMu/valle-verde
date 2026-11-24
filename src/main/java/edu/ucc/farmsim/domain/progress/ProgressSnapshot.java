package edu.ucc.farmsim.domain.progress;

import java.util.Set;

/**
 * Copia inmutable de los logros desbloqueados y de sus contadores, para el memento.
 *
 * <p>Stories: HU-26, HU-30.
 *
 * @param unlocked       los logros desbloqueados
 * @param harvests       cultivos cosechados hasta ahora
 * @param unitsSold      unidades vendidas hasta ahora
 * @param buildingsBuilt edificios construidos hasta ahora
 * @param animalsCured   animales curados hasta ahora
 */
public record ProgressSnapshot(Set<Achievement> unlocked, int harvests, int unitsSold, int buildingsBuilt,
                               int animalsCured) {

    /** Hace del conjunto una copia no modificable. */
    public ProgressSnapshot {
        unlocked = Set.copyOf(unlocked);
    }
}
