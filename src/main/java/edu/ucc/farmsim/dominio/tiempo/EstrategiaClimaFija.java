package edu.ucc.farmsim.dominio.tiempo;

import java.util.Random;

/**
 * Pattern: Strategy — Role: ConcreteStrategy (always the same weather).
 *
 * <p>Se usa para el primer día fijo de una partida (RN-13) y en las pruebas que necesitan un clima conocido.
 *
 * <p>Stories: HU-11.
 */
public final class EstrategiaClimaFija implements EstrategiaClima {

    private final Clima clima;

    /**
     * Crea una estrategia que siempre devuelve el mismo clima.
     *
     * @param clima el clima que se devuelve
     */
    public EstrategiaClimaFija(Clima clima) {
        this.clima = clima;
    }

    @Override
    public Clima climaSiguiente(int dia, Random rnd) {
        return clima;
    }
}
