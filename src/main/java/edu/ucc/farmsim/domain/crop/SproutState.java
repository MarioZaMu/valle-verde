package edu.ucc.farmsim.domain.crop;

/**
 * Pattern: State — Role: ConcreteState (sprout).
 *
 * <p>Stories: HU-01, HU-03.
 */
final class SproutState extends GrowingPhaseState {

    static final SproutState INSTANCE = new SproutState();

    private SproutState() {
                // instancia compartida
    }

    @Override
    public CropStage stage() {
        return CropStage.SPROUT;
    }
}
