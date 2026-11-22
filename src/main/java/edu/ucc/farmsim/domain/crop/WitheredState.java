package edu.ucc.farmsim.domain.crop;

/**
 * Pattern: State — Role: ConcreteState (withered).
 *
 * <p>Stories: HU-01.
 */
final class WitheredState implements CropState {

    static final WitheredState INSTANCE = new WitheredState();

    private WitheredState() {
        // shared instance
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
}
