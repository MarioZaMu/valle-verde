package edu.ucc.farmsim.domain.crop;

/**
 * Pattern: State — Role: ConcreteState (seed).
 *
 * <p>Stories: HU-01, HU-03.
 */
final class SeedState extends GrowingPhaseState {

    static final SeedState INSTANCE = new SeedState();

    private SeedState() {
        // shared instance
    }

    @Override
    public CropStage stage() {
        return CropStage.SEED;
    }
}
