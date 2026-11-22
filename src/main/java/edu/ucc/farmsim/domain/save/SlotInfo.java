package edu.ucc.farmsim.domain.save;

import edu.ucc.farmsim.domain.time.GameDate;
import java.time.Instant;

/**
 * What the slots list shows about one slot (RN-31): empty, damaged, or the date, money and moment of the save.
 *
 * <p>Stories: HU-32.
 *
 * @param slot    slot number, from 1
 * @param status  empty, usable or damaged
 * @param date    calendar date of the saved game, or {@code null} if the slot is not usable
 * @param money   money of the saved game, 0 if the slot is not usable
 * @param savedAt when the game was saved, or {@code null} if the slot is not usable
 */
public record SlotInfo(int slot, Status status, GameDate date, int money, Instant savedAt) {

    /** State of a slot. */
    public enum Status { EMPTY, OK, DAMAGED }

    /** A slot with no document. */
    public static SlotInfo empty(int slot) {
        return new SlotInfo(slot, Status.EMPTY, null, 0, null);
    }

    /** A slot whose document cannot be read. */
    public static SlotInfo damaged(int slot) {
        return new SlotInfo(slot, Status.DAMAGED, null, 0, null);
    }

    /** A usable slot. */
    public static SlotInfo ok(int slot, GameDate date, int money, Instant savedAt) {
        return new SlotInfo(slot, Status.OK, date, money, savedAt);
    }
}
