package edu.ucc.farmsim.support;

import edu.ucc.farmsim.domain.game.GameMemento;
import edu.ucc.farmsim.domain.save.SaveLoadException;
import edu.ucc.farmsim.domain.save.SaveRepository;
import edu.ucc.farmsim.domain.save.SaveStorageException;
import edu.ucc.farmsim.domain.save.SlotInfo;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * In-memory stand-in for the save repository. Tests can say which slots "exist", that a slot is damaged, or that
 * the storage is down; saving and loading really keep the game, so a round trip can be checked without MongoDB.
 */
public final class FakeSaveRepository implements SaveRepository {

    private record Stored(GameMemento memento, Instant savedAt) { }

    private final Map<Integer, Stored> stored = new HashMap<>();
    private final Set<Integer> occupied = new HashSet<>();
    private final Set<Integer> damaged = new HashSet<>();
    private boolean down;

    /** Marks a slot as occupied (its content cannot be loaded). */
    public FakeSaveRepository withSlot(int slot) {
        occupied.add(slot);
        return this;
    }

    /** Marks a slot as holding a damaged document. */
    public FakeSaveRepository withDamagedSlot(int slot) {
        damaged.add(slot);
        return this;
    }

    /** Makes every call fail as if MongoDB did not answer. */
    public FakeSaveRepository unreachable() {
        this.down = true;
        return this;
    }

    /** Brings the storage back. */
    public void reachable() {
        this.down = false;
    }

    /** Forgets every slot and brings the storage back (for tests that share one repository). */
    public void reset() {
        stored.clear();
        occupied.clear();
        damaged.clear();
        down = false;
    }

    private void checkUp() {
        if (down) {
            throw new SaveStorageException("MongoDB no respondió", new IllegalStateException("simulated outage"));
        }
    }

    @Override
    public boolean slotExists(int slot) {
        checkUp();
        return occupied.contains(slot) || damaged.contains(slot) || stored.containsKey(slot);
    }

    @Override
    public void save(int slot, GameMemento memento, Instant savedAt) {
        checkUp();
        stored.put(slot, new Stored(memento, savedAt));
        damaged.remove(slot);
    }

    @Override
    public GameMemento load(int slot) {
        checkUp();
        if (damaged.contains(slot)) {
            throw new SaveLoadException(SaveLoadException.Reason.CORRUPT, "simulated damage");
        }
        Stored found = stored.get(slot);
        if (found == null) {
            throw new SaveLoadException(SaveLoadException.Reason.MISSING, "slot " + slot);
        }
        return found.memento();
    }

    @Override
    public List<SlotInfo> slots() {
        checkUp();
        List<SlotInfo> slots = new ArrayList<>();
        for (int slot = 1; slot <= 3; slot++) {
            Stored found = stored.get(slot);
            if (damaged.contains(slot)) {
                slots.add(SlotInfo.damaged(slot));
            } else if (found != null) {
                slots.add(SlotInfo.ok(slot, found.memento().date(), found.memento().money(), found.savedAt()));
            } else {
                slots.add(SlotInfo.empty(slot));
            }
        }
        return slots;
    }
}
