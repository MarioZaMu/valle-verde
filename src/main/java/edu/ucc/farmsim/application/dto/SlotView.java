package edu.ucc.farmsim.application.dto;

/**
 * One occupied inventory slot.
 *
 * <p>Stories: HU-37.
 *
 * @param itemId item id, for example {@code CARROT_SEED}
 * @param name   Spanish name
 * @param amount units in the slot (1 to 99)
 */
public record SlotView(String itemId, String name, int amount) { }
