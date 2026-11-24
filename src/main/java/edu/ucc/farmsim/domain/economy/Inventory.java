package edu.ucc.farmsim.domain.economy;

import edu.ucc.farmsim.domain.config.GameRules;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Almacenamiento limitado: un número fijo de casillas, cada una con un tipo de objeto en una pila de hasta {@link
 * GameRules#MAX_STACK} unidades (RN-15).
 *
 * <p>La inserción es atómica: si no cabe toda la cantidad, no se inserta nada. Las operaciones del dominio señalan un
 * rechazo normal con {@code false}; la capa de aplicación lo convierte en el error {@code INVENTARIO_LLENO}. Una
 * cantidad no positiva es un error de programación y lanza {@link IllegalArgumentException}.
 *
 * <p>Stories: HU-10, HU-13.
 */
public final class Inventory {

    /** Una casilla ocupada. */
    public record ItemStack(ItemType type, int amount) { }

    private ItemType[] types;
    private int[] amounts;

    /**
     * Crea un inventario vacío.
     *
     * @param capacity número de casillas
     */
    public Inventory(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive: " + capacity);
        }
        this.types = new ItemType[capacity];
        this.amounts = new int[capacity];
    }

    /** Crea un inventario vacío con el número base de casillas (RN-15). */
    public static Inventory standard() {
        return new Inventory(GameRules.INVENTORY_BASE_SLOTS);
    }

    /** Número de casillas. */
    public int capacity() {
        return types.length;
    }

    /**
     * Agrega casillas vacías (un almacén, RN-20). Nada de lo ya guardado se mueve.
     *
     * @param extra cuántas casillas agregar
     */
    public void addSlots(int extra) {
        requirePositive(extra);
        types = Arrays.copyOf(types, types.length + extra);
        amounts = Arrays.copyOf(amounts, amounts.length + extra);
    }

    /** Número de casillas ocupadas actualmente. */
    public int usedSlots() {
        int used = 0;
        for (int amount : amounts) {
            if (amount > 0) {
                used++;
            }
        }
        return used;
    }

    /** Total de unidades del objeto dado en todas las casillas. */
    public int count(ItemType type) {
        int total = 0;
        for (int i = 0; i < types.length; i++) {
            if (types[i] == type) {
                total += amounts[i];
            }
        }
        return total;
    }

    /** Casillas ocupadas en orden de casilla. */
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
     * Inserta unidades, completando primero las pilas del mismo tipo y usando después las casillas vacías.
     *
     * @return {@code true} si se insertó todo; {@code false} (y no se inserta nada) si no cabe
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
     * Quita unidades, empezando por la última pila de ese tipo.
     *
     * @return {@code true} si se quitó; {@code false} (y no se quita nada) si hay menos unidades
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

    /** Una copia inmutable de todas las casillas, incluidas las vacías, para el memento. */
    public InventorySnapshot snapshot() {
        List<ItemStack> slots = new ArrayList<>();
        for (int i = 0; i < types.length; i++) {
            slots.add(new ItemStack(types[i], amounts[i]));
        }
        return new InventorySnapshot(types.length, List.copyOf(slots));
    }

    /** Reemplaza todo el contenido (y el número de casillas) con una instantánea tomada antes. */
    public void restoreFrom(InventorySnapshot snapshot) {
        this.types = new ItemType[snapshot.capacity()];
        this.amounts = new int[snapshot.capacity()];
        for (int i = 0; i < snapshot.slots().size(); i++) {
            ItemStack slot = snapshot.slots().get(i);
            types[i] = slot.amount() > 0 ? slot.type() : null;
            amounts[i] = slot.amount();
        }
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
