package edu.ucc.farmsim.aplicacion.dto;

/**
 * Una fila de la pestaña «Comprar»: un producto y su precio fijo.
 *
 * <p>Stories: HU-14, HU-38.
 *
 * @param idArticulo id del objeto, por ejemplo {@code CORN_SEED}
 * @param name   nombre en español
 * @param precio  precio en m de una unidad
 */
public record VistaOfertaComprar(String idArticulo, String name, int precio) { }
