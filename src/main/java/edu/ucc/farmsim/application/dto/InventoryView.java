package edu.ucc.farmsim.application.dto;

import java.util.List;

/**
 * El inventario como una grilla: cuántas casillas hay y las ocupadas en orden; el resto están vacías.
 *
 * <p>Stories: HU-37.
 *
 * @param capacity número de casillas (24, o 36 con un almacén)
 * @param slots    las casillas ocupadas, en orden de casilla
 */
public record InventoryView(int capacity, List<SlotView> slots) { }
