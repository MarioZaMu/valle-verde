package edu.ucc.farmsim.domain.crop;

/**
 * Comportamiento compartido por las tres etapas vivas que aún no maduran (Semilla, Brote y Crecimiento): necesitan agua
 * para crecer y no se pueden cosechar.
 *
 * <p>Stories: HU-03, HU-05.
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
    public void onDayEnd(Crop crop, int dryDaysToWither) {
        if (crop.isWatered()) {
            crop.growOneDay();
        } else {
            crop.missWater();
            if (crop.daysWithoutWater() >= dryDaysToWither) {
                crop.wither();
            }
        }
    }
}
