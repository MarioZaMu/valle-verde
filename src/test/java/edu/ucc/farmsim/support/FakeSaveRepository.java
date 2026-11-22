package edu.ucc.farmsim.support;

import edu.ucc.farmsim.domain.save.SaveRepository;
import edu.ucc.farmsim.domain.save.SaveStorageException;
import java.util.HashSet;
import java.util.Set;

/** In-memory stand-in for the save repository: tests say which slots "exist", or that the storage is down. */
public final class FakeSaveRepository implements SaveRepository {

    private final Set<Integer> occupied = new HashSet<>();
    private boolean down;

    /** Marks a slot as occupied. */
    public FakeSaveRepository withSlot(int slot) {
        occupied.add(slot);
        return this;
    }

    /** Makes every call fail as if MongoDB did not answer. */
    public FakeSaveRepository unreachable() {
        this.down = true;
        return this;
    }

    /** Forgets every slot and brings the storage back (for tests that share one repository). */
    public void reset() {
        occupied.clear();
        down = false;
    }

    @Override
    public boolean slotExists(int slot) {
        if (down) {
            throw new SaveStorageException("MongoDB no respondió", new IllegalStateException("simulated outage"));
        }
        return occupied.contains(slot);
    }
}
