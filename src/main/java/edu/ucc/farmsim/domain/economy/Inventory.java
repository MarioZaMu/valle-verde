package edu.ucc.farmsim.domain.economy;

import edu.ucc.farmsim.domain.config.GameRules;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Limited storage: a fixed number of slots, each holding one item type in a stack of up to
 * {@link GameRules#MAX_STACK} units (RN-15).
 *
 * <p>Insertion is atomic: if the whole amount does not fit, nothing is inserted. Domain operations signal
 * a normal rejection with {@code false}; the application layer turns it into the {@code INVENTARIO_LLENO}
 * error. A non-positive amount is a programming error and raises {@link IllegalArgumentException}.
 *
 * <p>Stories: HU-13.
 */
public final class Inventory {

    /** One occupied slot. */
    public record ItemStack(ItemType type, int amount) { }

    private final ItemType[] types;
    private final int[] amounts;

    /**
     * Creates an empty inventory.
     *
     * @param capacity number of slots
     */
    public Inventory(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive: " + capacity);
        }
        this.types = new ItemType[capacity];
        this.amounts = new int[capacity];
    }

    /** Creates an empty inventory with the base number of slots (RN-15). */
    public static Inventory standard() {
        return new Inventory(GameRules.INVENTORY_BASE_SLOTS);
    }

    /** Number of slots. */
    public int capacity() {
        return types.length;
    }

    /** Number of slots currently occupied. */
    public int usedSlots() {
        int used = 0;
        for (int amount : amounts) {
            if (amount > 0) {
                used++;
            }
        }
        return used;
    }

    /** Total units of the given item across all slots. */
    public int count(ItemType type) {
        int total = 0;
        for (int i = 0; i < types.length; i++) {
            if (types[i] == type) {
                total += amounts[i];
            }
        }
        return total;
    }

    /** Occupied slots in slot order. */
    public List<ItemStack> stacks() {
        List<ItemStack> result = new ArrayList<>();
        for (int i = 0; i < types.length; i++) {
            if (amounts[i] > 0) {
                result.add(new ItemStack(types[i], amounts[i]));
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Inserts units, first completing stacks of the same type and then using empty slots.
     *
     * @return {@code true} if everything was inserted; {@code false} (and nothing inserted) if it does not fit
     */
    public boolean add(ItemType type, int amount) {
        requirePositive(amount);
        if (roomFor(type) < amount) {
            return false;
        }
        int remaining = amount;
        for (int i = 0; i < types.length && remaining > 0; i++) {
            if (types[i] == type && amounts[i] > 0) {
                int moved = Math.min(remaining, GameRules.MAX_STACK - amounts[i]);
                amounts[i] += moved;
                remaining -= moved;
            }
        }
        for (int i = 0; i < types.length && remaining > 0; i++) {
            if (amounts[i] == 0) {
                int moved = Math.min(remaining, GameRules.MAX_STACK);
                types[i] = type;
                amounts[i] = moved;
                remaining -= moved;
            }
        }
        return true;
    }

    /**
     * Removes units, starting from the last stack of that type.
     *
     * @return {@code true} if removed; {@code false} (and nothing removed) if there are fewer units
     */
    public boolean remove(ItemType type, int amount) {
        requirePositive(amount);
        if (count(type) < amount) {
            return false;
        }
        int remaining = amount;
        for (int i = types.length - 1; i >= 0 && remaining > 0; i--) {
            if (types[i] == type && amounts[i] > 0) {
                int taken = Math.min(remaining, amounts[i]);
                amounts[i] -= taken;
                remaining -= taken;
                if (amounts[i] == 0) {
                    types[i] = null;
                }
            }
        }
        return true;
    }

    private int roomFor(ItemType type) {
        int room = 0;
        for (int i = 0; i < types.length; i++) {
            if (amounts[i] == 0) {
                room += GameRules.MAX_STACK;
            } else if (types[i] == type) {
                room += GameRules.MAX_STACK - amounts[i];
            }
        }
        return room;
    }

    private static void requirePositive(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive: " + amount);
        }
    }
}
