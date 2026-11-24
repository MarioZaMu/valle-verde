package edu.ucc.farmsim.domain.game;

import edu.ucc.farmsim.domain.animal.Animal;
import edu.ucc.farmsim.domain.animal.AnimalCondition;
import edu.ucc.farmsim.domain.building.Building;

/**
 * Paso 3 del final del día: cada animal come o pasa hambre, puede enfermarse y, si está sano y alimentado, produce
 * (RN-03, RN-25, RN-26).
 *
 * <p>Stories: HU-21, HU-22.
 */
final class AnimalsStep implements DayStep {

    @Override
    public DayPhase phase() {
        return DayPhase.ANIMALS;
    }

    @Override
    public void apply(GameState state, DayReport report) {
        for (Building building : state.farm().buildings()) {
            for (Animal animal : building.animals()) {
                AnimalCondition before = animal.condition();
                int pendingBefore = animal.pending();
                animal.endOfDay();
                if (before != AnimalCondition.SICK && animal.condition() == AnimalCondition.SICK) {
                    report.addSickAnimal();
                }
                report.addProducts(animal.pending() - pendingBefore);
            }
        }
    }
}
