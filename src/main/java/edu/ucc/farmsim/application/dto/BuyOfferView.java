package edu.ucc.farmsim.application.dto;

/**
 * Una fila de la pestaña «Comprar»: un producto y su precio fijo.
 *
 * <p>Stories: HU-14, HU-38.
 *
 * @param itemId id del objeto, por ejemplo {@code CORN_SEED}
 * @param name   nombre en español
 * @param price  precio en m de una unidad
 */
public record BuyOfferView(String itemId, String name, int price) { }
