package edu.ucc.farmsim.domain.animal;

import edu.ucc.farmsim.domain.config.GameRules;
import edu.ucc.farmsim.domain.economy.ItemType;

/**
 * Catalog of animals (RN-23): what each costs and the product it gives.
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

    /** Price in m of one animal. */
    public int price() {
        return price;
    }

    /** What the animal produces. */
    public ItemType product() {
        return product;
    }
}
