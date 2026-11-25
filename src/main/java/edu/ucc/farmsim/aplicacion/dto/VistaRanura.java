package edu.ucc.farmsim.aplicacion.dto;

/**
 * Una casilla ocupada del inventario.
 *
 * <p>Stories: HU-37.
 *
 * @param idArticulo id del objeto, por ejemplo {@code CARROT_SEED}
 * @param name   nombre en español
 * @param monto unidades en la casilla (1 a 99)
 */
public record VistaRanura(String idArticulo, String name, int monto) { }
