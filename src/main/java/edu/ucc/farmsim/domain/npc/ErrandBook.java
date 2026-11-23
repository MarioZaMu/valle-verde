package edu.ucc.farmsim.domain.npc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Which errands of the people of the valley are already done (RN-38). An errand is done once and never again.
 *
 * <p>Stories: HU-47.
 */
public final class ErrandBook {

    private final Set<Npc> done = EnumSet.noneOf(Npc.class);

    /** True if the errand of that person was already delivered. */
    public boolean isDone(Npc npc) {
        return done.contains(npc);
    }

    /** Marks the errand of that person as delivered. */
    public void complete(Npc npc) {
        done.add(npc);
    }

    /** The people whose errand is done, in the order of {@link Npc}, for the memento. */
    public List<Npc> snapshot() {
        return List.copyOf(new ArrayList<>(done));
    }

    /** Puts the book back as the snapshot says; {@code null} (an older save) means none is done. */
    public void restoreFrom(Collection<Npc> saved) {
        done.clear();
        if (saved != null) {
            done.addAll(saved);
        }
    }
}
