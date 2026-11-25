package edu.ucc.farmsim.dominio.progreso;

import java.util.Set;

/**
 * Copia inmutable de los logros desbloqueados y de sus contadores, para el memento.
 *
 * <p>Stories: HU-26, HU-30.
 *
 * @param desbloqueado       los logros desbloqueados
 * @param cosechas       cultivos cosechados hasta ahora
 * @param unidadesVendidos      unidades vendidas hasta ahora
 * @param edificiosConstruidos edificios construidos hasta ahora
 * @param animalesCurados   animales curados hasta ahora
 */
public record InstantaneaProgreso(Set<Logro> desbloqueado, int cosechas, int unidadesVendidos, int edificiosConstruidos,
                               int animalesCurados) {

    /** Hace del conjunto una copia no modificable. */
    public InstantaneaProgreso {
        desbloqueado = Set.copyOf(desbloqueado);
    }
}
