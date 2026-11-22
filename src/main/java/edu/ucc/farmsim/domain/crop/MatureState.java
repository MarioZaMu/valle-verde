package edu.ucc.farmsim.domain.crop;

/**
 * Pattern: State — Role: ConcreteState (mature).
 *
 * <p>Stories: HU-01, HU-03.
 */
final class MatureState implements CropState {

    static final MatureState INSTANCE = new MatureState();

    private MatureState() {
        // shared instance
    }

    @Override
    public CropStage stage() {
        return CropStage.MATURE;
    }

    @Override
    public boolean isAlive() {
        return true;
    }

    @Override
    public boolean canHarvest() {
        return true;
    }

    @Override
    public void onDayEnd(Crop crop, int dryDaysToWither) {
        // a mature crop neither grows nor needs water
    }
}
