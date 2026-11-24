package edu.ucc.farmsim.domain.npc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Qué encargos de las personas del valle ya están hechos (RN-38). Un encargo se hace una vez y nunca más.
 *
 * <p>Stories: HU-47.
 */
public final class ErrandBook {

    private final Set<Npc> done = EnumSet.noneOf(Npc.class);

    /** Verdadero si el encargo de esa persona ya fue entregado. */
    public boolean isDone(Npc npc) {
        return done.contains(npc);
    }

    /** Marca el encargo de esa persona como entregado. */
    public void complete(Npc npc) {
        done.add(npc);
    }

    /** Las personas cuyo encargo está hecho, en el orden de {@link Npc}, para el memento. */
    public List<Npc> snapshot() {
        return List.copyOf(new ArrayList<>(done));
    }

    /**
     * Deja el libro como dice la instantánea; {@code null} (un guardado más antiguo) significa que ninguno está hecho.
     */
    public void restoreFrom(Collection<Npc> saved) {
        done.clear();
        if (saved != null) {
            done.addAll(saved);
        }
    }
}
