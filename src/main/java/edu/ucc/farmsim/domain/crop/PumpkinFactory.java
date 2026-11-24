package edu.ucc.farmsim.domain.crop;

/**
 * Pattern: Factory Method — Role: ConcreteCreator (pumpkin).
 *
 * <p>Stories: HU-01.
 */
final class PumpkinFactory extends CropFactory {

    static final PumpkinFactory INSTANCE = new PumpkinFactory();

    private PumpkinFactory() {
                // instancia compartida
    }

    @Override
    public Crop createCrop() {
        return new Crop(CropType.PUMPKIN);
    }
}
