package edu.ucc.farmsim.domain.crop;

/**
 * Pattern: Factory Method — Role: ConcreteCreator (potato).
 *
 * <p>Stories: HU-01.
 */
final class PotatoFactory extends CropFactory {

    static final PotatoFactory INSTANCE = new PotatoFactory();

    private PotatoFactory() {
        // shared instance
    }

    @Override
    public Crop createCrop() {
        return new Crop(CropType.POTATO);
    }
}
