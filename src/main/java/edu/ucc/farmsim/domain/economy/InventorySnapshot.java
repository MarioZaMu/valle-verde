package edu.ucc.farmsim.domain.economy;

import java.util.List;

/**
 * Copia inmutable de un inventario: su número de casillas y el contenido de cada casilla (una casilla vacía es una pila
 * con tipo {@code null} y cantidad 0).
 *
 * <p>Stories: HU-10.
 *
 * @param capacity número de casillas
 * @param slots    contenido de cada casilla, en orden de casilla
 */
public record InventorySnapshot(int capacity, List<Inventory.ItemStack> slots) { }
