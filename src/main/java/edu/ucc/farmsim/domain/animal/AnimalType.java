package edu.ucc.farmsim.domain.animal;

import edu.ucc.farmsim.domain.config.GameRules;
import edu.ucc.farmsim.domain.economy.ItemType;

/**
 * Catálogo de animales (RN-23): lo que cuesta cada uno y el producto que da.
 *
 * <p>Stories: HU-19.
 */
public enum AnimalType {
    HEN(GameRules.PRICE_HEN, ItemType.EGG),
    COW(GameRules.PRICE_COW, ItemType.MILK);

    private final int price;
    private final ItemType product;

    AnimalType(int price, ItemType product) {
        this.price = price;
        this.product = product;
    }

    /** Precio en m de un animal. */
    public int price() {
        return price;
    }

    /** Lo que produce el animal. */
    public ItemType product() {
        return product;
    }
}
