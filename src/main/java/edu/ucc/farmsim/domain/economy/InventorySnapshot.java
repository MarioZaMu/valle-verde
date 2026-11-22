package edu.ucc.farmsim.domain.economy;

import java.util.List;

/**
 * Immutable copy of an inventory: its number of slots and the content of every slot (an empty slot is a
 * stack with {@code null} type and amount 0).
 *
 * <p>Stories: HU-10.
 *
 * @param capacity number of slots
 * @param slots    content of every slot, in slot order
 */
public record InventorySnapshot(int capacity, List<Inventory.ItemStack> slots) { }
