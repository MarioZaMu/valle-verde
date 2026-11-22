package edu.ucc.farmsim.application.dto;

import java.util.List;

/**
 * Everything the market dialog shows: what can be sold and what can be bought.
 *
 * <p>Stories: HU-14, HU-15, HU-16, HU-19, HU-38.
 *
 * @param sells   the sellable products with their price and trend
 * @param buys    the purchase catalogue
 * @param animals the animals for sale
 */
public record MarketView(List<SellOfferView> sells, List<BuyOfferView> buys, List<AnimalOfferView> animals) {

    /** A market without animals. */
    public MarketView(List<SellOfferView> sells, List<BuyOfferView> buys) {
        this(sells, buys, List.of());
    }
}
