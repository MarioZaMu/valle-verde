package edu.ucc.farmsim.application.dto;

/**
 * One row of the "Vender" tab: a product with how many units the player has, the price of the next unit and
 * its trend.
 *
 * <p>Stories: HU-15, HU-16, HU-38.
 *
 * @param itemId    item id, for example {@code CARROT}
 * @param name      Spanish name
 * @param owned     units in the inventory
 * @param unitPrice price in m of the next unit
 * @param trend     {@code UP}, {@code DOWN} or {@code SAME} (RN-19)
 */
public record SellOfferView(String itemId, String name, int owned, int unitPrice, String trend) { }
