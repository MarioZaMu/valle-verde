package edu.ucc.farmsim.domain.economy;

/**
 * Every item that can be stored in the inventory (RN-15): 5 seeds, 5 crops, egg, milk, hay and medicine.
 *
 * <p>Stories: HU-13.
 */
public enum ItemType {
    CARROT_SEED(Kind.SEED),
    POTATO_SEED(Kind.SEED),
    TOMATO_SEED(Kind.SEED),
    CORN_SEED(Kind.SEED),
    PUMPKIN_SEED(Kind.SEED),
    CARROT(Kind.CROP),
    POTATO(Kind.CROP),
    TOMATO(Kind.CROP),
    CORN(Kind.CROP),
    PUMPKIN(Kind.CROP),
    EGG(Kind.ANIMAL_PRODUCT),
    MILK(Kind.ANIMAL_PRODUCT),
    HAY(Kind.SUPPLY),
    MEDICINE(Kind.SUPPLY);

    /** Broad category of an item. */
    public enum Kind { SEED, CROP, ANIMAL_PRODUCT, SUPPLY }

    private final Kind kind;

    ItemType(Kind kind) {
        this.kind = kind;
    }

    /** Category of this item. */
    public Kind kind() {
        return kind;
    }
}
