package edu.ucc.farmsim.domain.crop;

/**
 * Pattern: State — Role: ConcreteState (sprout).
 *
 * <p>Stories: HU-01.
 */
final class SproutState implements CropState {

    static final SproutState INSTANCE = new SproutState();

    private SproutState() {
        // shared instance
    }

    @Override
    public CropStage stage() {
        return CropStage.SPROUT;
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
