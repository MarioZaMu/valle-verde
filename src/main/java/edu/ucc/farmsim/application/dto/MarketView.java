package edu.ucc.farmsim.application.dto;

import java.util.List;

/**
 * Todo lo que muestra el diálogo del mercado: lo que se puede vender y lo que se puede comprar.
 *
 * <p>Stories: HU-14, HU-15, HU-16, HU-19, HU-38.
 *
 * @param sells   los productos vendibles con su precio y tendencia
 * @param buys    el catálogo de compra
 * @param animals los animales en venta
 */
public record MarketView(List<SellOfferView> sells, List<BuyOfferView> buys, List<AnimalOfferView> animals) {

    /** Un mercado sin animales. */
    public MarketView(List<SellOfferView> sells, List<BuyOfferView> buys) {
        this(sells, buys, List.of());
    }
}
