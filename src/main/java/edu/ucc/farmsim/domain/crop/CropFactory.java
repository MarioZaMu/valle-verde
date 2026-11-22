package edu.ucc.farmsim.domain.crop;

/**
 * Pattern: Factory Method — Role: Creator (declares the factory method that builds a crop).
 *
 * <p>Sowing only knows this class: each concrete creator decides which kind of crop to build, so a new crop
 * does not force changes in the code that sows.
 *
 * <p>Stories: HU-01.
 */
public abstract class CropFactory {

    /** The factory method: builds a new crop in the Seed stage. */
    public abstract Crop createCrop();

    /** Returns the creator of a kind of crop. */
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
