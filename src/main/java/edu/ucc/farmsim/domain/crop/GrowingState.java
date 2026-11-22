package edu.ucc.farmsim.domain.crop;

/**
 * Pattern: State — Role: ConcreteState (growing).
 *
 * <p>Stories: HU-01, HU-03.
 */
final class GrowingState extends GrowingPhaseState {

    static final GrowingState INSTANCE = new GrowingState();

    private GrowingState() {
        // shared instance
    }

    @Override
    public CropStage stage() {
        return CropStage.GROWING;
    }
}
