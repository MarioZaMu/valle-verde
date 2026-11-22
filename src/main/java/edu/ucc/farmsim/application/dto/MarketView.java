package edu.ucc.farmsim.application.dto;

import java.util.List;

/**
 * Everything the market dialog shows: what can be sold and what can be bought.
 *
 * <p>Stories: HU-14, HU-15, HU-16, HU-38.
 *
 * @param sells the sellable products with their price and trend
 * @param buys  the purchase catalogue
 */
public record MarketView(List<SellOfferView> sells, List<BuyOfferView> buys) { }
