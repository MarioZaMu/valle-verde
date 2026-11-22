package edu.ucc.farmsim.domain.crop;

/**
 * Pattern: State — Role: ConcreteState (growing).
 *
 * <p>Stories: HU-01.
 */
final class GrowingState implements CropState {

    static final GrowingState INSTANCE = new GrowingState();

    private GrowingState() {
        // shared instance
    }

    @Override
    public CropStage stage() {
        return CropStage.GROWING;
    }

    @Override
    public boolean isAlive() {
        return true;
    }

    @Override
    public boolean canHarvest() {
        return false;
    }
}
