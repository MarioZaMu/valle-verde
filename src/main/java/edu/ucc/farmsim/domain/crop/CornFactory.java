package edu.ucc.farmsim.domain.crop;

/**
 * Pattern: Factory Method — Role: ConcreteCreator (corn).
 *
 * <p>Stories: HU-01.
 */
final class CornFactory extends CropFactory {

    static final CornFactory INSTANCE = new CornFactory();

    private CornFactory() {
                // instancia compartida
    }

    @Override
    public Crop createCrop() {
        return new Crop(CropType.CORN);
    }
}
