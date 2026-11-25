package edu.ucc.farmsim.dominio.juego;

import edu.ucc.farmsim.dominio.animal.Animal;
import edu.ucc.farmsim.dominio.animal.CondicionAnimal;
import edu.ucc.farmsim.dominio.edificio.Edificio;

/**
 * Paso 3 del final del día: cada animal come o pasa hambre, puede enfermarse y, si está sano y alimentado, produce
 * (RN-03, RN-25, RN-26).
 *
 * <p>Stories: HU-21, HU-22.
 */
final class PasoAnimales implements PasoDia {

    @Override
    public FaseDia fase() {
        return FaseDia.ANIMALS;
    }

    @Override
    public void apply(EstadoJuego estado, ReporteDia reporte) {
        for (Edificio edificio : estado.granja().edificios()) {
            for (Animal animal : edificio.animales()) {
                CondicionAnimal antes = animal.condicion();
                int antesPendiente = animal.pendiente();
                animal.terminarDeDia();
                if (antes != CondicionAnimal.SICK && animal.condicion() == CondicionAnimal.SICK) {
                    reporte.animalAgregarEnfermo();
                }
                reporte.productosAgregar(animal.pendiente() - antesPendiente);
            }
        }
    }
}
