package edu.ucc.farmsim.domain.economy;

import edu.ucc.farmsim.domain.crop.CropType;

/**
 * Todo objeto que se puede guardar en el inventario (RN-15): 5 semillas, 5 cultivos, huevo, leche, heno y medicina.
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

    /** Categoría general de un objeto. */
    public enum Kind { SEED, CROP, ANIMAL_PRODUCT, SUPPLY }

    private final Kind kind;
    private final CropType cropType;

    ItemType(Kind kind, CropType cropType) {
        this.kind = kind;
        this.cropType = cropType;
    }

    /** Categoría de este objeto. */
    public Kind kind() {
        return kind;
    }

    /**
     * El cultivo del que este objeto es la semilla o la cosecha, o {@code null} para los objetos sin relación con
     * cultivos.
     */
    public CropType cropType() {
        return cropType;
    }

    /** El objeto semilla de un cultivo. */
    public static ItemType seedOf(CropType type) {
        return find(Kind.SEED, type);
    }

    /** El objeto cosecha de un cultivo. */
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

