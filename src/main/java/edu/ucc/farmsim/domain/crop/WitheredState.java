package edu.ucc.farmsim.domain.crop;

/**
 * Pattern: State — Role: ConcreteState (withered).
 *
 * <p>Stories: HU-01, HU-03.
 */
final class WitheredState implements CropState {

    static final WitheredState INSTANCE = new WitheredState();

    private WitheredState() {
                // instancia compartida
    }

    @Override
    public CropStage stage() {
        return CropStage.WITHERED;
    }

    @Override
    public boolean isAlive() {
        return false;
    }

    @Override
    public boolean canHarvest() {
        return false;
    }

    @Override
    public void onDayEnd(Crop crop, int dryDaysToWither) {
                // un cultivo marchito ni crece ni necesita agua
    }
}
