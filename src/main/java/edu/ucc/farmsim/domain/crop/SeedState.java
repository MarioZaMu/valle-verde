package edu.ucc.farmsim.domain.crop;

/**
 * Pattern: State — Role: ConcreteState (seed).
 *
 * <p>Stories: HU-01.
 */
final class SeedState implements CropState {

    static final SeedState INSTANCE = new SeedState();

    private SeedState() {
        // shared instance
    }

    @Override
    public CropStage stage() {
        return CropStage.SEED;
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
