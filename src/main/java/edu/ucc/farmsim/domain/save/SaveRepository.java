package edu.ucc.farmsim.domain.save;

import edu.ucc.farmsim.domain.game.GameMemento;
import java.time.Instant;
import java.util.List;

/**
 * Port the domain defines and the infrastructure implements: where games are stored (dependency
 * inversion, DT-11). Every technical failure of the storage is a {@link SaveStorageException}; a slot that
 * cannot be turned into a game is a {@link SaveLoadException}.
 *
 * <p>Stories: HU-29, HU-30, HU-31, HU-32, HU-33.
 */
public interface SaveRepository {

    /**
     * Whether a slot holds a saved game.
     *
     * @param slot slot number, from 1 to the number of slots
     */
    boolean slotExists(int slot);

    /**
     * Writes the game into the slot in one atomic operation: the previous content is replaced or, if the write
     * fails, stays as it was (RN-31).
     *
     * @param slot    slot number
     * @param memento the game to keep
     * @param savedAt moment of the save
     * @throws SaveStorageException if the storage fails
     */
    void save(int slot, GameMemento memento, Instant savedAt);

    /**
     * Reads the game kept in the slot, checking existence, format, version and checksum in that order (RN-33).
     *
     * @param slot slot number
     * @throws SaveLoadException    if the slot is empty, damaged or from an unsupported version
     * @throws SaveStorageException if the storage fails
     */
    GameMemento load(int slot);

    /**
     * The state of every slot, in order. A damaged slot does not stop the rest of the list.
     *
     * @throws SaveStorageException if the storage fails
     */
    List<SlotInfo> slots();
}
