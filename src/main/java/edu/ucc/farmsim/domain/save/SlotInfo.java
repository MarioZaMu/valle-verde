package edu.ucc.farmsim.domain.save;

import edu.ucc.farmsim.domain.time.GameDate;
import java.time.Instant;

/**
 * Lo que muestra la lista de ranuras sobre una ranura (RN-31): vacía, dañada, o la fecha, el dinero y el momento del
 * guardado.
 *
 * <p>Stories: HU-32.
 *
 * @param slot    número de ranura, desde 1
 * @param status  vacía, utilizable o dañada
 * @param date    fecha del calendario de la partida guardada, o {@code null} si la ranura no es utilizable
 * @param money   dinero de la partida guardada, 0 si la ranura no es utilizable
 * @param savedAt cuándo se guardó la partida, o {@code null} si la ranura no es utilizable
 */
public record SlotInfo(int slot, Status status, GameDate date, int money, Instant savedAt) {

    /** Estado de una ranura. */
    public enum Status { EMPTY, OK, DAMAGED }

    /** Una ranura sin documento. */
    public static SlotInfo empty(int slot) {
        return new SlotInfo(slot, Status.EMPTY, null, 0, null);
    }

    /** Una ranura cuyo documento no se puede leer. */
    public static SlotInfo damaged(int slot) {
        return new SlotInfo(slot, Status.DAMAGED, null, 0, null);
    }

    /** Una ranura utilizable. */
    public static SlotInfo ok(int slot, GameDate date, int money, Instant savedAt) {
        return new SlotInfo(slot, Status.OK, date, money, savedAt);
    }
}
