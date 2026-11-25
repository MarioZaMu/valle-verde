package edu.ucc.farmsim.dominio.tiempo;

import java.util.Random;

/**
 * Pattern: Strategy — Role: Strategy interface (how the weather of a day is drawn).
 *
 * <p>Stories: HU-11.
 */
public interface EstrategiaClima {

    /**
     * Sortea el clima de un día.
     *
     * @param dia día de la estación (1 a 28) para el que se sortea el clima
     * @param rnd generador aleatorio inyectado
     */
    Clima climaSiguiente(int dia, Random rnd);
}
