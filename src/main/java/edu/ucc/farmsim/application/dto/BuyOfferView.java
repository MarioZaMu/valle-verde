package edu.ucc.farmsim.application.dto;

/**
 * One row of the "Comprar" tab: a product and its fixed price.
 *
 * <p>Stories: HU-14, HU-38.
 *
 * @param itemId item id, for example {@code CORN_SEED}
 * @param name   Spanish name
 * @param price  price in m of one unit
 */
public record BuyOfferView(String itemId, String name, int price) { }
