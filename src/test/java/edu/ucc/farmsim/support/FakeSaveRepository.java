package edu.ucc.farmsim.support;

import edu.ucc.farmsim.domain.save.SaveRepository;
import java.util.HashSet;
import java.util.Set;

/** In-memory stand-in for the save repository: tests say which slots "exist". */
public final class FakeSaveRepository implements SaveRepository {

    private final Set<Integer> occupied = new HashSet<>();

    /** Marks a slot as occupied. */
    public FakeSaveRepository withSlot(int slot) {
        occupied.add(slot);
        return this;
    }

    @Override
    public boolean slotExists(int slot) {
        return occupied.contains(slot);
    }
}
