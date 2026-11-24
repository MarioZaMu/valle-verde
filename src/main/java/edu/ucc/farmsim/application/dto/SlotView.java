package edu.ucc.farmsim.application.dto;

/**
 * Una casilla ocupada del inventario.
 *
 * <p>Stories: HU-37.
 *
 * @param itemId id del objeto, por ejemplo {@code CARROT_SEED}
 * @param name   nombre en español
 * @param amount unidades en la casilla (1 a 99)
 */
public record SlotView(String itemId, String name, int amount) { }
