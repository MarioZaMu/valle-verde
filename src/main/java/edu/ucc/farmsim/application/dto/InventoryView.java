package edu.ucc.farmsim.application.dto;

import java.util.List;

/**
 * The inventory as a grid: how many slots there are and the occupied ones in order; the rest are empty.
 *
 * <p>Stories: HU-37.
 *
 * @param capacity number of slots (24, or 36 with a warehouse)
 * @param slots    the occupied slots, in slot order
 */
public record InventoryView(int capacity, List<SlotView> slots) { }
