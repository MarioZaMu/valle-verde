package edu.ucc.farmsim.domain.economy;

import edu.ucc.farmsim.domain.crop.CropType;

/**
 * Every item that can be stored in the inventory (RN-15): 5 seeds, 5 crops, egg, milk, hay and medicine.
 *
 * <p>Stories: HU-01, HU-13.
 */
public enum ItemType {
    CARROT_SEED(Kind.SEED, CropType.CARROT),
    POTATO_SEED(Kind.SEED, CropType.POTATO),
    TOMATO_SEED(Kind.SEED, CropType.TOMATO),
    CORN_SEED(Kind.SEED, CropType.CORN),
    PUMPKIN_SEED(Kind.SEED, CropType.PUMPKIN),
    CARROT(Kind.CROP, CropType.CARROT),
    POTATO(Kind.CROP, CropType.POTATO),
    TOMATO(Kind.CROP, CropType.TOMATO),
    CORN(Kind.CROP, CropType.CORN),
    PUMPKIN(Kind.CROP, CropType.PUMPKIN),
    EGG(Kind.ANIMAL_PRODUCT, null),
    MILK(Kind.ANIMAL_PRODUCT, null),
    HAY(Kind.SUPPLY, null),
    MEDICINE(Kind.SUPPLY, null);

    /** Broad category of an item. */
    public enum Kind { SEED, CROP, ANIMAL_PRODUCT, SUPPLY }

    private final Kind kind;
    private final CropType cropType;

    ItemType(Kind kind, CropType cropType) {
        this.kind = kind;
        this.cropType = cropType;
    }

    /** Category of this item. */
    public Kind kind() {
        return kind;
    }

    /** The crop this item is the seed or the harvest of, or {@code null} for items unrelated to crops. */
    public CropType cropType() {
        return cropType;
    }

    /** The seed item of a crop. */
    public static ItemType seedOf(CropType type) {
        return find(Kind.SEED, type);
    }

    /** The harvest item of a crop. */
    public static ItemType produceOf(CropType type) {
        return find(Kind.CROP, type);
    }

    private static ItemType find(Kind kind, CropType type) {
        for (ItemType item : values()) {
            if (item.kind == kind && item.cropType == type) {
                return item;
            }
        }
        throw new IllegalStateException("No " + kind + " item for " + type);
    }
}

