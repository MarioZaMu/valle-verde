package edu.ucc.farmsim.domain.crop;

/**
 * Pattern: Factory Method — Role: ConcreteCreator (tomato).
 *
 * <p>Stories: HU-01.
 */
final class TomatoFactory extends CropFactory {

    static final TomatoFactory INSTANCE = new TomatoFactory();

    private TomatoFactory() {
        // shared instance
    }

    @Override
    public Crop createCrop() {
        return new Crop(CropType.TOMATO);
    }
}
