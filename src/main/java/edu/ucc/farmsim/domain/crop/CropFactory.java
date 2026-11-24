package edu.ucc.farmsim.domain.crop;

/**
 * Pattern: Factory Method — Role: Creator (declares the factory method that builds a crop).
 *
 * <p>Sembrar solo conoce esta clase: cada creador concreto decide qué tipo de cultivo construir, de modo que un cultivo
 * nuevo no obliga a cambiar el código que siembra.
 *
 * <p>Stories: HU-01.
 */
public abstract class CropFactory {

    /** El método fábrica: construye un cultivo nuevo en la etapa Semilla. */
    public abstract Crop createCrop();

    /** Devuelve el creador de un tipo de cultivo. */
    public static CropFactory forType(CropType type) {
        return switch (type) {
            case CARROT -> CarrotFactory.INSTANCE;
            case POTATO -> PotatoFactory.INSTANCE;
            case TOMATO -> TomatoFactory.INSTANCE;
            case CORN -> CornFactory.INSTANCE;
            case PUMPKIN -> PumpkinFactory.INSTANCE;
        };
    }
}
