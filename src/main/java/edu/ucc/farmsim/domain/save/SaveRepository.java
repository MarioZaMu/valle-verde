package edu.ucc.farmsim.domain.save;

/**
 * Port the domain defines and the infrastructure implements: where games are stored (dependency
 * inversion, DT-11). Saving, loading and listing arrive with HU-29, HU-30 and HU-32; for now it only knows
 * whether a slot holds a game.
 *
 * <p>Stories: HU-33.
 */
public interface SaveRepository {

    /**
     * Whether a slot holds a saved game.
     *
     * @param slot slot number, from 1 to the number of slots
     */
    boolean slotExists(int slot);
}
