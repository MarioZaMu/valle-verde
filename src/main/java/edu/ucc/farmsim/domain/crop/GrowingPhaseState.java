package edu.ucc.farmsim.domain.crop;

/**
 * Behaviour shared by the three living, not yet mature stages (Seed, Sprout and Growing): they need water to
 * grow, and cannot be harvested.
 *
 * <p>Stories: HU-03.
 */
abstract class GrowingPhaseState implements CropState {

    @Override
    public final boolean isAlive() {
        return true;
    }

    @Override
    public final boolean canHarvest() {
        return false;
    }

    @Override
    public void onDayEnd(Crop crop) {
        if (crop.isWatered()) {
            crop.growOneDay();
        } else {
            crop.missWater();
        }
    }
}
