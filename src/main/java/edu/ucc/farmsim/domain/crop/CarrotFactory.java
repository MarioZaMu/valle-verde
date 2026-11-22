package edu.ucc.farmsim.domain.crop;

/**
 * Pattern: Factory Method — Role: ConcreteCreator (carrot).
 *
 * <p>Stories: HU-01.
 */
final class CarrotFactory extends CropFactory {

    static final CarrotFactory INSTANCE = new CarrotFactory();

    private CarrotFactory() {
        // shared instance
    }

    @Override
    public Crop createCrop() {
        return new Crop(CropType.CARROT);
    }
}
